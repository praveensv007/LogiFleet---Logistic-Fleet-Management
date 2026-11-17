import { useState } from 'react';
import { Alert } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import AuthLayout from '../../components/layout/AuthLayout';
import FormBuilder from '../../components/common/FormBuilder';
import authService from '../../services/authService';
import { v, yup } from '../../utils/validators';

const schema = yup.object({ email: v.email() });
const fields = [{ name: 'email', label: 'Email', type: 'email', required: true, col: 12 }];

export default function ForgotPassword() {
  const [sent, setSent] = useState(null);
  return (
    <AuthLayout title="Forgot password" subtitle="We'll email you a reset link valid for 30 minutes">
      {sent ? (
        <Alert variant="success"><i className="bi bi-envelope-check me-2" />{sent}</Alert>
      ) : (
        <FormBuilder fields={fields} schema={schema} defaultValues={{ email: '' }} submitLabel="Send reset link"
          onSubmit={async ({ email }) => setSent((await authService.forgotPassword(email)).message)} />
      )}
      <p className="text-center mt-3"><Link to="/login"><i className="bi bi-arrow-left me-1" />Back to login</Link></p>
    </AuthLayout>
  );
}
