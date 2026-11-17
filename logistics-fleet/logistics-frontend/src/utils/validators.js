import * as yup from 'yup';

// Same rules as the Spring Boot DTOs (ValidationPatterns.java).
export const PATTERNS = {
  name: /^[A-Za-z][A-Za-z .]{2,99}$/,
  username: /^[A-Za-z0-9._]{4,30}$/,
  phone: /^[0-9]{10}$/,
  pincode: /^[0-9]{6}$/,
  strongPassword: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/,
  gstin: /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/,
  vehicleNumber: /^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$/,
  warehouseCode: /^[A-Z0-9-]{3,20}$/,
  otp: /^[0-9]{6}$/,
};

export const PASSWORD_MESSAGE = 'Min 8 characters with 1 upper, 1 lower, 1 digit and 1 special character';

/** Empty string -> undefined so optional number/date fields pass validation. */
const emptyToUndefined = (v, original) => (original === '' || original === null ? undefined : v);

export const v = {
  req: (label) => yup.string().trim().required(`${label} is required`),
  opt: (max = 255) => yup.string().trim().max(max, `Max ${max} characters`),
  name: () => yup.string().trim().required('Full name is required').matches(PATTERNS.name, '3-100 letters and spaces'),
  username: () => yup.string().trim().required('Username is required').matches(PATTERNS.username, '4-30 letters, digits, . or _'),
  email: (required = true) => {
    const s = yup.string().trim().email('Enter a valid email').max(100);
    return required ? s.required('Email is required') : s;
  },
  phone: (required = true) => {
    const s = yup.string().trim().test('phone', 'Phone must be 10 digits', (x) => !x || PATTERNS.phone.test(x));
    return required ? s.required('Phone is required') : s;
  },
  pincode: () => yup.string().trim().required('Pincode is required').matches(PATTERNS.pincode, 'Pincode must be 6 digits'),
  password: () => yup.string().required('Password is required').matches(PATTERNS.strongPassword, PASSWORD_MESSAGE),
  confirm: (ref = 'password') => yup.string().required('Confirm the password').oneOf([yup.ref(ref)], 'Passwords do not match'),
  select: (label) => yup.string().required(`${label} is required`),
  id: (label) => yup.number().transform(emptyToUndefined).typeError(`${label} is required`).required(`${label} is required`),
  optId: () => yup.number().transform(emptyToUndefined).nullable(),
  num: (label) => yup.number().transform(emptyToUndefined).typeError(`${label} must be a number`),
  date: (label) => yup.string().required(`${label} is required`),
  optDate: () => yup.string().nullable(),
  bool: () => yup.boolean(),
};

export const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
};

/** Pushes server-side field errors ({errors:[{field,message}]}) into react-hook-form. Returns true if any. */
export const applyServerErrors = (err, setError) => {
  const errors = err?.response?.data?.errors;
  if (!Array.isArray(errors) || !setError) return false;
  errors.forEach((e) => setError(e.field, { type: 'server', message: e.message }));
  return errors.length > 0;
};

export { yup };
