import api, { params, unwrap } from './api';

const warehouseService = {
  list: (status) => unwrap(api.get('/warehouses', { params: params({ status }) })),
  get: (id) => unwrap(api.get(`/warehouses/${id}`)),
  create: (body) => unwrap(api.post('/warehouses', body)),
  update: (id, body) => unwrap(api.put(`/warehouses/${id}`, body)),
  // floor operations
  receive: (body) => unwrap(api.post('/warehouse/receive', body)),
  pack: (body) => unwrap(api.post('/warehouse/pack', body)),
  ready: (body) => unwrap(api.post('/warehouse/ready', body)),
  load: (body) => unwrap(api.post('/warehouse/load', body)),
  dispatch: (body) => unwrap(api.post('/warehouse/dispatch', body)),
  queue: (warehouseId) => unwrap(api.get(`/warehouse/${warehouseId}/queue`)),
  lookup: (trackingNumber) => unwrap(api.get(`/warehouse/lookup/${encodeURIComponent(trackingNumber)}`)),
  transactions: (shipmentId) => unwrap(api.get(`/warehouse/shipments/${shipmentId}/transactions`)),
};

export default warehouseService;
