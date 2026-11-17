import { useMemo } from 'react';
import { Card, Col, Row } from 'react-bootstrap';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import authService from '../../services/authService';
import customerService from '../../services/customerService';
import { CustomerEditForm } from '../customers/CustomerForm';
import { ROLE_LABEL } from '../../utils/constants';
import { clean, formatDateTime } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({ fullName: v.name(), phone: v.phone(), profilePhotoUrl: yup.string().nullable() });

const fields = [
  { name: 'fullName', label: 'Full name', required: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'email', label: 'Email', disabled: true, help: 'Contact the administrator to change your email' },
  { name: 'username', label: 'Username', disabled: true },
  { name: 'profilePhotoUrl', label: 'Profile photo', type: 'file', category: 'photo', col: 12 },
];

export default function MyProfile() {
  const { user, updateUser } = useAuth();
  const { data: me, loading, error, reload } = useFetch(() => authService.me(), []);
  const customer = useFetch(() => (user.role === 'CUSTOMER' ? customerService.me() : Promise.resolve(null)), [user.role]);

  const defaults = useMemo(() => (me ? {
    fullName: me.fullName, phone: me.phone || '', email: me.email, username: me.username, profilePhotoUrl: me.profilePhotoUrl || '',
  } : undefined), [me]);

  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;

  const save = async (values) => {
    const res = await authService.updateProfile(clean({
      fullName: values.fullName, phone: values.phone, profilePhotoUrl: values.profilePhotoUrl,
    }));
    updateUser({ fullName: res.fullName, profilePhotoUrl: res.profilePhotoUrl });
    toast.success('Profile updated');
    reload(true);
  };

  return (
    <>
      <PageHeader title="My profile" subtitle={`${ROLE_LABEL[me.role]} · last login ${formatDateTime(me.lastLoginAt)}`} />
      <Row className="g-3">
        <Col lg={7}>
          <Card>
            <Card.Header>Personal details</Card.Header>
            <Card.Body>
              <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save} submitLabel="Save profile" />
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card>
            <Card.Header>Access</Card.Header>
            <Card.Body>
              <div className="mb-2"><span className="text-muted">Role:</span> <strong>{ROLE_LABEL[me.role]}</strong></div>
              {me.warehouseName && <div className="mb-2"><span className="text-muted">Warehouse:</span> {me.warehouseName}</div>}
              <div className="text-muted small mb-1">Permissions</div>
              <div className="d-flex flex-wrap gap-1">
                {(me.permissions || []).map((p) => <span key={p} className="badge text-bg-light border">{p}</span>)}
              </div>
            </Card.Body>
          </Card>
        </Col>
        {customer.data && (
          <Col xs={12}>
            <Card>
              <Card.Header>Customer details · {customer.data.customerCode}</Card.Header>
              <Card.Body>
                <CustomerEditForm customer={customer.data} staff={false} onSaved={() => customer.reload(true)} />
              </Card.Body>
            </Card>
          </Col>
        )}
      </Row>
    </>
  );
}
