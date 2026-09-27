import request from './request';

export function workerLoginByPassword(phone, password, confirmCancel = false) {
  return request({
    url: '/pass/auth/worker/login/password',
    method: 'POST',
    data: { phone, password, confirmCancel }
  });
}

export function workerSendLoginCode(phone) {
  return request({
    url: '/pass/auth/worker/code/send',
    method: 'POST',
    data: { phone }
  });
}

export function workerLoginByCode(phone, code, confirmCancel = false) {
  return request({
    url: '/pass/auth/worker/login/code',
    method: 'POST',
    data: { phone, code, confirmCancel }
  });
}

export function workerSendResetPasswordCode(phone) {
  return request({
    url: '/pass/auth/worker/password/reset/code/send',
    method: 'POST',
    data: { phone }
  });
}

export function workerResetPasswordByPhone(phone, code, newPassword, confirmPassword) {
  return request({
    url: '/pass/auth/worker/password/reset',
    method: 'POST',
    data: { phone, code, newPassword, confirmPassword }
  });
}
