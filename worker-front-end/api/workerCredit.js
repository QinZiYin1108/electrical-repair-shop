import request from './request';

/** 获取当前信用积分 */
export function getCreditScore() {
  return request({
    url: '/worker/credit/score',
    method: 'GET'
  });
}

/** 信用积分变动历史 */
export function getCreditHistory(params) {
  return request({
    url: '/worker/credit/history',
    method: 'GET',
    params
  });
}
