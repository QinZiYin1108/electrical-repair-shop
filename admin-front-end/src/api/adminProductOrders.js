import request from './request';

export function fetchAdminProductOrders(params) {
  return request({
    url: '/admin/orders/product',
    method: 'get',
    params
  });
}

export function fetchAdminProductOrderDetail(id) {
  return request({
    url: `/admin/orders/product/${id}`,
    method: 'get'
  });
}

export function shipAdminProductOrder(id, data) {
  return request({
    url: `/admin/orders/product/${id}/ship`,
    method: 'post',
    data: data || {}
  });
}
