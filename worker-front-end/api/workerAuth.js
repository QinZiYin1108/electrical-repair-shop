import request from './request';

export function workerLoginByPassword(email, password, confirmCancel = false) {
  return request({
    url: '/pass/auth/worker/login/password',
    method: 'POST',
    data: { email, password, confirmCancel }
  });
}

export function workerSendLoginCode(email) {
  return request({
    url: '/pass/auth/worker/code/send',
    method: 'POST',
    data: { email }
  });
}

export function workerLoginByCode(email, code, confirmCancel = false) {
  return request({
    url: '/pass/auth/worker/login/code',
    method: 'POST',
    data: { email, code, confirmCancel }
  });
}

export function workerSendResetPasswordCode(email) {
  return request({
    url: '/pass/auth/worker/password/reset/code/send',
    method: 'POST',
    data: { email }
  });
}

export function workerResetPasswordByEmail(email, code, newPassword, confirmPassword) {
  return request({
    url: '/pass/auth/worker/password/reset',
    method: 'POST',
    data: { email, code, newPassword, confirmPassword }
  });
}
