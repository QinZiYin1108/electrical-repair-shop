import request from './request';

/** 处罚列表 */
export function fetchPenalties(params) {
  return request({
    url: '/admin/penalties/list',
    method: 'get',
    params
  });
}

/** 处罚详情 */
export function fetchPenaltyDetail(id) {
  return request({
    url: `/admin/penalties/${id}`,
    method: 'get'
  });
}

/** 手动执行处罚 */
export function executePenalty(data) {
  return request({
    url: '/admin/penalties/execute',
    method: 'post',
    data
  });
}

/** 处理申诉 */
export function processAppeal(id, data) {
  return request({
    url: `/admin/penalties/${id}/appeal/process`,
    method: 'post',
    data
  });
}

/** 解除处罚 */
export function liftPenalty(id) {
  return request({
    url: `/admin/penalties/${id}/lift`,
    method: 'post'
  });
}
