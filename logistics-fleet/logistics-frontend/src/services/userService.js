import api, { params, unwrap } from './api';

const userService = {
  list: (q) => unwrap(api.get('/users', { params: params(q) })),
  get: (id) => unwrap(api.get(`/users/${id}`)),
  create: (body) => unwrap(api.post('/users', body)),
  update: (id, body) => unwrap(api.put(`/users/${id}`, body)),
  setStatus: (id, status) => unwrap(api.patch(`/users/${id}/status`, { status })),
  resetPassword: (id) => unwrap(api.post(`/users/${id}/reset-password`)),
  roles: () => unwrap(api.get('/roles')),
  permissions: () => unwrap(api.get('/permissions')),
  saveRolePermissions: (id, body) => unwrap(api.put(`/roles/${id}/permissions`, body)),
};

export default userService;
