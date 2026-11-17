import { useState } from 'react';
import { Button, Card, Col, Dropdown, Row, Tab, Tabs } from 'react-bootstrap';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import vehicleService from '../../services/vehicleService';
import { DocumentType, VehicleStatus } from '../../utils/constants';
import { clean, errorMessage, fileUrl, formatDate, formatDateTime, formatKg, formatMoney, pretty } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const Item = ({ label, children, danger }) => (
  <Col sm={6} md={4} className="mb-3">
    <div className="small text-muted">{label}</div>
    <div className={danger ? 'text-danger fw-semibold' : ''}>{children || '—'}</div>
  </Col>
);

const docSchema = yup.object({
  documentType: v.select('Document type'),
  documentNumber: v.opt(50),
  fileUrl: yup.string().required('Upload the document'),
  expiryDate: v.optDate(),
});
const docFields = [
  { name: 'documentType', label: 'Document type', type: 'select', options: DocumentType, required: true },
  { name: 'documentNumber', label: 'Document number' },
  { name: 'expiryDate', label: 'Expiry date', type: 'date' },
  { name: 'fileUrl', label: 'File', type: 'file', category: 'document', required: true, col: 12 },
];

export default function VehicleDetail() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const canEdit = hasRole('ADMIN', 'FLEET_MANAGER');
  const { data: vh, loading, error, reload } = useFetch(() => vehicleService.get(id), [id]);
  const [showDoc, setShowDoc] = useState(false);

  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;

  const expired = (d) => d && new Date(d) <= new Date();
  const setStatus = async (status) => {
    try {
      await vehicleService.setStatus(id, status);
      toast.success(`Status set to ${pretty(status)}`);
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };
  const removeDoc = async (doc) => {
    try {
      await vehicleService.deleteDocument(id, doc.id);
      toast.success('Document removed');
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  return (
    <>
      <PageHeader title={vh.vehicleNumber} subtitle={`${pretty(vh.vehicleType)} · ${vh.make} ${vh.model} (${vh.manufactureYear})`}>
        <StatusBadge status={vh.status} className="align-self-center fs-6" />
        {canEdit && (
          <>
            <Dropdown>
              <Dropdown.Toggle variant="outline-secondary" disabled={vh.status === 'ON_TRIP'}>Set status</Dropdown.Toggle>
              <Dropdown.Menu>
                {VehicleStatus.filter((s) => s !== 'ON_TRIP' && s !== vh.status).map((s) => (
                  <Dropdown.Item key={s} onClick={() => setStatus(s)}>{pretty(s)}</Dropdown.Item>
                ))}
              </Dropdown.Menu>
            </Dropdown>
            <Button as={Link} to={`/maintenance/new?vehicleId=${id}`} variant="outline-primary"><i className="bi bi-tools me-1" />Schedule service</Button>
            <Button as={Link} to={`/vehicles/${id}/edit`}><i className="bi bi-pencil me-1" />Edit</Button>
          </>
        )}
      </PageHeader>
      <Card className="mb-3">
        <Card.Body>
          <Row>
            <Item label="Capacity">{formatKg(vh.capacityKg)}{vh.capacityCbm && ` · ${vh.capacityCbm} m³`}</Item>
            <Item label="Fuel">{pretty(vh.fuelType)}</Item>
            <Item label="Odometer">{vh.odometerKm?.toLocaleString('en-IN')} km</Item>
            <Item label="Chassis / engine">{vh.chassisNumber} / {vh.engineNumber}</Item>
            <Item label="Insurance">{vh.insuranceNumber}</Item>
            <Item label="Insurance expiry" danger={expired(vh.insuranceExpiry)}>{formatDate(vh.insuranceExpiry)}</Item>
            <Item label="Permit expiry" danger={expired(vh.permitExpiry)}>{formatDate(vh.permitExpiry)}</Item>
            <Item label="Fitness expiry" danger={expired(vh.fitnessExpiry)}>{formatDate(vh.fitnessExpiry)}</Item>
            <Item label="PUC expiry" danger={expired(vh.pucExpiry)}>{formatDate(vh.pucExpiry)}</Item>
            <Item label="Home warehouse">{vh.homeWarehouseName}</Item>
            <Item label="Last service">{formatDate(vh.lastServiceDate)}</Item>
            <Item label="Next service">{formatDate(vh.nextServiceDate)}</Item>
          </Row>
        </Card.Body>
      </Card>
      <Card>
        <Tabs defaultActiveKey="docs" className="px-3 pt-2">
          <Tab eventKey="docs" title={`Documents (${vh.documents?.length || 0})`}>
            {canEdit && <div className="p-3 pb-0 text-end"><Button size="sm" onClick={() => setShowDoc(true)}><i className="bi bi-upload me-1" />Add document</Button></div>}
            <DataTable rows={vh.documents} emptyText="No documents uploaded" columns={[
              { key: 'documentType', label: 'Type' },
              { key: 'documentNumber', label: 'Number' },
              { key: 'expiryDate', label: 'Expiry', render: (d) => <span className={expired(d.expiryDate) ? 'text-danger' : ''}>{formatDate(d.expiryDate)}</span> },
              { key: 'fileUrl', label: 'File', render: (d) => <a href={fileUrl(d.fileUrl)} target="_blank" rel="noreferrer">Open</a> },
              ...(canEdit ? [{ key: 'x', label: '', className: 'text-end', render: (d) => <Button size="sm" variant="link" className="text-danger" onClick={() => removeDoc(d)}>Remove</Button> }] : []),
            ]} />
          </Tab>
          <Tab eventKey="maintenance" title={`Maintenance (${vh.maintenanceHistory?.length || 0})`}>
            <DataTable rows={vh.maintenanceHistory} emptyText="No maintenance records" columns={[
              { key: 'maintenanceType', label: 'Type', render: (m) => pretty(m.maintenanceType) },
              { key: 'description', label: 'Description' },
              { key: 'scheduledDate', label: 'Scheduled', render: (m) => formatDate(m.scheduledDate) },
              { key: 'completedDate', label: 'Completed', render: (m) => formatDate(m.completedDate) },
              { key: 'totalCost', label: 'Cost', render: (m) => formatMoney(m.totalCost) },
              { key: 'status', label: 'Status', render: (m) => <StatusBadge status={m.status} /> },
            ]} />
          </Tab>
          <Tab eventKey="trips" title="Recent trips">
            <DataTable rows={vh.recentTrips} emptyText="No trips yet" columns={[
              { key: 'assignmentCode', label: 'Trip', render: (a) => <span className="mono">{a.assignmentCode}</span> },
              { key: 'trackingNumber', label: 'Shipment', render: (a) => <span className="mono">{a.trackingNumber}</span> },
              { key: 'driverName', label: 'Driver' },
              { key: 'assignedAt', label: 'Assigned', render: (a) => formatDateTime(a.assignedAt) },
              { key: 'status', label: 'Status', render: (a) => <StatusBadge status={a.status} /> },
            ]} />
          </Tab>
        </Tabs>
      </Card>
      <Modal show={showDoc} onHide={() => setShowDoc(false)} title="Add vehicle document">
        <FormBuilder fields={docFields} schema={docSchema} submitLabel="Save document"
          defaultValues={{ documentType: '', documentNumber: '', expiryDate: '', fileUrl: '' }}
          onCancel={() => setShowDoc(false)}
          onSubmit={async (values) => {
            await vehicleService.addDocument(id, clean(values));
            toast.success('Document added');
            setShowDoc(false);
            reload(true);
          }} />
      </Modal>
    </>
  );
}
