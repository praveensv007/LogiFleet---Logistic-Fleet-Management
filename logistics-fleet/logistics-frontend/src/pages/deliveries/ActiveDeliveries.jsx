import { useState } from 'react';
import { Badge, Button, Card, Tab, Tabs } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import FormBuilder from '../../components/common/FormBuilder';
import { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useWebSocket from '../../hooks/useWebSocket';
import deliveryService from '../../services/deliveryService';
import { ResolutionStatus } from '../../utils/constants';
import { fileUrl, formatDateTime, pretty, timeAgo } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const resolveSchema = yup.object({ resolutionStatus: v.select('Status'), resolutionNotes: v.opt(500) });

export default function ActiveDeliveries() {
  const navigate = useNavigate();
  const active = useFetch(() => deliveryService.active(), [], { initial: [] });
  const problems = useFetch(() => deliveryService.problems(), [], { initial: [] });
  const [resolving, setResolving] = useState(null);
  const [lastEvent, setLastEvent] = useState(null);

  useWebSocket('/topic/fleet/active-deliveries', (m) => {
    setLastEvent(m);
    active.reload(true);
    if (m.event === 'PROBLEM') problems.reload(true);
  });

  const open = problems.data.filter((p) => p.resolutionStatus !== 'RESOLVED');

  return (
    <>
      <PageHeader title="Live deliveries" subtitle={lastEvent
        ? `Last update ${timeAgo(lastEvent.timestamp)}: ${lastEvent.trackingNumber} ${pretty(lastEvent.status)}`
        : 'Updates arrive live — no refresh needed'}>
        <Badge bg="success" className="align-self-center"><i className="bi bi-broadcast me-1" />Live</Badge>
      </PageHeader>
      <ErrorAlert error={active.error || problems.error} />
      <Card>
        <Tabs defaultActiveKey="active" className="px-3 pt-2">
          <Tab eventKey="active" title={`On the road (${active.data.length})`}>
            <DataTable rows={active.data} loading={active.loading} emptyText="No active deliveries" onRowClick={(d) => navigate(`/deliveries/${d.id}`)}
              columns={[
                { key: 'assignmentCode', label: 'Trip', render: (d) => <span className="mono">{d.assignmentCode}</span> },
                { key: 'trackingNumber', label: 'Shipment', render: (d) => <span className="mono">{d.trackingNumber}</span> },
                { key: 'driver', label: 'Driver / vehicle', render: (d) => <><div>{d.driverName}</div><div className="small text-muted">{d.vehicleNumber} · {d.driverPhone}</div></> },
                { key: 'to', label: 'Destination', render: (d) => d.deliveryAddress?.city },
                { key: 'status', label: 'Status', render: (d) => <StatusBadge status={d.currentStatus} /> },
                { key: 'expected', label: 'Expected', render: (d) => (
                  <span className={d.delayed ? 'text-danger fw-semibold' : ''}>
                    {formatDateTime(d.expectedDeliveryTime)}{d.delayed && ` (+${d.delayMinutes} min)`}
                  </span>
                ) },
                { key: 'loc', label: 'Last location', render: (d) => (d.lastLocationUpdate
                  ? <a href={`https://www.openstreetmap.org/?mlat=${d.currentLatitude}&mlon=${d.currentLongitude}#map=14/${d.currentLatitude}/${d.currentLongitude}`}
                    target="_blank" rel="noreferrer" onClick={(e) => e.stopPropagation()}>{timeAgo(d.lastLocationUpdate)}</a>
                  : '—') },
                { key: 'openProblems', label: 'Problems', render: (d) => (d.openProblems ? <Badge bg="danger">{d.openProblems}</Badge> : '—') },
              ]} />
          </Tab>
          <Tab eventKey="problems" title={<>Problems {open.length > 0 && <Badge bg="danger">{open.length}</Badge>}</>}>
            <DataTable rows={problems.data} loading={problems.loading} emptyText="No problems reported" columns={[
              { key: 'reportedAt', label: 'Reported', render: (p) => formatDateTime(p.reportedAt) },
              { key: 'trackingNumber', label: 'Shipment', render: (p) => <span className="mono">{p.trackingNumber}</span> },
              { key: 'driverName', label: 'Driver', render: (p) => `${p.driverName} · ${p.vehicleNumber}` },
              { key: 'problemType', label: 'Type', render: (p) => pretty(p.problemType) },
              { key: 'description', label: 'Description', render: (p) => <>{p.description}{p.photoUrl && <> · <a href={fileUrl(p.photoUrl)} target="_blank" rel="noreferrer">photo</a></>}</> },
              { key: 'resolutionStatus', label: 'Status', render: (p) => <><StatusBadge status={p.resolutionStatus} />{p.resolvedByName && <div className="small text-muted">{p.resolvedByName}</div>}</> },
              { key: 'x', label: '', className: 'text-end', render: (p) => p.resolutionStatus !== 'RESOLVED' && (
                <Button size="sm" variant="outline-primary" onClick={() => setResolving(p)}>Update</Button>
              ) },
            ]} />
          </Tab>
        </Tabs>
      </Card>
      <Modal show={!!resolving} onHide={() => setResolving(null)} title={`${pretty(resolving?.problemType)} · ${resolving?.trackingNumber}`}>
        {resolving && (
          <>
            <p className="small">{resolving.description}</p>
            <FormBuilder schema={resolveSchema} submitLabel="Save"
              defaultValues={{ resolutionStatus: resolving.resolutionStatus === 'OPEN' ? 'IN_PROGRESS' : 'RESOLVED', resolutionNotes: resolving.resolutionNotes || '' }}
              fields={[
                { name: 'resolutionStatus', label: 'Resolution status', type: 'select', options: ResolutionStatus, required: true, col: 12 },
                { name: 'resolutionNotes', label: 'Resolution notes', type: 'textarea', col: 12 },
              ]}
              onCancel={() => setResolving(null)}
              onSubmit={async (values) => {
                await deliveryService.resolveProblem(resolving.id, values);
                toast.success('Problem updated; driver notified');
                setResolving(null);
                problems.reload(true);
                active.reload(true);
              }} />
          </>
        )}
      </Modal>
    </>
  );
}
