import api, { params, unwrap } from './api';

const assignmentService = {
  list: (q) => unwrap(api.get('/assignments', { params: params(q) })),
  pendingShipments: () => unwrap(api.get('/assignments/pending-shipments')),
  get: (id) => unwrap(api.get(`/assignments/${id}`)),
  create: (body) => unwrap(api.post('/assignments', body)),
  update: (id, body) => unwrap(api.put(`/assignments/${id}`, body)),
  cancel: (id, reason) => unwrap(api.patch(`/assignments/${id}/cancel`, { reason })),
};

export default assignmentService;
