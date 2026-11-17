import api, { params, unwrap } from './api';

const driverService = {
  list: (q) => unwrap(api.get('/drivers', { params: params(q) })),
  available: () => unwrap(api.get('/drivers/available')),
  get: (id) => unwrap(api.get(`/drivers/${id}`)),
  create: (body) => unwrap(api.post('/drivers', body)),
  update: (id, body) => unwrap(api.put(`/drivers/${id}`, body)),
  setAvailability: (id, availabilityStatus) => unwrap(api.patch(`/drivers/${id}/availability`, { availabilityStatus })),
};

export default driverService;
