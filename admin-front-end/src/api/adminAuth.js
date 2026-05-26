import request from './request';

export function adminLoginByPassword(data) {
  return request({
    url: '/pass/auth/admin/login/password',
    method: 'post',
    data
  });
}

export function adminSendLoginCode(data) {
  return request({
    url: '/pass/auth/admin/code/send',
    method: 'post',
    data: {
      email: data.email,
      type: 'ADMIN_LOGIN'
    }
  });
}

export function adminLoginByCode(data) {
  return request({
    url: '/pass/auth/admin/login/code',
    method: 'post',
    data
  });
}

export function adminSendResetCode(data) {
  return request({
    url: '/pass/auth/admin/code/send',
    method: 'post',
    data: {
      email: data.email,
      type: 'ADMIN_RESET_PASSWORD'
    }
  });
}

export function adminResetPassword(data) {
  return request({
    url: '/pass/auth/admin/password/reset',
    method: 'post',
    data
  });
}
