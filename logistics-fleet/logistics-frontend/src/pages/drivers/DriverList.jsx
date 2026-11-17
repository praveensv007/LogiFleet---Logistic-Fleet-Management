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
import driverService from '../../services/driverService';
import { DriverAvailability } from '../../utils/constants';
import { fileUrl, formatDate, pretty } from '../../utils/formatters';

export default function DriverList() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ search: '', availability: '' });
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => driverService.list({ ...filters, page, size: 10 }), [filters, page]);

  const columns = [
    { key: 'driverCode', label: 'Code', render: (d) => <span className="mono">{d.driverCode}</span> },
    {
      key: 'fullName', label: 'Driver', render: (d) => (
        <div className="d-flex align-items-center gap-2">
          {d.photoUrl ? <img src={fileUrl(d.photoUrl)} alt="" width={32} height={32} className="rounded-circle object-fit-cover" />
            : <i className="bi bi-person-circle fs-4 text-muted" />}
          <div><div className="fw-semibold">{d.fullName}</div><div className="small text-muted">{d.phone}</div></div>
        </div>
      ),
    },
    { key: 'license', label: 'License', render: (d) => <><div className="mono">{d.licenseNumber}</div><div className="small text-muted">{d.licenseType}</div></> },
    {
      key: 'licenseExpiryDate', label: 'License expiry', render: (d) => (
        <span className={d.licenseValid ? '' : 'text-danger fw-semibold'}>{formatDate(d.licenseExpiryDate)}{!d.licenseValid && ' (expired)'}</span>
      ),
    },
    { key: 'experienceYears', label: 'Experience', render: (d) => `${d.experienceYears} yrs` },
    { key: 'city', label: 'City' },
    { key: 'availabilityStatus', label: 'Availability', render: (d) => <StatusBadge status={d.availabilityStatus} /> },
  ];

  return (
    <>
      <PageHeader title="Drivers" subtitle="Licenses, availability and trips">
        <Button as={Link} to="/drivers/new"><i className="bi bi-plus-lg me-1" />New driver</Button>
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Name, code, license or phone"
          filters={[{ name: 'availability', label: 'Availability', options: DriverAvailability.map((x) => ({ value: x, label: pretty(x) })) }]} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(d) => navigate(`/drivers/${d.id}`)} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  );
}
