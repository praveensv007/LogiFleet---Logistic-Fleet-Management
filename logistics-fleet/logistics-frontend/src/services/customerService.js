import api, { params, unwrap } from './api';

const customerService = {
  list: (q) => unwrap(api.get('/customers', { params: params(q) })),
  me: () => unwrap(api.get('/customers/me')),
  get: (id) => unwrap(api.get(`/customers/${id}`)),
  update: (id, body) => unwrap(api.put(`/customers/${id}`, body)),
  shipments: (id) => unwrap(api.get(`/customers/${id}/shipments`)),
};

export default customerService;
