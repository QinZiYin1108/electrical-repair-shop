import request from './request';

export function fetchAdminDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  });
}

export function fetchAdminDashboardProductSales() {
  return request({
    url: '/admin/dashboard/product-sales',
    method: 'get'
  });
}
