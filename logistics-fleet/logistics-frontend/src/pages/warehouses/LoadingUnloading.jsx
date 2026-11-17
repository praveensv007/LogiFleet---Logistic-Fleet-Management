import { useMemo, useState } from 'react';
import { Card, Col, ListGroup, Row } from 'react-bootstrap';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import StatusBadge from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/Loader';
import warehouseService from '../../services/warehouseService';
import useAuth from '../../hooks/useAuth';
import useWarehouseScope from './WarehouseScope';
import ShipmentSummary from './ShipmentSummary';
import { formatDateTime } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  transactionType: v.select('Transaction type'),
  packageCount: v.num('Package count').required('Package count is required').integer().min(1),
  remarks: v.opt(500),
});

/** Confirms loading onto (or unloading from) the vehicle of an accepted assignment. */
export default function LoadingUnloading() {
  const { user } = useAuth();
  const { queue, selector } = useWarehouseScope();
  const list = queue.data.filter((s) => s.currentStatus === 'ASSIGNED' && s.assignment);
  const [selectedId, setSelectedId] = useState(null);
  const shipment = list.find((s) => s.id === selectedId) || list[0] || null;
  const accepted = shipment?.assignment?.status === 'ACCEPTED';
  const allLoaded = shipment?.packages?.every((p) => p.currentStatus === 'LOADED');

  // assignment / vehicle / time / confirmed-by are the read-only "auto" fields from the spec
  const defaults = useMemo(() => (shipment ? {
    assignment: shipment.assignment.assignmentCode,
    vehicle: shipment.assignment.vehicleNumber,
    time: formatDateTime(new Date()),
    confirmedBy: user.fullName,
    transactionType: allLoaded ? 'UNLOADING' : 'LOADING',
    packageCount: shipment.totalPackages,
    remarks: '',
  } : undefined), [shipment, allLoaded, user.fullName]);

  return (
    <>
      <PageHeader title="Loading / unloading" subtitle="Load accepted trips onto their vehicle before dispatch">{selector}</PageHeader>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>Assigned trips ({list.length})</Card.Header>
            {list.length === 0 ? <EmptyState icon="truck-flatbed" text="No assigned shipments at this warehouse" /> : (
              <ListGroup variant="flush">
                {list.map((s) => (
                  <ListGroup.Item key={s.id} action active={s.id === shipment?.id} onClick={() => setSelectedId(s.id)}>
                    <div className="d-flex justify-content-between">
                      <span className="mono fw-semibold">{s.trackingNumber}</span>
                      <StatusBadge status={s.assignment.status} />
                    </div>
                    <div className="small opacity-75">{s.assignment.vehicleNumber} · {s.assignment.driverName}</div>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            )}
          </Card>
        </Col>
        <Col lg={8}>
          {shipment && (
            <>
              <ShipmentSummary shipment={shipment} />
              <Card>
                <Card.Header>Record {allLoaded ? 'unloading' : 'loading'}</Card.Header>
                <Card.Body>
                  {!accepted && !allLoaded ? (
                    <div className="text-muted"><i className="bi bi-hourglass-split me-2" />Waiting for {shipment.assignment.driverName} to accept the trip.
                      You'll be notified the moment they do.</div>
                  ) : (
                    <FormBuilder key={shipment.id + String(allLoaded)} schema={schema} defaultValues={defaults}
                      submitLabel={allLoaded ? 'Confirm unloading' : 'Confirm loading'}
                      fields={[
                        { name: 'assignment', label: 'Assignment', disabled: true, col: 4 },
                        { name: 'vehicle', label: 'Vehicle', disabled: true, col: 4 },
                        { name: 'transactionType', label: 'Transaction type', type: 'select', options: ['LOADING', 'UNLOADING'], required: true, col: 4 },
                        { name: 'packageCount', label: 'Package count', type: 'number', required: true, col: 4 },
                        { name: 'time', label: 'Date-time', disabled: true, col: 4 },
                        { name: 'confirmedBy', label: 'Confirmed by', disabled: true, col: 4 },
                        { name: 'remarks', label: 'Remarks', type: 'textarea', rows: 2, col: 12 },
                      ]}
                      onSubmit={async (values) => {
                        await warehouseService.load({
                          assignmentId: shipment.assignment.id, transactionType: values.transactionType,
                          packageCount: values.packageCount, remarks: values.remarks,
                        });
                        toast.success(values.transactionType === 'LOADING' ? 'Loading confirmed — ready to dispatch' : 'Unloading recorded');
                        queue.reload(true);
                      }} />
                  )}
                </Card.Body>
              </Card>
            </>
          )}
        </Col>
      </Row>
    </>
  );
}
