import { useMemo } from 'react';
import { Alert, Card } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import userService from '../../services/userService';
import warehouseService from '../../services/warehouseService';
import { ROLE_LABEL, ROLES, UserStatus } from '../../utils/constants';
import { clean } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

export default function UserForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const user = useFetch(() => (editing ? userService.get(id) : Promise.resolve(null)), [id]);
  const warehouses = useFetch(() => warehouseService.list('ACTIVE'), [], { initial: [] });

  const schema = useMemo(() => yup.object({
    fullName: v.name(),
    username: v.username(),
    email: v.email(),
    phone: v.phone(),
    role: v.select('Role'),
    warehouseId: v.optId().when('role', {
      is: 'WAREHOUSE_STAFF', then: (s) => s.required('Assigned warehouse is required'),
    }),
    temporaryPassword: editing ? yup.string() : v.password(),
    status: v.select('Status'),
  }), [editing]);

  // Driver and customer accounts keep their role; a new user cannot be a driver (use Driver management).
  const lockedRole = editing && ['DRIVER', 'CUSTOMER'].includes(user.data?.role) ? user.data.role : null;
  const roleOptions = Object.values(ROLES)
    .filter((r) => (lockedRole ? r === lockedRole : r !== ROLES.DRIVER && (!editing || r !== ROLES.CUSTOMER)))
    .map((r) => ({ value: r, label: ROLE_LABEL[r] }));

  const fields = [
    { name: 'fullName', label: 'Full name', required: true },
    { name: 'username', label: 'Username', required: true },
    { name: 'email', label: 'Email', type: 'email', required: true },
    { name: 'phone', label: 'Phone', required: true },
    { name: 'role', label: 'Role', type: 'select', options: roleOptions, required: true,
      help: lockedRole ? 'Driver and customer accounts cannot change role' : undefined },
    { name: 'warehouseId', label: 'Assigned warehouse', type: 'select', required: true,
      options: warehouses.data.map((w) => ({ value: w.id, label: `${w.name} (${w.warehouseCode})` })),
      showIf: (x) => x.role === 'WAREHOUSE_STAFF' },
    ...(editing ? [] : [{ name: 'temporaryPassword', label: 'Temporary password', type: 'password', required: true,
      help: 'The user must change it on first login', autoComplete: 'new-password' }]),
    { name: 'status', label: 'Status', type: 'select', options: UserStatus, required: true },
  ];

  const defaults = useMemo(() => (editing ? (user.data && {
    fullName: user.data.fullName, username: user.data.username, email: user.data.email, phone: user.data.phone || '',
    role: user.data.role, warehouseId: user.data.warehouseId ?? '', status: user.data.status,
  }) : { fullName: '', username: '', email: '', phone: '', role: '', warehouseId: '', temporaryPassword: '', status: 'ACTIVE' }),
  [editing, user.data]);

  if (user.loading) return <Loader />;
  if (user.error) return <ErrorAlert error={user.error} onRetry={user.reload} />;

  const save = async (values) => {
    const body = clean({ ...values, warehouseId: values.role === 'WAREHOUSE_STAFF' ? values.warehouseId : null });
    if (editing) {
      delete body.temporaryPassword;
      await userService.update(id, body);
      toast.success('User updated');
    } else {
      await userService.create(body);
      toast.success('User created. Share the temporary password with them.');
    }
    navigate('/users');
  };

  return (
    <>
      <PageHeader title={editing ? `Edit user · ${user.data?.username}` : 'New user'} />
      {!editing && (
        <Alert variant="info" className="small">
          Drivers are created from <strong>Driver management</strong> so their license details are captured.
          Customers normally register themselves; a customer created here gets a placeholder address to complete later.
        </Alert>
      )}
      <Card>
        <Card.Body>
          {defaults && (
            <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save}
              submitLabel={editing ? 'Save changes' : 'Create user'} onCancel={() => navigate('/users')} />
          )}
        </Card.Body>
      </Card>
    </>
  );
}
