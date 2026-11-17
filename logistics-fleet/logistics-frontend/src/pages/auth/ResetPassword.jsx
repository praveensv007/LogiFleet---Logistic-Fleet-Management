import { Alert } from 'react-bootstrap';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthLayout from '../../components/layout/AuthLayout';
import FormBuilder from '../../components/common/FormBuilder';
import authService from '../../services/authService';
import { v, yup } from '../../utils/validators';

const schema = yup.object({ newPassword: v.password(), confirmPassword: v.confirm('newPassword') });
const fields = [
  { name: 'newPassword', label: 'New password', type: 'password', required: true, col: 12, autoComplete: 'new-password' },
  { name: 'confirmPassword', label: 'Confirm new password', type: 'password', required: true, col: 12, autoComplete: 'new-password' },
];

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token');

  return (
    <AuthLayout title="Choose a new password">
      {!token ? (
        <Alert variant="danger">This reset link is missing its token. <Link to="/forgot-password">Request a new link</Link>.</Alert>
      ) : (
        <FormBuilder fields={fields} schema={schema} defaultValues={{ newPassword: '', confirmPassword: '' }}
          submitLabel="Reset password"
          onSubmit={async (values) => {
            const res = await authService.resetPassword({ ...values, token });
            toast.success(res.message);
            navigate('/login', { replace: true });
          }} />
      )}
    </AuthLayout>
  );
}
