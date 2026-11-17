import { useEffect, useMemo, useState } from 'react';
import { Badge, Card, Col, ListGroup, Row } from 'react-bootstrap';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import { EmptyState } from '../../components/common/Loader';
import warehouseService from '../../services/warehouseService';
import useWarehouseScope from './WarehouseScope';
import ShipmentSummary from './ShipmentSummary';
import { PackingType } from '../../utils/constants';
import { v, yup } from '../../utils/validators';

const packSchema = yup.object({
  packageId: v.id('Package'),
  packingType: v.select('Packing type'),
  labelPrinted: yup.boolean(),
  remarks: v.opt(500),
});
const readySchema = yup.object({ storageLocation: v.req('Storage bin / location').max(30), remarks: v.opt(500) });

/** Prepare / pack each package, then mark the shipment READY_FOR_DISPATCH with its storage bin. */
export default function PackShipment() {
  const { queue, selector } = useWarehouseScope();
  const list = queue.data.filter((s) => s.currentStatus === 'RECEIVED' || s.currentStatus === 'PACKED');
  const [selectedId, setSelectedId] = useState(null);
  const shipment = list.find((s) => s.id === selectedId) || null;

  useEffect(() => {
    if (!shipment && list.length) setSelectedId(list[0].id);
  }, [list, shipment]);

  const unpacked = shipment?.packages?.filter((p) => p.currentStatus !== 'PACKED') || [];
  const packDefaults = useMemo(() => ({ packageId: unpacked[0]?.id ?? '', packingType: 'BOX', labelPrinted: true, remarks: '' }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [shipment?.id, unpacked.length]);

  return (
    <>
      <PageHeader title="Pack & mark ready" subtitle="Pack every package, then store the shipment in a bin">{selector}</PageHeader>
      <Row className="g-3">
        <Col lg={4}>
          <Card>
            <Card.Header>To pack ({list.length})</Card.Header>
            {list.length === 0 ? <EmptyState icon="box2" text="Nothing to pack. Receive shipments first." /> : (
              <ListGroup variant="flush">
                {list.map((s) => (
                  <ListGroup.Item key={s.id} action active={s.id === selectedId} onClick={() => setSelectedId(s.id)}>
                    <div className="d-flex justify-content-between">
                      <span className="mono fw-semibold">{s.trackingNumber}</span>
                      <Badge bg={s.currentStatus === 'PACKED' ? 'success' : 'info'}>{s.currentStatus}</Badge>
                    </div>
                    <div className="small opacity-75">
                      {s.packages.filter((p) => p.currentStatus === 'PACKED').length}/{s.packages.length} packed
                    </div>
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
              {unpacked.length > 0 ? (
                <Card>
                  <Card.Header>Prepare / pack</Card.Header>
                  <Card.Body>
                    <FormBuilder schema={packSchema} defaultValues={packDefaults} submitLabel="Mark package PACKED"
                      fields={[
                        { name: 'packageId', label: 'Package', type: 'select', required: true, col: 6,
                          options: unpacked.map((p) => ({ value: p.id, label: `${p.packageCode} · ${p.description}` })) },
                        { name: 'packingType', label: 'Packing type', type: 'select', options: PackingType, required: true, col: 6 },
                        { name: 'labelPrinted', label: 'Label printed', type: 'checkbox', col: 12 },
                        { name: 'remarks', label: 'Remarks', type: 'textarea', rows: 2, col: 12 },
                      ]}
                      onSubmit={async (values) => {
                        await warehouseService.pack(values);
                        toast.success('Package packed');
                        await queue.reload(true);
                      }} />
                  </Card.Body>
                </Card>
              ) : (
                <Card>
                  <Card.Header>Mark ready for dispatch</Card.Header>
                  <Card.Body>
                    <FormBuilder schema={readySchema} defaultValues={{ storageLocation: '', remarks: '' }}
                      submitLabel="Mark READY_FOR_DISPATCH"
                      fields={[
                        { name: 'storageLocation', label: 'Storage bin / location', required: true, placeholder: 'RACK-A3-B2' },
                        { name: 'remarks', label: 'Remarks' },
                      ]}
                      onSubmit={async (values) => {
                        await warehouseService.ready({ ...values, shipmentId: shipment.id });
                        toast.success('Ready for dispatch — fleet managers notified');
                        setSelectedId(null);
                        await queue.reload(true);
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
