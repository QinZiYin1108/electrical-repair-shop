import request from './request';

export function fetchAdminReserveOrders(params) {
  return request({
    url: '/admin/orders/reserve',
    method: 'get',
    params
  });
}

export function fetchAdminReserveOrderDetail(id) {
  return request({
    url: `/admin/orders/reserve/${id}`,
    method: 'get'
  });
}

export function fetchAdminOfflineOrderTechnicians(params) {
  return request({
    url: '/admin/orders/offline/technicians',
    method: 'get',
    params
  });
}

export function createAdminOfflineOrder(data) {
  return request({
    url: '/admin/orders/offline/submit',
    method: 'post',
    data
  });
}

export function uploadAdminOfflineOrderMedia(file, mediaType) {
  const formData = new FormData();
  formData.append('file', file);
  if (mediaType) {
    formData.append('mediaType', mediaType);
  }
  return request({
    url: '/admin/orders/offline/upload-media',
    method: 'post',
    data: formData
  });
}
