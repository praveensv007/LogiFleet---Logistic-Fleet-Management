import { BrowserRouter } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import AppRoutes from './routes/AppRoutes';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <NotificationProvider>
          <AppRoutes />
          <ToastContainer position="top-right" autoClose={4000} newestOnTop theme="colored" />
        </NotificationProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
