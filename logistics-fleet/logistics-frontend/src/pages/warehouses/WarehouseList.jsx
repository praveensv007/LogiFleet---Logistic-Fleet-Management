import { useState } from 'react';
import { Button, Card } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import SearchFilter from '../../components/common/SearchFilter';
import StatusBadge from '../../components/common/StatusBadge';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import warehouseService from '../../services/warehouseService';
import { ActiveStatus } from '../../utils/constants';
import { formatTime } from '../../utils/formatters';

export default function WarehouseList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ status: '' });
  const { data, loading, error, reload } = useFetch(() => warehouseService.list(filters.status), [filters.status], { initial: [] });

  return (
    <>
      <PageHeader title="Warehouses" subtitle="Hubs that receive, pack and dispatch shipments">
        <Button as={Link} to="/warehouses/new"><i className="bi bi-plus-lg me-1" />New warehouse</Button>
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter showSearch={false} value={filters} onChange={setFilters}
          filters={[{ name: 'status', label: 'Status', options: ActiveStatus, md: 3 }]} />
        <DataTable rows={data} loading={loading} onRowClick={(w) => navigate(`/warehouses/${w.id}/edit`)} columns={[
          { key: 'warehouseCode', label: 'Code', render: (w) => <span className="mono">{w.warehouseCode}</span> },
          { key: 'name', label: 'Name', render: (w) => <span className="fw-semibold">{w.name}</span> },
          { key: 'city', label: 'Location', render: (w) => `${w.city}, ${w.state} - ${w.pincode}` },
          { key: 'managerName', label: 'Manager' },
          { key: 'contact', label: 'Contact', render: (w) => <><div>{w.contactPhone}</div><div className="small text-muted">{w.contactEmail}</div></> },
          { key: 'hours', label: 'Hours', render: (w) => `${formatTime(w.openingTime)} – ${formatTime(w.closingTime)}` },
          { key: 'capacity', label: 'Capacity', render: (w) => w.capacity?.toLocaleString('en-IN') },
          { key: 'pendingShipments', label: 'In queue', className: 'text-center' },
          { key: 'status', label: 'Status', render: (w) => <StatusBadge status={w.status} /> },
        ]} />
      </Card>
    </>
  );
}
