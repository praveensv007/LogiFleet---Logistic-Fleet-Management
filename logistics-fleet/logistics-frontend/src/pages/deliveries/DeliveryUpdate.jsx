import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Row, Table } from 'react-bootstrap';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import FormBuilder from '../../components/common/FormBuilder';
import StatusTimeline from '../../components/StatusTimeline';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import useWebSocket from '../../hooks/useWebSocket';
import deliveryService from '../../services/deliveryService';
import { getPosition, mapsLink, round } from '../../utils/geo';
import { clean, errorMessage, fileUrl, formatDateTime, formatMoney, pretty, timeAgo } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const NEXT = { null: 'PICKED_UP', PICKED_UP: 'IN_TRANSIT', IN_TRANSIT: 'OUT_FOR_DELIVERY' };
const LABEL = { PICKED_UP: 'Mark picked up', IN_TRANSIT: 'Mark in transit', OUT_FOR_DELIVERY: 'Mark out for delivery' };

const AddressBlock = ({ title, a }) => a && (
  <Col md={6} className="mb-3">
    <div className="small text-muted text-uppercase fw-semibold">{title}</div>
    <div className="fw-semibold">{a.contactName} · <a href={`tel:${a.contactPhone}`}>{a.contactPhone}</a></div>
    <div>{a.addressLine1}{a.addressLine2 && `, ${a.addressLine2}`}{a.landmark && ` (near ${a.landmark})`}</div>
    <div>{a.city}, {a.state} - {a.pincode}</div>
    <a href={mapsLink(a)} target="_blank" rel="noreferrer" className="small"><i className="bi bi-map me-1" />Open in maps</a>
  </Col>
);

