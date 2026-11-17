import { NavLink } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import { MENU, ROLE_LABEL } from '../../utils/constants';

export default function Sidebar({ open, onNavigate }) {
  const { user } = useAuth();
  const items = MENU[user?.role] || [];
  return (
    <aside className={`app-sidebar ${open ? 'open' : ''}`}>
      <NavLink to="/" className="brand" onClick={onNavigate}>
        <span className="logo"><i className="bi bi-truck" /></span> LogiFleet
      </NavLink>
      <div className="section-label">{ROLE_LABEL[user?.role]}</div>
      <nav className="nav flex-column">
        {items.map((m) => (
          <NavLink key={m.to} to={m.to} end={m.to.split('/').length <= 2} className="nav-link" onClick={onNavigate}>
            <i className={`bi bi-${m.icon}`} /> {m.label}
          </NavLink>
        ))}
      </nav>
      <div className="section-label mt-auto">Account</div>
      <nav className="nav flex-column mb-3">
        <NavLink to="/notifications" className="nav-link" onClick={onNavigate}><i className="bi bi-bell" /> Notifications</NavLink>
        <NavLink to="/profile" className="nav-link" onClick={onNavigate}><i className="bi bi-person-circle" /> My profile</NavLink>
      </nav>
    </aside>
  );
}
