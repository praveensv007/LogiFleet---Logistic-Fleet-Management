// import { useEffect, useMemo, useState } from 'react';
// import { Alert, Button, Card, Col, Form, Row, Spinner, Table } from 'react-bootstrap';
// import { useFieldArray, useForm } from 'react-hook-form';
// import { yupResolver } from '@hookform/resolvers/yup';
// import { useNavigate, useParams } from 'react-router-dom';
// import { toast } from 'react-toastify';
// import PageHeader from '../../components/common/PageHeader';
// import { FieldGrid, FormField, submitHandler } from '../../components/common/FormBuilder';
// import Loader, { ErrorAlert } from '../../components/common/Loader';
// import useFetch from '../../hooks/useFetch';
// import useAuth from '../../hooks/useAuth';
// import shipmentService from '../../services/shipmentService';
// import warehouseService from '../../services/warehouseService';
// import customerService from '../../services/customerService';
// import { INDIAN_STATES, PackageType, PaymentMode, PickupTimeSlot, ServiceType } from '../../utils/constants';
// import { clean, formatMoney, isoDate } from '../../utils/formatters';
// import { v, yup } from '../../utils/validators';

// const RATE = { STANDARD: 50, EXPRESS: 80, SAME_DAY: 120 };
// const DAYS = { STANDARD: 5, EXPRESS: 2, SAME_DAY: 0 };
// const stateOptions = INDIAN_STATES.map((s) => ({ value: s, label: s }));

// const address = (withEmail) => yup.object({
//   contactName: v.req('Contact name').max(100),
//   contactPhone: v.phone(),
//   contactEmail: withEmail ? v.email(false) : yup.string().nullable(),
//   addressLine1: v.req('Address line 1').max(200),
//   addressLine2: v.opt(200),
//   landmark: v.opt(150),
//   city: v.req('City'),
//   state: v.select('State'),
//   pincode: v.pincode(),
// });

// const packageSchema = yup.object({
//   description: v.req('Description').max(200),
//   packageType: v.select('Type'),
//   quantity: v.num('Quantity').required('Required').integer().min(1, '≥ 1'),
//   weightKg: v.num('Weight').required('Required').moreThan(0, '> 0'),
//   lengthCm: v.num('Length').required('Required').moreThan(0, '> 0'),
//   widthCm: v.num('Width').required('Required').moreThan(0, '> 0'),
//   heightCm: v.num('Height').required('Required').moreThan(0, '> 0'),
//   fragile: yup.boolean(),
// });

// const addressFields = (prefix, receiver) => [
//   { name: `${prefix}.contactName`, label: receiver ? 'Receiver name' : 'Contact name', required: true, col: 4 },
//   { name: `${prefix}.contactPhone`, label: receiver ? 'Receiver phone' : 'Contact phone', required: true, col: 4 },
//   ...(receiver ? [{ name: `${prefix}.contactEmail`, label: 'Receiver email', type: 'email', col: 4, help: 'Gets the delivery OTP' }] : []),
//   { name: `${prefix}.addressLine1`, label: 'Address line 1', required: true, col: 6 },
//   { name: `${prefix}.addressLine2`, label: 'Address line 2', col: 6 },
//   { name: `${prefix}.city`, label: 'City', required: true, col: 3 },
//   { name: `${prefix}.state`, label: 'State', type: 'select', options: stateOptions, required: true, col: 3 },
//   { name: `${prefix}.pincode`, label: 'Pincode', required: true, col: 3 },
//   { name: `${prefix}.landmark`, label: 'Landmark', col: 3 },
// ];

// const emptyAddress = { contactName: '', contactPhone: '', contactEmail: '', addressLine1: '', addressLine2: '', landmark: '', city: '', state: '', pincode: '' };
// const emptyPackage = { description: '', packageType: 'BOX', quantity: 1, weightKg: '', lengthCm: '', widthCm: '', heightCm: '', fragile: false };
// const toAddressDefaults = (a) => (a ? Object.fromEntries(Object.keys(emptyAddress).map((k) => [k, a[k] ?? ''])) : emptyAddress);

// export default function CreateShipment() {
//   const { id } = useParams();
//   const editing = Boolean(id);
//   const navigate = useNavigate();
//   const { user } = useAuth();
//   const isCustomer = user.role === 'CUSTOMER';
//   const todayStr = isoDate();
//   const existing = useFetch(() => (editing ? shipmentService.get(id) : Promise.resolve(null)), [id]);
//   const warehouses = useFetch(() => warehouseService.list('ACTIVE'), [], { initial: [] });
//   const me = useFetch(() => (isCustomer ? customerService.me() : Promise.resolve(null)), [isCustomer]);
//   const customers = useFetch(() => (!isCustomer && !editing ? customerService.list({ size: 100, status: 'ACTIVE' }).then((p) => p.content) : Promise.resolve([])),
//     [isCustomer, editing], { initial: [] });
//   const [busy, setBusy] = useState(false);

//   const schema = useMemo(() => yup.object({
//     customerId: isCustomer || editing ? v.optId() : v.id('Customer'),
//     pickup: address(false),
//     pickupDate: v.date('Pickup date').test('future', 'Pickup date cannot be in the past', (x) => !x || x >= todayStr),
//     pickupTimeSlot: v.select('Pickup time slot'),
//     delivery: address(true),
//     serviceType: v.select('Service type'),
//     description: v.req('Shipment description').max(500),
//     declaredValue: v.num('Declared value').required('Declared value is required').min(0, 'Must be 0 or more'),
//     paymentMode: v.select('Payment mode'),
//     specialInstructions: v.opt(500),
//     originWarehouseId: v.optId(),
//     packages: editing ? yup.array() : yup.array().of(packageSchema).min(1, 'Add at least one package'),
//   }), [isCustomer, editing, todayStr]);

