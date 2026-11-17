import { Badge, Dropdown } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext';
import useAuth from '../hooks/useAuth';
import { timeAgo } from '../utils/formatters';

/** Where a notification should take the user. */
export const notificationLink = (n, role) => {
  if (!n.referenceId) return null;
  switch (n.referenceType) {
    case 'SHIPMENT': return `/shipments/${n.referenceId}`;
    case 'ASSIGNMENT': return role === 'DRIVER' ? '/deliveries/my' : '/assignments';
    case 'DELIVERY': return role === 'DRIVER' ? `/deliveries/${n.referenceId}` : '/deliveries/active';
    case 'VEHICLE': return `/vehicles/${n.referenceId}`;
    case 'DRIVER': return role === 'DRIVER' ? '/profile' : `/drivers/${n.referenceId}`;
    case 'MAINTENANCE': return `/maintenance/${n.referenceId}/edit`;
    default: return null;
  }
};

export default function NotificationBell() {
  const { unread, latest, markRead, markAllRead } = useNotifications();
  const { user } = useAuth();
  const navigate = useNavigate();

  const open = async (n) => {
    if (!n.read) await markRead(n.id);
    const link = notificationLink(n, user?.role);
    if (link) navigate(link);
  };

  return (
    <Dropdown align="end">
      <Dropdown.Toggle variant="light" className="position-relative border-0 bg-transparent" id="notif-bell">
        <i className="bi bi-bell fs-5" />
        {unread > 0 && (
          <Badge bg="danger" pill className="position-absolute top-0 start-100 translate-middle" style={{ fontSize: '0.65rem' }}>
            {unread > 99 ? '99+' : unread}
          </Badge>
        )}
      </Dropdown.Toggle>
      <Dropdown.Menu style={{ width: 360, maxHeight: 440, overflowY: 'auto' }} className="p-0">
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
          <strong>Notifications</strong>
          {unread > 0 && <button type="button" className="btn btn-link btn-sm p-0" onClick={markAllRead}>Mark all read</button>}
        </div>
        {latest.length === 0 && <div className="text-muted small p-3">You're all caught up.</div>}
        {latest.map((n) => (
          <Dropdown.Item key={n.id} onClick={() => open(n)} className={`notif-item py-2 ${n.read ? '' : 'unread'}`}
            style={{ whiteSpace: 'normal' }}>
            <div className="fw-semibold small">{n.title}</div>
            <div className="small text-muted">{n.message}</div>
            <div className="text-muted" style={{ fontSize: '0.7rem' }}>{timeAgo(n.createdAt)}</div>
          </Dropdown.Item>
        ))}
        <div className="text-center border-top py-2">
          <Link to="/notifications" className="small">View all</Link>
        </div>
      </Dropdown.Menu>
    </Dropdown>
  );
}
