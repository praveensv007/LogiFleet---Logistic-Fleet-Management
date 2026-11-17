import StatusBadge from './common/StatusBadge';
import { formatDateTime } from '../utils/formatters';

/** Vertical status history. items: [{ status, time, location, remarks, changedBy }] */
export default function StatusTimeline({ items = [], showUser = false }) {
  if (!items.length) return <div className="text-muted small">No status updates yet.</div>;
  return (
    <ul className="timeline">
      {items.map((h, i) => (
        <li key={h.id ?? i} className={i === items.length - 1 ? 'current' : 'done'}>
          <span className="dot" />
          <div className="d-flex flex-wrap align-items-center gap-2">
            <StatusBadge status={h.status} />
            <span className="small text-muted">{formatDateTime(h.time)}</span>
          </div>
          {(h.location || h.remarks) && (
            <div className="small mt-1">
              {h.location && <span className="me-2"><i className="bi bi-geo-alt me-1" />{h.location}</span>}
              {h.remarks && <span className="text-muted">{h.remarks}</span>}
            </div>
          )}
          {showUser && h.changedBy && <div className="small text-muted">by {h.changedBy}</div>}
        </li>
      ))}
    </ul>
  );
}