//   const form = useForm({
//     resolver: yupResolver(schema),
//     mode: 'onTouched',
//     defaultValues: {
//       customerId: '', pickup: emptyAddress, pickupDate: todayStr, pickupTimeSlot: 'MORNING', delivery: emptyAddress,
//       serviceType: 'STANDARD', description: '', declaredValue: '', paymentMode: 'PREPAID', specialInstructions: '',
//       originWarehouseId: '', packages: [emptyPackage],
//     },
//   });
//   const { fields: pkgRows, append, remove } = useFieldArray({ control: form.control, name: 'packages' });

//   // Prefill: edit mode from the shipment, create mode with the customer's own address as pickup.
//   useEffect(() => {
//     const s = existing.data;
//     if (s) {
//       form.reset({
//         customerId: s.customerId, pickup: toAddressDefaults(s.pickup), pickupDate: s.pickupDate, pickupTimeSlot: s.pickupTimeSlot,
//         delivery: toAddressDefaults(s.delivery), serviceType: s.serviceType, description: s.description,
//         declaredValue: s.declaredValue, paymentMode: s.paymentMode, specialInstructions: s.specialInstructions || '',
//         originWarehouseId: s.originWarehouseId || '', packages: [],
//       });
//     } else if (me.data && !editing) {
//       const c = me.data;
//       form.setValue('pickup', {
//         ...emptyAddress, contactName: c.fullName, contactPhone: c.phone, addressLine1: c.addressLine1,
//         addressLine2: c.addressLine2 || '', city: c.city, state: INDIAN_STATES.includes(c.state) ? c.state : '', pincode: c.pincode,
//       });
//     }
//     // eslint-disable-next-line react-hooks/exhaustive-deps
//   }, [existing.data, me.data]);

//   const watched = form.watch(['packages', 'serviceType', 'pickupDate']);
//   const estimate = useMemo(() => {
//     const [pkgs = [], service, pickupDate] = watched;
//     let weight = 0;
//     let chargeable = 0;
//     let count = 0;
//     pkgs.forEach((p) => {
//       const q = Number(p.quantity) || 0;
//       const w = Number(p.weightKg) || 0;
//       const vol = ((Number(p.lengthCm) || 0) * (Number(p.widthCm) || 0) * (Number(p.heightCm) || 0)) / 5000;
//       count += q;
//       weight += w * q;
//       chargeable += Math.max(w, vol) * q;
//     });
//     const charge = Math.max(100, chargeable * (RATE[service] || 50));
//     const eta = pickupDate ? new Date(new Date(pickupDate).getTime() + (DAYS[service] || 0) * 86400000) : null;
//     return { weight, chargeable, count, charge, eta };
//   }, [watched]);

//   if (existing.loading) return <Loader />;
//   if (existing.error) return <ErrorAlert error={existing.error} onRetry={existing.reload} />;
//   if (editing && existing.data?.currentStatus !== 'CREATED') {
//     return <Alert variant="warning">This shipment is {existing.data?.currentStatus} and can no longer be edited.</Alert>;
//   }

//   const onSubmit = form.handleSubmit(async (values) => {
//     setBusy(true);
//     await submitHandler(form, async (vals) => {
//       const body = clean({ ...vals, pickup: { ...vals.pickup, country: 'India' }, delivery: { ...vals.delivery, country: 'India' } });
//       if (editing) {
//         delete body.packages;
//         delete body.customerId;
//         delete body.originWarehouseId;
//         await shipmentService.update(id, body);
//         toast.success('Shipment updated');
//         navigate(`/shipments/${id}`);
//       } else {
//         if (isCustomer) delete body.customerId;
//         const res = await shipmentService.create(body);
//         toast.success(`Shipment booked · ${res.trackingNumber}`);
//         navigate(`/shipments/${res.id}`);
//       }
//     })(values);
//     setBusy(false);
//   });

//   const pkgError = (i, k) => form.formState.errors.packages?.[i]?.[k]?.message;

