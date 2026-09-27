import request from './request';

export function fetchOrderSla(params) {
  return request({ url: '/admin/order-sla/list', method: 'get', params });
}
