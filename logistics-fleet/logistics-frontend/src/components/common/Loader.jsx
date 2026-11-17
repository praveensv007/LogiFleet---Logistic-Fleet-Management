import { Alert, Spinner } from 'react-bootstrap';

export default function Loader({ text = 'Loading…', full = false }) {
  return (
    <div className={`d-flex align-items-center justify-content-center gap-2 text-muted ${full ? 'vh-100' : 'py-5'}`}>
      <Spinner animation="border" size="sm" /> {text}
    </div>
  );
}

export function ErrorAlert({ error, onRetry }) {
  if (!error) return null;
  return (
    <Alert variant="danger" className="d-flex justify-content-between align-items-center">
      <span><i className="bi bi-exclamation-triangle me-2" />{error}</span>
      {onRetry && <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => onRetry()}>Retry</button>}
    </Alert>
  );
}

export function EmptyState({ icon = 'inbox', text = 'Nothing here yet', children }) {
  return (
    <div className="empty-state">
      <i className={`bi bi-${icon}`} />
      <div>{text}</div>
      {children}
    </div>
  );
}
