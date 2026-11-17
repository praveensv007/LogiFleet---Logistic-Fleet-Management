import { Link } from 'react-router-dom';

const FLOW = ['Created', 'Received', 'Packed', 'Ready', 'Assigned', 'Dispatched', 'In transit', 'Delivered'];

/** Split screen used by login, register and password pages. */
export default function AuthLayout({ title, subtitle, children, wide = false }) {
  return (
    <div className="auth-wrap">
      <div className="auth-hero">
        <Link to="/" className="text-white text-decoration-none fw-bold fs-4">
          <i className="bi bi-truck me-2" />LogiFleet
        </Link>
        <div>
          <h2>Every parcel, every truck, one live view.</h2>
          <p className="opacity-75 mt-3">
            Customers book and track, warehouses pack and dispatch, fleet managers assign, and drivers deliver with
            OTP-verified proof — all updated live.
          </p>
          <div className="flow">{FLOW.map((s) => <span key={s}>{s}</span>)}</div>
        </div>
        <Link to="/track" className="text-white small"><i className="bi bi-geo-alt me-1" />Track a shipment without logging in</Link>
      </div>
      <div className={`auth-form ${wide ? 'wide' : ''}`}>
        <div className="inner">
          <h3 className="fw-bold mb-1">{title}</h3>
          {subtitle && <p className="text-muted mb-4">{subtitle}</p>}
          {children}
        </div>
      </div>
    </div>
  );
}
