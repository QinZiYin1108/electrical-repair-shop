import request from './request';

export function getWorkerFundsSummary() {
  return request({
    url: '/worker/funds/summary',
    method: 'GET'
  });
}

export function listWorkerFundFlows(params) {
  return request({
    url: '/worker/funds/flows',
    method: 'GET',
    data: params || {}
  });
}

export function applyWorkerWithdrawal(data) {
  return request({
    url: '/worker/withdrawals/apply',
    method: 'POST',
    data: data || {}
  });
}

export function listWorkerWithdrawals(params) {
  return request({
    url: '/worker/withdrawals/list',
    method: 'GET',
    data: params || {}
  });
}
