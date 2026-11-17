import { useState } from 'react';
import { Button, Card, Col, Dropdown, Row } from 'react-bootstrap';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import driverService from '../../services/driverService';
import { DriverAvailability } from '../../utils/constants';
import { errorMessage, fileUrl, formatDate, formatDateTime, pretty } from '../../utils/formatters';

const Item = ({ label, children }) => (
  <Col sm={6} className="mb-3"><div className="small text-muted">{label}</div><div>{children || '—'}</div></Col>
);

export default function DriverDetail() {
  const { id } = useParams();
  const { data: d, loading, error, reload } = useFetch(() => driverService.get(id), [id]);
  const [busy, setBusy] = useState(false);

  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;

  const setAvailability = async (status) => {
    setBusy(true);
    try {
      await driverService.setAvailability(id, status);
      toast.success(`Availability set to ${pretty(status)}`);
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title={d.fullName} subtitle={`${d.driverCode} · ${d.username}`}>
        <StatusBadge status={d.availabilityStatus} className="align-self-center fs-6" />
        <Dropdown>
          <Dropdown.Toggle variant="outline-secondary" disabled={busy || d.availabilityStatus === 'ON_TRIP'}>Set availability</Dropdown.Toggle>
          <Dropdown.Menu>
            {DriverAvailability.filter((x) => x !== 'ON_TRIP' && x !== d.availabilityStatus).map((x) => (
              <Dropdown.Item key={x} onClick={() => setAvailability(x)}>{pretty(x)}</Dropdown.Item>
            ))}
          </Dropdown.Menu>
        </Dropdown>
        <Button as={Link} to={`/drivers/${id}/edit`}><i className="bi bi-pencil me-1" />Edit</Button>
      </PageHeader>
      <Row className="g-3">
        <Col lg={5}>
          <Card>
            <Card.Body className="d-flex gap-3 align-items-center border-bottom">
              {d.photoUrl ? <img src={fileUrl(d.photoUrl)} alt="" width={72} height={72} className="rounded-circle object-fit-cover" />
                : <i className="bi bi-person-circle text-muted" style={{ fontSize: 64 }} />}
              <div>
                <div className="fw-bold fs-5">{d.fullName}</div>
                <div className="text-muted">{d.phone} · {d.email}</div>
                <div className="small text-muted">{d.age} yrs · {pretty(d.gender)} · {d.experienceYears} yrs experience</div>
              </div>
            </Card.Body>
            <Card.Body>
              <Row>
                <Item label="License">{d.licenseNumber} ({d.licenseType})</Item>
                <Item label="Valid">{formatDate(d.licenseIssueDate)} → <span className={d.licenseValid ? '' : 'text-danger fw-semibold'}>{formatDate(d.licenseExpiryDate)}</span></Item>
                <Item label="License copy">{d.licenseDocUrl ? <a href={fileUrl(d.licenseDocUrl)} target="_blank" rel="noreferrer">View</a> : null}</Item>
                <Item label="ID proof">{pretty(d.idProofType)} · {d.idProofNumber}</Item>
                <Item label="Address">{d.address}, {d.city}, {d.state} - {d.pincode}</Item>
                <Item label="Joined">{formatDate(d.joiningDate)}</Item>
                <Item label="Emergency contact">{d.emergencyContactName} · {d.emergencyContactPhone}</Item>
                <Item label="Account">{pretty(d.userStatus)}</Item>
              </Row>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={7}>
          <Card>
            <Card.Header>Recent trips</Card.Header>
            <DataTable rows={d.recentTrips} emptyText="No trips yet" columns={[
              { key: 'assignmentCode', label: 'Trip', render: (a) => <span className="mono">{a.assignmentCode}</span> },
              { key: 'trackingNumber', label: 'Shipment', render: (a) => <Link to={`/shipments/${a.shipmentId}`} className="mono">{a.trackingNumber}</Link> },
              { key: 'vehicleNumber', label: 'Vehicle' },
              { key: 'route', label: 'Route', render: (a) => `${a.pickupCity} → ${a.deliveryCity}` },
              { key: 'assignedAt', label: 'Assigned', render: (a) => formatDateTime(a.assignedAt) },
              { key: 'status', label: 'Status', render: (a) => <><StatusBadge status={a.status} />{a.delayed && <StatusBadge status="DELAYED" className="ms-1" />}</> },
            ]} />
          </Card>
        </Col>
      </Row>
    </>
  );
}
