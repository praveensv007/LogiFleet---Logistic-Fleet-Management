import { Card, Col, Row, Table } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate, formatKg, pretty } from '../../utils/formatters';

/** Compact shipment card used on the warehouse floor screens. */
export default function ShipmentSummary({ shipment: s, showPackages = true }) {
  if (!s) return null;
  return (
    <Card className="mb-3">
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span><Link to={`/shipments/${s.id}`} className="mono">{s.trackingNumber}</Link> · {s.customerName}</span>
        <StatusBadge status={s.currentStatus} />
      </Card.Header>
      <Card.Body className="pb-1">
        <Row className="small">
          <Col sm={6} className="mb-2"><span className="text-muted">From:</span> {s.pickup?.city}, {s.pickup?.state}</Col>
          <Col sm={6} className="mb-2"><span className="text-muted">To:</span> {s.delivery?.city}, {s.delivery?.state}</Col>
          <Col sm={4} className="mb-2"><span className="text-muted">Packages:</span> {s.totalPackages}</Col>
          <Col sm={4} className="mb-2"><span className="text-muted">Weight:</span> {formatKg(s.totalWeightKg)}</Col>
          <Col sm={4} className="mb-2"><span className="text-muted">Service:</span> {pretty(s.serviceType)}</Col>
          <Col sm={6} className="mb-2"><span className="text-muted">Pickup:</span> {formatDate(s.pickupDate)} {pretty(s.pickupTimeSlot)}</Col>
          {s.storageLocation && <Col sm={6} className="mb-2"><span className="text-muted">Bin:</span> {s.storageLocation}</Col>}
          {s.assignment && (
            <Col xs={12} className="mb-2">
              <span className="text-muted">Trip:</span> {s.assignment.assignmentCode} · {s.assignment.vehicleNumber} · {s.assignment.driverName}{' '}
              <StatusBadge status={s.assignment.status} />
            </Col>
          )}
          {s.specialInstructions && <Col xs={12} className="mb-2 text-warning-emphasis"><i className="bi bi-info-circle me-1" />{s.specialInstructions}</Col>}
        </Row>
      </Card.Body>
      {showPackages && s.packages && (
        <Table size="sm" className="mb-0">
          <thead><tr><th>Package</th><th>Type</th><th>Qty</th><th>Weight</th><th>Size (cm)</th><th>Packing</th><th>Status</th></tr></thead>
          <tbody>
            {s.packages.map((p) => (
              <tr key={p.id}>
                <td className="mono">{p.packageCode}{p.fragile && <i className="bi bi-exclamation-diamond text-danger ms-1" title="Fragile" />}</td>
                <td>{pretty(p.packageType)}</td>
                <td>{p.quantity}</td>
                <td>{formatKg(p.weightKg)}</td>
                <td>{p.lengthCm}×{p.widthCm}×{p.heightCm}</td>
                <td>{pretty(p.packingType)}</td>
                <td><StatusBadge status={p.currentStatus} /></td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </Card>
  );
}
