import { Dropdown } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import NotificationBell from '../NotificationBell';
import { ROLE_LABEL } from '../../utils/constants';
import { fileUrl } from '../../utils/formatters';

export default function Navbar({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const initials = (user?.fullName || '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
  return (
    <header className="app-topbar">
      <button type="button" className="btn btn-light d-lg-none" onClick={onToggleSidebar} aria-label="Menu">
        <i className="bi bi-list" />
      </button>
      <Link to="/track" className="btn btn-sm btn-outline-secondary d-none d-md-inline-flex align-items-center gap-1">
        <i className="bi bi-geo-alt" /> Track a shipment
      </Link>
      <div className="ms-auto d-flex align-items-center gap-2">
        <NotificationBell />
        <Dropdown align="end">
          <Dropdown.Toggle variant="light" className="d-flex align-items-center gap-2 border-0 bg-transparent" id="user-menu">
            {user?.profilePhotoUrl
              ? <img src={fileUrl(user.profilePhotoUrl)} alt="" width={32} height={32} className="rounded-circle object-fit-cover" />
              : <span className="rounded-circle d-inline-grid text-white fw-semibold"
                style={{ width: 32, height: 32, placeItems: 'center', background: 'var(--lf-navy)', fontSize: '0.8rem' }}>{initials}</span>}
            <span className="text-start d-none d-sm-block lh-sm">
              <span className="d-block fw-semibold small">{user?.fullName}</span>
              <span className="d-block text-muted" style={{ fontSize: '0.72rem' }}>{ROLE_LABEL[user?.role]}</span>
            </span>
          </Dropdown.Toggle>
          <Dropdown.Menu>
            <Dropdown.Item as={Link} to="/profile"><i className="bi bi-person me-2" />My profile</Dropdown.Item>
            <Dropdown.Item as={Link} to="/change-password"><i className="bi bi-key me-2" />Change password</Dropdown.Item>
            <Dropdown.Divider />
            <Dropdown.Item onClick={logout} className="text-danger"><i className="bi bi-box-arrow-right me-2" />Log out</Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </div>
    </header>
  );
}
