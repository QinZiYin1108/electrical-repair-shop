import request from './request';

/** 查询信用积分 */
export function fetchCreditScore(accountId, accountType) {
  return request({
    url: `/admin/credits/${accountId}`,
    method: 'get',
    params: { accountType }
  });
}

/** 信用积分变动历史 */
export function fetchCreditHistory(accountId, accountType, params) {
  return request({
    url: `/admin/credits/${accountId}/history`,
    method: 'get',
    params: { accountType, ...params }
  });
}

/** 手动恢复积分（仅超级管理员） */
export function manuallyRecoverCredit(accountId, data) {
  return request({
    url: `/admin/credits/${accountId}/recover`,
    method: 'post',
    data
  });
}
