import api, { params, unwrap } from './api';

const vehicleService = {
  list: (q) => unwrap(api.get('/vehicles', { params: params(q) })),
  available: (minKg) => unwrap(api.get('/vehicles/available', { params: params({ minKg }) })),
  get: (id) => unwrap(api.get(`/vehicles/${id}`)),
  create: (body) => unwrap(api.post('/vehicles', body)),
  update: (id, body) => unwrap(api.put(`/vehicles/${id}`, body)),
  setStatus: (id, status) => unwrap(api.patch(`/vehicles/${id}/status`, { status })),
  addDocument: (id, body) => unwrap(api.post(`/vehicles/${id}/documents`, body)),
  deleteDocument: (id, docId) => unwrap(api.delete(`/vehicles/${id}/documents/${docId}`)),
};

export default vehicleService;
