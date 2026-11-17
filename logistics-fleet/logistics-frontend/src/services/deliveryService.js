import api, { params, unwrap, unwrapWithMessage } from './api';

const deliveryService = {
  // driver
  my: () => unwrap(api.get('/deliveries/my')),
  accept: (assignmentId) => unwrap(api.patch(`/assignments/${assignmentId}/accept`)),
  reject: (assignmentId, reason) => unwrap(api.patch(`/assignments/${assignmentId}/reject`, { reason })),
  get: (id) => unwrap(api.get(`/deliveries/${id}`)),
  updateStatus: (id, body) => unwrap(api.patch(`/deliveries/${id}/status`, body)),
  sendLocation: (id, body) => unwrap(api.post(`/deliveries/${id}/location`, body)),
  sendOtp: (id) => unwrapWithMessage(api.post(`/deliveries/${id}/send-otp`)),
  complete: (id, body) => unwrap(api.post(`/deliveries/${id}/complete`, body)),
  reportProblem: (id, body) => unwrap(api.post(`/deliveries/${id}/problems`, body)),
  // fleet manager
  active: () => unwrap(api.get('/deliveries/active')),
  problems: (status) => unwrap(api.get('/problems', { params: params({ status }) })),
  resolveProblem: (id, body) => unwrap(api.patch(`/problems/${id}/resolve`, body)),
};

export default deliveryService;
