import { useState } from 'react';
import { Button, ButtonGroup, Card, Col, Form, Row, Tab, Tabs } from 'react-bootstrap';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatCard from '../../components/common/StatCard';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import reportService from '../../services/reportService';
import warehouseService from '../../services/warehouseService';
import driverService from '../../services/driverService';
import vehicleService from '../../services/vehicleService';
import { ShipmentStatus } from '../../utils/constants';
import { PALETTE } from '../../utils/chartSetup';
import { errorMessage, formatMoney, isoDate, pretty } from '../../utils/formatters';

const REPORTS = [
  { key: 'shipments-by-day', title: 'Shipments per day', load: reportService.shipmentsByDay },
  { key: 'on-time', title: 'On-time vs delayed', load: reportService.onTime },
  { key: 'driver-performance', title: 'Deliveries per driver', load: reportService.driverPerformance },
  { key: 'vehicle-utilisation', title: 'Vehicle utilisation', load: reportService.vehicleUtilisation },
  { key: 'maintenance-cost', title: 'Maintenance cost', load: reportService.maintenanceCost },
  { key: 'pending-by-warehouse', title: 'Pending by warehouse', load: reportService.pendingByWarehouse },
];

const columnsFor = (rows) => (rows?.[0] ? Object.keys(rows[0]).map((k) => ({
  key: k,
  label: k.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').toLowerCase(),
  render: (r) => (k.toLowerCase().includes('cost') ? formatMoney(r[k]) : String(r[k] ?? '—')),
})) : []);

function ReportChart({ report, rows }) {
  if (!rows?.length) return null;
  const opts = { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } };
  switch (report) {
    case 'shipments-by-day':
      return (
        <Line options={opts} data={{
          labels: rows.map((r) => r.date.slice(5)),
          datasets: [
            { label: 'Created', data: rows.map((r) => r.created), borderColor: PALETTE[0], backgroundColor: 'rgba(29,53,87,0.1)', fill: true, tension: 0.3 },
            { label: 'Delivered', data: rows.map((r) => r.delivered), borderColor: PALETTE[2], tension: 0.3 },
            { label: 'Cancelled', data: rows.map((r) => r.cancelled), borderColor: PALETTE[3], tension: 0.3 },
          ],
        }} />
      );
    case 'on-time': {
      const r = rows[0];
      return (
        <Row className="align-items-center">
          <Col md={5} style={{ height: 260 }}>
            <Doughnut options={opts} data={{ labels: ['On time', 'Delayed'], datasets: [{ data: [r.onTime, r.delayed], backgroundColor: [PALETTE[2], PALETTE[3]] }] }} />
          </Col>
          <Col md={7}>
            <Row className="g-2">
              <Col sm={4}><StatCard icon="box-seam" label="Delivered" value={r.totalDelivered} /></Col>
              <Col sm={4}><StatCard icon="check2-circle" color="green" label="On time" value={`${r.onTimePercent}%`} /></Col>
              <Col sm={4}><StatCard icon="alarm" color="red" label="Delayed" value={`${r.delayedPercent}%`} /></Col>
            </Row>
          </Col>
        </Row>
      );
    }
    case 'driver-performance':
      return (
        <Bar options={opts} data={{
          labels: rows.map((r) => r.driverName),
          datasets: [
            { label: 'On time', data: rows.map((r) => r.onTime), backgroundColor: PALETTE[2] },
            { label: 'Delayed', data: rows.map((r) => r.delayed), backgroundColor: PALETTE[3] },
            { label: 'Rejected', data: rows.map((r) => r.rejected), backgroundColor: PALETTE[5] },
          ],
        }} />
      );
    case 'vehicle-utilisation':
      return (
        <Bar options={{ ...opts, scales: { y: { max: 100, title: { display: true, text: '%' } } } }} data={{
          labels: rows.map((r) => r.vehicleNumber),
          datasets: [{ label: 'Utilisation %', data: rows.map((r) => r.utilisationPercent), backgroundColor: PALETTE[1] }],
        }} />
      );
    case 'maintenance-cost':
      return (
        <Bar options={opts} data={{
          labels: rows.map((r) => r.vehicleNumber),
          datasets: [
            { label: 'Labour', data: rows.map((r) => r.labourCost), backgroundColor: PALETTE[0] },
            { label: 'Parts', data: rows.map((r) => r.partsCost), backgroundColor: PALETTE[1] },
          ],
        }} />
      );
    case 'pending-by-warehouse':
      return (
        <Bar options={{ ...opts, scales: { x: { stacked: true }, y: { stacked: true } } }} data={{
          labels: rows.map((r) => r.warehouseName),
          datasets: ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED'].map((s, i) => ({
            label: pretty(s), data: rows.map((r) => r[s]), backgroundColor: PALETTE[i],
          })),
        }} />
      );
    default:
      return null;
  }
}

