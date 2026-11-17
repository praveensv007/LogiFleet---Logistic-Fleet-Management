import { Link } from 'react-router-dom';

export default function Unauthorized() {
  return (
    <div className="vh-100 d-flex flex-column align-items-center justify-content-center text-center p-4">
      <i className="bi bi-shield-lock display-3 text-danger" />
      <h2 className="mt-3">Unauthorized</h2>
      <p className="text-muted">Your role does not have access to this page.</p>
      <Link to="/" className="btn btn-primary">Back to my dashboard</Link>
    </div>
  );
}
