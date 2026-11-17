import { useMemo } from 'react';
import { Card } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import warehouseService from '../../services/warehouseService';
import userService from '../../services/userService';
import { ActiveStatus, INDIAN_STATES } from '../../utils/constants';
import { clean, formatTime } from '../../utils/formatters';
import { PATTERNS, v, yup } from '../../utils/validators';

const KEYS = ['warehouseCode', 'name', 'addressLine1', 'city', 'state', 'pincode', 'country', 'latitude', 'longitude',
  'managerUserId', 'contactPhone', 'contactEmail', 'capacity', 'openingTime', 'closingTime', 'status'];

const schema = yup.object({
  warehouseCode: yup.string().trim().uppercase().required('Warehouse code is required')
    .matches(PATTERNS.warehouseCode, 'Use a code like WH-CHN-01'),
  name: v.req('Name').min(3, '3-100 characters').max(100),
  addressLine1: v.req('Address').max(200),
  city: v.req('City'),
  state: v.select('State'),
  pincode: v.pincode(),
  country: v.req('Country'),
  latitude: v.num('Latitude').min(-90, '-90 to 90').max(90, '-90 to 90').nullable(),
  longitude: v.num('Longitude').min(-180, '-180 to 180').max(180, '-180 to 180').nullable(),
  managerUserId: v.id('Manager'),
  contactPhone: v.phone(),
  contactEmail: v.email(),
  capacity: v.num('Capacity').required('Capacity is required').integer().moreThan(0, 'Must be greater than 0'),
  openingTime: v.req('Opening time'),
  closingTime: v.req('Closing time').test('after', 'Closing time must be after opening time',
    function after(x) { return !x || !this.parent.openingTime || x > this.parent.openingTime; }),
  status: v.select('Status'),
});

export default function WarehouseForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const warehouse = useFetch(() => (editing ? warehouseService.get(id) : Promise.resolve(null)), [id]);
  const managers = useFetch(async () => {
    const [staff, admins] = await Promise.all([
      userService.list({ role: 'WAREHOUSE_STAFF', size: 100 }), userService.list({ role: 'ADMIN', size: 100 })]);
    return [...staff.content, ...admins.content];
  }, [], { initial: [] });

  const fields = [
    { name: 'warehouseCode', label: 'Warehouse code', required: true, col: 4, placeholder: 'WH-CHN-01' },
    { name: 'name', label: 'Warehouse name', required: true, col: 8 },
    { name: 'addressLine1', label: 'Address', required: true, col: 12 },
    { name: 'city', label: 'City', required: true, col: 3 },
    { name: 'state', label: 'State', type: 'select', options: INDIAN_STATES.map((s) => ({ value: s, label: s })), required: true, col: 3 },
    { name: 'pincode', label: 'Pincode', required: true, col: 3 },
    { name: 'country', label: 'Country', required: true, col: 3 },
    { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', col: 3 },
    { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', col: 3 },
    { name: 'managerUserId', label: 'Manager', type: 'select', required: true, col: 6,
      options: managers.data.map((u) => ({ value: u.id, label: `${u.fullName} (${u.role === 'ADMIN' ? 'Admin' : 'Warehouse staff'})` })) },
    { name: 'contactPhone', label: 'Contact phone', required: true, col: 4 },
    { name: 'contactEmail', label: 'Contact email', type: 'email', required: true, col: 4 },
    { name: 'capacity', label: 'Total capacity (kg or pallets)', type: 'number', required: true, col: 4 },
    { name: 'openingTime', label: 'Opening time', type: 'time', required: true, col: 4 },
    { name: 'closingTime', label: 'Closing time', type: 'time', required: true, col: 4 },
    { name: 'status', label: 'Status', type: 'select', options: ActiveStatus, required: true, col: 4 },
  ];

  const defaults = useMemo(() => {
    if (!editing) return { ...Object.fromEntries(KEYS.map((k) => [k, ''])), country: 'India', status: 'ACTIVE', openingTime: '08:00', closingTime: '20:00' };
    if (!warehouse.data) return undefined;
    const w = warehouse.data;
    return { ...Object.fromEntries(KEYS.map((k) => [k, w[k] ?? ''])), openingTime: formatTime(w.openingTime), closingTime: formatTime(w.closingTime) };
  }, [editing, warehouse.data]);

  if (warehouse.loading) return <Loader />;
  if (warehouse.error) return <ErrorAlert error={warehouse.error} onRetry={warehouse.reload} />;

  const save = async (values) => {
    const body = clean(Object.fromEntries(KEYS.map((k) => [k, values[k]])));
    if (editing) await warehouseService.update(id, body);
    else await warehouseService.create(body);
    toast.success(editing ? 'Warehouse updated' : 'Warehouse created');
    navigate('/warehouses');
  };

  return (
    <>
      <PageHeader title={editing ? `Edit warehouse · ${warehouse.data?.warehouseCode}` : 'New warehouse'} />
      <Card>
        <Card.Body>
          {defaults && <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save}
            submitLabel={editing ? 'Save changes' : 'Create warehouse'} onCancel={() => navigate('/warehouses')} />}
        </Card.Body>
      </Card>
    </>
  );
}
