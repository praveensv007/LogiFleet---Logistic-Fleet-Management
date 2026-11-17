import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthLayout from '../../components/layout/AuthLayout';
import FormBuilder from '../../components/common/FormBuilder';
import authService from '../../services/authService';
import useAuth from '../../hooks/useAuth';
import { CustomerType, INDIAN_STATES } from '../../utils/constants';
import { clean } from '../../utils/formatters';
import { PATTERNS, v, yup } from '../../utils/validators';

const schema = yup.object({
  fullName: v.name(),
  username: v.username(),
  email: v.email(),
  phone: v.phone(),
  password: v.password(),
  confirmPassword: v.confirm('password'),
  customerType: v.select('Customer type'),
  companyName: yup.string().trim().max(150).when('customerType', {
    is: 'BUSINESS', then: (s) => s.required('Company name is required for business customers'),
  }),
  gstNumber: yup.string().trim().uppercase()
    .test('gst', 'Enter a valid 15-character GSTIN', (x) => !x || PATTERNS.gstin.test(x)),
  addressLine1: v.req('Address line 1').max(200),
  addressLine2: v.opt(200),
  city: v.req('City'),
  state: v.select('State'),
  pincode: v.pincode(),
  country: v.req('Country'),
  acceptTerms: yup.boolean().oneOf([true], 'You must accept the terms'),
});

const fields = [
  { section: 'Account' },
  { name: 'fullName', label: 'Full name', required: true },
  { name: 'username', label: 'Username', required: true, help: '4-30 letters, digits, . or _' },
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'phone', label: 'Phone', required: true, placeholder: '10 digits' },
  { name: 'password', label: 'Password', type: 'password', required: true, autoComplete: 'new-password',
    help: 'Min 8 with upper, lower, digit and special character' },
  { name: 'confirmPassword', label: 'Confirm password', type: 'password', required: true, autoComplete: 'new-password' },
  { section: 'Customer details' },
  { name: 'customerType', label: 'Customer type', type: 'select', options: CustomerType, required: true, col: 4 },
  { name: 'companyName', label: 'Company name', col: 4, showIf: (x) => x.customerType === 'BUSINESS', required: true },
  { name: 'gstNumber', label: 'GST number', col: 4, placeholder: '22AAAAA0000A1Z5' },
  { section: 'Address' },
  { name: 'addressLine1', label: 'Address line 1', required: true },
  { name: 'addressLine2', label: 'Address line 2' },
  { name: 'city', label: 'City', required: true, col: 3 },
  { name: 'state', label: 'State', type: 'select', options: INDIAN_STATES.map((s) => ({ value: s, label: s })), required: true, col: 3 },
  { name: 'pincode', label: 'Pincode', required: true, col: 3 },
  { name: 'country', label: 'Country', required: true, col: 3 },
  { name: 'acceptTerms', label: 'I accept the terms of service and privacy policy', type: 'checkbox', required: true, col: 12 },
];

const defaults = {
  fullName: '', username: '', email: '', phone: '', password: '', confirmPassword: '', customerType: 'INDIVIDUAL',
  companyName: '', gstNumber: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', country: 'India',
  acceptTerms: false,
};

export default function Register() {
  const { loginWithResponse } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (values) => {
    const res = await authService.register(clean(values));
    loginWithResponse(res);
    toast.success('Welcome! Your customer account is ready.');
    navigate('/customer', { replace: true });
  };

  return (
    <AuthLayout title="Create your customer account" subtitle="Book shipments and track them live" wide>
      <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={onSubmit} submitLabel="Create account" />
      <p className="text-center mt-3">Already registered? <Link to="/login">Log in</Link></p>
    </AuthLayout>
  );
}
