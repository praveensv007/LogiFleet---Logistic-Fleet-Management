import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import DashboardLayout from '../components/layout/DashboardLayout';
import useAuth from '../hooks/useAuth';
import { ROLES } from '../utils/constants';

import Login from '../pages/auth/Login';
import Register from '../pages/auth/Register';
import ForgotPassword from '../pages/auth/ForgotPassword';
import ResetPassword from '../pages/auth/ResetPassword';
import AdminDashboard from '../pages/dashboard/AdminDashboard';
import FleetDashboard from '../pages/dashboard/FleetDashboard';
import WarehouseDashboard from '../pages/dashboard/WarehouseDashboard';
import DriverDashboard from '../pages/dashboard/DriverDashboard';
import CustomerDashboard from '../pages/dashboard/CustomerDashboard';
import MyProfile from '../pages/profile/MyProfile';
import ChangePassword from '../pages/profile/ChangePassword';
import UserList from '../pages/users/UserList';
import UserForm from '../pages/users/UserForm';
import RolePermissions from '../pages/users/RolePermissions';
import CustomerList from '../pages/customers/CustomerList';
import CustomerForm from '../pages/customers/CustomerForm';
import CustomerDetail from '../pages/customers/CustomerDetail';
import DriverList from '../pages/drivers/DriverList';
import DriverForm from '../pages/drivers/DriverForm';
import DriverDetail from '../pages/drivers/DriverDetail';
import VehicleList from '../pages/vehicles/VehicleList';
import VehicleForm from '../pages/vehicles/VehicleForm';
import VehicleDetail from '../pages/vehicles/VehicleDetail';
import WarehouseList from '../pages/warehouses/WarehouseList';
import WarehouseForm from '../pages/warehouses/WarehouseForm';
import ReceiveShipment from '../pages/warehouses/ReceiveShipment';
import PackShipment from '../pages/warehouses/PackShipment';
import LoadingUnloading from '../pages/warehouses/LoadingUnloading';
import DispatchShipment from '../pages/warehouses/DispatchShipment';
import ShipmentList from '../pages/shipments/ShipmentList';
import CreateShipment from '../pages/shipments/CreateShipment';
import ShipmentDetail from '../pages/shipments/ShipmentDetail';
import TrackShipment from '../pages/shipments/TrackShipment';
import PackageList from '../pages/packages/PackageList';
import AssignmentList from '../pages/assignments/AssignmentList';
import CreateAssignment from '../pages/assignments/CreateAssignment';
import MyDeliveries from '../pages/deliveries/MyDeliveries';
import DeliveryUpdate from '../pages/deliveries/DeliveryUpdate';
import CompleteDelivery from '../pages/deliveries/CompleteDelivery';
import ReportProblem from '../pages/deliveries/ReportProblem';
import ActiveDeliveries from '../pages/deliveries/ActiveDeliveries';
import MaintenanceList from '../pages/maintenance/MaintenanceList';
import MaintenanceForm from '../pages/maintenance/MaintenanceForm';
import NotificationList from '../pages/notifications/NotificationList';
import ReportsDashboard from '../pages/reports/ReportsDashboard';
import AuditLogList from '../pages/audit/AuditLogList';
import NotFound from '../pages/errors/NotFound';
import Unauthorized from '../pages/errors/Unauthorized';

const { ADMIN, FLEET_MANAGER: FM, WAREHOUSE_STAFF: WS, DRIVER, CUSTOMER } = ROLES;

function HomeRedirect() {
  const { user, homePath } = useAuth();
  return <Navigate to={user ? homePath : '/login'} replace />;
}

const guard = (roles, element) => <ProtectedRoute roles={roles}>{element}</ProtectedRoute>;

