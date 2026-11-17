import { useState } from 'react';
import { Alert, Card, Col, ListGroup, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import StatusBadge from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/Loader';
import warehouseService from '../../services/warehouseService';
import useWarehouseScope from './WarehouseScope';
import ShipmentSummary from './ShipmentSummary';
import { isoDateTime } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  dispatchTime: v.req('Dispatch date-time'),
  gatePassNumber: v.req('Gate pass number').max(30),
  remarks: v.opt(500),
});

export default function DispatchShipment() {
  const { queue, selector } = useWarehouseScope();
  const list = queue.data.filter((s) => s.currentStatus === 'ASSIGNED' && s.assignment?.status === 'ACCEPTED');
  const [selectedId, setSelectedId] = useState(null);
  const shipment = list.find((s) => s.id === selectedId) || list[0] || null;
  const loaded = shipment?.packages?.every((p) => p.currentStatus === 'LOADED');

  return (
    <>
      <PageHeader title="Dispatch" subtitle="Issue the gate pass once loading is confirmed">{selector}</PageHeader>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>Accepted trips ({list.length})</Card.Header>
            {list.length === 0 ? <EmptyState icon="send" text="No accepted trips waiting to leave" /> : (
              <ListGroup variant="flush">
                {list.map((s) => {
                  const ready = s.packages.every((p) => p.currentStatus === 'LOADED');
                  return (
                    <ListGroup.Item key={s.id} action active={s.id === shipment?.id} onClick={() => setSelectedId(s.id)}>
                      <div className="d-flex justify-content-between">
                        <span className="mono fw-semibold">{s.trackingNumber}</span>
                        <StatusBadge status={ready ? 'LOADED' : 'PACKED'} />
                      </div>
                      <div className="small opacity-75">{s.assignment.vehicleNumber} · {s.assignment.driverName}</div>
                    </ListGroup.Item>
                  );
                })}
              </ListGroup>
            )}
          </Card>
        </Col>
        <Col lg={8}>
          {shipment && (
            <>
              <ShipmentSummary shipment={shipment} />
              {!loaded ? (
                <Alert variant="warning">
                  Confirm loading of every package first. <Link to="/warehouse/loading">Go to loading</Link>
                </Alert>
              ) : (
                <Card>
                  <Card.Header>Dispatch {shipment.trackingNumber}</Card.Header>
                  <Card.Body>
                    <FormBuilder key={shipment.id} schema={schema} submitLabel="Dispatch"
                      defaultValues={{ dispatchTime: isoDateTime(), gatePassNumber: '', remarks: '' }}
                      fields={[
                        { name: 'dispatchTime', label: 'Dispatch date-time', type: 'datetime-local', required: true },
                        { name: 'gatePassNumber', label: 'Gate pass number', required: true, placeholder: 'GP-2026-0101' },
                        { name: 'remarks', label: 'Remarks', type: 'textarea', rows: 2, col: 12 },
                      ]}
                      onSubmit={async (values) => {
                        await warehouseService.dispatch({ ...values, shipmentId: shipment.id });
                        toast.success(`${shipment.trackingNumber} dispatched — driver and customer notified`);
                        setSelectedId(null);
                        queue.reload(true);
                      }} />
                  </Card.Body>
                </Card>
              )}
            </>
          )}
        </Col>
      </Row>
    </>
  );
}
