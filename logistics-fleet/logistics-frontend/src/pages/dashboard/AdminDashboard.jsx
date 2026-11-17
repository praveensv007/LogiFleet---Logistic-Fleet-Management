import { Card, Col, ListGroup, Row } from 'react-bootstrap';
import { Bar, Line } from 'react-chartjs-2';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useWebSocket from '../../hooks/useWebSocket';
import reportService from '../../services/reportService';
import auditService from '../../services/auditService';
import { PALETTE } from '../../utils/chartSetup';
import { isoDate, pretty, timeAgo } from '../../utils/formatters';

export default function AdminDashboard() {
  const summary = useFetch(() => reportService.summary(), []);
  const trend = useFetch(() => reportService.shipmentsByDay({ from: isoDate(new Date(Date.now() - 13 * 86400000)), to: isoDate() }), [], { initial: [] });
  const audit = useFetch(() => auditService.search({ page: 0, size: 6 }), []);

  // Counters change live (created, delivered, delayed...)
  useWebSocket('/topic/admin/dashboard', () => {
    summary.reload(true);
    trend.reload(true);
  });

  if (summary.loading) return <Loader />;
  const s = summary.data;
  if (!s) return <ErrorAlert error={summary.error} onRetry={summary.reload} />;
  const breakdown = Object.entries(s.statusBreakdown || {}).filter(([, n]) => n > 0);

  return (
    <>
      <PageHeader title="Admin dashboard" subtitle="Everything moving through the network, live" />
      <Row className="g-3 mb-3">
        <Col xl={3} md={6}><StatCard icon="box-seam" label="Shipments today" value={s.shipmentsToday} to="/shipments" /></Col>
        <Col xl={3} md={6}><StatCard icon="patch-check" color="green" label="Delivered today" value={s.deliveredToday} /></Col>
        <Col xl={3} md={6}><StatCard icon="truck" color="sand" label="On the road" value={s.onRoad} to="/deliveries/active" /></Col>
        <Col xl={3} md={6}><StatCard icon="alarm" color="red" label="Delayed deliveries" value={s.delayedDeliveries} to="/deliveries/active" /></Col>
        <Col xl={3} md={6}><StatCard icon="hourglass-split" color="purple" label="Pending (not received)" value={s.pendingShipments} /></Col>
        <Col xl={3} md={6}><StatCard icon="exclamation-triangle" color="red" label="Open problems" value={s.openProblems} to="/deliveries/active" /></Col>
        <Col xl={3} md={6}><StatCard icon="person-badge" color="blue" label={`Drivers available · ${s.driversOnTrip} on trip`} value={s.availableDrivers} to="/drivers" /></Col>
        <Col xl={3} md={6}><StatCard icon="tools" color="sand" label={`Vehicles available · ${s.vehiclesInMaintenance} in service`} value={s.availableVehicles} to="/vehicles" /></Col>
      </Row>
      <Row className="g-3 mb-3">
        <Col lg={8}>
          <Card className="h-100">
            <Card.Header>Shipments — last 14 days</Card.Header>
            <Card.Body style={{ height: 280 }}>
              <Line options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }} data={{
                labels: trend.data.map((r) => r.date.slice(5)),
                datasets: [
                  { label: 'Created', data: trend.data.map((r) => r.created), borderColor: PALETTE[0], backgroundColor: 'rgba(29,53,87,0.08)', fill: true, tension: 0.3 },
                  { label: 'Delivered', data: trend.data.map((r) => r.delivered), borderColor: PALETTE[2], tension: 0.3 },
                ],
              }} />
            </Card.Body>
          </Card>
        </Col>
        <Col lg={4}>
          <Card className="h-100">
            <Card.Header>By status</Card.Header>
            <Card.Body style={{ height: 280 }}>
              <Bar options={{ indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }} data={{
                labels: breakdown.map(([k]) => pretty(k)),
                datasets: [{ data: breakdown.map(([, n]) => n), backgroundColor: breakdown.map((_, i) => PALETTE[i % PALETTE.length]) }],
              }} />
            </Card.Body>
          </Card>
        </Col>
      </Row>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>Network</Card.Header>
            <ListGroup variant="flush">
              <ListGroup.Item className="d-flex justify-content-between"><span>Total shipments</span><strong>{s.totalShipments}</strong></ListGroup.Item>
              <ListGroup.Item className="d-flex justify-content-between"><span>Customers</span><strong>{s.totalCustomers}</strong></ListGroup.Item>
              <ListGroup.Item className="d-flex justify-content-between"><span>Warehouses</span><strong>{s.totalWarehouses}</strong></ListGroup.Item>
              <ListGroup.Item className="d-flex justify-content-between"><span>Users</span><strong>{s.totalUsers}</strong></ListGroup.Item>
              <ListGroup.Item className="d-flex justify-content-between"><span>Maintenance due (7 days)</span><strong>{s.maintenanceDue}</strong></ListGroup.Item>
            </ListGroup>
          </Card>
        </Col>
        <Col lg={8}>
          <Card>
            <Card.Header className="d-flex justify-content-between">Recent activity <Link to="/audit-logs" className="small">All audit logs</Link></Card.Header>
            <ListGroup variant="flush">
              {(audit.data?.content || []).map((a) => (
                <ListGroup.Item key={a.id} className="d-flex justify-content-between small">
                  <span><strong>{a.username}</strong> {pretty(a.action).toLowerCase()} {a.entityName}{a.entityId ? ` #${a.entityId}` : ''} <span className="text-muted">({a.module})</span></span>
                  <span className="text-muted">{timeAgo(a.time)}</span>
                </ListGroup.Item>
              ))}
            </ListGroup>
          </Card>
        </Col>
      </Row>
    </>
  );
}