export default function AppRoutes() {
  return (
    <Routes>
      {/* public */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/track" element={<TrackShipment />} />
      <Route path="/track/:trackingNumber" element={<TrackShipment />} />
      <Route path="/" element={<HomeRedirect />} />

      {/* logged in */}
      <Route element={<ProtectedRoute><DashboardLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<HomeRedirect />} />
        <Route path="/admin" element={guard([ADMIN], <AdminDashboard />)} />
        <Route path="/fleet" element={guard([FM], <FleetDashboard />)} />
        <Route path="/warehouse" element={guard([WS, ADMIN], <WarehouseDashboard />)} />
        <Route path="/driver" element={guard([DRIVER], <DriverDashboard />)} />
        <Route path="/customer" element={guard([CUSTOMER], <CustomerDashboard />)} />

        <Route path="/profile" element={<MyProfile />} />
        <Route path="/change-password" element={<ChangePassword />} />
        <Route path="/notifications" element={<NotificationList />} />

        <Route path="/users" element={guard([ADMIN], <UserList />)} />
        <Route path="/users/new" element={guard([ADMIN], <UserForm />)} />
        <Route path="/users/:id/edit" element={guard([ADMIN], <UserForm />)} />
        <Route path="/roles" element={guard([ADMIN], <RolePermissions />)} />

        <Route path="/customers" element={guard([ADMIN, FM], <CustomerList />)} />
        <Route path="/customers/:id" element={guard([ADMIN, FM], <CustomerDetail />)} />
        <Route path="/customers/:id/edit" element={guard([ADMIN], <CustomerForm />)} />

        <Route path="/drivers" element={guard([ADMIN, FM], <DriverList />)} />
        <Route path="/drivers/new" element={guard([ADMIN, FM], <DriverForm />)} />
        <Route path="/drivers/:id" element={guard([ADMIN, FM], <DriverDetail />)} />
        <Route path="/drivers/:id/edit" element={guard([ADMIN, FM], <DriverForm />)} />

        <Route path="/vehicles" element={guard([ADMIN, FM], <VehicleList />)} />
        <Route path="/vehicles/new" element={guard([ADMIN, FM], <VehicleForm />)} />
        <Route path="/vehicles/:id" element={guard([ADMIN, FM, WS], <VehicleDetail />)} />
        <Route path="/vehicles/:id/edit" element={guard([ADMIN, FM], <VehicleForm />)} />

        <Route path="/warehouses" element={guard([ADMIN], <WarehouseList />)} />
        <Route path="/warehouses/new" element={guard([ADMIN], <WarehouseForm />)} />
        <Route path="/warehouses/:id/edit" element={guard([ADMIN], <WarehouseForm />)} />
        <Route path="/warehouse/receive" element={guard([WS, ADMIN], <ReceiveShipment />)} />
        <Route path="/warehouse/pack" element={guard([WS, ADMIN], <PackShipment />)} />
        <Route path="/warehouse/loading" element={guard([WS, ADMIN], <LoadingUnloading />)} />
        <Route path="/warehouse/dispatch" element={guard([WS, ADMIN], <DispatchShipment />)} />

        <Route path="/shipments" element={guard([ADMIN, FM, WS, CUSTOMER], <ShipmentList />)} />
        <Route path="/shipments/new" element={guard([CUSTOMER, ADMIN], <CreateShipment />)} />
        <Route path="/shipments/:id" element={<ShipmentDetail />} />
        <Route path="/shipments/:id/edit" element={guard([CUSTOMER, ADMIN], <CreateShipment />)} />
        <Route path="/shipments/:id/packages" element={guard([CUSTOMER, ADMIN, WS], <PackageList />)} />

        <Route path="/assignments" element={guard([ADMIN, FM], <AssignmentList />)} />
        <Route path="/assignments/new" element={guard([ADMIN, FM], <CreateAssignment />)} />

        <Route path="/deliveries/my" element={guard([DRIVER], <MyDeliveries />)} />
        <Route path="/deliveries/active" element={guard([ADMIN, FM], <ActiveDeliveries />)} />
        <Route path="/deliveries/:id" element={guard([DRIVER, ADMIN, FM], <DeliveryUpdate />)} />
        <Route path="/deliveries/:id/complete" element={guard([DRIVER], <CompleteDelivery />)} />
        <Route path="/deliveries/:id/problem" element={guard([DRIVER], <ReportProblem />)} />

        <Route path="/maintenance" element={guard([ADMIN, FM], <MaintenanceList />)} />
        <Route path="/maintenance/new" element={guard([ADMIN, FM], <MaintenanceForm />)} />
        <Route path="/maintenance/:id/edit" element={guard([ADMIN, FM], <MaintenanceForm />)} />

        <Route path="/reports" element={guard([ADMIN, FM], <ReportsDashboard />)} />
        <Route path="/audit-logs" element={guard([ADMIN], <AuditLogList />)} />
      </Route>

      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
