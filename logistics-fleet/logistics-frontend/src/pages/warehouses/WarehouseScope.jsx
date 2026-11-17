import { useEffect, useState } from 'react';
import { Form } from 'react-bootstrap';
import useAuth from '../../hooks/useAuth';
import useFetch from '../../hooks/useFetch';
import useWebSocket from '../../hooks/useWebSocket';
import warehouseService from '../../services/warehouseService';

const KEY = 'logifleet_admin_warehouse';

/**
 * Warehouse staff work in their own warehouse; admins pick one from a dropdown.
 * Returns the warehouse id, its live queue and a selector element. The queue refreshes on
 * /topic/warehouse/{id} pushes.
 */
export default function useWarehouseScope() {
  const { user } = useAuth();
  const isStaff = user.role === 'WAREHOUSE_STAFF';
  const warehouses = useFetch(() => warehouseService.list('ACTIVE'), [], { initial: [] });
  const [selected, setSelected] = useState(() => (isStaff ? user.warehouseId : Number(localStorage.getItem(KEY)) || null));

  useEffect(() => {
    if (!isStaff && !selected && warehouses.data.length) setSelected(warehouses.data[0].id);
  }, [isStaff, selected, warehouses.data]);

  const warehouseId = isStaff ? user.warehouseId : selected;
  const queue = useFetch(() => (warehouseId ? warehouseService.queue(warehouseId) : Promise.resolve([])), [warehouseId], { initial: [] });
  useWebSocket(warehouseId ? `/topic/warehouse/${warehouseId}` : null, () => queue.reload(true));

  const current = warehouses.data.find((w) => w.id === warehouseId);
  const selector = isStaff ? (
    <span className="badge text-bg-light border align-self-center fs-6 fw-normal">
      <i className="bi bi-building me-1" />{current?.name || 'No warehouse assigned'}
    </span>
  ) : (
    <Form.Select size="sm" style={{ maxWidth: 280 }} value={warehouseId || ''}
      onChange={(e) => { setSelected(Number(e.target.value)); localStorage.setItem(KEY, e.target.value); }}>
      {warehouses.data.map((w) => <option key={w.id} value={w.id}>{w.name} ({w.warehouseCode})</option>)}
    </Form.Select>
  );

  return { warehouseId, warehouse: current, queue, selector, missing: isStaff && !user.warehouseId };
}
