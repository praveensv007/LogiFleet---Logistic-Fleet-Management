import { useEffect, useState } from 'react';
import { Button, Card, Col, Form, InputGroup, Row } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import { useNotifications } from '../../context/NotificationContext';
import shipmentService from '../../services/shipmentService';
import { formatDate } from '../../utils/formatters';

const ON_ROAD = ['DISPATCHED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY'];

export default function CustomerDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tn, setTn] = useState('');
  const { data, loading, reload } = useFetch(() => shipmentService.list({ page: 0, size: 100 }), []);
  const { onNotification } = useNotifications();
  useEffect(() => onNotification(() => reload(true)), [onNotification, reload]);

  const list = data?.content || [];
  const count = (pred) => list.filter(pred).length;

  return (
    <>
      <PageHeader title={`Welcome, ${user.fullName.split(' ')[0]}`} subtitle="Book, track and review your shipments">
        <Button as={Link} to="/shipments/new" className="btn-accent"><i className="bi bi-plus-lg me-1" />New shipment</Button>
      </PageHeader>
      <Row className="g-3 mb-3">
        <Col md={3} xs={6}><StatCard icon="box-seam" label="Total shipments" value={data?.totalElements ?? 0} to="/shipments" /></Col>
        <Col md={3} xs={6}><StatCard icon="building" color="purple" label="At warehouse" value={count((s) => ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED'].includes(s.currentStatus))} /></Col>
        <Col md={3} xs={6}><StatCard icon="truck" color="sand" label="On the way" value={count((s) => ON_ROAD.includes(s.currentStatus))} /></Col>
        <Col md={3} xs={6}><StatCard icon="patch-check" color="green" label="Delivered" value={count((s) => s.currentStatus === 'DELIVERED')} /></Col>
      </Row>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>Track a shipment</Card.Header>
            <Card.Body>
              <Form onSubmit={(e) => { e.preventDefault(); if (tn.trim()) navigate(`/track/${tn.trim().toUpperCase()}`); }}>
                <InputGroup>
                  <Form.Control className="mono" placeholder="LGX-…" value={tn} onChange={(e) => setTn(e.target.value)} />
                  <Button type="submit">Track</Button>
                </InputGroup>
              </Form>
              <p className="small text-muted mt-2 mb-0">Status updates arrive live — no refresh needed.</p>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={8}>
          <Card>
            <Card.Header className="d-flex justify-content-between">Recent shipments <Link to="/shipments" className="small">View all</Link></Card.Header>
            <DataTable rows={list.slice(0, 8)} loading={loading} onRowClick={(s) => navigate(`/shipments/${s.id}`)}
              emptyText="No shipments yet — book your first one!" columns={[
                { key: 'trackingNumber', label: 'Tracking no.', render: (s) => <span className="mono">{s.trackingNumber}</span> },
                { key: 'createdAt', label: 'Date', render: (s) => formatDate(s.createdAt) },
                { key: 'dest', label: 'Destination', render: (s) => s.delivery?.city },
                { key: 'eta', label: 'ETA', render: (s) => formatDate(s.estimatedDeliveryDate) },
                { key: 'status', label: 'Status', render: (s) => <StatusBadge status={s.currentStatus} /> },
              ]} />
          </Card>
        </Col>
      </Row>
    </>
  );
}
