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
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import userService from '../../services/userService';
import { ROLE_LABEL, ROLES, UserStatus } from '../../utils/constants';
import { errorMessage, formatDateTime } from '../../utils/formatters';

export default function UserList() {
  const navigate = useNavigate();
  const { user: me } = useAuth();
  const [filters, setFilters] = useState({ search: '', role: '', status: '' });
  const [page, setPage] = useState(0);
  const [confirm, setConfirm] = useState(null);
  const [tempPassword, setTempPassword] = useState(null);
  const { data, loading, error, reload } = useFetch(
    () => userService.list({ ...filters, page, size: 10 }), [filters, page],
  );

  const toggleStatus = async (u) => {
    try {
      await userService.setStatus(u.id, u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
      toast.success('Status updated');
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setConfirm(null);
    }
  };

  const resetPassword = async (u) => {
    try {
      const res = await userService.resetPassword(u.id);
      setTempPassword({ user: u, password: res.temporaryPassword });
      reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setConfirm(null);
    }
  };

  const columns = [
    { key: 'username', label: 'Username', render: (u) => <span className="fw-semibold">{u.username}</span> },
    { key: 'fullName', label: 'Full name' },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Role', render: (u) => ROLE_LABEL[u.role] || u.role },
    { key: 'status', label: 'Status', render: (u) => <StatusBadge status={u.status} /> },
    { key: 'lastLoginAt', label: 'Last login', render: (u) => formatDateTime(u.lastLoginAt) },
    {
      key: 'actions', label: '', className: 'text-end', render: (u) => (
        <ButtonGroup size="sm" onClick={(e) => e.stopPropagation()}>
          <Button variant="outline-primary" as={Link} to={`/users/${u.id}/edit`} title="Edit"><i className="bi bi-pencil" /></Button>
          <Button variant="outline-secondary" title="Reset password"
            onClick={() => setConfirm({ type: 'reset', user: u })}><i className="bi bi-key" /></Button>
          {u.id !== me.userId && (
            <Button variant={u.status === 'ACTIVE' ? 'outline-danger' : 'outline-success'}
              title={u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
              onClick={() => setConfirm({ type: 'status', user: u })}>
              <i className={`bi bi-${u.status === 'ACTIVE' ? 'person-x' : 'person-check'}`} />
            </Button>
          )}
        </ButtonGroup>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Users" subtitle="Staff accounts, roles and access">
        <Button as={Link} to="/roles" variant="outline-secondary"><i className="bi bi-shield-lock me-1" />Roles &amp; permissions</Button>
        <Button as={Link} to="/users/new"><i className="bi bi-plus-lg me-1" />New user</Button>
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        <SearchFilter value={filters} onChange={(f) => { setFilters(f); setPage(0); }} placeholder="Username, name or email"
          filters={[
            { name: 'role', label: 'Role', options: Object.values(ROLES).map((r) => ({ value: r, label: ROLE_LABEL[r] })) },
            { name: 'status', label: 'Status', options: UserStatus },
          ]} />
        <DataTable columns={columns} rows={data?.content} loading={loading} onRowClick={(u) => navigate(`/users/${u.id}/edit`)} />
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>

      <ConfirmDialog show={!!confirm} onHide={() => setConfirm(null)}
        title={confirm?.type === 'reset' ? 'Reset password' : 'Change status'}
        variant={confirm?.type === 'reset' ? 'primary' : 'danger'}
        confirmLabel={confirm?.type === 'reset' ? 'Generate temporary password'
          : confirm?.user.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        message={confirm?.type === 'reset'
          ? `${confirm?.user.fullName} will get a temporary password and must change it on next login.`
          : `${confirm?.user.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} ${confirm?.user.fullName}?`}
        onConfirm={() => (confirm.type === 'reset' ? resetPassword(confirm.user) : toggleStatus(confirm.user))} />

      <Modal show={!!tempPassword} onHide={() => setTempPassword(null)} title="Temporary password"
        footer={<Button onClick={() => setTempPassword(null)}>Done</Button>}>
        <p>Share this password with <strong>{tempPassword?.user.fullName}</strong>. It is shown only once.</p>
        <div className="d-flex gap-2 align-items-center">
          <code className="fs-5 p-2 bg-light rounded flex-grow-1 text-center">{tempPassword?.password}</code>
          <Button variant="outline-secondary" onClick={() => { navigator.clipboard?.writeText(tempPassword.password); toast.info('Copied'); }}>
            <i className="bi bi-clipboard" />
          </Button>
        </div>
      </Modal>
    </>
  );
}
