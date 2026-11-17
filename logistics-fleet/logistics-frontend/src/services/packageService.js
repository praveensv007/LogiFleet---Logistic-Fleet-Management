import api, { unwrap } from './api';

const packageService = {
  list: (shipmentId) => unwrap(api.get(`/shipments/${shipmentId}/packages`)),
  add: (shipmentId, body) => unwrap(api.post(`/shipments/${shipmentId}/packages`, body)),
  update: (id, body) => unwrap(api.put(`/packages/${id}`, body)),
  setStatus: (id, status, remarks) => unwrap(api.patch(`/packages/${id}/status`, { status, remarks })),
  remove: (id) => unwrap(api.delete(`/packages/${id}`)),
};

export default packageService;
