import request from './request';

export function changeWorkerPassword(data) {
  return request({
    url: '/worker/security/password/change',
    method: 'POST',
    data: data || {}
  });
}

export function sendWorkerResetPasswordCode() {
  return request({
    url: '/worker/security/password/reset/code/send',
    method: 'POST',
    data: {}
  });
}

export function resetWorkerPasswordByCode(data) {
  return request({
    url: '/worker/security/password/reset',
    method: 'POST',
    data: data || {}
  });
}

export function sendWorkerChangeEmailCode(newEmail) {
  return request({
    url: '/worker/security/email/change/code/send',
    method: 'POST',
    data: { newEmail }
  });
}

export function changeWorkerEmail(data) {
  return request({
    url: '/worker/security/email/change',
    method: 'POST',
    data: data || {}
  });
}

