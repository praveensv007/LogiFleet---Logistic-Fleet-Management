import { Card, Col, Row } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import authService from '../../services/authService';
import useAuth from '../../hooks/useAuth';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  currentPassword: yup.string().required('Current password is required'),
  newPassword: v.password().notOneOf([yup.ref('currentPassword')], 'New password must be different'),
  confirmPassword: v.confirm('newPassword'),
});

const fields = [
  { name: 'currentPassword', label: 'Current password', type: 'password', required: true, col: 12, autoComplete: 'current-password' },
  { name: 'newPassword', label: 'New password', type: 'password', required: true, col: 12, autoComplete: 'new-password',
    help: 'Min 8 with upper, lower, digit and special character' },
  { name: 'confirmPassword', label: 'Confirm new password', type: 'password', required: true, col: 12, autoComplete: 'new-password' },
];

export default function ChangePassword() {
  const { updateUser, homePath } = useAuth();
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Change password" />
      <Row>
        <Col lg={6}>
          <Card>
            <Card.Body>
              <FormBuilder fields={fields} schema={schema}
                defaultValues={{ currentPassword: '', newPassword: '', confirmPassword: '' }}
                submitLabel="Change password"
                onSubmit={async (values, form) => {
                  const res = await authService.changePassword(values);
                  toast.success(res.message);
                  updateUser({ mustChangePassword: false });
                  form.reset();
                  navigate(homePath);
                }} />
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );
}
