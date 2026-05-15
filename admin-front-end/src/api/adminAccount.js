import request from './request';

export function fetchAdminProfile() {
  return request({
    url: '/admin/account/me',
    method: 'get'
  });
}

