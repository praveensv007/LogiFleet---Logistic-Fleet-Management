import api, { params, unwrap } from './api';

const maintenanceService = {
  list: (q) => unwrap(api.get('/maintenance', { params: params(q) })),
  due: () => unwrap(api.get('/maintenance/due')),
  get: (id) => unwrap(api.get(`/maintenance/${id}`)),
  create: (body) => unwrap(api.post('/maintenance', body)),
  update: (id, body) => unwrap(api.put(`/maintenance/${id}`, body)),
  complete: (id, body) => unwrap(api.patch(`/maintenance/${id}/complete`, body)),
};

export default maintenanceService;
