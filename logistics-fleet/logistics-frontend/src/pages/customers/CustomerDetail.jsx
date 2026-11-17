import { Button, Card, Col, Row } from 'react-bootstrap';
import { Link, useNavigate, useParams } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import customerService from '../../services/customerService';
import { formatDate, pretty } from '../../utils/formatters';

const Item = ({ label, children }) => (
  <div className="mb-2"><div className="small text-muted">{label}</div><div>{children || '—'}</div></div>
);

export default function CustomerDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const customer = useFetch(() => customerService.get(id), [id]);
  const shipments = useFetch(() => customerService.shipments(id), [id], { initial: [] });

  if (customer.loading) return <Loader />;
  if (customer.error) return <ErrorAlert error={customer.error} onRetry={customer.reload} />;
  const c = customer.data;

  return (
    <>
      <PageHeader title={c.fullName} subtitle={`${c.customerCode} · ${pretty(c.customerType)}`}>
        <StatusBadge status={c.status} className="align-self-center" />
        {hasRole('ADMIN') && <Button as={Link} to={`/customers/${id}/edit`}><i className="bi bi-pencil me-1" />Edit</Button>}
      </PageHeader>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>Profile</Card.Header>
            <Card.Body>
              <Item label="Username">{c.username}</Item>
              <Item label="Email">{c.email}</Item>
              <Item label="Phone">{c.phone}{c.alternatePhone && ` / ${c.alternatePhone}`}</Item>
              {c.companyName && <Item label="Company">{c.companyName}</Item>}
              <Item label="GST number">{c.gstNumber}</Item>
              <Item label="Address">{c.addressLine1}{c.addressLine2 && `, ${c.addressLine2}`}<br />{c.city}, {c.state} - {c.pincode}, {c.country}</Item>
              <Item label="Customer since">{formatDate(c.createdAt)}</Item>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={8}>
          <Card>
            <Card.Header>Shipment history ({shipments.data.length})</Card.Header>
            <DataTable loading={shipments.loading} rows={shipments.data} onRowClick={(s) => navigate(`/shipments/${s.id}`)}
              emptyText="No shipments yet"
              columns={[
                { key: 'trackingNumber', label: 'Tracking no.', render: (s) => <span className="mono">{s.trackingNumber}</span> },
                { key: 'createdAt', label: 'Date', render: (s) => formatDate(s.createdAt) },
                { key: 'destination', label: 'Destination', render: (s) => s.delivery ? `${s.delivery.city}, ${s.delivery.state}` : '—' },
                { key: 'currentStatus', label: 'Status', render: (s) => <StatusBadge status={s.currentStatus} /> },
              ]} />
          </Card>
        </Col>
      </Row>
    </>
  );
}
