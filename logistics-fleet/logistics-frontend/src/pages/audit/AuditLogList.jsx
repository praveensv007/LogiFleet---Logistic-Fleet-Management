import { useState } from 'react';
import { Button, Card, Col, Row } from 'react-bootstrap';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import SearchFilter from '../../components/common/SearchFilter';
import Modal from '../../components/common/Modal';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import auditService from '../../services/auditService';
import { AUDIT_MODULES, AuditAction, ROLE_LABEL } from '../../utils/constants';
import { formatDateTime, pretty } from '../../utils/formatters';

const prettyJson = (s) => {
  if (!s) return '—';
  try {
    return JSON.stringify(JSON.parse(s), null, 2);
  } catch {
    return s;
  }
};

export default function AuditLogList() {
  const [filters, setFilters] = useState({ search: '', module: '', action: '', from: '', to: '' });
  const [page, setPage] = useState(0);
  const [viewing, setViewing] = useState(null);
  const { data, loading, error, reload } = useFetch(() => auditService.search({
    user: filters.search, module: filters.module, action: filters.action, from: filters.from, to: filters.to, page, size: 20,
  }), [filters, page]);

  return (
    <>
      <PageHeader title="Audit logs" subtitle="Who did what, when, and from where" />
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Username"
          filters={[
            { name: 'module', label: 'Module', options: AUDIT_MODULES },
            { name: 'action', label: 'Action', options: AuditAction },
            { name: 'from', label: 'From', type: 'date' },
            { name: 'to', label: 'To', type: 'date' },
          ]} />
        <DataTable rows={data?.content} loading={loading} onRowClick={setViewing} columns={[
          { key: 'time', label: 'Time', render: (a) => formatDateTime(a.time) },
          { key: 'username', label: 'User', render: (a) => <span className="fw-semibold">{a.username}</span> },
          { key: 'role', label: 'Role', render: (a) => ROLE_LABEL[a.role] || a.role || '—' },
          { key: 'action', label: 'Action', render: (a) => <span className="badge text-bg-light border">{pretty(a.action)}</span> },
          { key: 'module', label: 'Module' },
          { key: 'entity', label: 'Record', render: (a) => (a.entityId ? `${a.entityName} #${a.entityId}` : a.entityName) },
          { key: 'ipAddress', label: 'IP', render: (a) => <span className="mono">{a.ipAddress}</span> },
          { key: 'v', label: '', render: () => <Button size="sm" variant="link">View</Button> },
        ]} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
      <Modal show={!!viewing} onHide={() => setViewing(null)} size="lg"
        title={viewing && `${pretty(viewing.action)} · ${viewing.module} · ${viewing.entityName ?? ''} ${viewing.entityId ? `#${viewing.entityId}` : ''}`}>
        {viewing && (
          <>
            <div className="small text-muted mb-2">{formatDateTime(viewing.time)} · {viewing.username} · {viewing.ipAddress} · {viewing.userAgent}</div>
            <Row className="g-2">
              <Col md={6}><div className="small fw-semibold mb-1">Old value</div><pre className="json-box">{prettyJson(viewing.oldValue)}</pre></Col>
              <Col md={6}><div className="small fw-semibold mb-1">New value</div><pre className="json-box">{prettyJson(viewing.newValue)}</pre></Col>
            </Row>
          </>
        )}
      </Modal>
    </>
  );
}