//   return (
//     <>
//       <PageHeader title={editing ? `Edit shipment · ${existing.data.trackingNumber}` : 'Create shipment'}
//         subtitle={editing ? 'Packages are edited separately' : 'Pickup, delivery, shipment details and packages'} />
//       <Form noValidate onSubmit={onSubmit}>
//         <Row className="g-3">
//           <Col xl={8}>
//             {!isCustomer && !editing && (
//               <Card className="mb-3"><Card.Body>
//                 <FormField form={form} field={{ name: 'customerId', label: 'Customer', type: 'select', required: true,
//                   options: customers.data.map((c) => ({ value: c.id, label: `${c.fullName} (${c.customerCode})` })) }} />
//               </Card.Body></Card>
//             )}
//             <Card className="mb-3">
//               <Card.Header>Pickup details</Card.Header>
//               <Card.Body>
//                 <FieldGrid form={form} fields={[
//                   ...addressFields('pickup', false),
//                   { name: 'pickupDate', label: 'Pickup date', type: 'date', required: true, col: 4, min: todayStr },
//                   { name: 'pickupTimeSlot', label: 'Pickup time slot', type: 'select', options: PickupTimeSlot, required: true, col: 4 },
//                 ]} />
//               </Card.Body>
//             </Card>
//             <Card className="mb-3">
//               <Card.Header>Delivery details</Card.Header>
//               <Card.Body><FieldGrid form={form} fields={addressFields('delivery', true)} /></Card.Body>
//             </Card>
//             <Card className="mb-3">
//               <Card.Header>Shipment details</Card.Header>
//               <Card.Body>
//                 <FieldGrid form={form} fields={[
//                   { name: 'serviceType', label: 'Service type', type: 'select', options: ServiceType, required: true, col: 4 },
//                   { name: 'declaredValue', label: 'Declared value (₹)', type: 'number', required: true, col: 4, step: '0.01', min: 0 },
//                   { name: 'paymentMode', label: 'Payment mode', type: 'select', options: PaymentMode, required: true, col: 4 },
//                   { name: 'description', label: 'Shipment description', type: 'textarea', required: true, col: 12, maxLength: 500 },
//                   { name: 'specialInstructions', label: 'Special instructions', type: 'textarea', col: 12, maxLength: 500, rows: 2 },
//                   ...(!editing ? [{ name: 'originWarehouseId', label: 'Origin warehouse', type: 'select', col: 6,
//                     placeholder: 'Auto: nearest active warehouse',
//                     options: warehouses.data.map((w) => ({ value: w.id, label: `${w.name} · ${w.city}` })) }] : []),
//                 ]} />
//               </Card.Body>
//             </Card>
//             {!editing && (
//               <Card className="mb-3">
//                 <Card.Header className="d-flex justify-content-between align-items-center">
//                   Packages
//                   <Button size="sm" variant="outline-primary" onClick={() => append(emptyPackage)}><i className="bi bi-plus-lg me-1" />Add package</Button>
//                 </Card.Header>
//                 <div className="table-responsive">
//                   <Table size="sm" className="mb-0 align-top">
//                     <thead><tr><th>Description *</th><th>Type *</th><th>Qty *</th><th>Weight kg *</th><th>L × W × H cm *</th><th>Fragile</th><th /></tr></thead>
//                     <tbody>
//                       {pkgRows.map((row, i) => (
//                         <tr key={row.id}>
//                           <td style={{ minWidth: 180 }}>
//                             <Form.Control size="sm" {...form.register(`packages.${i}.description`)} isInvalid={!!pkgError(i, 'description')} />
//                             <div className="invalid-feedback">{pkgError(i, 'description')}</div>
//                           </td>
//                           <td style={{ minWidth: 120 }}>
//                             <Form.Select size="sm" {...form.register(`packages.${i}.packageType`)}>
//                               {PackageType.map((t) => <option key={t} value={t}>{t}</option>)}
//                             </Form.Select>
//                           </td>
//                           <td style={{ width: 80 }}>
//                             <Form.Control size="sm" type="number" min={1} {...form.register(`packages.${i}.quantity`)} isInvalid={!!pkgError(i, 'quantity')} />
//                           </td>
//                           <td style={{ width: 100 }}>
//                             <Form.Control size="sm" type="number" step="0.01" {...form.register(`packages.${i}.weightKg`)} isInvalid={!!pkgError(i, 'weightKg')} />
//                             <div className="invalid-feedback">{pkgError(i, 'weightKg')}</div>
//                           </td>
//                           <td style={{ minWidth: 220 }}>
//                             <div className="d-flex gap-1">
//                               {['lengthCm', 'widthCm', 'heightCm'].map((k) => (
//                                 <Form.Control key={k} size="sm" type="number" step="0.1" placeholder={k[0].toUpperCase()}
//                                   {...form.register(`packages.${i}.${k}`)} isInvalid={!!pkgError(i, k)} />
//                               ))}
//                             </div>
//                           </td>
//                           <td className="text-center"><Form.Check {...form.register(`packages.${i}.fragile`)} /></td>
//                           <td>
//                             <Button size="sm" variant="link" className="text-danger p-0" disabled={pkgRows.length === 1}
//                               onClick={() => remove(i)} title="Remove"><i className="bi bi-trash" /></Button>
//                           </td>
//                         </tr>
//                       ))}
//                     </tbody>
//                   </Table>
//                 </div>
//                 {form.formState.errors.packages?.message && <div className="text-danger small p-2">{form.formState.errors.packages.message}</div>}
//               </Card>
//             )}
//           </Col>
//           <Col xl={4}>
//             <Card className="position-sticky" style={{ top: 80 }}>
//               <Card.Header>Summary</Card.Header>
//               <Card.Body>
//                 {!editing && (
//                   <>
//                     <div className="d-flex justify-content-between mb-1"><span className="text-muted">Packages</span><span>{estimate.count}</span></div>
//                     <div className="d-flex justify-content-between mb-1"><span className="text-muted">Actual weight</span><span>{estimate.weight.toFixed(2)} kg</span></div>
//                     <div className="d-flex justify-content-between mb-1"><span className="text-muted">Chargeable weight</span><span>{estimate.chargeable.toFixed(2)} kg</span></div>
//                     <div className="d-flex justify-content-between mb-1"><span className="text-muted">Estimated charge</span><strong>{formatMoney(estimate.charge)}</strong></div>
//                   </>
//                 )}
//                 <div className="d-flex justify-content-between mb-3"><span className="text-muted">Estimated delivery</span><span>{estimate.eta ? estimate.eta.toLocaleDateString('en-IN') : '—'}</span></div>
//                 <p className="small text-muted">Chargeable weight is the higher of actual and volumetric (L×W×H/5000). Final charge is confirmed on booking.</p>
//                 <Button type="submit" className="w-100" size="lg" disabled={busy}>
//                   {busy && <Spinner size="sm" className="me-2" />}{editing ? 'Save changes' : 'Book shipment'}
//                 </Button>
//                 <Button variant="light" className="w-100 mt-2" onClick={() => navigate(-1)}>Cancel</Button>
//               </Card.Body>
//             </Card>
//           </Col>
//         </Row>
//       </Form>
//     </>
//   );
// }


