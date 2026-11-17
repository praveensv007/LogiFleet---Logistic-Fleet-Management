import { Alert, Badge, Card, Col, ListGroup, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Loader from '../../components/common/Loader';
import useWarehouseScope from '../warehouses/WarehouseScope';
import { timeAgo } from '../../utils/formatters';

const COLUMNS = [
  { status: 'CREATED', title: 'To receive', action: '/warehouse/receive', icon: 'box-arrow-in-down', color: 'purple' },
  { status: 'RECEIVED', title: 'To pack', action: '/warehouse/pack', icon: 'box2', color: 'blue' },
  { status: 'PACKED', title: 'Packed · mark ready', action: '/warehouse/pack', icon: 'boxes', color: 'navy' },
  { status: 'READY_FOR_DISPATCH', title: 'Waiting for driver', action: null, icon: 'hourglass-split', color: 'sand' },
  { status: 'ASSIGNED', title: 'Load & dispatch', action: '/warehouse/loading', icon: 'truck-flatbed', color: 'green' },
];

/** Kanban of the warehouse queue; refreshes on /topic/warehouse/{id}. */
export default function WarehouseDashboard() {
  const { queue, selector, warehouse, missing } = useWarehouseScope();
  if (missing) return <Alert variant="warning">Your account is not linked to a warehouse. Ask the admin to assign one.</Alert>;

  return (
    <>
      <PageHeader title="Warehouse dashboard" subtitle={warehouse ? `${warehouse.name} · ${warehouse.city}` : ''}>{selector}</PageHeader>
      <Row className="g-3 mb-3">
        {COLUMNS.map((c) => (
          <Col key={c.status} xl md={4} xs={6}>
            <StatCard icon={c.icon} color={c.color} label={c.title} value={queue.data.filter((s) => s.currentStatus === c.status).length} to={c.action || undefined} />
          </Col>
        ))}
      </Row>
      {queue.loading ? <Loader /> : (
        <div className="d-flex gap-3 overflow-auto pb-2">
          {COLUMNS.map((c) => {
            const items = queue.data.filter((s) => s.currentStatus === c.status);
            return (
              <Card key={c.status} className="kanban-col flex-fill">
                <Card.Header className="d-flex justify-content-between">{c.title} <Badge bg="secondary">{items.length}</Badge></Card.Header>
                <ListGroup variant="flush">
                  {items.length === 0 && <ListGroup.Item className="text-muted small">Empty</ListGroup.Item>}
                  {items.map((s) => (
                    <ListGroup.Item key={s.id} action as={Link} to={c.action || `/shipments/${s.id}`}>
                      <div className="mono small fw-semibold">{s.trackingNumber}</div>
                      <div className="small text-muted">{s.totalPackages} pkg · {s.totalWeightKg} kg → {s.delivery?.city}</div>
                      {s.assignment && <div className="small">{s.assignment.vehicleNumber} · <Badge bg={s.assignment.status === 'ACCEPTED' ? 'success' : 'warning'} text={s.assignment.status === 'ACCEPTED' ? undefined : 'dark'}>{s.assignment.status}</Badge></div>}
                      {s.storageLocation && <div className="small"><i className="bi bi-geo me-1" />{s.storageLocation}</div>}
                      <div className="text-muted" style={{ fontSize: '0.7rem' }}>booked {timeAgo(s.createdAt)}</div>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              </Card>
            );
          })}
        </div>
      )}
    </>
  );
}
