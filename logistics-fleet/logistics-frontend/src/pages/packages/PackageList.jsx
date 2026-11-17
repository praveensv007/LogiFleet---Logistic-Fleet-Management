import { useState } from 'react';
import { Button, ButtonGroup, Card, Dropdown } from 'react-bootstrap';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import PackageForm from './PackageForm';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import shipmentService from '../../services/shipmentService';
import packageService from '../../services/packageService';
import { PackageStatus } from '../../utils/constants';
import { errorMessage, formatKg, pretty } from '../../utils/formatters';

const WAREHOUSE_EDITABLE = ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED'];

export default function PackageList() {
  const { id } = useParams();
  const { can } = useAuth();
  const warehouse = can('WAREHOUSE_OPS');
  const shipment = useFetch(() => shipmentService.get(id), [id]);
  const packages = useFetch(() => packageService.list(id), [id], { initial: [] });
  const [editing, setEditing] = useState(null); // {} for new, pkg for edit
  const [removing, setRemoving] = useState(null);

  if (shipment.loading) return <Loader />;
  if (shipment.error) return <ErrorAlert error={shipment.error} onRetry={shipment.reload} />;
  const s = shipment.data;
  const editable = warehouse ? WAREHOUSE_EDITABLE.includes(s.currentStatus) : s.currentStatus === 'CREATED';

  const refresh = () => { packages.reload(true); shipment.reload(true); };

  const save = async (values) => {
    if (editing?.id) await packageService.update(editing.id, values);
    else await packageService.add(id, values);
    toast.success(editing?.id ? 'Package updated' : 'Package added');
    setEditing(null);
    refresh();
  };

  const setStatus = async (p, status) => {
    try {
      await packageService.setStatus(p.id, status, 'Updated from package list');
      toast.success(`${p.packageCode} → ${pretty(status)}`);
      refresh();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  const remove = async () => {
    try {
      await packageService.remove(removing.id);
      toast.success('Package removed');
      setRemoving(null);
      refresh();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  return (
    <>
      <PageHeader title="Packages" subtitle={<><Link to={`/shipments/${id}`} className="mono">{s.trackingNumber}</Link> · {s.totalPackages} pcs · {formatKg(s.totalWeightKg)}</>}>
        <StatusBadge status={s.currentStatus} className="align-self-center" />
        {editable && <Button onClick={() => setEditing({})}><i className="bi bi-plus-lg me-1" />Add package</Button>}
      </PageHeader>
      {!editable && <div className="alert alert-info small">Packages are locked at this stage of the shipment.</div>}
      <Card>
        <DataTable rows={packages.data} loading={packages.loading} columns={[
          { key: 'packageCode', label: 'Code', render: (p) => <span className="mono">{p.packageCode}</span> },
          { key: 'description', label: 'Description', render: (p) => <>{p.description}{p.fragile && <span className="badge text-bg-danger ms-1">Fragile</span>}</> },
          { key: 'packageType', label: 'Type', render: (p) => pretty(p.packageType) },
          { key: 'quantity', label: 'Qty' },
          { key: 'weightKg', label: 'Weight', render: (p) => formatKg(p.weightKg) },
          { key: 'size', label: 'L×W×H (cm)', render: (p) => `${p.lengthCm}×${p.widthCm}×${p.heightCm}` },
          { key: 'volumetricWeightKg', label: 'Volumetric kg' },
          { key: 'packingType', label: 'Packing', render: (p) => pretty(p.packingType) },
          { key: 'currentStatus', label: 'Status', render: (p) => <StatusBadge status={p.currentStatus} /> },
          {
            key: 'actions', label: '', className: 'text-end', render: (p) => editable && (
              <ButtonGroup size="sm">
                <Button variant="outline-primary" onClick={() => setEditing(p)} title="Edit"><i className="bi bi-pencil" /></Button>
                {warehouse && (
                  <Dropdown as={ButtonGroup}>
                    <Dropdown.Toggle size="sm" variant="outline-secondary">Status</Dropdown.Toggle>
                    <Dropdown.Menu>
                      {PackageStatus.filter((x) => x !== p.currentStatus && x !== 'DELIVERED').map((x) => (
                        <Dropdown.Item key={x} onClick={() => setStatus(p, x)}>{pretty(x)}</Dropdown.Item>
                      ))}
                    </Dropdown.Menu>
                  </Dropdown>
                )}
                <Button variant="outline-danger" disabled={packages.data.length <= 1} onClick={() => setRemoving(p)} title="Remove"><i className="bi bi-trash" /></Button>
              </ButtonGroup>
            ),
          },
        ]} />
      </Card>
      <Modal show={!!editing} onHide={() => setEditing(null)} title={editing?.id ? `Edit ${editing.packageCode}` : 'Add package'} size="lg">
        {editing && <PackageForm pkg={editing.id ? editing : null} onSubmit={save} onCancel={() => setEditing(null)} />}
      </Modal>
      <ConfirmDialog show={!!removing} onHide={() => setRemoving(null)} title="Remove package"
        message={`Remove ${removing?.packageCode}? Totals and charge are recalculated.`} confirmLabel="Remove" onConfirm={remove} />
    </>
  );
}
