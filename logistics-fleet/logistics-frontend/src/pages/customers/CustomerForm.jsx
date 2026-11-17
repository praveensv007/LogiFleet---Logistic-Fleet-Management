import { useMemo } from 'react';
import { Card } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import customerService from '../../services/customerService';
import { ActiveStatus, CustomerType, INDIAN_STATES } from '../../utils/constants';
import { clean } from '../../utils/formatters';
import { PATTERNS, v, yup } from '../../utils/validators';

const schema = yup.object({
  fullName: v.name(),
  email: v.email(),
  phone: v.phone(),
  alternatePhone: v.phone(false),
  customerType: v.select('Customer type'),
  companyName: yup.string().trim().max(150).when('customerType', {
    is: 'BUSINESS', then: (s) => s.required('Company name is required for business customers'),
  }),
  gstNumber: yup.string().trim().uppercase().test('gst', 'Enter a valid 15-character GSTIN', (x) => !x || PATTERNS.gstin.test(x)),
  addressLine1: v.req('Address line 1').max(200),
  addressLine2: v.opt(200),
  city: v.req('City'),
  state: v.select('State'),
  pincode: v.pincode(),
  country: v.req('Country'),
  status: yup.string(),
});

const stateOptions = INDIAN_STATES.map((s) => ({ value: s, label: s }));

/** Reused by the admin edit page and the customer's own profile. */
export function CustomerEditForm({ customer, staff, onSaved, onCancel }) {
  const fields = [
    { name: 'customerCode', label: 'Customer code', disabled: true, col: 4 },
    { name: 'fullName', label: 'Full name', required: true, col: 8 },
    { name: 'email', label: 'Email', type: 'email', required: true, col: 4 },
    { name: 'phone', label: 'Phone', required: true, col: 4 },
    { name: 'alternatePhone', label: 'Alternate phone', col: 4 },
    { name: 'customerType', label: 'Customer type', type: 'select', options: CustomerType, required: true, col: 4 },
    { name: 'companyName', label: 'Company name', col: 4, required: true, showIf: (x) => x.customerType === 'BUSINESS' },
    { name: 'gstNumber', label: 'GST number', col: 4 },
    { name: 'addressLine1', label: 'Address line 1', required: true },
    { name: 'addressLine2', label: 'Address line 2' },
    { name: 'city', label: 'City', required: true, col: 3 },
    { name: 'state', label: 'State', type: 'select', options: stateOptions, required: true, col: 3 },
    { name: 'pincode', label: 'Pincode', required: true, col: 3 },
    { name: 'country', label: 'Country', required: true, col: 3 },
    ...(staff ? [{ name: 'status', label: 'Status', type: 'select', options: ActiveStatus, required: true, col: 3 }] : []),
  ];

  const defaults = useMemo(() => ({
    customerCode: customer.customerCode, fullName: customer.fullName, email: customer.email, phone: customer.phone,
    alternatePhone: customer.alternatePhone || '', customerType: customer.customerType, companyName: customer.companyName || '',
    gstNumber: customer.gstNumber || '', addressLine1: customer.addressLine1, addressLine2: customer.addressLine2 || '',
    city: customer.city, state: INDIAN_STATES.includes(customer.state) ? customer.state : '', pincode: customer.pincode,
    country: customer.country, status: customer.status,
  }), [customer]);

  const save = async (values) => {
    const body = clean({ ...values });
    delete body.customerCode;
    if (!staff) delete body.status;
    const res = await customerService.update(customer.id, body);
    toast.success('Customer details saved');
    onSaved?.(res);
  };

  return <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save} onCancel={onCancel} submitLabel="Save details" />;
}

export default function CustomerForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, loading, error, reload } = useFetch(() => customerService.get(id), [id]);
  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;
  return (
    <>
      <PageHeader title={`Edit customer · ${data.customerCode}`} />
      <Card>
        <Card.Body>
          <CustomerEditForm customer={data} staff onSaved={() => navigate(`/customers/${id}`)} onCancel={() => navigate(-1)} />
        </Card.Body>
      </Card>
    </>
  );
}