export default function DeliveryUpdate() {
  const { id } = useParams();
  const { user } = useAuth();
  const isDriver = user.role === 'DRIVER';
  const { data: d, loading, error, reload } = useFetch(() => deliveryService.get(id), [id]);
  const [sharing, setSharing] = useState(false);
  const watchId = useRef(null);
  const lastSent = useRef(0);

  useWebSocket(`/topic/deliveries/${id}/location`, () => reload(true));
  useWebSocket(!isDriver ? '/topic/fleet/active-deliveries' : null, (m) => { if (String(m.deliveryId) === String(id)) reload(true); });

  // Live location sharing: send at most every 15 s while the page is open.
  useEffect(() => {
    if (!sharing) return undefined;
    if (!navigator.geolocation) {
      toast.error('Location is not available in this browser');
      setSharing(false);
      return undefined;
    }
    watchId.current = navigator.geolocation.watchPosition(async (p) => {
      if (Date.now() - lastSent.current < 15000) return;
      lastSent.current = Date.now();
      try {
        await deliveryService.sendLocation(id, { latitude: round(p.coords.latitude), longitude: round(p.coords.longitude) });
      } catch (e) {
        toast.error(errorMessage(e));
        setSharing(false);
      }
    }, (e) => { toast.error(e.message); setSharing(false); }, { enableHighAccuracy: true });
    return () => navigator.geolocation.clearWatch(watchId.current);
  }, [sharing, id]);

  const next = d ? NEXT[d.currentStatus ?? null] : null;
  const schema = useMemo(() => yup.object({
    locationText: v.opt(150),
    latitude: v.num('Latitude').nullable(),
    longitude: v.num('Longitude').nullable(),
    remarks: v.opt(500),
    packagesPickedCount: next === 'PICKED_UP' ? v.num('Packages picked').required('Packages picked is required').integer().min(0) : yup.mixed().strip(),
    pickupPhotoUrl: yup.string().nullable(),
  }), [next]);

  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;

  const active = ['ACCEPTED', 'IN_PROGRESS'].includes(d.assignmentStatus);
  const waitingDispatch = next === 'PICKED_UP' && d.shipmentStatus !== 'DISPATCHED';

  return (
    <>
      <PageHeader title={<>Trip <span className="mono">{d.assignmentCode}</span></>}
        subtitle={<><Link to={`/shipments/${d.shipmentId}`} className="mono">{d.trackingNumber}</Link> · {d.driverName} · {d.vehicleNumber}</>}>
        <span className="align-self-center">
          <StatusBadge status={d.currentStatus} className="fs-6" />
          {d.delayed && <Badge bg="danger" className="ms-1 fs-6">Delayed {d.delayMinutes} min</Badge>}
        </span>
        {isDriver && active && (
          <>
            <Button variant={sharing ? 'success' : 'outline-success'} onClick={() => setSharing((s) => !s)}>
              <i className={`bi bi-broadcast${sharing ? '-pin' : ''} me-1`} />{sharing ? 'Sharing location' : 'Share live location'}
            </Button>
            <Button variant="outline-danger" as={Link} to={`/deliveries/${id}/problem`}><i className="bi bi-exclamation-triangle me-1" />Report problem</Button>
          </>
        )}
      </PageHeader>
      <Row className="g-3">
        <Col lg={8}>
          <Card className="mb-3">
            <Card.Body>
              <Row>
                <AddressBlock title="Pickup" a={d.pickupAddress} />
                <AddressBlock title="Deliver to" a={d.deliveryAddress} />
              </Row>
              <Row className="small">
                <Col sm={4}><span className="text-muted">Packages:</span> {d.totalPackages}</Col>
                <Col sm={4}><span className="text-muted">Expected by:</span> {formatDateTime(d.expectedDeliveryTime)}</Col>
                <Col sm={4}><span className="text-muted">Payment:</span> {pretty(d.paymentMode)}{d.paymentMode === 'COD' && <strong className="text-danger"> · collect {formatMoney(d.codAmountDue)}</strong>}</Col>
                {d.lastLocationUpdate && <Col xs={12} className="mt-2 text-muted">Last location {timeAgo(d.lastLocationUpdate)} ({d.currentLatitude}, {d.currentLongitude})</Col>}
              </Row>
            </Card.Body>
          </Card>

          {isDriver && active && next && (
            <Card className="mb-3 border-primary">
              <Card.Header>Update status → {pretty(next)}</Card.Header>
              <Card.Body>
                {waitingDispatch ? (
                  <Alert variant="info" className="mb-0"><i className="bi bi-hourglass-split me-2" />Waiting for the warehouse to load and dispatch the shipment (now {pretty(d.shipmentStatus)}).</Alert>
                ) : (
                  <FormBuilder key={next} schema={schema} submitLabel={LABEL[next]}
                    defaultValues={{ locationText: '', latitude: '', longitude: '', remarks: '', packagesPickedCount: d.totalPackages, pickupPhotoUrl: '' }}
                    fields={[
                      { name: 'locationText', label: 'Current location', placeholder: 'e.g. Guindy, Chennai', col: 6 },
                      { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', col: 3 },
                      { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', col: 3 },
                      ...(next === 'PICKED_UP' ? [
                        { name: 'packagesPickedCount', label: 'Packages picked', type: 'number', required: true, col: 6, help: `Expected ${d.totalPackages}` },
                        { name: 'pickupPhotoUrl', label: 'Pickup photo (optional)', type: 'file', category: 'photo', col: 6 },
                      ] : []),
                      { name: 'remarks', label: 'Remarks', type: 'textarea', rows: 2, col: 12 },
                    ]}
                    footerExtra={null}
                    onSubmit={async (values) => {
                      await deliveryService.updateStatus(id, clean({ ...values, status: next }));
                      toast.success(`Status: ${pretty(next)}`);
                      reload(true);
                    }}>
                    {(form) => (
                      <Button size="sm" variant="outline-secondary" className="mb-3" onClick={async () => {
                        try {
                          const p = await getPosition();
                          form.setValue('latitude', p.latitude);
                          form.setValue('longitude', p.longitude);
                          toast.info('GPS location filled in');
                        } catch (e) {
                          toast.error(e.message);
                        }
                      }}><i className="bi bi-crosshair me-1" />Use my GPS location</Button>
                    )}
                  </FormBuilder>
                )}
              </Card.Body>
            </Card>
          )}

          {isDriver && d.currentStatus === 'OUT_FOR_DELIVERY' && (
            <Card className="mb-3 border-success">
              <Card.Body className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <div><strong>At the door?</strong> Verify the receiver's OTP and capture proof of delivery.</div>
                <div className="d-flex gap-2">
                  <Button variant="outline-secondary" onClick={async () => {
                    await deliveryService.updateStatus(id, { status: 'IN_TRANSIT', remarks: 'Delivery attempt failed, back in transit' })
                      .then(() => { toast.info('Back to in transit'); reload(true); }).catch((e) => toast.error(errorMessage(e)));
                  }}>Receiver not available</Button>
                  <Button variant="success" as={Link} to={`/deliveries/${id}/complete`}><i className="bi bi-patch-check me-1" />Complete delivery</Button>
                </div>
              </Card.Body>
            </Card>
          )}

          {d.currentStatus === 'DELIVERED' && (
            <Card className="mb-3 border-success">
              <Card.Header className="text-success"><i className="bi bi-patch-check-fill me-1" />Delivered {formatDateTime(d.deliveredTime)}</Card.Header>
              <Card.Body className="small">
                <div>Received by <strong>{d.receiverName}</strong> ({d.receiverPhone}) · OTP {d.otpVerified ? 'verified' : 'not verified'}</div>
                {d.codAmountCollected && <div>COD collected: {formatMoney(d.codAmountCollected)}</div>}
                <div className="d-flex gap-3 mt-2">
                  {d.signatureUrl && <img src={fileUrl(d.signatureUrl)} alt="signature" style={{ maxHeight: 100, border: '1px solid #eee' }} />}
                  {d.podPhotoUrl && <img src={fileUrl(d.podPhotoUrl)} alt="pod" style={{ maxHeight: 140 }} />}
                </div>
              </Card.Body>
            </Card>
          )}

          {d.problems?.length > 0 && (
            <Card>
              <Card.Header>Reported problems</Card.Header>
              <Table size="sm" className="mb-0">
                <thead><tr><th>Time</th><th>Type</th><th>Description</th><th>Status</th><th>Resolution</th></tr></thead>
                <tbody>
                  {d.problems.map((p) => (
                    <tr key={p.id}>
                      <td>{formatDateTime(p.reportedAt)}</td><td>{pretty(p.problemType)}</td>
                      <td>{p.description}{p.photoUrl && <> · <a href={fileUrl(p.photoUrl)} target="_blank" rel="noreferrer">photo</a></>}</td>
                      <td><StatusBadge status={p.resolutionStatus} /></td><td className="small">{p.resolutionNotes}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card>
          )}
        </Col>
        <Col lg={4}>
          <Card>
            <Card.Header>Delivery history</Card.Header>
            <Card.Body>
              <StatusTimeline items={(d.history || []).map((h, i) => ({ id: i, status: h.status, time: h.time, location: h.locationText, remarks: h.remarks }))} />
              {d.pickupPhotoUrl && <div className="mt-3"><div className="small text-muted">Pickup photo</div><img src={fileUrl(d.pickupPhotoUrl)} alt="pickup" className="img-fluid rounded" /></div>}
            </Card.Body>
          </Card>
          {!isDriver && (
            <Form.Text className="d-block mt-2">Live view: this page refreshes when the driver updates the trip.</Form.Text>
          )}
        </Col>
      </Row>
    </>
  );
}
