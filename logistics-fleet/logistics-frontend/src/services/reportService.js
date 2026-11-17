import api, { download, params, unwrap } from './api';

const get = (path) => (filters) => unwrap(api.get(`/reports/${path}`, { params: params(filters) }));

const reportService = {
  summary: () => unwrap(api.get('/reports/summary')),
  shipmentsByDay: get('shipments-by-day'),
  onTime: get('on-time'),
  driverPerformance: get('driver-performance'),
  vehicleUtilisation: get('vehicle-utilisation'),
  maintenanceCost: get('maintenance-cost'),
  pendingByWarehouse: get('pending-by-warehouse'),
  exportReport: (report, type, filters) =>
    download('/reports/export', { ...filters, report, type }, `${report}.${type}`),
};

export default reportService;
