import { useMemo } from 'react';
import { Card } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import vehicleService from '../../services/vehicleService';
import warehouseService from '../../services/warehouseService';
import { FuelType, VehicleStatus, VehicleType } from '../../utils/constants';
import { clean, isoDate } from '../../utils/formatters';
import { PATTERNS, v, yup } from '../../utils/validators';

const KEYS = ['vehicleNumber', 'vehicleType', 'make', 'model', 'manufactureYear', 'fuelType', 'capacityKg', 'capacityCbm',
  'chassisNumber', 'engineNumber', 'insuranceNumber', 'insuranceExpiry', 'permitExpiry', 'fitnessExpiry', 'pucExpiry',
  'odometerKm', 'homeWarehouseId', 'status'];

export default function VehicleForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const vehicle = useFetch(() => (editing ? vehicleService.get(id) : Promise.resolve(null)), [id]);
  const warehouses = useFetch(() => warehouseService.list('ACTIVE'), [], { initial: [] });
  const todayStr = isoDate();
  const year = new Date().getFullYear();
  const minOdometer = vehicle.data?.odometerKm ?? 0;

  const schema = useMemo(() => yup.object({
    vehicleNumber: yup.string().trim().uppercase().required('Vehicle number is required')
      .transform((x) => (x ? x.replace(/[\s-]/g, '') : x))
      .matches(PATTERNS.vehicleNumber, 'Use a registration like TN09AB1234'),
    vehicleType: v.select('Vehicle type'),
    make: v.req('Make').max(50),
    model: v.req('Model').max(50),
    manufactureYear: v.num('Year').required('Year is required').integer().min(1990, 'From 1990').max(year, `Up to ${year}`),
    fuelType: v.select('Fuel type'),
    capacityKg: v.num('Capacity').required('Capacity is required').moreThan(0, 'Must be greater than 0'),
    capacityCbm: v.num('Capacity').moreThan(0, 'Must be greater than 0').nullable(),
    chassisNumber: v.req('Chassis number').max(30),
    engineNumber: v.req('Engine number').max(30),
    insuranceNumber: v.req('Insurance policy number').max(30),
    insuranceExpiry: v.date('Insurance expiry').test('future', 'Expiry must be after today', (x) => !x || x > todayStr),
    permitExpiry: v.date('Permit expiry'),
    fitnessExpiry: v.date('Fitness expiry'),
    pucExpiry: v.date('PUC expiry'),
    odometerKm: v.num('Odometer').required('Odometer is required').integer().min(minOdometer, `Must be ≥ ${minOdometer}`),
    homeWarehouseId: v.optId(),
    status: v.select('Status'),
  }), [todayStr, year, minOdometer]);

  const statusOptions = VehicleStatus.filter((x) => x !== 'ON_TRIP' || vehicle.data?.status === 'ON_TRIP');
  const fields = [
    { section: 'Vehicle' },
    { name: 'vehicleNumber', label: 'Vehicle number (registration)', required: true, col: 4, placeholder: 'TN09AB1234' },
    { name: 'vehicleType', label: 'Vehicle type', type: 'select', options: VehicleType, required: true, col: 4 },
    { name: 'fuelType', label: 'Fuel type', type: 'select', options: FuelType, required: true, col: 4 },
    { name: 'make', label: 'Make', required: true, col: 4, placeholder: 'Tata' },
    { name: 'model', label: 'Model', required: true, col: 4, placeholder: 'Ace' },
    { name: 'manufactureYear', label: 'Manufacture year', type: 'number', required: true, col: 4, min: 1990, max: year },
    { name: 'capacityKg', label: 'Capacity (kg)', type: 'number', required: true, col: 4, step: '0.01' },
    { name: 'capacityCbm', label: 'Capacity (m³)', type: 'number', col: 4, step: '0.01' },
    { name: 'odometerKm', label: 'Current odometer (km)', type: 'number', required: true, col: 4 },
    { name: 'chassisNumber', label: 'Chassis number', required: true, col: 6 },
    { name: 'engineNumber', label: 'Engine number', required: true, col: 6 },
    { section: 'Insurance & compliance' },
    { name: 'insuranceNumber', label: 'Insurance policy number', required: true, col: 4 },
    { name: 'insuranceExpiry', label: 'Insurance expiry', type: 'date', required: true, col: 4, min: todayStr },
    { name: 'permitExpiry', label: 'Permit expiry', type: 'date', required: true, col: 4 },
    { name: 'fitnessExpiry', label: 'Fitness expiry', type: 'date', required: true, col: 4 },
    { name: 'pucExpiry', label: 'PUC expiry', type: 'date', required: true, col: 4 },
    { section: 'Operations' },
    { name: 'homeWarehouseId', label: 'Home warehouse', type: 'select', col: 6, placeholder: 'None',
      options: warehouses.data.map((w) => ({ value: w.id, label: `${w.name} (${w.warehouseCode})` })) },
    { name: 'status', label: 'Status', type: 'select', options: statusOptions, required: true, col: 6 },
  ];

  const defaults = useMemo(() => {
    if (!editing) return { ...Object.fromEntries(KEYS.map((k) => [k, ''])), status: 'AVAILABLE', odometerKm: 0 };
    if (!vehicle.data) return undefined;
    return Object.fromEntries(KEYS.map((k) => [k, vehicle.data[k] ?? '']));
  }, [editing, vehicle.data]);

  if (vehicle.loading) return <Loader />;
  if (vehicle.error) return <ErrorAlert error={vehicle.error} onRetry={vehicle.reload} />;

  const save = async (values) => {
    const body = clean(Object.fromEntries(KEYS.map((k) => [k, values[k]])));
    if (editing) {
      await vehicleService.update(id, body);
      toast.success('Vehicle updated');
      navigate(`/vehicles/${id}`);
    } else {
      const res = await vehicleService.create(body);
      toast.success('Vehicle added. Upload RC, insurance and permit on the next screen.');
      navigate(`/vehicles/${res.id}`);
    }
  };

  return (
    <>
      <PageHeader title={editing ? `Edit vehicle · ${vehicle.data?.vehicleNumber}` : 'New vehicle'} />
      <Card>
        <Card.Body>
          {defaults && <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save}
            submitLabel={editing ? 'Save changes' : 'Add vehicle'} onCancel={() => navigate(-1)} />}
        </Card.Body>
      </Card>
    </>
  );
}
