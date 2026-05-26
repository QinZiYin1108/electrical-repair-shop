import request from './request';

export function fetchAdminCoupons(params) {
  return request({
    url: '/admin/products/coupons',
    method: 'get',
    params
  });
}

export function createAdminCoupon(data) {
  return request({
    url: '/admin/products/coupons',
    method: 'post',
    data
  });
}

export function updateAdminCoupon(id, data) {
  return request({
    url: `/admin/products/coupons/${id}/update`,
    method: 'post',
    data
  });
}

export function updateAdminCouponStatus(id, data) {
  return request({
    url: `/admin/products/coupons/${id}/status`,
    method: 'post',
    data
  });
}

export function grantAdminCoupon(id, data) {
  return request({
    url: `/admin/products/coupons/${id}/grant`,
    method: 'post',
    data
  });
}
