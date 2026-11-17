import { useMemo, useState } from 'react';
import { Button, Card, Col, Form, InputGroup, ListGroup, Row, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import { EmptyState } from '../../components/common/Loader';
import warehouseService from '../../services/warehouseService';
import useWarehouseScope from './WarehouseScope';
import ShipmentSummary from './ShipmentSummary';
import { ConditionStatus } from '../../utils/constants';
import { errorMessage, formatDateTime, timeAgo } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  packageCount: v.num('Package count').required('Package count is required').integer().min(1, 'At least 1'),
  conditionStatus: v.select('Condition'),
  remarks: v.opt(500),
});

export default function ReceiveShipment() {
  const { queue, selector } = useWarehouseScope();
  const [tracking, setTracking] = useState('');
  const [shipment, setShipment] = useState(null);
  const [looking, setLooking] = useState(false);
  const waiting = queue.data.filter((s) => s.currentStatus === 'CREATED');

  const lookup = async (tn = tracking) => {
    if (!tn.trim()) return;
    setLooking(true);
    try {
      const s = await warehouseService.lookup(tn.trim().toUpperCase());
      setShipment(s);
      setTracking(s.trackingNumber);
      if (s.currentStatus !== 'CREATED') toast.warn(`This shipment is already ${s.currentStatus}`);
    } catch (e) {
      setShipment(null);
      toast.error(errorMessage(e, 'Shipment not found'));
    } finally {
      setLooking(false);
    }
  };

  const defaults = useMemo(() => (shipment ? { packageCount: shipment.totalPackages, conditionStatus: 'GOOD', remarks: '' } : undefined), [shipment]);

  return (
    <>
      <PageHeader title="Receive shipment" subtitle="Scan or type the tracking number, confirm count and condition">{selector}</PageHeader>
      <Row className="g-3">
        <Col lg={8}>
          <Card className="mb-3">
            <Card.Body>
              <Form onSubmit={(e) => { e.preventDefault(); lookup(); }}>
                <Form.Label>Tracking number</Form.Label>
                <InputGroup>
                  <InputGroup.Text><i className="bi bi-upc-scan" /></InputGroup.Text>
                  <Form.Control autoFocus value={tracking} placeholder="LGX-20261012-00057" className="mono"
                    onChange={(e) => setTracking(e.target.value)} />
                  <Button type="submit" disabled={looking}>{looking ? <Spinner size="sm" /> : 'Find'}</Button>
                </InputGroup>
              </Form>
            </Card.Body>
          </Card>
          {shipment && (
            <>
              <ShipmentSummary shipment={shipment} />
              {shipment.currentStatus === 'CREATED' && (
                <Card>
                  <Card.Header>Receive · {formatDateTime(new Date())}</Card.Header>
                  <Card.Body>
                    <FormBuilder schema={schema} defaultValues={defaults} submitLabel="Confirm receipt"
                      fields={[
                        { name: 'packageCount', label: 'Package count received', type: 'number', required: true, col: 4,
                          help: `Expected ${shipment.totalPackages}` },
                        { name: 'conditionStatus', label: 'Condition', type: 'select', options: ConditionStatus, required: true, col: 4 },
                        { name: 'remarks', label: 'Remarks', type: 'textarea', col: 12, rows: 2 },
                      ]}
                      onSubmit={async (values) => {
                        const res = await warehouseService.receive({ ...values, trackingNumber: shipment.trackingNumber });
                        toast.success(`${res.trackingNumber} received`);
                        setShipment(res);
                        queue.reload(true);
                      }} />
                  </Card.Body>
                </Card>
              )}
            </>
          )}
        </Col>
        <Col lg={4}>
          <Card>
            <Card.Header>Expected at this warehouse ({waiting.length})</Card.Header>
            {waiting.length === 0 ? <EmptyState icon="check2-circle" text="No shipments waiting" /> : (
              <ListGroup variant="flush">
                {waiting.map((s) => (
                  <ListGroup.Item key={s.id} action onClick={() => lookup(s.trackingNumber)}>
                    <div className="mono fw-semibold">{s.trackingNumber}</div>
                    <div className="small text-muted">{s.customerName} · {s.totalPackages} pkg · booked {timeAgo(s.createdAt)}</div>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            )}
          </Card>
        </Col>
      </Row>
    </>
  );
}
