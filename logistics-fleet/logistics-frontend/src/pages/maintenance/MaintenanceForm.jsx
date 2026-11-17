import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, Form, Row, Spinner, Table } from 'react-bootstrap';
import { useFieldArray, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { FieldGrid, submitHandler } from '../../components/common/FormBuilder';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import maintenanceService from '../../services/maintenanceService';
import vehicleService from '../../services/vehicleService';
import { MaintenanceStatus, MaintenanceType } from '../../utils/constants';
import { clean, formatMoney, isoDate } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  vehicleId: v.id('Vehicle'),
  maintenanceType: v.select('Maintenance type'),
  description: v.req('Description').max(500),
  scheduledDate: v.date('Scheduled date'),
  status: v.select('Status'),
  completedDate: yup.string().when('status', {
    is: 'COMPLETED',
    then: (s) => s.required('Completed date is required when COMPLETED')
      .test('after', 'Must be on or after the scheduled date', function after(x) { return !x || x >= this.parent.scheduledDate; }),
    otherwise: (s) => s.nullable(),
  }),
  odometerKm: v.num('Odometer').required('Odometer is required').integer().min(0)
    .test('veh', 'Must be at least the vehicle odometer', function veh(x) { return x == null || x >= Number(this.parent.vehicleOdometer || 0); }),
  vehicleOdometer: yup.mixed().strip(),
  serviceCenter: v.opt(100),
  labourCost: v.num('Labour cost').min(0, 'Must be 0 or more').nullable(),
  totalCost: v.num('Total cost').min(0, 'Must be 0 or more').nullable(),
  nextServiceDate: v.optDate(),
  nextServiceOdometer: v.num('Next service odometer').integer().min(0).nullable(),
  invoiceUrl: yup.string().nullable(),
  parts: yup.array().of(yup.object({
    partName: v.req('Part name').max(100),
    partNumber: v.opt(50),
    quantity: v.num('Qty').required('Required').integer().min(1, '≥ 1'),
    unitCost: v.num('Unit cost').required('Required').min(0),
  })),
});

const empty = {
  vehicleId: '', maintenanceType: '', description: '', scheduledDate: isoDate(), completedDate: '', odometerKm: '', vehicleOdometer: 0,
  serviceCenter: '', labourCost: '', totalCost: '', nextServiceDate: '', nextServiceOdometer: '', status: 'SCHEDULED', invoiceUrl: '', parts: [],
};

