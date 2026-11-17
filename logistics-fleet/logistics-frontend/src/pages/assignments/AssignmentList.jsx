import { useState } from 'react';
import { Button, ButtonGroup, Card } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import SearchFilter from '../../components/common/SearchFilter';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Modal from '../../components/common/Modal';
import FormBuilder from '../../components/common/FormBuilder';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useWebSocket from '../../hooks/useWebSocket';
import assignmentService from '../../services/assignmentService';
import { AssignmentStatus } from '../../utils/constants';
import { errorMessage, formatDateTime, isoDateTime } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const editSchema = yup.object({
  plannedStartTime: v.req('Planned start'),
  expectedDeliveryTime: v.req('Expected delivery').test('after', 'Must be after planned start',
    function after(x) { return !x || new Date(x) > new Date(this.parent.plannedStartTime); }),
  routeNotes: v.opt(500),
});

export default function AssignmentList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ search: '', status: '' });
  const [page, setPage] = useState(0);
  const [cancelling, setCancelling] = useState(null);
  const [editing, setEditing] = useState(null);
  const { data, loading, error, reload } = useFetch(() => assignmentService.list({ ...filters, page, size: 10 }), [filters, page]);
  const pending = useFetch(() => assignmentService.pendingShipments(), [], { initial: [] });
  useWebSocket('/topic/fleet/active-deliveries', () => { reload(true); pending.reload(true); });

  const cancel = async (reason) => {
    try {
      await assignmentService.cancel(cancelling.id, reason);
      toast.success('Assignment cancelled; driver and vehicle released');
      setCancelling(null);
      reload(true);
      pending.reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  const columns = [
    { key: 'assignmentCode', label: 'Trip', render: (a) => <span className="mono">{a.assignmentCode}</span> },
    { key: 'trackingNumber', label: 'Shipment', render: (a) => <Link to={`/shipments/${a.shipmentId}`} className="mono" onClick={(e) => e.stopPropagation()}>{a.trackingNumber}</Link> },
    { key: 'route', label: 'Route', render: (a) => `${a.pickupCity} → ${a.deliveryCity}` },
    { key: 'driverName', label: 'Driver', render: (a) => <><div>{a.driverName}</div><div className="small text-muted">{a.driverPhone}</div></> },
    { key: 'vehicleNumber', label: 'Vehicle' },
    { key: 'plannedStartTime', label: 'Start', render: (a) => formatDateTime(a.plannedStartTime) },
    { key: 'expectedDeliveryTime', label: 'Expected', render: (a) => formatDateTime(a.expectedDeliveryTime) },
    { key: 'status', label: 'Status', render: (a) => (
      <>
        <StatusBadge status={a.status} />{a.deliveryStatus && <StatusBadge status={a.deliveryStatus} className="ms-1" />}
        {a.delayed && <StatusBadge status="DELAYED" className="ms-1" />}
        {a.rejectionReason && <div className="small text-danger">{a.rejectionReason}</div>}
      </>
    ) },
    { key: 'actions', label: '', className: 'text-end', render: (a) => ['ASSIGNED', 'ACCEPTED'].includes(a.status) && (
      <ButtonGroup size="sm" onClick={(e) => e.stopPropagation()}>
        <Button variant="outline-primary" onClick={() => setEditing(a)} title="Edit times"><i className="bi bi-clock" /></Button>
        <Button variant="outline-danger" onClick={() => setCancelling(a)} title="Cancel"><i className="bi bi-x-lg" /></Button>
      </ButtonGroup>
    ) },
  ];

  return (
    <>
      <PageHeader title="Fleet assignments" subtitle="Driver + vehicle for each shipment">
        <Button as={Link} to="/assignments/new"><i className="bi bi-plus-lg me-1" />New assignment</Button>
      </PageHeader>
      {pending.data.length > 0 && (
        <Card className="mb-3 border-warning">
          <Card.Header className="bg-warning-subtle">
            <i className="bi bi-hourglass-split me-1" />{pending.data.length} shipment(s) waiting for a driver
          </Card.Header>
          <DataTable rows={pending.data} columns={[
            { key: 'trackingNumber', label: 'Shipment', render: (s) => <span className="mono">{s.trackingNumber}</span> },
            { key: 'route', label: 'Route', render: (s) => `${s.pickup?.city} → ${s.delivery?.city}` },
            { key: 'weight', label: 'Weight', render: (s) => `${s.totalWeightKg} kg` },
            { key: 'serviceType', label: 'Service' },
            { key: 'go', label: '', className: 'text-end', render: (s) => <Button size="sm" onClick={() => navigate(`/assignments/new?shipmentId=${s.id}`)}>Assign</Button> },
          ]} />
        </Card>
      )}
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Trip, tracking no., driver or vehicle"
          filters={[{ name: 'status', label: 'Status', options: AssignmentStatus }]} />
        <DataTable columns={columns} rows={data?.content} loading={loading}
          onRowClick={(a) => (a.deliveryId ? navigate(`/deliveries/${a.deliveryId}`) : navigate(`/shipments/${a.shipmentId}`))} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
      <ConfirmDialog show={!!cancelling} onHide={() => setCancelling(null)} title={`Cancel ${cancelling?.assignmentCode}?`}
        message="The shipment goes back to READY_FOR_DISPATCH and the driver and vehicle become available."
        askReason confirmLabel="Cancel assignment" onConfirm={cancel} />
      <Modal show={!!editing} onHide={() => setEditing(null)} title={`Edit ${editing?.assignmentCode}`}>
        {editing && (
          <FormBuilder schema={editSchema} submitLabel="Save"
            defaultValues={{
              plannedStartTime: isoDateTime(new Date(editing.plannedStartTime)),
              expectedDeliveryTime: isoDateTime(new Date(editing.expectedDeliveryTime)),
              routeNotes: editing.routeNotes || '',
            }}
            fields={[
              { name: 'plannedStartTime', label: 'Planned start', type: 'datetime-local', required: true },
              { name: 'expectedDeliveryTime', label: 'Expected delivery', type: 'datetime-local', required: true },
              { name: 'routeNotes', label: 'Route notes', type: 'textarea', col: 12 },
            ]}
            onCancel={() => setEditing(null)}
            onSubmit={async (values) => {
              await assignmentService.update(editing.id, values);
              toast.success('Assignment updated');
              setEditing(null);
              reload(true);
            }} />
        )}
      </Modal>
    </>
  );
}
