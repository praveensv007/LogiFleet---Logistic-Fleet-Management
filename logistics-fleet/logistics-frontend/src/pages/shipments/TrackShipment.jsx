import { useCallback, useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Container, Form, InputGroup, ProgressBar, Row, Spinner } from 'react-bootstrap';
import { Link, useNavigate, useParams } from 'react-router-dom';
import StatusBadge from '../../components/common/StatusBadge';
import StatusTimeline from '../../components/StatusTimeline';
import useAuth from '../../hooks/useAuth';
import useWebSocket from '../../hooks/useWebSocket';
import shipmentService from '../../services/shipmentService';
import { errorMessage, fileUrl, formatDate, formatDateTime, formatKg, pretty, timeAgo } from '../../utils/formatters';

const STEPS = ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED', 'DISPATCHED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED'];

const mapUrl = (lat, lng) => {
  const d = 0.02;
  return `https://www.openstreetmap.org/export/embed.html?bbox=${lng - d}%2C${lat - d}%2C${lng + d}%2C${lat + d}&layer=mapnik&marker=${lat}%2C${lng}`;
};

/** Public tracking page: works without login, updates live over WebSocket. */
export default function TrackShipment() {
  const { trackingNumber } = useParams();
  const navigate = useNavigate();
  const { user, homePath } = useAuth();
  const [input, setInput] = useState(trackingNumber || '');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [pulse, setPulse] = useState(null);

  const load = useCallback(async (tn, silent = false) => {
    if (!tn) return;
    if (!silent) setLoading(true);
    setError(null);
    try {
      setData(await shipmentService.track(tn));
    } catch (e) {
      setData(null);
      setError(errorMessage(e, 'Shipment not found'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (trackingNumber) {
      setInput(trackingNumber);
      load(trackingNumber);
    }
  }, [trackingNumber, load]);

  useWebSocket(data ? `/topic/shipments/${data.trackingNumber}` : null, (msg) => {
    setPulse(`Status changed to ${pretty(msg.status)}`);
    load(data.trackingNumber, true);
  });
  useWebSocket(data?.deliveryId ? `/topic/deliveries/${data.deliveryId}/location` : null, (msg) => {
    if (msg.latitude && msg.longitude) {
      setData((d) => ({ ...d, currentLatitude: msg.latitude, currentLongitude: msg.longitude, lastLocationUpdate: msg.timestamp }));
      setPulse('Driver location updated');
    }
  });

  const submit = (e) => {
    e.preventDefault();
    const tn = input.trim().toUpperCase();
    if (tn) navigate(`/track/${tn}`);
  };

  const stepIndex = data ? STEPS.indexOf(data.currentStatus) : -1;
  const progress = data?.currentStatus === 'CANCELLED' ? 100 : Math.round(((stepIndex + 1) / STEPS.length) * 100);

  return (
    <div style={{ minHeight: '100vh', background: 'var(--lf-bg)' }}>
      <div style={{ background: 'var(--lf-navy)' }} className="text-white py-4 mb-4">
        <Container>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <Link to="/" className="text-white text-decoration-none fw-bold fs-5"><i className="bi bi-truck me-2" />LogiFleet</Link>
            {user ? <Link to={homePath} className="btn btn-sm btn-outline-light">My dashboard</Link>
              : <Link to="/login" className="btn btn-sm btn-outline-light">Log in</Link>}
          </div>
          <h2 className="fw-bold">Track your shipment</h2>
          <Form onSubmit={submit} className="mt-3" style={{ maxWidth: 560 }}>
            <InputGroup size="lg">
              <Form.Control value={input} onChange={(e) => setInput(e.target.value)} placeholder="LGX-20261012-00057" className="mono" />
              <Button type="submit" className="btn-accent" disabled={loading}>{loading ? <Spinner size="sm" /> : 'Track'}</Button>
            </InputGroup>
          </Form>
        </Container>
      </div>
      <Container className="pb-5">
        {error && <Alert variant="danger">{error}</Alert>}
        {pulse && <Alert variant="info" dismissible onClose={() => setPulse(null)}><i className="bi bi-broadcast me-2" />{pulse}</Alert>}
        {data && (
          <Row className="g-3">
            <Col lg={8}>
              <Card className="mb-3">
                <Card.Body>
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <div className="text-muted small">Tracking number</div>
                      <div className="mono fs-5 fw-bold">{data.trackingNumber}</div>
                    </div>
                    <div className="text-end">
                      <StatusBadge status={data.currentStatus} className="fs-6" />
                      {data.delayed && data.currentStatus !== 'DELIVERED' && <Badge bg="danger" className="ms-1">Delayed</Badge>}
                    </div>
                  </div>
                  <ProgressBar now={progress} variant={data.currentStatus === 'CANCELLED' ? 'secondary' : 'warning'} className="my-3" style={{ height: 8 }} />
                  <Row className="small">
                    <Col sm={4} className="mb-2"><div className="text-muted">From</div><strong>{data.originCity}</strong>, {data.originState}</Col>
                    <Col sm={4} className="mb-2"><div className="text-muted">To</div><strong>{data.destinationCity}</strong>, {data.destinationState}</Col>
                    <Col sm={4} className="mb-2"><div className="text-muted">{data.actualDeliveryDate ? 'Delivered' : 'Estimated delivery'}</div>
                      <strong>{data.actualDeliveryDate ? formatDateTime(data.actualDeliveryDate) : formatDate(data.estimatedDeliveryDate)}</strong></Col>
                    <Col sm={4} className="mb-2"><div className="text-muted">Service</div>{pretty(data.serviceType)}</Col>
                    <Col sm={4} className="mb-2"><div className="text-muted">Packages</div>{data.totalPackages} · {formatKg(data.totalWeightKg)}</Col>
                    <Col sm={4} className="mb-2"><div className="text-muted">Receiver</div>{data.receiverName}</Col>
                  </Row>
                </Card.Body>
              </Card>
              {data.currentLatitude && data.currentLongitude && data.currentStatus !== 'DELIVERED' && (
                <Card className="mb-3">
                  <Card.Header className="d-flex justify-content-between">
                    <span><i className="bi bi-geo-alt-fill text-danger me-1" />Live location</span>
                    <span className="small text-muted">updated {timeAgo(data.lastLocationUpdate)}</span>
                  </Card.Header>
                  <Card.Body className="p-2">
                    <iframe title="map" className="map-frame" src={mapUrl(Number(data.currentLatitude), Number(data.currentLongitude))} />
                  </Card.Body>
                </Card>
              )}
              {data.currentStatus === 'DELIVERED' && (
                <Card className="mb-3 border-success">
                  <Card.Header className="text-success"><i className="bi bi-patch-check-fill me-1" />Proof of delivery</Card.Header>
                  <Card.Body>
                    <div className="mb-2">Received by <strong>{data.podReceiverName}</strong> on {formatDateTime(data.deliveredTime)} (OTP verified)</div>
                    <div className="d-flex gap-3 flex-wrap">
                      {data.signatureUrl && <div><div className="small text-muted">Signature</div><img src={fileUrl(data.signatureUrl)} alt="signature" style={{ maxHeight: 120, border: '1px solid #eee', borderRadius: 8 }} /></div>}
                      {data.podPhotoUrl && <div><div className="small text-muted">Photo</div><img src={fileUrl(data.podPhotoUrl)} alt="proof" style={{ maxHeight: 160, borderRadius: 8 }} /></div>}
                    </div>
                  </Card.Body>
                </Card>
              )}
            </Col>
            <Col lg={4}>
              {data.driverName && (
                <Card className="mb-3">
                  <Card.Header>Your driver</Card.Header>
                  <Card.Body>
                    <div className="fw-semibold">{data.driverName}</div>
                    <div><a href={`tel:${data.driverPhone}`}>{data.driverPhone}</a></div>
                    <div className="text-muted small">Vehicle {data.vehicleNumber}</div>
                  </Card.Body>
                </Card>
              )}
              <Card>
                <Card.Header>Status timeline</Card.Header>
                <Card.Body><StatusTimeline items={data.timeline} /></Card.Body>
              </Card>
            </Col>
          </Row>
        )}
        {!data && !error && !loading && (
          <div className="text-center text-muted py-5"><i className="bi bi-search fs-1 d-block mb-2" />Enter a tracking number to see live status.</div>
        )}
      </Container>
    </div>
  );
}