export default function MaintenanceForm() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const record = useFetch(() => (editing ? maintenanceService.get(id) : Promise.resolve(null)), [id]);
  const vehicles = useFetch(() => vehicleService.list({ size: 100 }).then((p) => p.content), [], { initial: [] });
  const [busy, setBusy] = useState(false);
  const form = useForm({ resolver: yupResolver(schema), mode: 'onTouched', defaultValues: { ...empty, vehicleId: params.get('vehicleId') || '' } });
  const { fields: parts, append, remove } = useFieldArray({ control: form.control, name: 'parts' });

  useEffect(() => {
    const m = record.data;
    if (m) {
      form.reset({
        ...Object.fromEntries(Object.keys(empty).map((k) => [k, m[k] ?? empty[k]])),
        parts: m.parts.map((p) => ({ partName: p.partName, partNumber: p.partNumber || '', quantity: p.quantity, unitCost: p.unitCost })),
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [record.data]);

  const vehicleId = Number(form.watch('vehicleId'));
  const vehicle = vehicles.data.find((vh) => vh.id === vehicleId);
  useEffect(() => {
    if (vehicle) {
      form.setValue('vehicleOdometer', vehicle.odometerKm);
      if (!editing && !form.getValues('odometerKm')) form.setValue('odometerKm', vehicle.odometerKm);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vehicle?.id]);

  const watchedParts = form.watch('parts');
  const labour = form.watch('labourCost');
  const partsTotal = useMemo(() => (watchedParts || []).reduce((sum, p) => sum + (Number(p.quantity) || 0) * (Number(p.unitCost) || 0), 0), [watchedParts]);

  if (record.loading) return <Loader />;
  if (record.error) return <ErrorAlert error={record.error} onRetry={record.reload} />;
  const locked = editing && ['COMPLETED', 'CANCELLED'].includes(record.data?.status);

  const onSubmit = form.handleSubmit(async (values) => {
    setBusy(true);
    await submitHandler(form, async (vals) => {
      const body = clean(vals);
      if (body.status !== 'COMPLETED') body.completedDate = null;
      if (editing) await maintenanceService.update(id, body);
      else await maintenanceService.create(body);
      toast.success(editing ? 'Maintenance updated' : 'Maintenance scheduled');
      navigate('/maintenance');
    })(values);
    setBusy(false);
  });

  const pErr = (i, k) => form.formState.errors.parts?.[i]?.[k]?.message;

  return (
    <>
      <PageHeader title={editing ? `Maintenance · ${record.data?.vehicleNumber}` : 'Schedule maintenance'}
        subtitle={locked ? 'Completed and cancelled records are read-only' : undefined} />
      <Form noValidate onSubmit={onSubmit}>
        <fieldset disabled={locked}>
          <Row className="g-3">
            <Col xl={8}>
              <Card className="mb-3">
                <Card.Body>
                  <FieldGrid form={form} fields={[
                    { name: 'vehicleId', label: 'Vehicle', type: 'select', required: true, col: 6,
                      options: vehicles.data.map((vh) => ({ value: vh.id, label: `${vh.vehicleNumber} · ${vh.make} ${vh.model} · ${vh.odometerKm} km` })) },
                    { name: 'maintenanceType', label: 'Maintenance type', type: 'select', options: MaintenanceType, required: true, col: 3 },
                    { name: 'status', label: 'Status', type: 'select', options: MaintenanceStatus, required: true, col: 3 },
                    { name: 'description', label: 'Description', type: 'textarea', required: true, col: 12, maxLength: 500 },
                    { name: 'scheduledDate', label: 'Scheduled date', type: 'date', required: true, col: 3 },
                    { name: 'completedDate', label: 'Completed date', type: 'date', col: 3, required: true, showIf: (x) => x.status === 'COMPLETED' },
                    { name: 'odometerKm', label: 'Odometer at service (km)', type: 'number', required: true, col: 3,
                      help: vehicle ? `Vehicle reads ${vehicle.odometerKm} km` : undefined },
                    { name: 'serviceCenter', label: 'Service centre / vendor', col: 3 },
                    { name: 'labourCost', label: 'Labour cost (₹)', type: 'number', step: '0.01', col: 3 },
                    { name: 'totalCost', label: 'Total cost (₹)', type: 'number', step: '0.01', col: 3, help: 'Blank = labour + parts' },
                    { name: 'nextServiceDate', label: 'Next service date', type: 'date', col: 3 },
                    { name: 'nextServiceOdometer', label: 'Next service odometer', type: 'number', col: 3 },
                    { name: 'invoiceUrl', label: 'Invoice', type: 'file', category: 'document', col: 12 },
                  ]} />
                </Card.Body>
              </Card>
              <Card>
                <Card.Header className="d-flex justify-content-between align-items-center">
                  Parts used
                  <Button size="sm" variant="outline-primary" onClick={() => append({ partName: '', partNumber: '', quantity: 1, unitCost: '' })}>
                    <i className="bi bi-plus-lg me-1" />Add part
                  </Button>
                </Card.Header>
                {parts.length === 0 ? <div className="text-muted small p-3">No parts added.</div> : (
                  <Table size="sm" className="mb-0">
                    <thead><tr><th>Part name *</th><th>Part number</th><th style={{ width: 90 }}>Qty *</th><th style={{ width: 130 }}>Unit cost *</th><th>Total</th><th /></tr></thead>
                    <tbody>
                      {parts.map((row, i) => (
                        <tr key={row.id}>
                          <td><Form.Control size="sm" {...form.register(`parts.${i}.partName`)} isInvalid={!!pErr(i, 'partName')} /></td>
                          <td><Form.Control size="sm" {...form.register(`parts.${i}.partNumber`)} /></td>
                          <td><Form.Control size="sm" type="number" {...form.register(`parts.${i}.quantity`)} isInvalid={!!pErr(i, 'quantity')} /></td>
                          <td><Form.Control size="sm" type="number" step="0.01" {...form.register(`parts.${i}.unitCost`)} isInvalid={!!pErr(i, 'unitCost')} /></td>
                          <td>{formatMoney((Number(watchedParts?.[i]?.quantity) || 0) * (Number(watchedParts?.[i]?.unitCost) || 0))}</td>
                          <td><Button size="sm" variant="link" className="text-danger p-0" onClick={() => remove(i)}><i className="bi bi-trash" /></Button></td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                )}
              </Card>
            </Col>
            <Col xl={4}>
              <Card>
                <Card.Header>Cost</Card.Header>
                <Card.Body>
                  <div className="d-flex justify-content-between"><span className="text-muted">Labour</span><span>{formatMoney(Number(labour) || 0)}</span></div>
                  <div className="d-flex justify-content-between"><span className="text-muted">Parts</span><span>{formatMoney(partsTotal)}</span></div>
                  <hr />
                  <div className="d-flex justify-content-between fw-bold"><span>Computed total</span><span>{formatMoney(partsTotal + (Number(labour) || 0))}</span></div>
                  <p className="small text-muted mt-2">IN_PROGRESS puts the vehicle under maintenance; COMPLETED makes it available again and updates its odometer and service dates.</p>
                  {!locked && (
                    <Button type="submit" className="w-100" disabled={busy}>{busy && <Spinner size="sm" className="me-2" />}{editing ? 'Save changes' : 'Save'}</Button>
                  )}
                  <Button variant="light" className="w-100 mt-2" onClick={() => navigate('/maintenance')}>Back</Button>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </fieldset>
      </Form>
    </>
  );
}
