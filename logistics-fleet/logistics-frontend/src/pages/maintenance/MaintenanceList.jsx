import { useState } from 'react';
import { Badge, Button, Card } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import SearchFilter from '../../components/common/SearchFilter';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import FormBuilder from '../../components/common/FormBuilder';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import maintenanceService from '../../services/maintenanceService';
import vehicleService from '../../services/vehicleService';
import { MaintenanceStatus } from '../../utils/constants';
import { clean, formatDate, formatMoney, isoDate, pretty } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const completeSchema = yup.object({
  completedDate: v.date('Completed date'),
  totalCost: v.num('Total cost').min(0).nullable(),
  nextServiceDate: v.optDate(),
  nextServiceOdometer: v.num('Odometer').integer().min(0).nullable(),
});

export default function MaintenanceList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ vehicleId: '', status: '' });
  const [page, setPage] = useState(0);
  const [completing, setCompleting] = useState(null);
  const { data, loading, error, reload } = useFetch(() => maintenanceService.list({ ...filters, page, size: 10, sort: 'scheduledDate,desc' }), [filters, page]);
  const due = useFetch(() => maintenanceService.due(), [], { initial: [] });
  const vehicles = useFetch(() => vehicleService.list({ size: 100 }).then((p) => p.content), [], { initial: [] });

  const columns = [
    { key: 'vehicleNumber', label: 'Vehicle', render: (m) => <span className="mono fw-semibold">{m.vehicleNumber}</span> },
    { key: 'maintenanceType', label: 'Type', render: (m) => pretty(m.maintenanceType) },
    { key: 'description', label: 'Description', render: (m) => <span className="small">{m.description}</span> },
    { key: 'scheduledDate', label: 'Scheduled', render: (m) => <span className={m.overdue ? 'text-danger fw-semibold' : ''}>{formatDate(m.scheduledDate)}{m.overdue && ' (overdue)'}</span> },
    { key: 'completedDate', label: 'Completed', render: (m) => formatDate(m.completedDate) },
    { key: 'serviceCenter', label: 'Vendor' },
    { key: 'totalCost', label: 'Cost', render: (m) => formatMoney(m.totalCost) },
    { key: 'status', label: 'Status', render: (m) => <StatusBadge status={m.status} /> },
    { key: 'x', label: '', className: 'text-end', render: (m) => ['SCHEDULED', 'IN_PROGRESS'].includes(m.status) && (
      <Button size="sm" variant="outline-success" onClick={(e) => { e.stopPropagation(); setCompleting(m); }}>Complete</Button>
    ) },
  ];

  return (
    <>
      <PageHeader title="Vehicle maintenance" subtitle="Schedules, repairs and service costs">
        <Button as={Link} to="/maintenance/new"><i className="bi bi-plus-lg me-1" />Schedule maintenance</Button>
      </PageHeader>
      {due.data.length > 0 && (
        <Card className="mb-3 border-warning">
          <Card.Body className="py-2 small">
            <i className="bi bi-alarm text-warning me-2" /><strong>{due.data.length} due within 7 days:</strong>{' '}
            {due.data.map((m) => (
              <Badge key={m.id} bg={m.overdue ? 'danger' : 'warning'} text={m.overdue ? undefined : 'dark'} className="me-1"
                role="button" onClick={() => navigate(`/maintenance/${m.id}/edit`)}>
                {m.vehicleNumber} · {formatDate(m.scheduledDate)}
              </Badge>
            ))}
          </Card.Body>
        </Card>
      )}
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter showSearch={false} value={filters} onChange={(f) => { setFilters(f); setPage(0); }} filters={[
          { name: 'vehicleId', label: 'Vehicle', md: 3, options: vehicles.data.map((vh) => ({ value: String(vh.id), label: vh.vehicleNumber })) },
          { name: 'status', label: 'Status', md: 3, options: MaintenanceStatus },
        ]} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(m) => navigate(`/maintenance/${m.id}/edit`)} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
      <Modal show={!!completing} onHide={() => setCompleting(null)} title={`Complete · ${completing?.vehicleNumber}`}>
        {completing && (
          <FormBuilder schema={completeSchema} submitLabel="Mark completed" onCancel={() => setCompleting(null)}
            defaultValues={{ completedDate: isoDate(), totalCost: completing.totalCost ?? '', nextServiceDate: completing.nextServiceDate || '', nextServiceOdometer: completing.nextServiceOdometer ?? '' }}
            fields={[
              { name: 'completedDate', label: 'Completed date', type: 'date', required: true, min: completing.scheduledDate },
              { name: 'totalCost', label: 'Total cost (₹)', type: 'number', step: '0.01' },
              { name: 'nextServiceDate', label: 'Next service date', type: 'date' },
              { name: 'nextServiceOdometer', label: 'Next service odometer', type: 'number' },
            ]}
            onSubmit={async (values) => {
              await maintenanceService.complete(completing.id, clean(values));
              toast.success('Maintenance completed; vehicle is available again');
              setCompleting(null);
              reload(true);
              due.reload(true);
            }} />
        )}
      </Modal>
    </>
  );
}
