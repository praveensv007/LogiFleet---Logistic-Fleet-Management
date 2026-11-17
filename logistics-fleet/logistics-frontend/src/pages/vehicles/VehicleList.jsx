import { useState } from 'react';
import { Button, Card } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import SearchFilter from '../../components/common/SearchFilter';
import StatusBadge from '../../components/common/StatusBadge';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import vehicleService from '../../services/vehicleService';
import { VehicleStatus, VehicleType } from '../../utils/constants';
import { formatDate, formatKg, pretty } from '../../utils/formatters';

export default function VehicleList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ search: '', status: '', type: '' });
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => vehicleService.list({ ...filters, page, size: 10 }), [filters, page]);

  const soon = (d) => d && new Date(d) < new Date(Date.now() + 30 * 86400000);

  const columns = [
    { key: 'vehicleNumber', label: 'Number', render: (v) => <span className="mono fw-semibold">{v.vehicleNumber}</span> },
    { key: 'type', label: 'Type', render: (v) => <><div>{pretty(v.vehicleType)}</div><div className="small text-muted">{v.make} {v.model} · {v.manufactureYear}</div></> },
    { key: 'capacityKg', label: 'Capacity', render: (v) => formatKg(v.capacityKg) },
    { key: 'fuelType', label: 'Fuel', render: (v) => pretty(v.fuelType) },
    { key: 'insuranceExpiry', label: 'Insurance', render: (v) => <span className={soon(v.insuranceExpiry) ? 'text-danger fw-semibold' : ''}>{formatDate(v.insuranceExpiry)}</span> },
    { key: 'nextServiceDate', label: 'Next service', render: (v) => <span className={soon(v.nextServiceDate) ? 'text-warning fw-semibold' : ''}>{formatDate(v.nextServiceDate)}</span> },
    { key: 'homeWarehouseName', label: 'Home' },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge status={v.status} /> },
  ];

  return (
    <>
      <PageHeader title="Vehicles" subtitle="Fleet register, documents and service status">
        <Button as={Link} to="/vehicles/new"><i className="bi bi-plus-lg me-1" />New vehicle</Button>
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Number, make or model"
          filters={[{ name: 'status', label: 'Status', options: VehicleStatus }, { name: 'type', label: 'Type', options: VehicleType }]} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(v) => navigate(`/vehicles/${v.id}`)} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  );
}
