import { useState } from 'react';
import { Alert } from 'react-bootstrap';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import Footer from './Footer';
import useAuth from '../../hooks/useAuth';

export default function DashboardLayout() {
  const [open, setOpen] = useState(false);
  const { user } = useAuth();
  const location = useLocation();
  return (
    <div className="app-shell">
      <Sidebar open={open} onNavigate={() => setOpen(false)} />
      {open && <div className="position-fixed top-0 start-0 w-100 h-100 d-lg-none" style={{ zIndex: 1030 }} onClick={() => setOpen(false)} />}
      <div className="app-main">
        <Navbar onToggleSidebar={() => setOpen((o) => !o)} />
        <main className="app-content">
          {user?.mustChangePassword && location.pathname !== '/change-password' && (
            <Alert variant="warning" className="d-flex justify-content-between align-items-center">
              <span><i className="bi bi-shield-exclamation me-2" />You are using a temporary password. Please change it now.</span>
              <Link to="/change-password" className="btn btn-sm btn-warning">Change password</Link>
            </Alert>
          )}
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  );
}
