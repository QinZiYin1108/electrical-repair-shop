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

