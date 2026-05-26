import request from './request';

export function updateWorkerLocation(data) {
  return request({
    url: '/worker/location/update',
    method: 'POST',
    data: data || {}
  });
}

