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
import useAuth from '../../hooks/useAuth';
import shipmentService from '../../services/shipmentService';
import warehouseService from '../../services/warehouseService';
import { ShipmentStatus } from '../../utils/constants';
import { formatDate, formatKg, formatMoney, pretty } from '../../utils/formatters';

export default function ShipmentList() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isCustomer = user.role === 'CUSTOMER';
  const [filters, setFilters] = useState({
    search: '', status: '', from: '', to: '', warehouseId: user.role === 'WAREHOUSE_STAFF' ? String(user.warehouseId || '') : '',
  });
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => shipmentService.list({ ...filters, page, size: 10 }), [filters, page]);
  const warehouses = useFetch(() => (isCustomer ? Promise.resolve([]) : warehouseService.list()), [isCustomer], { initial: [] });

  const columns = [
    { key: 'trackingNumber', label: 'Tracking no.', render: (s) => <span className="mono fw-semibold">{s.trackingNumber}</span> },
    { key: 'createdAt', label: 'Booked', render: (s) => formatDate(s.createdAt) },
    ...(isCustomer ? [] : [{ key: 'customerName', label: 'Customer' }]),
    { key: 'route', label: 'Route', render: (s) => <>{s.pickup?.city} <i className="bi bi-arrow-right small" /> {s.delivery?.city}</> },
    { key: 'receiver', label: 'Receiver', render: (s) => s.delivery?.contactName },
    { key: 'pkgs', label: 'Pkgs / weight', render: (s) => `${s.totalPackages} · ${formatKg(s.totalWeightKg)}` },
    { key: 'service', label: 'Service', render: (s) => pretty(s.serviceType) },
    { key: 'charge', label: 'Charge', render: (s) => formatMoney(s.shippingCharge) },
    { key: 'eta', label: 'ETA', render: (s) => formatDate(s.estimatedDeliveryDate) },
    { key: 'currentStatus', label: 'Status', render: (s) => <StatusBadge status={s.currentStatus} /> },
  ];

  const filterDefs = [
    { name: 'status', label: 'Status', options: ShipmentStatus },
    { name: 'from', label: 'From', type: 'date' },
    { name: 'to', label: 'To', type: 'date' },
    ...(isCustomer ? [] : [{ name: 'warehouseId', label: 'Warehouse', options: warehouses.data.map((w) => ({ value: String(w.id), label: w.name })) }]),
  ];

  return (
    <>
      <PageHeader title={isCustomer ? 'My shipments' : 'Shipments'} subtitle="Click a row for details and live status">
        {(isCustomer || user.role === 'ADMIN') && <Button as={Link} to="/shipments/new"><i className="bi bi-plus-lg me-1" />New shipment</Button>}
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} filters={filterDefs}
          placeholder={isCustomer ? 'Tracking number or description' : 'Tracking no., customer or description'} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(s) => navigate(`/shipments/${s.id}`)}
          emptyText={isCustomer ? 'No shipments yet — book your first one!' : 'No shipments match the filters'} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  );
}
