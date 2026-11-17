// Enum lists mirror com.logistics.entity.enums so dropdowns and backend checks match.

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
export const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
export const API_ORIGIN = API_URL.replace(/\/api\/?$/, '');

export const ROLES = {
  ADMIN: 'ADMIN',
  FLEET_MANAGER: 'FLEET_MANAGER',
  WAREHOUSE_STAFF: 'WAREHOUSE_STAFF',
  DRIVER: 'DRIVER',
  CUSTOMER: 'CUSTOMER',
};

export const ROLE_HOME = {
  ADMIN: '/admin',
  FLEET_MANAGER: '/fleet',
  WAREHOUSE_STAFF: '/warehouse',
  DRIVER: '/driver',
  CUSTOMER: '/customer',
};

export const ROLE_LABEL = {
  ADMIN: 'Admin',
  FLEET_MANAGER: 'Fleet Manager',
  WAREHOUSE_STAFF: 'Warehouse Staff',
  DRIVER: 'Driver',
  CUSTOMER: 'Customer',
};

export const ShipmentStatus = ['CREATED', 'RECEIVED', 'PACKED', 'READY_FOR_DISPATCH', 'ASSIGNED', 'DISPATCHED',
  'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];
export const PackageStatus = ['CREATED', 'RECEIVED', 'PACKED', 'LOADED', 'DELIVERED', 'DAMAGED'];
export const AssignmentStatus = ['ASSIGNED', 'ACCEPTED', 'REJECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
export const DeliveryStatus = ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'];
export const DriverAvailability = ['AVAILABLE', 'ON_TRIP', 'ON_LEAVE', 'INACTIVE'];
export const VehicleStatus = ['AVAILABLE', 'ON_TRIP', 'UNDER_MAINTENANCE', 'INACTIVE'];
export const VehicleType = ['TRUCK', 'MINI_TRUCK', 'VAN', 'PICKUP', 'TWO_WHEELER'];
export const FuelType = ['DIESEL', 'PETROL', 'CNG', 'ELECTRIC'];
export const MaintenanceType = ['SCHEDULED', 'REPAIR', 'BREAKDOWN', 'INSPECTION', 'TYRE', 'OIL_CHANGE'];
export const MaintenanceStatus = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
export const WarehouseTxnType = ['RECEIVED', 'PACKED', 'LOADING', 'UNLOADING', 'DISPATCHED'];
export const ProblemType = ['VEHICLE_BREAKDOWN', 'ADDRESS_NOT_FOUND', 'CUSTOMER_UNAVAILABLE', 'DAMAGED', 'ACCIDENT', 'OTHER'];
export const ResolutionStatus = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
export const AuditAction = ['CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'STATUS_CHANGE'];
export const UserStatus = ['ACTIVE', 'INACTIVE', 'LOCKED'];
export const ActiveStatus = ['ACTIVE', 'INACTIVE'];
export const ServiceType = ['STANDARD', 'EXPRESS', 'SAME_DAY'];
export const PaymentMode = ['PREPAID', 'COD', 'CREDIT'];
export const CustomerType = ['INDIVIDUAL', 'BUSINESS'];
export const LicenseType = ['LMV', 'HMV', 'TRANSPORT'];
export const IdProofType = ['AADHAAR', 'PAN', 'PASSPORT'];
export const Gender = ['MALE', 'FEMALE', 'OTHER'];
export const DocumentType = ['RC', 'INSURANCE', 'PERMIT', 'FITNESS', 'PUC'];
export const PackageType = ['DOCUMENT', 'BOX', 'PALLET', 'FRAGILE', 'OTHER'];
export const PackingType = ['BOX', 'PALLET', 'ENVELOPE', 'CRATE'];
export const PickupTimeSlot = ['MORNING', 'AFTERNOON', 'EVENING'];
export const ConditionStatus = ['GOOD', 'DAMAGED'];
export const AUDIT_MODULES = ['AUTH', 'USERS', 'ROLES', 'PROFILE', 'CUSTOMERS', 'DRIVERS', 'VEHICLES', 'WAREHOUSES',
  'WAREHOUSE', 'SHIPMENTS', 'SHIPMENT', 'PACKAGES', 'ASSIGNMENTS', 'DELIVERIES', 'MAINTENANCE'];

export const INDIAN_STATES = ['Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Delhi', 'Goa',
  'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jammu and Kashmir', 'Jharkhand', 'Karnataka', 'Kerala', 'Ladakh',
  'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Puducherry', 'Punjab',
  'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal'];

/** Colour per status for StatusBadge */
export const STATUS_VARIANT = {
  CREATED: 'secondary', RECEIVED: 'info', PACKED: 'info', READY_FOR_DISPATCH: 'primary', ASSIGNED: 'primary',
  DISPATCHED: 'primary', PICKED_UP: 'warning', IN_TRANSIT: 'warning', OUT_FOR_DELIVERY: 'warning',
  DELIVERED: 'success', CANCELLED: 'dark', LOADED: 'primary', DAMAGED: 'danger',
  ACCEPTED: 'info', REJECTED: 'danger', IN_PROGRESS: 'warning', COMPLETED: 'success', FAILED: 'danger',
  AVAILABLE: 'success', ON_TRIP: 'warning', ON_LEAVE: 'secondary', INACTIVE: 'dark', UNDER_MAINTENANCE: 'danger',
  ACTIVE: 'success', LOCKED: 'danger', SCHEDULED: 'info', OPEN: 'danger', RESOLVED: 'success',
  GOOD: 'success', AWAITING_PICKUP: 'secondary', DELAYED: 'danger',
};

/** Sidebar menu per role */
export const MENU = {
  ADMIN: [
    { to: '/admin', label: 'Dashboard', icon: 'speedometer2' },
    { to: '/shipments', label: 'Shipments', icon: 'box-seam' },
    { to: '/assignments', label: 'Assignments', icon: 'signpost-split' },
    { to: '/deliveries/active', label: 'Live deliveries', icon: 'broadcast' },
    { to: '/users', label: 'Users', icon: 'people' },
    { to: '/roles', label: 'Roles & permissions', icon: 'shield-lock' },
    { to: '/customers', label: 'Customers', icon: 'person-vcard' },
    { to: '/drivers', label: 'Drivers', icon: 'person-badge' },
    { to: '/vehicles', label: 'Vehicles', icon: 'truck' },
    { to: '/warehouses', label: 'Warehouses', icon: 'building' },
    { to: '/warehouse', label: 'Warehouse floor', icon: 'boxes' },
    { to: '/maintenance', label: 'Maintenance', icon: 'tools' },
    { to: '/reports', label: 'Reports', icon: 'bar-chart-line' },
    { to: '/audit-logs', label: 'Audit logs', icon: 'journal-text' },
  ],
  FLEET_MANAGER: [
    { to: '/fleet', label: 'Dashboard', icon: 'speedometer2' },
    { to: '/assignments', label: 'Assignments', icon: 'signpost-split' },
    { to: '/deliveries/active', label: 'Live deliveries', icon: 'broadcast' },
    { to: '/drivers', label: 'Drivers', icon: 'person-badge' },
    { to: '/vehicles', label: 'Vehicles', icon: 'truck' },
    { to: '/maintenance', label: 'Maintenance', icon: 'tools' },
    { to: '/shipments', label: 'Shipments', icon: 'box-seam' },
    { to: '/customers', label: 'Customers', icon: 'person-vcard' },
    { to: '/reports', label: 'Reports', icon: 'bar-chart-line' },
  ],
  WAREHOUSE_STAFF: [
    { to: '/warehouse', label: 'Dashboard', icon: 'speedometer2' },
    { to: '/warehouse/receive', label: 'Receive', icon: 'box-arrow-in-down' },
    { to: '/warehouse/pack', label: 'Pack & ready', icon: 'box2' },
    { to: '/warehouse/loading', label: 'Loading', icon: 'truck-flatbed' },
    { to: '/warehouse/dispatch', label: 'Dispatch', icon: 'send' },
    { to: '/shipments', label: 'Shipments', icon: 'box-seam' },
  ],
  DRIVER: [
    { to: '/driver', label: 'Dashboard', icon: 'speedometer2' },
    { to: '/deliveries/my', label: 'My deliveries', icon: 'truck' },
  ],
  CUSTOMER: [
    { to: '/customer', label: 'Dashboard', icon: 'speedometer2' },
    { to: '/shipments/new', label: 'New shipment', icon: 'plus-square' },
    { to: '/shipments', label: 'My shipments', icon: 'box-seam' },
    { to: '/track', label: 'Track', icon: 'geo-alt' },
  ],
};
