import { useState } from 'react';
import { Card } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import SearchFilter from '../../components/common/SearchFilter';
import StatusBadge from '../../components/common/StatusBadge';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import customerService from '../../services/customerService';
import { ActiveStatus, CustomerType } from '../../utils/constants';
import { formatDate, pretty } from '../../utils/formatters';

export default function CustomerList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ search: '', type: '', status: '' });
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => customerService.list({ ...filters, page, size: 10 }), [filters, page]);

  const columns = [
    { key: 'customerCode', label: 'Code', render: (c) => <span className="mono">{c.customerCode}</span> },
    { key: 'fullName', label: 'Name', render: (c) => <><div className="fw-semibold">{c.fullName}</div>{c.companyName && <div className="small text-muted">{c.companyName}</div>}</> },
    { key: 'contact', label: 'Contact', render: (c) => <><div>{c.email}</div><div className="small text-muted">{c.phone}</div></> },
    { key: 'customerType', label: 'Type', render: (c) => pretty(c.customerType) },
    { key: 'city', label: 'City', render: (c) => `${c.city}, ${c.state}` },
    { key: 'totalShipments', label: 'Shipments', className: 'text-center' },
    { key: 'status', label: 'Status', render: (c) => <StatusBadge status={c.status} /> },
    { key: 'createdAt', label: 'Since', render: (c) => formatDate(c.createdAt) },
  ];

  return (
    <>
      <PageHeader title="Customers" subtitle="Self-registered individuals and businesses" />
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Name, code, email, phone, city"
          filters={[{ name: 'type', label: 'Type', options: CustomerType }, { name: 'status', label: 'Status', options: ActiveStatus }]} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(c) => navigate(`/customers/${c.id}`)} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  );
}