import { useEffect, useMemo, useState } from 'react';

import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Row,
  Spinner,
  Table
} from 'react-bootstrap';

import {
  useFieldArray,
  useForm,
  useWatch
} from 'react-hook-form';

import { yupResolver } from '@hookform/resolvers/yup';

import { useNavigate, useParams } from 'react-router-dom';

import { toast } from 'react-toastify';

import PageHeader from '../../components/common/PageHeader';

import {
  FieldGrid,
  FormField,
  submitHandler
} from '../../components/common/FormBuilder';

import Loader, {
  ErrorAlert
} from '../../components/common/Loader';

import useFetch from '../../hooks/useFetch';

import useAuth from '../../hooks/useAuth';

import shipmentService from '../../services/shipmentService';

import warehouseService from '../../services/warehouseService';

import customerService from '../../services/customerService';

import {
  INDIAN_STATES,
  PackageType,
  PaymentMode,
  PickupTimeSlot,
  ServiceType
} from '../../utils/constants';

import {
  formatMoney,
  isoDate,
  clean
} from '../../utils/formatters';

import {
  v,
  yup
} from '../../utils/validators';


// ============================================
// 1. SERVICE RATES AND DELIVERY DAYS
// ============================================

const RATE = {
  STANDARD: 50,
  EXPRESS: 80,
  SAME_DAY: 120
};

const DAYS = {
  STANDARD: 5,
  EXPRESS: 2,
  SAME_DAY: 0
};

// Minimum shipping charge
const MINIMUM_CHARGE = 100;


// ============================================
// 2. STATE OPTIONS
// ============================================

const stateOptions = INDIAN_STATES.map((s) => ({
  value: s,
  label: s
}));


// ============================================
// 3. ADDRESS VALIDATION
// ============================================

const address = (withEmail) => yup.object({

  contactName: v.req('Contact name').max(100),

  contactPhone: v.phone(),

  contactEmail: withEmail
    ? v.email(false)
    : yup.string().nullable(),

  addressLine1: v.req('Address line 1').max(200),

  addressLine2: v.opt(200),

  landmark: v.opt(150),

  city: v.req('City'),

  state: v.select('State'),

  pincode: v.pincode(),

});


// ============================================
// 4. PACKAGE VALIDATION
// ============================================

const packageSchema = yup.object({

  description: v.req('Description').max(200),

  packageType: v.select('Type'),

  quantity: v.num('Quantity')
    .required('Required')
    .integer()
    .min(1, 'Minimum quantity is 1'),

  weightKg: v.num('Weight')
    .required('Required')
    .moreThan(0, 'Weight must be greater than 0'),

  lengthCm: v.num('Length')
    .required('Required')
    .moreThan(0, 'Length must be greater than 0'),

  widthCm: v.num('Width')
    .required('Required')
    .moreThan(0, 'Width must be greater than 0'),

  heightCm: v.num('Height')
    .required('Required')
    .moreThan(0, 'Height must be greater than 0'),

  fragile: yup.boolean(),

});


// ============================================
// 5. ADDRESS FIELD CONFIGURATION
// ============================================

const addressFields = (prefix, receiver) => [

  {
    name: `${prefix}.contactName`,
    label: receiver ? 'Receiver name' : 'Contact name',
    required: true,
    col: 4
  },

  {
    name: `${prefix}.contactPhone`,
    label: receiver ? 'Receiver phone' : 'Contact phone',
    required: true,
    col: 4
  },

  ...(receiver
    ? [{
        name: `${prefix}.contactEmail`,
        label: 'Receiver email',
        type: 'email',
        col: 4,
        help: 'Gets the delivery OTP'
      }]
    : []),

  {
    name: `${prefix}.addressLine1`,
    label: 'Address line 1',
    required: true,
    col: 6
  },

  {
    name: `${prefix}.addressLine2`,
    label: 'Address line 2',
    col: 6
  },

  {
    name: `${prefix}.city`,
    label: 'City',
    required: true,
    col: 3
  },

  {
    name: `${prefix}.state`,
    label: 'State',
    type: 'select',
    options: stateOptions,
    required: true,
    col: 3
  },

  {
    name: `${prefix}.pincode`,
    label: 'Pincode',
    required: true,
    col: 3
  },

  {
    name: `${prefix}.landmark`,
    label: 'Landmark',
    col: 3
  },

];


// ============================================
// 6. DEFAULT VALUES
// ============================================

const emptyAddress = {
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  addressLine1: '',
  addressLine2: '',
  landmark: '',
  city: '',
  state: '',
  pincode: ''
};

const emptyPackage = {
  description: '',
  packageType: 'BOX',
  quantity: 1,
  weightKg: '',
  lengthCm: '',
  widthCm: '',
  heightCm: '',
  fragile: false
};


// Convert address values into form defaults
const toAddressDefaults = (a) => {

  return a
    ? Object.fromEntries(
        Object.keys(emptyAddress).map((k) => [
          k,
          a[k] ?? ''
        ])
      )
    : { ...emptyAddress };

};


