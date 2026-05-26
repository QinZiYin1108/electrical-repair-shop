import request from './request';

export function fetchAdminWarrantyCards(params) {
  return request({
    url: '/admin/products/warranty',
    method: 'get',
    params
  });
}

export function fetchAdminWarrantyCardDetail(id) {
  return request({
    url: `/admin/products/warranty/${id}`,
    method: 'get'
  });
}

export function createAdminWarrantyCard(data) {
  return request({
    url: '/admin/products/warranty',
    method: 'post',
    data
  });
}

export function fetchAdminWarrantyUsageRecords(id) {
  return request({
    url: `/admin/products/warranty/${id}/usage-records`,
    method: 'get'
  });
}

export function processAdminWarrantyUsageRecord(recordId, data) {
  return request({
    url: `/admin/products/warranty/usage-records/${recordId}/process`,
    method: 'post',
    data
  });
}
