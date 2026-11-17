import { useState } from 'react';
import { Button, Card, Col, Row, Table } from 'react-bootstrap';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusTimeline from '../../components/StatusTimeline';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import useWebSocket from '../../hooks/useWebSocket';
import shipmentService from '../../services/shipmentService';
import warehouseService from '../../services/warehouseService';
import { errorMessage, formatDate, formatDateTime, formatKg, formatMoney, pretty } from '../../utils/formatters';

const CANCELLABLE = ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED', 'DISPATCHED'];

const Address = ({ title, a }) => (
  <Col md={6} className="mb-3">
    <div className="small text-muted text-uppercase fw-semibold mb-1">{title}</div>
    {a ? (
      <>
        <div className="fw-semibold">{a.contactName} · {a.contactPhone}</div>
        {a.contactEmail && <div className="small">{a.contactEmail}</div>}
        <div>{a.addressLine1}{a.addressLine2 && `, ${a.addressLine2}`}</div>
        {a.landmark && <div className="small text-muted">Near {a.landmark}</div>}
        <div>{a.city}, {a.state} - {a.pincode}</div>
      </>
    ) : '—'}
  </Col>
);

export default function ShipmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, can } = useAuth();
  const shipment = useFetch(() => shipmentService.get(id), [id]);
  const history = useFetch(() => shipmentService.history(id), [id], { initial: [] });
  const staff = ['ADMIN', 'WAREHOUSE_STAFF', 'FLEET_MANAGER'].includes(user.role);
  const txns = useFetch(() => (can('WAREHOUSE_OPS') ? warehouseService.transactions(id) : Promise.resolve([])), [id], { initial: [] });
  const [cancelOpen, setCancelOpen] = useState(false);

  const s = shipment.data;
  useWebSocket(s ? `/topic/shipments/${s.trackingNumber}` : null, () => {
    shipment.reload(true);
    history.reload(true);
    txns.reload(true);
  });

  if (shipment.loading) return <Loader />;
  if (shipment.error) return <ErrorAlert error={shipment.error} onRetry={shipment.reload} />;

  const isOwner = user.role === 'CUSTOMER';
  const canEdit = (isOwner || user.role === 'ADMIN') && s.currentStatus === 'CREATED';
  const canCancel = (isOwner || can('SHIPMENT_EDIT')) && CANCELLABLE.includes(s.currentStatus);
  const a = s.assignment;

  const cancel = async (reason) => {
    try {
      await shipmentService.cancel(id, reason);
      toast.success('Shipment cancelled');
      setCancelOpen(false);
      shipment.reload(true);
      history.reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  return (
    <>
      <PageHeader title={<span className="mono">{s.trackingNumber}</span>}
        subtitle={`Booked ${formatDateTime(s.createdAt)}${staff ? ` by ${s.customerName} (${s.customerCode})` : ''}`}>
        <StatusBadge status={s.currentStatus} className="align-self-center fs-6" />
        <Button variant="outline-secondary" as={Link} to={`/track/${s.trackingNumber}`}><i className="bi bi-geo-alt me-1" />Live tracking</Button>
        {canEdit && <Button variant="outline-primary" as={Link} to={`/shipments/${id}/edit`}><i className="bi bi-pencil me-1" />Edit</Button>}
        {(canEdit || can('WAREHOUSE_OPS')) && <Button variant="outline-primary" as={Link} to={`/shipments/${id}/packages`}><i className="bi bi-box me-1" />Packages</Button>}
        {canCancel && <Button variant="outline-danger" onClick={() => setCancelOpen(true)}><i className="bi bi-x-circle me-1" />Cancel</Button>}
      </PageHeader>
      <Row className="g-3">
        <Col lg={8}>
          <Card className="mb-3">
            <Card.Body>
              <Row>
                <Address title="Pickup" a={s.pickup} />
                <Address title="Delivery" a={s.delivery} />
              </Row>
              <Row className="small">
                <Col sm={3} className="mb-2"><div className="text-muted">Service</div>{pretty(s.serviceType)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Pickup</div>{formatDate(s.pickupDate)} · {pretty(s.pickupTimeSlot)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Estimated delivery</div>{formatDate(s.estimatedDeliveryDate)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Delivered</div>{formatDateTime(s.actualDeliveryDate)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Declared value</div>{formatMoney(s.declaredValue)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Shipping charge</div>{formatMoney(s.shippingCharge)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Payment</div>{pretty(s.paymentMode)}</Col>
                <Col sm={3} className="mb-2"><div className="text-muted">Origin warehouse</div>{s.originWarehouseName}</Col>
                <Col xs={12} className="mb-2"><div className="text-muted">Description</div>{s.description}</Col>
                {s.specialInstructions && <Col xs={12} className="mb-2"><div className="text-muted">Special instructions</div>{s.specialInstructions}</Col>}
                {s.cancelReason && <Col xs={12} className="text-danger"><div className="text-muted">Cancel reason</div>{s.cancelReason}</Col>}
              </Row>
            </Card.Body>
          </Card>
          <Card className="mb-3">
            <Card.Header>Packages · {s.totalPackages} pcs · {formatKg(s.totalWeightKg)}</Card.Header>
            <div className="table-responsive">
              <Table size="sm" className="mb-0">
                <thead><tr><th>Code</th><th>Description</th><th>Type</th><th>Qty</th><th>Weight</th><th>Size (cm)</th><th>Vol. kg</th><th>Packing</th><th>Status</th></tr></thead>
                <tbody>
                  {s.packages?.map((p) => (
                    <tr key={p.id}>
                      <td className="mono">{p.packageCode}</td>
                      <td>{p.description}{p.fragile && <span className="badge text-bg-danger ms-1">Fragile</span>}</td>
                      <td>{pretty(p.packageType)}</td>
                      <td>{p.quantity}</td>
                      <td>{formatKg(p.weightKg)}</td>
                      <td>{p.lengthCm}×{p.widthCm}×{p.heightCm}</td>
                      <td>{p.volumetricWeightKg}</td>
                      <td>{pretty(p.packingType)}</td>
                      <td><StatusBadge status={p.currentStatus} /></td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          </Card>
          {txns.data.length > 0 && (
            <Card>
              <Card.Header>Warehouse transactions</Card.Header>
              <div className="table-responsive">
                <Table size="sm" className="mb-0">
                  <thead><tr><th>Time</th><th>Type</th><th>Count</th><th>Condition</th><th>Bin</th><th>Gate pass</th><th>By</th><th>Remarks</th></tr></thead>
                  <tbody>
                    {txns.data.map((t) => (
                      <tr key={t.id}>
                        <td>{formatDateTime(t.transactionTime)}</td><td>{pretty(t.transactionType)}</td><td>{t.packageCount}</td>
                        <td>{pretty(t.conditionStatus)}</td><td>{t.storageLocation || '—'}</td><td>{t.gatePassNumber || '—'}</td>
                        <td>{t.performedBy}</td><td className="small">{t.remarks}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            </Card>
          )}
        </Col>
        <Col lg={4}>
          {a && (
            <Card className="mb-3">
              <Card.Header>Trip {a.assignmentCode}</Card.Header>
              <Card.Body className="small">
                <div className="mb-1"><StatusBadge status={a.status} /> {a.delayed && <StatusBadge status="DELAYED" />}</div>
                <div><span className="text-muted">Driver:</span> {a.driverName} · {a.driverPhone}</div>
                <div><span className="text-muted">Vehicle:</span> {a.vehicleNumber} ({pretty(a.vehicleType)})</div>
                <div><span className="text-muted">Planned start:</span> {formatDateTime(a.plannedStartTime)}</div>
                <div><span className="text-muted">Expected delivery:</span> {formatDateTime(a.expectedDeliveryTime)}</div>
                {a.rejectionReason && <div className="text-danger mt-1">{a.rejectionReason}</div>}
                {a.deliveryId && staff && <Button size="sm" variant="link" className="p-0 mt-2" onClick={() => navigate(`/deliveries/${a.deliveryId}`)}>Delivery details</Button>}
              </Card.Body>
            </Card>
          )}
          <Card>
            <Card.Header>Status history</Card.Header>
            <Card.Body><StatusTimeline items={history.data} showUser={staff} /></Card.Body>
          </Card>
        </Col>
      </Row>
      <ConfirmDialog show={cancelOpen} onHide={() => setCancelOpen(false)} title={`Cancel ${s.trackingNumber}?`}
        message="Cancelling is allowed only before pickup. Any assigned driver and vehicle are released."
        askReason confirmLabel="Cancel shipment" onConfirm={cancel} />
    </>
  );
}
