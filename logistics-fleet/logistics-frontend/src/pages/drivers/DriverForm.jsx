import { useMemo } from 'react';
import { Card } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import driverService from '../../services/driverService';
import { DriverAvailability, Gender, IdProofType, INDIAN_STATES, LicenseType } from '../../utils/constants';
import { clean, isoDate } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const ageBetween = (dob, min, max) => {
  if (!dob) return false;
  const d = new Date(dob);
  const now = new Date();
  let age = now.getFullYear() - d.getFullYear();
  if (now < new Date(now.getFullYear(), d.getMonth(), d.getDate())) age -= 1;
  return age >= min && age <= max;
};

export default function DriverForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const driver = useFetch(() => (editing ? driverService.get(id) : Promise.resolve(null)), [id]);
  const todayStr = isoDate();

  const schema = useMemo(() => yup.object({
    username: editing ? yup.string() : v.username(),
    email: v.email(),
    temporaryPassword: editing ? yup.string() : v.password(),
    fullName: v.name(),
    dateOfBirth: v.date('Date of birth').test('age', 'Driver age must be between 18 and 65', (x) => ageBetween(x, 18, 65)),
    gender: yup.string(),
    phone: v.phone(),
    address: v.req('Address').max(255),
    city: v.req('City'),
    state: v.select('State'),
    pincode: v.pincode(),
    licenseNumber: v.req('License number').min(15, 'License number must be 15-16 characters').max(16, 'License number must be 15-16 characters'),
    licenseType: v.select('License type'),
    licenseIssueDate: v.date('Issue date').test('past', 'Issue date cannot be in the future', (x) => !x || x <= todayStr),
    licenseExpiryDate: v.date('Expiry date').test('future', 'Expiry must be after today', (x) => !x || x > todayStr),
    licenseDocUrl: yup.string().required('Upload the license copy'),
    idProofType: v.select('ID proof type'),
    idProofNumber: v.req('ID proof number').max(30),
    experienceYears: v.num('Experience').required('Experience is required').min(0, 'Min 0').max(45, 'Max 45').integer(),
    joiningDate: v.date('Joining date').test('past', 'Joining date cannot be in the future', (x) => !x || x <= todayStr),
    emergencyContactName: v.req('Emergency contact name'),
    emergencyContactPhone: v.phone(),
    photoUrl: yup.string().nullable(),
    availabilityStatus: v.select('Availability'),
  }), [editing, todayStr]);

  const fields = [
    { section: 'Login account' },
    { name: 'driverCode', label: 'Driver code', disabled: true, col: 4, showIf: () => editing },
    { name: 'username', label: 'Username', required: true, col: 4, disabled: editing },
    { name: 'email', label: 'Email', type: 'email', required: true, col: 4 },
    ...(editing ? [] : [{ name: 'temporaryPassword', label: 'Temporary password', type: 'password', required: true, col: 4,
      help: 'Driver must change it on first login', autoComplete: 'new-password' }]),
    { section: 'Personal' },
    { name: 'fullName', label: 'Full name', required: true, col: 4 },
    { name: 'dateOfBirth', label: 'Date of birth', type: 'date', required: true, col: 4, max: todayStr },
    { name: 'gender', label: 'Gender', type: 'select', options: Gender, col: 4 },
    { name: 'phone', label: 'Phone', required: true, col: 4 },
    { name: 'address', label: 'Address', required: true, col: 8 },
    { name: 'city', label: 'City', required: true, col: 4 },
    { name: 'state', label: 'State', type: 'select', options: INDIAN_STATES.map((s) => ({ value: s, label: s })), required: true, col: 4 },
    { name: 'pincode', label: 'Pincode', required: true, col: 4 },
    { name: 'photoUrl', label: 'Photo', type: 'file', category: 'photo', col: 12 },
    { section: 'License & ID' },
    { name: 'licenseNumber', label: 'License number', required: true, col: 4, placeholder: 'TN0920150012345' },
    { name: 'licenseType', label: 'License type', type: 'select', options: LicenseType, required: true, col: 4 },
    { name: 'licenseIssueDate', label: 'Issue date', type: 'date', required: true, col: 2, max: todayStr },
    { name: 'licenseExpiryDate', label: 'Expiry date', type: 'date', required: true, col: 2, min: todayStr },
    { name: 'licenseDocUrl', label: 'License copy', type: 'file', category: 'document', required: true, col: 12 },
    { name: 'idProofType', label: 'ID proof type', type: 'select', options: IdProofType, required: true, col: 4 },
    { name: 'idProofNumber', label: 'ID proof number', required: true, col: 4 },
    { section: 'Employment' },
    { name: 'experienceYears', label: 'Experience (years)', type: 'number', required: true, col: 3, min: 0, max: 45 },
    { name: 'joiningDate', label: 'Joining date', type: 'date', required: true, col: 3, max: todayStr },
    { name: 'availabilityStatus', label: 'Availability', type: 'select', required: true, col: 3,
      options: DriverAvailability.filter((x) => x !== 'ON_TRIP' || driver.data?.availabilityStatus === 'ON_TRIP') },
    { name: 'emergencyContactName', label: 'Emergency contact name', required: true, col: 6 },
    { name: 'emergencyContactPhone', label: 'Emergency contact phone', required: true, col: 6 },
  ];

  const defaults = useMemo(() => {
    if (!editing) {
      return { username: '', email: '', temporaryPassword: '', fullName: '', dateOfBirth: '', gender: '', phone: '',
        address: '', city: '', state: '', pincode: '', licenseNumber: '', licenseType: '', licenseIssueDate: '',
        licenseExpiryDate: '', licenseDocUrl: '', idProofType: '', idProofNumber: '', experienceYears: '', joiningDate: '',
        emergencyContactName: '', emergencyContactPhone: '', photoUrl: '', availabilityStatus: 'AVAILABLE' };
    }
    const d = driver.data;
    if (!d) return undefined;
    return { ...Object.fromEntries(Object.entries(d).map(([k, val]) => [k, val ?? ''])), recentTrips: undefined };
  }, [editing, driver.data]);

  if (driver.loading) return <Loader />;
  if (driver.error) return <ErrorAlert error={driver.error} onRetry={driver.reload} />;

  const save = async (values) => {
    const body = clean({
      ...values,
      licenseNumber: values.licenseNumber.toUpperCase(),
      gender: values.gender || null,
    });
    ['driverCode', 'recentTrips', 'age', 'licenseValid', 'userId', 'userStatus', 'id', 'currentLatitude', 'currentLongitude']
      .forEach((k) => delete body[k]);
    if (editing) {
      await driverService.update(id, body);
      toast.success('Driver updated');
      navigate(`/drivers/${id}`);
    } else {
      const res = await driverService.create(body);
      toast.success(`Driver ${res.driverCode} created`);
      navigate(`/drivers/${res.id}`);
    }
  };

  return (
    <>
      <PageHeader title={editing ? `Edit driver · ${driver.data?.driverCode}` : 'New driver'}
        subtitle={editing ? undefined : 'Creates the driver profile and their login in one step'} />
      <Card>
        <Card.Body>
          {defaults && <FormBuilder fields={fields} schema={schema} defaultValues={defaults} onSubmit={save}
            submitLabel={editing ? 'Save changes' : 'Create driver'} onCancel={() => navigate(-1)} />}
        </Card.Body>
      </Card>
    </>
  );
}
