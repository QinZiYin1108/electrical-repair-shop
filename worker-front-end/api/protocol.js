import request from './request';

export function fetchProtocol(type) {
  return request({
    url: `/pass/protocols/${type}`,
    method: 'GET'
  });
}
