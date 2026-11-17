import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="vh-100 d-flex flex-column align-items-center justify-content-center text-center p-4">
      <div className="display-1 fw-bold" style={{ color: 'var(--lf-navy)' }}>404</div>
      <p className="lead">This page took a wrong turn.</p>
      <Link to="/" className="btn btn-primary">Go to my dashboard</Link>
    </div>
  );
}
