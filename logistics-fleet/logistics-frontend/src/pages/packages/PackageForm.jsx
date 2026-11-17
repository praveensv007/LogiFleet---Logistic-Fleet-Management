import FormBuilder from '../../components/common/FormBuilder';
import { PackageType } from '../../utils/constants';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  description: v.req('Package description').max(200),
  packageType: v.select('Package type'),
  quantity: v.num('Quantity').required('Quantity is required').integer().min(1, 'At least 1'),
  weightKg: v.num('Weight').required('Weight is required').moreThan(0, 'Must be greater than 0'),
  lengthCm: v.num('Length').required('Length is required').moreThan(0, 'Must be greater than 0'),
  widthCm: v.num('Width').required('Width is required').moreThan(0, 'Must be greater than 0'),
  heightCm: v.num('Height').required('Height is required').moreThan(0, 'Must be greater than 0'),
  fragile: yup.boolean(),
});

const fields = [
  { name: 'description', label: 'Package description', required: true, col: 8 },
  { name: 'packageType', label: 'Package type', type: 'select', options: PackageType, required: true, col: 4 },
  { name: 'quantity', label: 'Quantity', type: 'number', required: true, col: 3, min: 1 },
  { name: 'weightKg', label: 'Weight (kg)', type: 'number', required: true, col: 3, step: '0.01' },
  { name: 'lengthCm', label: 'Length (cm)', type: 'number', required: true, col: 2, step: '0.1' },
  { name: 'widthCm', label: 'Width (cm)', type: 'number', required: true, col: 2, step: '0.1' },
  { name: 'heightCm', label: 'Height (cm)', type: 'number', required: true, col: 2, step: '0.1' },
  { name: 'fragile', label: 'Fragile', type: 'checkbox', col: 12 },
];

const empty = { description: '', packageType: 'BOX', quantity: 1, weightKg: '', lengthCm: '', widthCm: '', heightCm: '', fragile: false };

/** Create / edit one package row (used inside a modal on PackageList). */
export default function PackageForm({ pkg, onSubmit, onCancel }) {
  const defaults = pkg ? Object.fromEntries(Object.keys(empty).map((k) => [k, pkg[k] ?? empty[k]])) : empty;
  return <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={onSubmit} onCancel={onCancel}
    submitLabel={pkg ? 'Save package' : 'Add package'} />;
}
