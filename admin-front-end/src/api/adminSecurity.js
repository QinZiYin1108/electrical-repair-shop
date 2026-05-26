import request from './request';

export function changeAdminPassword(data) {
  return request({
    url: '/admin/security/password/change',
    method: 'post',
    data
  });
}

export function sendAdminChangeEmailCode(newEmail) {
  return request({
    url: '/admin/security/email/change/code/send',
    method: 'post',
    data: { newEmail }
  });
}

export function changeAdminEmail(data) {
  return request({
    url: '/admin/security/email/change',
    method: 'post',
    data
  });
}
