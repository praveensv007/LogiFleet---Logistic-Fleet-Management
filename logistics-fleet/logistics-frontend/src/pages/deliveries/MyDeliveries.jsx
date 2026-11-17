import { useEffect, useState } from 'react';
import { Badge, Button, Card, Col, Row } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import DataTable from '../../components/common/DataTable';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Loader, { EmptyState, ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import { useNotifications } from '../../context/NotificationContext';
import deliveryService from '../../services/deliveryService';
import { errorMessage, formatDateTime, formatKg, formatMoney, pretty } from '../../utils/formatters';

const OPEN = ['ASSIGNED', 'ACCEPTED', 'IN_PROGRESS'];

/** One trip card with the actions the driver can take right now. */
export function TripCard({ trip: a, onAccept, onReject }) {
  const navigate = useNavigate();
  return (
    <Card className={`h-100 ${a.status === 'ASSIGNED' ? 'border-warning' : ''}`}>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span className="mono">{a.assignmentCode}</span>
        <span>
          <StatusBadge status={a.status} />
          {a.deliveryStatus && <StatusBadge status={a.deliveryStatus} className="ms-1" />}
          {a.delayed && <Badge bg="danger" className="ms-1">Delayed</Badge>}
        </span>
      </Card.Header>
      <Card.Body className="small">
        <div className="fw-semibold mb-1">{a.pickupCity} <i className="bi bi-arrow-right" /> {a.deliveryCity}</div>
        <div className="text-muted mb-2">{a.deliveryAddress}</div>
        <div><span className="text-muted">Shipment:</span> <span className="mono">{a.trackingNumber}</span> · <StatusBadge status={a.shipmentStatus} /></div>
        <div><span className="text-muted">Load:</span> {a.totalPackages} pkg · {formatKg(a.totalWeightKg)} · {pretty(a.serviceType)}</div>
        <div><span className="text-muted">Vehicle:</span> {a.vehicleNumber} · pick up at {a.originWarehouseName}</div>
        <div><span className="text-muted">Receiver:</span> {a.receiverName} · <a href={`tel:${a.receiverPhone}`}>{a.receiverPhone}</a></div>
        <div><span className="text-muted">Start:</span> {formatDateTime(a.plannedStartTime)} · <span className="text-muted">due</span> {formatDateTime(a.expectedDeliveryTime)}</div>
        {a.paymentMode === 'COD' && <div className="text-danger fw-semibold mt-1">Collect {formatMoney(a.codAmountDue)} cash</div>}
        {a.routeNotes && <div className="mt-1"><i className="bi bi-sticky me-1" />{a.routeNotes}</div>}
      </Card.Body>
      <Card.Footer className="bg-white d-flex gap-2">
        {a.status === 'ASSIGNED' && (
          <>
            <Button size="sm" variant="success" onClick={() => onAccept(a)}><i className="bi bi-check-lg me-1" />Accept</Button>
            <Button size="sm" variant="outline-danger" onClick={() => onReject(a)}>Reject</Button>
          </>
        )}
        {a.deliveryId && OPEN.includes(a.status) && (
          <Button size="sm" onClick={() => navigate(`/deliveries/${a.deliveryId}`)}><i className="bi bi-truck me-1" />Open trip</Button>
        )}
        {a.status === 'ACCEPTED' && a.shipmentStatus === 'ASSIGNED' && (
          <span className="small text-muted align-self-center">Warehouse is loading your vehicle…</span>
        )}
      </Card.Footer>
    </Card>
  );
}

/** Accept / reject logic shared by the driver dashboard. */
export function useTripActions(reload) {
  const [rejecting, setRejecting] = useState(null);
  const accept = async (a) => {
    try {
      await deliveryService.accept(a.id);
      toast.success(`${a.assignmentCode} accepted. The warehouse will load your vehicle.`);
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };
  const reject = async (reason) => {
    try {
      await deliveryService.reject(rejecting.id, reason);
      toast.info('Trip rejected. The fleet manager will reassign it.');
      setRejecting(null);
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };
  const dialog = (
    <ConfirmDialog show={!!rejecting} onHide={() => setRejecting(null)} title={`Reject ${rejecting?.assignmentCode}?`}
      askReason reasonLabel="Rejection reason" confirmLabel="Reject trip" onConfirm={reject} />
  );
  return { accept, startReject: setRejecting, dialog };
}

export default function MyDeliveries() {
  const { data, loading, error, reload } = useFetch(() => deliveryService.my(), [], { initial: [] });
  const { onNotification } = useNotifications();
  const { accept, startReject, dialog } = useTripActions(reload);

  useEffect(() => onNotification(() => reload(true)), [onNotification, reload]);

  if (loading) return <Loader />;
  const open = data.filter((a) => OPEN.includes(a.status));
  const past = data.filter((a) => !OPEN.includes(a.status));

  return (
    <>
      <PageHeader title="My deliveries" subtitle="New trips appear here instantly" />
      <ErrorAlert error={error} onRetry={reload} />
      {open.length === 0 ? <Card className="mb-3"><EmptyState icon="cup-hot" text="No open trips. You'll be notified when one is assigned." /></Card> : (
        <Row className="g-3 mb-4">
          {open.map((a) => <Col md={6} xl={4} key={a.id}><TripCard trip={a} onAccept={accept} onReject={startReject} /></Col>)}
        </Row>
      )}
      <Card>
        <Card.Header>Trip history</Card.Header>
        <DataTable rows={past} emptyText="No completed trips yet" columns={[
          { key: 'assignmentCode', label: 'Trip', render: (a) => <span className="mono">{a.assignmentCode}</span> },
          { key: 'trackingNumber', label: 'Shipment', render: (a) => <span className="mono">{a.trackingNumber}</span> },
          { key: 'route', label: 'Route', render: (a) => `${a.pickupCity} → ${a.deliveryCity}` },
          { key: 'completedAt', label: 'Completed', render: (a) => formatDateTime(a.completedAt) },
          { key: 'status', label: 'Status', render: (a) => <><StatusBadge status={a.status} />{a.delayed && <Badge bg="danger" className="ms-1">Late {a.delayMinutes} min</Badge>}</> },
          { key: 'x', label: '', render: (a) => a.deliveryId && <Link to={`/deliveries/${a.deliveryId}`}>Details</Link> },
        ]} />
      </Card>
      {dialog}
    </>
  );
}
