import api, { params, unwrap } from './api';

const auditService = {
  search: (q) => unwrap(api.get('/audit-logs', { params: params(q) })),
};

export default auditService;
