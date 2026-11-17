import { useState } from 'react';
import { Alert, Button, Form, InputGroup, Spinner } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/layout/AuthLayout';
import useAuth from '../../hooks/useAuth';
import { yup } from '../../utils/validators';
import { ROLE_HOME } from '../../utils/constants';
import { errorMessage } from '../../utils/formatters';

const schema = yup.object({
  usernameOrEmail: yup.string().trim().required('Username or email is required')
    .test('fmt', 'Enter 4-50 characters or a valid email', (v) =>
      !v || (v.includes('@') ? yup.string().email().isValidSync(v) : v.length >= 4 && v.length <= 50)),
  password: yup.string().required('Password is required').min(8, 'Password must be 8-64 characters').max(64),
  rememberMe: yup.boolean(),
});

const DEMO = [
  ['admin', 'Admin@123'], ['fleet', 'Demo@1234'], ['warehouse', 'Demo@1234'], ['driver', 'Demo@1234'], ['customer', 'Demo@1234'],
];

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState(null);
  const [show, setShow] = useState(false);
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm({
    resolver: yupResolver(schema), defaultValues: { usernameOrEmail: '', password: '', rememberMe: false },
  });

  if (user) return <Navigate to={ROLE_HOME[user.role] || '/'} replace />;

  const onSubmit = async (values) => {
    setError(null);
    try {
      const profile = await login(values);
      const from = location.state?.from?.pathname;
      navigate(from && from !== '/' ? from : ROLE_HOME[profile.role], { replace: true });
    } catch (e) {
      setError(errorMessage(e, 'Login failed'));
    }
  };

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to continue to your dashboard">
      {location.state?.expired && <Alert variant="warning">Your session expired. Please log in again.</Alert>}
      {error && <Alert variant="danger">{error}</Alert>}
      <Form noValidate onSubmit={handleSubmit(onSubmit)}>
        <Form.Group className="mb-3">
          <Form.Label>Username or email</Form.Label>
          <Form.Control autoFocus autoComplete="username" {...register('usernameOrEmail')} isInvalid={!!errors.usernameOrEmail} />
          <Form.Control.Feedback type="invalid">{errors.usernameOrEmail?.message}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Password</Form.Label>
          <InputGroup hasValidation>
            <Form.Control type={show ? 'text' : 'password'} autoComplete="current-password" {...register('password')}
              isInvalid={!!errors.password} />
            <Button variant="outline-secondary" onClick={() => setShow((s) => !s)} tabIndex={-1}>
              <i className={`bi bi-eye${show ? '-slash' : ''}`} />
            </Button>
            <Form.Control.Feedback type="invalid">{errors.password?.message}</Form.Control.Feedback>
          </InputGroup>
        </Form.Group>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <Form.Check id="rememberMe" label="Remember me" {...register('rememberMe')} />
          <Link to="/forgot-password" className="small">Forgot password?</Link>
        </div>
        <Button type="submit" className="w-100" size="lg" disabled={isSubmitting}>
          {isSubmitting && <Spinner size="sm" className="me-2" />}Log in
        </Button>
      </Form>
      <p className="text-center mt-4 mb-2">New customer? <Link to="/register">Create an account</Link></p>
      <div className="border rounded p-2 small text-muted mt-3">
        <div className="mb-1">Demo logins (seeded on first start):</div>
        <div className="d-flex flex-wrap gap-1">
          {DEMO.map(([u, p]) => (
            <Button key={u} size="sm" variant="light" className="border"
              onClick={() => { setValue('usernameOrEmail', u); setValue('password', p); }}>{u}</Button>
          ))}
        </div>
      </div>
    </AuthLayout>
  );
}
