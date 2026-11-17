import api, { params, unwrap } from './api';

const notificationService = {
  list: (q) => unwrap(api.get('/notifications', { params: params(q) })),
  unreadCount: () => unwrap(api.get('/notifications/unread-count')).then((d) => d.count),
  markRead: (id) => unwrap(api.patch(`/notifications/${id}/read`)),
  markAllRead: () => unwrap(api.patch('/notifications/read-all')),
};

export default notificationService;
