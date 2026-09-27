import request from './request';

/** 分页查询审核日志 */
export function fetchContentCheckLogs(params) {
  return request({
    url: '/admin/content-check/logs',
    method: 'get',
    params
  });
}

/** 分页查询存疑复审列表 */
export function fetchContentCheckReviews(params) {
  return request({
    url: '/admin/content-check/reviews',
    method: 'get',
    params
  });
}

/** 改判文字审核 */
export function overrideTextReview(id, newStatus, remark) {
  return request({
    url: `/admin/content-check/reviews/${id}/override`,
    method: 'post',
    data: { newStatus, remark }
  });
}

/** 分页查询图片审核队列 */
export function fetchImageReviewQueue(params) {
  return request({
    url: '/admin/content-check/images',
    method: 'get',
    params
  });
}

/** 改判图片审核 */
export function overrideImageReview(id, newStatus, remark) {
  return request({
    url: `/admin/content-check/images/${id}/override`,
    method: 'post',
    data: { newStatus, remark }
  });
}
