import api, { params, unwrap } from './api';

const shipmentService = {
  create: (body) => unwrap(api.post('/shipments', body)),
  list: (q) => unwrap(api.get('/shipments', { params: params(q) })),
  get: (id) => unwrap(api.get(`/shipments/${id}`)),
  update: (id, body) => unwrap(api.put(`/shipments/${id}`, body)),
  cancel: (id, reason) => unwrap(api.patch(`/shipments/${id}/cancel`, { reason })),
  history: (id) => unwrap(api.get(`/shipments/${id}/history`)),
  track: (trackingNumber) => unwrap(api.get(`/track/${encodeURIComponent(trackingNumber.trim())}`)),
};

export default shipmentService;
