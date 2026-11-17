import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Form, Row, Spinner } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { FieldGrid, submitHandler } from '../../components/common/FormBuilder';
import StatusBadge from '../../components/common/StatusBadge';
import Loader from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import assignmentService from '../../services/assignmentService';
import driverService from '../../services/driverService';
import vehicleService from '../../services/vehicleService';
import { formatDate, formatKg, isoDateTime, pretty } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const HOURS = { SAME_DAY: 8, EXPRESS: 48, STANDARD: 120 };

const schema = yup.object({
  shipmentId: v.id('Shipment'),
  driverId: v.id('Driver'),
  vehicleId: v.id('Vehicle'),
  plannedStartTime: v.req('Planned start').test('future', 'Planned start must be now or later',
    (x) => !x || new Date(x).getTime() >= Date.now() - 5 * 60000),
  expectedDeliveryTime: v.req('Expected delivery').test('after', 'Expected delivery must be after planned start',
    function after(x) { return !x || !this.parent.plannedStartTime || new Date(x) > new Date(this.parent.plannedStartTime); }),
  routeNotes: v.opt(500),
});

export default function CreateAssignment() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const pending = useFetch(() => assignmentService.pendingShipments(), [], { initial: [] });
  const drivers = useFetch(() => driverService.available(), [], { initial: [] });
  const [busy, setBusy] = useState(false);

  const form = useForm({
    resolver: yupResolver(schema),
    mode: 'onTouched',
    defaultValues: {
      shipmentId: params.get('shipmentId') || '', driverId: '', vehicleId: '',
      plannedStartTime: isoDateTime(new Date(Date.now() + 30 * 60000)), expectedDeliveryTime: '', routeNotes: '',
    },
  });
  const shipmentId = Number(form.watch('shipmentId'));
  const shipment = useMemo(() => pending.data.find((s) => s.id === shipmentId), [pending.data, shipmentId]);
  const vehicles = useFetch(() => (shipment ? vehicleService.available(shipment.totalWeightKg) : Promise.resolve([])),
    [shipment?.id], { initial: [] });

  // Suggest an expected delivery time from the service type.
  useEffect(() => {
    if (!shipment) return;
    const start = new Date(form.getValues('plannedStartTime') || Date.now());
    form.setValue('expectedDeliveryTime', isoDateTime(new Date(start.getTime() + (HOURS[shipment.serviceType] || 48) * 3600000)));
    form.setValue('vehicleId', '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shipment?.id]);

  if (pending.loading) return <Loader />;

  const onSubmit = form.handleSubmit(async (values) => {
    setBusy(true);
    await submitHandler(form, async (vals) => {
      const res = await assignmentService.create(vals);
      toast.success(`${res.assignmentCode} created — ${res.driverName} notified`);
      navigate('/assignments');
    })(values);
    setBusy(false);
  });

  return (
    <>
      <PageHeader title="Create assignment" subtitle="Pick a ready shipment, an available driver and a vehicle with enough capacity" />
      {pending.data.length === 0 && <Alert variant="info">No shipments are READY_FOR_DISPATCH right now.</Alert>}
      <Form noValidate onSubmit={onSubmit}>
        <Row className="g-3">
          <Col lg={8}>
            <Card>
              <Card.Body>
                <FieldGrid form={form} fields={[
                  { name: 'shipmentId', label: 'Shipment', type: 'select', required: true, col: 12,
                    options: pending.data.map((s) => ({ value: s.id, label: `${s.trackingNumber} · ${s.pickup?.city} → ${s.delivery?.city} · ${s.totalWeightKg} kg · ${pretty(s.serviceType)}` })) },
                  { name: 'driverId', label: 'Driver', type: 'select', required: true,
                    help: 'Only AVAILABLE drivers with a valid license',
                    options: drivers.data.map((d) => ({ value: d.id, label: `${d.fullName} (${d.driverCode}) · ${d.licenseType} · ${d.experienceYears} yrs` })) },
                  { name: 'vehicleId', label: 'Vehicle', type: 'select', required: true,
                    help: shipment ? `AVAILABLE, insured, capacity ≥ ${shipment.totalWeightKg} kg` : 'Choose a shipment first',
                    options: vehicles.data.map((vh) => ({ value: vh.id, label: `${vh.vehicleNumber} · ${pretty(vh.vehicleType)} · ${vh.capacityKg} kg` })) },
                  { name: 'plannedStartTime', label: 'Planned start', type: 'datetime-local', required: true, min: isoDateTime() },
                  { name: 'expectedDeliveryTime', label: 'Expected delivery', type: 'datetime-local', required: true },
                  { name: 'routeNotes', label: 'Route notes', type: 'textarea', col: 12, maxLength: 500 },
                ]} />
                {shipment && vehicles.data.length === 0 && !vehicles.loading && (
                  <Alert variant="warning" className="small">No available vehicle can carry {shipment.totalWeightKg} kg.</Alert>
                )}
                <div className="text-end">
                  <Button variant="light" className="me-2" onClick={() => navigate(-1)}>Cancel</Button>
                  <Button type="submit" disabled={busy}>{busy && <Spinner size="sm" className="me-2" />}Assign &amp; notify driver</Button>
                </div>
              </Card.Body>
            </Card>
          </Col>
          <Col lg={4}>
            {shipment && (
              <Card>
                <Card.Header className="d-flex justify-content-between">
                  <span className="mono">{shipment.trackingNumber}</span><StatusBadge status={shipment.currentStatus} />
                </Card.Header>
                <Card.Body className="small">
                  <div><span className="text-muted">Customer:</span> {shipment.customerName}</div>
                  <div><span className="text-muted">From:</span> {shipment.pickup?.city} ({shipment.originWarehouseName})</div>
                  <div><span className="text-muted">To:</span> {shipment.delivery?.addressLine1}, {shipment.delivery?.city} - {shipment.delivery?.pincode}</div>
                  <div><span className="text-muted">Load:</span> {shipment.totalPackages} pkg · {formatKg(shipment.totalWeightKg)}</div>
                  <div><span className="text-muted">Service:</span> {pretty(shipment.serviceType)} · ETA {formatDate(shipment.estimatedDeliveryDate)}</div>
                  <div><span className="text-muted">Payment:</span> {pretty(shipment.paymentMode)}</div>
                </Card.Body>
              </Card>
            )}
          </Col>
        </Row>
      </Form>
    </>
  );
}