export default function ReportsDashboard() {
  const [filters, setFilters] = useState({
    from: isoDate(new Date(Date.now() - 29 * 86400000)), to: isoDate(), warehouseId: '', driverId: '', vehicleId: '', status: '',
  });
  const [active, setActive] = useState(REPORTS[0].key);
  const report = REPORTS.find((r) => r.key === active);
  const { data, loading, error, reload } = useFetch(() => report.load(filters), [active, filters]);
  const warehouses = useFetch(() => warehouseService.list(), [], { initial: [] });
  const drivers = useFetch(() => driverService.list({ size: 100 }).then((p) => p.content), [], { initial: [] });
  const vehicles = useFetch(() => vehicleService.list({ size: 100 }).then((p) => p.content), [], { initial: [] });

  const set = (k) => (e) => setFilters((f) => ({ ...f, [k]: e.target.value }));
  const exportAs = async (type) => {
    try {
      await reportService.exportReport(active, type, filters);
    } catch (e) {
      toast.error(errorMessage(e, 'Export failed'));
    }
  };

  const select = (k, label, options) => (
    <Col md={2} xs={6}>
      <Form.Label className="small">{label}</Form.Label>
      <Form.Select size="sm" value={filters[k]} onChange={set(k)}>
        <option value="">All</option>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </Form.Select>
    </Col>
  );

  return (
    <>
      <PageHeader title="Reports" subtitle="Filter, chart and export">
        <ButtonGroup>
          <Button variant="outline-secondary" onClick={() => exportAs('csv')}><i className="bi bi-filetype-csv me-1" />CSV</Button>
          <Button variant="outline-secondary" onClick={() => exportAs('pdf')}><i className="bi bi-file-earmark-pdf me-1" />PDF</Button>
        </ButtonGroup>
      </PageHeader>
      <Card className="mb-3">
        <Card.Body>
          <Row className="g-2">
            <Col md={2} xs={6}><Form.Label className="small">From</Form.Label><Form.Control size="sm" type="date" value={filters.from} max={filters.to} onChange={set('from')} /></Col>
            <Col md={2} xs={6}><Form.Label className="small">To</Form.Label><Form.Control size="sm" type="date" value={filters.to} min={filters.from} onChange={set('to')} /></Col>
            {select('warehouseId', 'Warehouse', warehouses.data.map((w) => ({ value: w.id, label: w.name })))}
            {select('driverId', 'Driver', drivers.data.map((d) => ({ value: d.id, label: d.fullName })))}
            {select('vehicleId', 'Vehicle', vehicles.data.map((vh) => ({ value: vh.id, label: vh.vehicleNumber })))}
            {select('status', 'Status', ShipmentStatus.map((s) => ({ value: s, label: pretty(s) })))}
          </Row>
        </Card.Body>
      </Card>
      <Card>
        <Tabs activeKey={active} onSelect={(k) => setActive(k)} className="px-3 pt-2">
          {REPORTS.map((r) => <Tab key={r.key} eventKey={r.key} title={r.title} />)}
        </Tabs>
        <Card.Body>
          <ErrorAlert error={error} onRetry={reload} />
          {loading ? <Loader /> : (
            <>
              <div style={{ height: active === 'on-time' ? 'auto' : 300 }} className="mb-3">
                <ReportChart report={active} rows={data} />
              </div>
              <DataTable rows={data} columns={columnsFor(data)} rowKey="__none" emptyText="No data for these filters" />
            </>
          )}
        </Card.Body>
      </Card>
    </>
  );
}
