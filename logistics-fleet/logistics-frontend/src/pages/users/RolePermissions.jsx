import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Nav, Spinner, Table } from 'react-bootstrap';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import userService from '../../services/userService';
import { errorMessage } from '../../utils/formatters';

export default function RolePermissions() {
  const roles = useFetch(() => userService.roles(), [], { initial: [] });
  const perms = useFetch(() => userService.permissions(), [], { initial: [] });
  const [activeId, setActiveId] = useState(null);
  const [selected, setSelected] = useState(new Set());
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);

  const active = roles.data.find((r) => r.id === activeId) || roles.data[0];

  useEffect(() => {
    if (active) {
      setActiveId(active.id);
      setSelected(new Set(active.permissions.map((p) => p.id)));
      setDescription(active.description || '');
    }
  }, [active?.id, roles.data]); // eslint-disable-line react-hooks/exhaustive-deps

  const byModule = useMemo(() => perms.data.reduce((acc, p) => {
    (acc[p.module] = acc[p.module] || []).push(p);
    return acc;
  }, {}), [perms.data]);

  if (roles.loading || perms.loading) return <Loader />;

  const toggle = (id) => setSelected((s) => {
    const n = new Set(s);
    if (n.has(id)) n.delete(id); else n.add(id);
    return n;
  });

  const save = async () => {
    setSaving(true);
    try {
      await userService.saveRolePermissions(active.id, { description, permissionIds: [...selected] });
      toast.success(`${active.name} saved. Users get the new rights on their next request.`);
      roles.reload(true);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Roles & permissions" subtitle="Tick what each role may do. The API checks these on every call." />
      <ErrorAlert error={roles.error || perms.error} />
      <Card>
        <Card.Header className="pb-0 border-0">
          <Nav variant="tabs" activeKey={String(active?.id)} onSelect={(k) => setActiveId(Number(k))}>
            {roles.data.map((r) => (
              <Nav.Item key={r.id}><Nav.Link eventKey={String(r.id)}>{r.name.replace('ROLE_', '').replace('_', ' ')}</Nav.Link></Nav.Item>
            ))}
          </Nav>
        </Card.Header>
        {active && (
          <Card.Body>
            <Form.Group className="mb-3">
              <Form.Label>Role name</Form.Label>
              <Form.Control value={active.name} disabled />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control value={description} maxLength={255} onChange={(e) => setDescription(e.target.value)} />
            </Form.Group>
            <Table bordered size="sm">
              <thead><tr><th style={{ width: 160 }}>Module</th><th>Permissions</th></tr></thead>
              <tbody>
                {Object.entries(byModule).map(([module, list]) => (
                  <tr key={module}>
                    <td className="fw-semibold">{module}</td>
                    <td>
                      <div className="d-flex flex-wrap gap-3">
                        {list.map((p) => (
                          <Form.Check key={p.id} id={`perm-${p.id}`} type="checkbox" checked={selected.has(p.id)}
                            onChange={() => toggle(p.id)} label={<span title={p.name}><code>{p.code}</code></span>} />
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <div className="text-end">
              <Button onClick={save} disabled={saving}>{saving && <Spinner size="sm" className="me-2" />}Save permissions</Button>
            </div>
          </Card.Body>
        )}
      </Card>
    </>
  );
}