// ============================================
// 7. MAIN CREATE SHIPMENT COMPONENT
// ============================================

export default function CreateShipment() {

  const { id } = useParams();

  const editing = Boolean(id);

  const navigate = useNavigate();

  const { user } = useAuth();

  const isCustomer = user.role === 'CUSTOMER';

  const todayStr = isoDate();


  // ==========================================
  // 8. FETCH BACKEND DATA
  // ==========================================

  // Existing shipment for edit mode
  const existing = useFetch(
    () => editing
      ? shipmentService.get(id)
      : Promise.resolve(null),
    [id]
  );


  // Active warehouses
  const warehouses = useFetch(
    () => warehouseService.list('ACTIVE'),
    [],
    { initial: [] }
  );


  // Logged-in customer details
  const me = useFetch(
    () => isCustomer
      ? customerService.me()
      : Promise.resolve(null),
    [isCustomer]
  );


  // Customer list for staff
  const customers = useFetch(
    () => (!isCustomer && !editing
      ? customerService
          .list({ size: 100, status: 'ACTIVE' })
          .then((p) => p.content)
      : Promise.resolve([])
    ),
    [isCustomer, editing],
    { initial: [] }
  );


  const [busy, setBusy] = useState(false);


  // ==========================================
  // 9. FORM VALIDATION SCHEMA
  // ==========================================

  const schema = useMemo(() => yup.object({

    customerId: isCustomer || editing
      ? v.optId()
      : v.id('Customer'),

    pickup: address(false),

    pickupDate: v.date('Pickup date')
      .test(
        'future',
        'Pickup date cannot be in the past',
        (x) => !x || x >= todayStr
      ),

    pickupTimeSlot: v.select('Pickup time slot'),

    delivery: address(true),

    serviceType: v.select('Service type'),

    description: v.req('Shipment description').max(500),

    declaredValue: v.num('Declared value')
      .required('Declared value is required')
      .min(0, 'Must be 0 or more'),

    paymentMode: v.select('Payment mode'),

    specialInstructions: v.opt(500),

    originWarehouseId: v.optId(),

    packages: editing
      ? yup.array()
      : yup.array()
          .of(packageSchema)
          .min(1, 'Add at least one package'),

  }), [isCustomer, editing, todayStr]);


  // ==========================================
  // 10. REACT HOOK FORM
  // ==========================================

  const form = useForm({

    resolver: yupResolver(schema),

    mode: 'onTouched',

    defaultValues: {

      customerId: '',

      pickup: { ...emptyAddress },

      pickupDate: todayStr,

      pickupTimeSlot: 'MORNING',

      delivery: { ...emptyAddress },

      serviceType: 'STANDARD',

      description: '',

      declaredValue: '',

      paymentMode: 'PREPAID',

      specialInstructions: '',

      originWarehouseId: '',

      packages: [{ ...emptyPackage }],

    },

  });


  // ==========================================
  // 11. PACKAGE FIELD ARRAY
  // ==========================================

  const {
    fields: pkgRows,
    append,
    remove
  } = useFieldArray({

    control: form.control,

    name: 'packages'

  });


  // ==========================================
  // 12. WATCH PACKAGE INPUT CHANGES
  // ==========================================

  // This watches all package fields.
  // Whenever weight, dimensions or quantity changes,
  // the calculation is automatically updated.

  const watchedPackages = useWatch({

    control: form.control,

    name: 'packages'

  }) || [];


  // Watch selected service type
  const service = useWatch({

    control: form.control,

    name: 'serviceType'

  });


  // Watch pickup date
  const pickupDate = useWatch({

    control: form.control,

    name: 'pickupDate'

  });


  // ==========================================
  // 13. WEIGHT AND RATE CALCULATION
  // ==========================================

  const estimate = useMemo(() => {

    let actualWeight = 0;

    let volumetricWeight = 0;

    let count = 0;


    // Calculate weights for every package
    watchedPackages.forEach((p) => {

      const quantity = Math.max(
        0,
        Number(p?.quantity) || 0
      );

      const weight = Math.max(
        0,
        Number(p?.weightKg) || 0
      );

      const length = Math.max(
        0,
        Number(p?.lengthCm) || 0
      );

      const width = Math.max(
        0,
        Number(p?.widthCm) || 0
      );

      const height = Math.max(
        0,
        Number(p?.heightCm) || 0
      );


      // Volumetric weight of one package
      const volumetric =
        (length * width * height) / 5000;


      // Total package quantity
      count += quantity;


      // Total actual weight
      actualWeight += weight * quantity;


      // Total volumetric weight
      volumetricWeight += volumetric * quantity;

    });


    // Chargeable weight is the higher total
    const chargeableWeight = Math.max(
      actualWeight,
      volumetricWeight
    );


    // Get rate according to selected service
    const ratePerKg = RATE[service] ?? RATE.STANDARD;


    // Calculate shipping charge
    // Minimum charge remains ₹100
    const calculatedCharge =
      chargeableWeight * ratePerKg;

    const charge = Math.max(
      MINIMUM_CHARGE,
      calculatedCharge
    );


    // Calculate estimated delivery date
    const eta = pickupDate
      ? new Date(
          new Date(pickupDate).getTime() +
          (DAYS[service] ?? 5) * 86400000
        )
      : null;


    return {

      count,

      actualWeight,

      volumetricWeight,

      chargeableWeight,

      ratePerKg,

      charge,

      eta

    };

  }, [watchedPackages, service, pickupDate]);


  // ==========================================
  // 14. PREFILL FORM DATA
  // ==========================================

  useEffect(() => {

    const s = existing.data;


    // Edit existing shipment
    if (s) {

      form.reset({

        customerId: s.customerId,

        pickup: toAddressDefaults(s.pickup),

        pickupDate: s.pickupDate,

        pickupTimeSlot: s.pickupTimeSlot,

        delivery: toAddressDefaults(s.delivery),

        serviceType: s.serviceType,

        description: s.description,

        declaredValue: s.declaredValue,

        paymentMode: s.paymentMode,

        specialInstructions: s.specialInstructions || '',

        originWarehouseId: s.originWarehouseId || '',

        packages: [],

      });

    }


    // Automatically fill pickup address for customers
    else if (me.data && !editing) {

      const c = me.data;

      form.setValue('pickup', {

        ...emptyAddress,

        contactName: c.fullName,

        contactPhone: c.phone,

        addressLine1: c.addressLine1,

        addressLine2: c.addressLine2 || '',

        city: c.city,

        state: INDIAN_STATES.includes(c.state)
          ? c.state
          : '',

        pincode: c.pincode,

      });

    }

    // eslint-disable-next-line react-hooks/exhaustive-deps

  }, [existing.data, me.data]);


  // ==========================================
  // 15. LOADING AND ERROR HANDLING
  // ==========================================

  if (existing.loading) {

    return <Loader />;

  }


  if (existing.error) {

    return (
      <ErrorAlert
        error={existing.error}
        onRetry={existing.reload}
      />
    );

  }


  // Only CREATED shipments can be edited
  if (
    editing &&
    existing.data?.currentStatus !== 'CREATED'
  ) {

    return (
      <Alert variant="warning">
        This shipment is {existing.data?.currentStatus}
        and can no longer be edited.
      </Alert>
    );

  }


  // ==========================================
  // 16. FORM SUBMISSION
  // ==========================================

  const onSubmit = form.handleSubmit(async (values) => {

    setBusy(true);

    try {

      await submitHandler(form, async (vals) => {

        const body = clean({

          ...vals,

          pickup: {
            ...vals.pickup,
            country: 'India'
          },

          delivery: {
            ...vals.delivery,
            country: 'India'
          }

        });


        // Update existing shipment
        if (editing) {

          delete body.packages;

          delete body.customerId;

          delete body.originWarehouseId;


          await shipmentService.update(id, body);

          toast.success('Shipment updated');

          navigate(`/shipments/${id}`);

        }


        // Create new shipment
        else {

          if (isCustomer) {

            delete body.customerId;

          }


          const res = await shipmentService.create(body);


          toast.success(
            `Shipment booked · ${res.trackingNumber}`
          );


          navigate(`/shipments/${res.id}`);

        }

      })(values);

    } finally {

      setBusy(false);

    }

  });


  // ==========================================
  // 17. PACKAGE VALIDATION ERRORS
  // ==========================================

  const pkgError = (i, k) => {

    return form.formState.errors.packages?.[i]?.[k]?.message;

  };


  // ==========================================
  // 18. INDIVIDUAL PACKAGE CALCULATION
  // ==========================================

  const getPackageWeight = (p) => {

    const actual = Number(p?.weightKg) || 0;

    const length = Number(p?.lengthCm) || 0;

    const width = Number(p?.widthCm) || 0;

    const height = Number(p?.heightCm) || 0;


    const volumetric =
      (length * width * height) / 5000;


    return {

      actual,

      volumetric,

      chargeable: Math.max(actual, volumetric)

    };

  };


  // ==========================================
  // 19. MAIN UI
  // ==========================================

  return (

    <>

      <PageHeader

        title={
          editing
            ? `Edit shipment · ${existing.data.trackingNumber}`
            : 'Create shipment'
        }

        subtitle={
          editing
            ? 'Packages are edited separately'
            : 'Pickup, delivery, shipment details and packages'
        }

      />


      <Form noValidate onSubmit={onSubmit}>

        <Row className="g-3">


          {/* ==================================
              LEFT SIDE - FORM DETAILS
          ================================== */}

          <Col xl={8}>


            {/* CUSTOMER SELECTION */}

            {!isCustomer && !editing && (

              <Card className="mb-3">

                <Card.Body>

                  <FormField
                    form={form}
                    field={{

                      name: 'customerId',

                      label: 'Customer',

                      type: 'select',

                      required: true,

                      options: customers.data.map((c) => ({

                        value: c.id,

                        label: `${c.fullName} (${c.customerCode})`

                      }))

                    }}
                  />

                </Card.Body>

              </Card>

            )}


            {/* ==================================
                PICKUP DETAILS
            ================================== */}

            <Card className="mb-3">

              <Card.Header>
                Pickup details
              </Card.Header>

              <Card.Body>

                <FieldGrid
                  form={form}
                  fields={[

                    ...addressFields('pickup', false),

                    {
                      name: 'pickupDate',
                      label: 'Pickup date',
                      type: 'date',
                      required: true,
                      col: 4,
                      min: todayStr
                    },

                    {
                      name: 'pickupTimeSlot',
                      label: 'Pickup time slot',
                      type: 'select',
                      options: PickupTimeSlot,
                      required: true,
                      col: 4
                    }

                  ]}
                />

              </Card.Body>

            </Card>


            {/* ==================================
                DELIVERY DETAILS
            ================================== */}

            <Card className="mb-3">

              <Card.Header>
                Delivery details
              </Card.Header>

              <Card.Body>

                <FieldGrid
                  form={form}
                  fields={addressFields('delivery', true)}
                />

              </Card.Body>

            </Card>


            {/* ==================================
                SHIPMENT DETAILS
            ================================== */}

            <Card className="mb-3">

              <Card.Header>
                Shipment details
              </Card.Header>

              <Card.Body>

                <FieldGrid
                  form={form}
                  fields={[

                    {
                      name: 'serviceType',
                      label: 'Service type',
                      type: 'select',
                      options: ServiceType,
                      required: true,
                      col: 4
                    },

                    {
                      name: 'declaredValue',
                      label: 'Declared value (₹)',
                      type: 'number',
                      required: true,
                      col: 4,
                      step: '0.01',
                      min: 0
                    },

                    {
                      name: 'paymentMode',
                      label: 'Payment mode',
                      type: 'select',
                      options: PaymentMode,
                      required: true,
                      col: 4
                    },

                    {
                      name: 'description',
                      label: 'Shipment description',
                      type: 'textarea',
                      required: true,
                      col: 12,
                      maxLength: 500
                    },

                    {
                      name: 'specialInstructions',
                      label: 'Special instructions',
                      type: 'textarea',
                      col: 12,
                      maxLength: 500,
                      rows: 2
                    },

                    ...(!editing
                      ? [{
                          name: 'originWarehouseId',
                          label: 'Origin warehouse',
                          type: 'select',
                          col: 6,

                          placeholder:
                            'Auto: nearest active warehouse',

                          options: warehouses.data.map((w) => ({

                            value: w.id,

                            label: `${w.name} · ${w.city}`

                          }))

                        }]
                      : [])

                  ]}
                />

              </Card.Body>

            </Card>


            {/* ==================================
                PACKAGE DETAILS
            ================================== */}

            {!editing && (

              <Card className="mb-3">

                <Card.Header className="d-flex justify-content-between align-items-center">

                  <strong>Packages</strong>

                  <Button

                    size="sm"

                    variant="outline-primary"

                    type="button"

                    onClick={() => append({ ...emptyPackage })}

                  >

                    <i className="bi bi-plus-lg me-1" />

                    Add package

                  </Button>

                </Card.Header>


                <div className="table-responsive">

                  <Table
                    size="sm"
                    className="mb-0 align-top"
                  >

                    <thead>

                      <tr>

                        <th>Description *</th>

                        <th>Type *</th>

                        <th>Qty *</th>

                        <th>Weight kg *</th>

                        <th>L × W × H cm *</th>

                        <th>Fragile</th>

                        <th>Weight calculation</th>

                        <th />

                      </tr>

                    </thead>


                    <tbody>

                      {pkgRows.map((row, i) => {

                        const packageWeight =
                          getPackageWeight(watchedPackages[i]);


                        return (

                          <tr key={row.id}>


                            {/* DESCRIPTION */}

                            <td style={{ minWidth: 160 }}>

                              <Form.Control

                                size="sm"

                                {...form.register(
                                  `packages.${i}.description`
                                )}

                                isInvalid={
                                  !!pkgError(i, 'description')
                                }

                              />

                              <div className="invalid-feedback">

                                {pkgError(i, 'description')}

                              </div>

                            </td>


                            {/* PACKAGE TYPE */}

                            <td style={{ minWidth: 110 }}>

                              <Form.Select

                                size="sm"

                                {...form.register(
                                  `packages.${i}.packageType`
                                )}

                                isInvalid={
                                  !!pkgError(i, 'packageType')
                                }

                              >

                                {PackageType.map((t) => (

                                  <option key={t} value={t}>
                                    {t}
                                  </option>

                                ))}

                              </Form.Select>

                              <div className="text-danger small">

                                {pkgError(i, 'packageType')}

                              </div>

                            </td>


                            {/* QUANTITY */}

                            <td style={{ width: 75 }}>

                              <Form.Control

                                size="sm"

                                type="number"

                                min={1}

                                {...form.register(
                                  `packages.${i}.quantity`
                                )}

                                isInvalid={
                                  !!pkgError(i, 'quantity')
                                }

                              />

                              <div className="invalid-feedback">

                                {pkgError(i, 'quantity')}

                              </div>

                            </td>


                            {/* WEIGHT */}

                            <td style={{ width: 95 }}>

                              <Form.Control

                                size="sm"

                                type="number"

                                min="0.01"

                                step="0.01"

                                {...form.register(
                                  `packages.${i}.weightKg`
                                )}

                                isInvalid={
                                  !!pkgError(i, 'weightKg')
                                }

                              />

                              <div className="invalid-feedback">

                                {pkgError(i, 'weightKg')}

                              </div>

                            </td>


                            {/* DIMENSIONS */}

                            <td style={{ minWidth: 210 }}>

                              <div className="d-flex gap-1">

                                {[
                                  'lengthCm',
                                  'widthCm',
                                  'heightCm'
                                ].map((k) => (

                                  <div key={k}>

                                    <Form.Control

                                      size="sm"

                                      type="number"

                                      min="0.01"

                                      step="0.1"

                                      placeholder={
                                        k === 'lengthCm'
                                          ? 'L'
                                          : k === 'widthCm'
                                            ? 'W'
                                            : 'H'
                                      }

                                      {...form.register(
                                        `packages.${i}.${k}`
                                      )}

                                      isInvalid={
                                        !!pkgError(i, k)
                                      }

                                    />

                                    <div className="text-danger small">

                                      {pkgError(i, k)}

                                    </div>

                                  </div>

                                ))}

                              </div>

                            </td>


                            {/* FRAGILE */}

                            <td className="text-center">

                              <Form.Check

                                {...form.register(
                                  `packages.${i}.fragile`
                                )}

                              />

                            </td>


                            {/* LIVE WEIGHT CALCULATION */}

                            <td style={{ minWidth: 155 }}>

                              <div className="small">

                                <div>
                                  Actual:
                                  <strong className="ms-1">
                                    {packageWeight.actual.toFixed(2)} kg
                                  </strong>
                                </div>

                                <div>
                                  Volumetric:
                                  <strong className="ms-1">
                                    {packageWeight.volumetric.toFixed(2)} kg
                                  </strong>
                                </div>

                                <div>
                                  Chargeable:
                                  <strong className="ms-1 text-primary">
                                    {packageWeight.chargeable.toFixed(2)} kg
                                  </strong>
                                </div>

                              </div>

                            </td>


                            {/* REMOVE PACKAGE */}

                            <td>

                              <Button

                                size="sm"

                                variant="link"

                                type="button"

                                className="text-danger p-0"

                                disabled={pkgRows.length === 1}

                                onClick={() => remove(i)}

                                title="Remove package"

                              >

                                <i className="bi bi-trash" />

                              </Button>

                            </td>

                          </tr>

                        );

                      })}

                    </tbody>

                  </Table>

                </div>


                {/* PACKAGE VALIDATION */}

                {form.formState.errors.packages?.message && (

                  <div className="text-danger small p-2">

                    {form.formState.errors.packages.message}

                  </div>

                )}

              </Card>

            )}

          </Col>


          {/* ==================================
              RIGHT SIDE - SUMMARY
          ================================== */}

          <Col xl={4}>

            <Card

              className="position-sticky"

              style={{ top: 80 }}

            >

              <Card.Header>

                <strong>Shipment summary</strong>

              </Card.Header>


              <Card.Body>


                {!editing && (

                  <>


                    {/* TOTAL PACKAGES */}

                    <div className="d-flex justify-content-between mb-2">

                      <span className="text-muted">
                        Total packages
                      </span>

                      <strong>
                        {estimate.count}
                      </strong>

                    </div>


                    {/* ACTUAL WEIGHT */}

                    <div className="d-flex justify-content-between mb-2">

                      <span className="text-muted">
                        Actual weight
                      </span>

                      <strong>
                        {estimate.actualWeight.toFixed(2)} kg
                      </strong>

                    </div>


                    {/* VOLUMETRIC WEIGHT */}

                    <div className="d-flex justify-content-between mb-2">

                      <span className="text-muted">
                        Volumetric weight
                      </span>

                      <strong>
                        {estimate.volumetricWeight.toFixed(2)} kg
                      </strong>

                    </div>


                    <hr />


                    {/* CHARGEABLE WEIGHT */}

                    <div className="d-flex justify-content-between mb-2">

                      <span>
                        Chargeable weight
                      </span>

                      <strong className="text-primary">

                        {estimate.chargeableWeight.toFixed(2)} kg

                      </strong>

                    </div>


                    {/* SERVICE TYPE */}

                    <div className="d-flex justify-content-between mb-2">

                      <span className="text-muted">
                        Service type
                      </span>

                      <strong>
                        {service?.replace('_', ' ')}
                      </strong>

                    </div>


                    {/* RATE PER KG */}

                    <div className="d-flex justify-content-between mb-2">

                      <span className="text-muted">
                        Rate per kg
                      </span>

                      <strong>
                        {formatMoney(estimate.ratePerKg)}
                      </strong>

                    </div>


                    <hr />


                    {/* SHIPPING CHARGE */}

                    <div className="d-flex justify-content-between align-items-center mb-3">

                      <span>
                        Estimated shipping charge
                      </span>

                      <strong className="text-primary fs-5">

                        {formatMoney(estimate.charge)}

                      </strong>

                    </div>


                  </>

                )}


                {/* ESTIMATED DELIVERY */}

                <div className="d-flex justify-content-between mb-3">

                  <span className="text-muted">
                    Estimated delivery
                  </span>

                  <strong>

                    {estimate.eta
                      ? estimate.eta.toLocaleDateString('en-IN')
                      : '—'
                    }

                  </strong>

                </div>


                {/* CALCULATION INFORMATION */}

                {!editing && (

                  <Alert
                    variant="info"
                    className="small"
                  >

                    <strong>Calculation details</strong>

                    <hr className="my-2" />

                    Volumetric weight:

                    <br />

                    Length × Width × Height / 5000

                    <br /><br />

                    Chargeable weight:

                    <br />

                    Higher of total actual and total volumetric weight.

                    <br /><br />

                    Shipping charge:

                    <br />

                    Chargeable weight × selected service rate.

                    <br /><br />

                    Minimum shipping charge: ₹100.

                  </Alert>

                )}


                {/* SUBMIT BUTTON */}

                <Button

                  type="submit"

                  className="w-100"

                  size="lg"

                  disabled={busy}

                >

                  {busy && (

                    <Spinner
                      size="sm"
                      className="me-2"
                    />

                  )}

                  {editing
                    ? 'Save changes'
                    : 'Book shipment'
                  }

                </Button>


                {/* CANCEL BUTTON */}

                <Button

                  variant="light"

                  type="button"

                  className="w-100 mt-2"

                  onClick={() => navigate(-1)}

                >

                  Cancel

                </Button>


              </Card.Body>

            </Card>

          </Col>

        </Row>

      </Form>

    </>

  );

}
