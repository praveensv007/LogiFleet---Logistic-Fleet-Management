import { Badge, Button, Card, Col, Row } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import useFetch from '../../hooks/useFetch';
import useWebSocket from '../../hooks/useWebSocket';
import reportService from '../../services/reportService';
import assignmentService from '../../services/assignmentService';
import deliveryService from '../../services/deliveryService';
import maintenanceService from '../../services/maintenanceService';
import { formatDate, formatDateTime, pretty, timeAgo } from '../../utils/formatters';

export default function FleetDashboard() {
  const navigate = useNavigate();
  const summary = useFetch(() => reportService.summary(), []);
  const pending = useFetch(() => assignmentService.pendingShipments(), [], { initial: [] });
  const active = useFetch(() => deliveryService.active(), [], { initial: [] });
  const due = useFetch(() => maintenanceService.due(), [], { initial: [] });

  // delivery status, delay and problem events
  useWebSocket('/topic/fleet/active-deliveries', () => {
    active.reload(true);
    pending.reload(true);
    summary.reload(true);
  });

  const s = summary.data || {};
  return (
    <>
      <PageHeader title="Fleet dashboard" subtitle="Assign, monitor and maintain">
        <Button as={Link} to="/assignments/new"><i className="bi bi-plus-lg me-1" />New assignment</Button>
      </PageHeader>
      <Row className="g-3 mb-3">
        <Col xl={2} md={4} xs={6}><StatCard icon="hourglass-split" color="sand" label="Waiting for driver" value={pending.data.length} to="/assignments" /></Col>
        <Col xl={2} md={4} xs={6}><StatCard icon="broadcast" label="Active deliveries" value={s.activeDeliveries} to="/deliveries/active" /></Col>
        <Col xl={2} md={4} xs={6}><StatCard icon="alarm" color="red" label="Delayed" value={s.delayedDeliveries} to="/deliveries/active" /></Col>
        <Col xl={2} md={4} xs={6}><StatCard icon="exclamation-triangle" color="red" label="Open problems" value={s.openProblems} to="/deliveries/active" /></Col>
        <Col xl={2} md={4} xs={6}><StatCard icon="person-check" color="green" label="Drivers available" value={s.availableDrivers} to="/drivers" /></Col>
        <Col xl={2} md={4} xs={6}><StatCard icon="truck" color="blue" label="Vehicles available" value={s.availableVehicles} to="/vehicles" /></Col>
      </Row>
      <Row className="g-3">
        <Col xl={7}>
          <Card className="mb-3">
            <Card.Header>Ready for dispatch — needs a driver</Card.Header>
            <DataTable rows={pending.data} loading={pending.loading} emptyText="Nothing waiting. Nice!" columns={[
              { key: 'trackingNumber', label: 'Shipment', render: (x) => <span className="mono">{x.trackingNumber}</span> },
              { key: 'route', label: 'Route', render: (x) => `${x.pickup?.city} → ${x.delivery?.city}` },
              { key: 'weight', label: 'Weight', render: (x) => `${x.totalWeightKg} kg` },
              { key: 'service', label: 'Service', render: (x) => pretty(x.serviceType) },
              { key: 'eta', label: 'ETA', render: (x) => formatDate(x.estimatedDeliveryDate) },
              { key: 'go', label: '', className: 'text-end', render: (x) => <Button size="sm" onClick={() => navigate(`/assignments/new?shipmentId=${x.id}`)}>Assign</Button> },
            ]} />
          </Card>
          <Card>
            <Card.Header className="d-flex justify-content-between">On the road <Badge bg="success"><i className="bi bi-broadcast me-1" />Live</Badge></Card.Header>
            <DataTable rows={active.data} loading={active.loading} emptyText="No active deliveries" onRowClick={(d) => navigate(`/deliveries/${d.id}`)} columns={[
              { key: 'trackingNumber', label: 'Shipment', render: (d) => <span className="mono">{d.trackingNumber}</span> },
              { key: 'driverName', label: 'Driver', render: (d) => `${d.driverName} · ${d.vehicleNumber}` },
              { key: 'status', label: 'Status', render: (d) => <><StatusBadge status={d.currentStatus} />{d.delayed && <Badge bg="danger" className="ms-1">+{d.delayMinutes}m</Badge>}{d.openProblems > 0 && <Badge bg="danger" className="ms-1"><i className="bi bi-exclamation-triangle" /></Badge>}</> },
              { key: 'exp', label: 'Expected', render: (d) => formatDateTime(d.expectedDeliveryTime) },
              { key: 'loc', label: 'Location', render: (d) => (d.lastLocationUpdate ? timeAgo(d.lastLocationUpdate) : '—') },
            ]} />
          </Card>
        </Col>
        <Col xl={5}>
          <Card>
            <Card.Header className="d-flex justify-content-between">Maintenance due (7 days) <Link to="/maintenance" className="small">All</Link></Card.Header>
            <DataTable rows={due.data} loading={due.loading} emptyText="No maintenance due" onRowClick={(m) => navigate(`/maintenance/${m.id}/edit`)} columns={[
              { key: 'vehicleNumber', label: 'Vehicle', render: (m) => <span className="mono">{m.vehicleNumber}</span> },
              { key: 'type', label: 'Type', render: (m) => pretty(m.maintenanceType) },
              { key: 'date', label: 'Scheduled', render: (m) => <span className={m.overdue ? 'text-danger fw-semibold' : ''}>{formatDate(m.scheduledDate)}</span> },
              { key: 'status', label: 'Status', render: (m) => <StatusBadge status={m.status} /> },
            ]} />
          </Card>
        </Col>
      </Row>
    </>
  );
}
