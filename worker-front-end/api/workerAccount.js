import request from './request';

export function getWorkerAccountInfo() {
  return request({
    url: '/worker/account/me',
    method: 'GET'
  });
}

export function updateWorkerWorkStatus(workStatus) {
  return request({
    url: '/worker/account/work-status',
    method: 'POST',
    data: { workStatus }
  });
}

export function fetchWorkerAccountCancelStatus() {
  return request({
    url: '/worker/account/cancel/status',
    method: 'GET'
  });
}

export function applyWorkerAccountCancel() {
  return request({
    url: '/worker/account/cancel/apply',
    method: 'POST'
  });
}

export function revokeWorkerAccountCancel() {
  return request({
    url: '/worker/account/cancel/revoke',
    method: 'POST'
  });
}
