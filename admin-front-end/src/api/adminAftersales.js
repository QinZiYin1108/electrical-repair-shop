import request from './request';

export function fetchAdminAfterSalesRequests(params) {
  return request({
    url: '/admin/after-sales/requests',
    method: 'get',
    params
  });
}

export function fetchAdminAfterSalesDetail(id) {
  return request({
    url: `/admin/after-sales/requests/${id}`,
    method: 'get'
  });
}

export function processAdminAfterSales(id, data) {
  return request({
    url: `/admin/after-sales/requests/${id}/process`,
    method: 'post',
    data: data || {}
  });
}

export function fetchAdminProductAfterSalesRequests(params) {
  return request({
    url: '/admin/product-after-sales/requests',
    method: 'get',
    params
  });
}

export function fetchAdminProductAfterSalesDetail(id) {
  return request({
    url: `/admin/product-after-sales/requests/${id}`,
    method: 'get'
  });
}

export function processAdminProductAfterSales(id, data) {
  return request({
    url: `/admin/product-after-sales/requests/${id}/process`,
    method: 'post',
    data: data || {}
  });
}

export function fetchAdminReviews(params) {
  return request({
    url: '/admin/reviews',
    method: 'get',
    params
  });
}

export function updateAdminReviewStatus(id, data) {
  return request({
    url: `/admin/reviews/${id}/status`,
    method: 'post',
    data: data || {}
  });
}

export function replyAdminReview(id, data) {
  return request({
    url: `/admin/reviews/${id}/reply`,
    method: 'post',
    data: data || {}
  });
}
