import request from './request';

export function updateWorkerLocation(data) {
  return request({
    url: '/worker/location/update',
    method: 'POST',
    data: data || {}
  });
}

export function setStoreAddress(data) {
  return request({
    url: '/worker/location/store/address',
    method: 'POST',
    data: data || {}
  });
}

