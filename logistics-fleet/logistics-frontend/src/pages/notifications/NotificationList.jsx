import { useEffect, useState } from 'react';
import { Button, ButtonGroup, Card, ListGroup } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import Pagination from '../../components/common/Pagination';
import Loader, { EmptyState, ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import { useNotifications } from '../../context/NotificationContext';
import { notificationLink } from '../../components/NotificationBell';
import notificationService from '../../services/notificationService';
import { formatDateTime, pretty } from '../../utils/formatters';

const ICON = {
  SHIPMENT_CREATED: 'box-seam', SHIPMENT_STATUS_CHANGED: 'arrow-repeat', SHIPMENT_DELAYED: 'alarm', PENDING_SHIPMENT: 'hourglass-split',
  ASSIGNMENT_NEW: 'signpost-split', DELIVERY_COMPLETED: 'patch-check', PROBLEM_REPORTED: 'exclamation-triangle',
  MAINTENANCE_DUE: 'tools', DOCUMENT_EXPIRING: 'file-earmark-x',
};

export default function NotificationList() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { markRead, markAllRead, onNotification, unread } = useNotifications();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => notificationService.list({ unreadOnly, page, size: 15 }), [unreadOnly, page]);

  useEffect(() => onNotification(() => reload(true)), [onNotification, reload]);

  const open = async (n) => {
    if (!n.read) {
      await markRead(n.id);
      reload(true);
    }
    const link = notificationLink(n, user.role);
    if (link) navigate(link);
  };

  return (
    <>
      <PageHeader title="Notifications" subtitle={`${unread} unread`}>
        <ButtonGroup>
          <Button variant={unreadOnly ? 'light' : 'primary'} onClick={() => { setUnreadOnly(false); setPage(0); }}>All</Button>
          <Button variant={unreadOnly ? 'primary' : 'light'} onClick={() => { setUnreadOnly(true); setPage(0); }}>Unread</Button>
        </ButtonGroup>
        <Button variant="outline-secondary" disabled={!unread} onClick={async () => { await markAllRead(); reload(true); }}>Mark all read</Button>
      </PageHeader>
      <ErrorAlert error={error} onRetry={reload} />
      <Card>
        {loading ? <Loader /> : !data?.content?.length ? <EmptyState icon="bell-slash" text="No notifications" /> : (
          <ListGroup variant="flush">
            {data.content.map((n) => (
              <ListGroup.Item key={n.id} action onClick={() => open(n)} className={`notif-item d-flex gap-3 ${n.read ? '' : 'unread'}`}>
                <i className={`bi bi-${ICON[n.type] || 'bell'} fs-4 text-secondary`} />
                <div className="flex-grow-1">
                  <div className="d-flex justify-content-between">
                    <strong>{n.title}</strong>
                    <span className="small text-muted">{formatDateTime(n.createdAt)}</span>
                  </div>
                  <div>{n.message}</div>
                  <div className="small text-muted">{pretty(n.type)}</div>
                </div>
              </ListGroup.Item>
            ))}
          </ListGroup>
        )}
        <Pagination page={page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  );
}
