import request from './request';

/** 提交举报 */
export function submitReport(data) {
  return request({
    url: '/worker/reports',
    method: 'POST',
    data
  });
}

/** 我的举报列表 */
export function getMyReports(params) {
  return request({
    url: '/worker/reports/my',
    method: 'GET',
    params
  });
}

/** 提交申诉 */
export function submitAppeal(data) {
  return request({
    url: '/worker/reports/appeal',
    method: 'POST',
    data
  });
}
