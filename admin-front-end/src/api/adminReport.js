import request from './request';

/** 举报列表 */
export function fetchReports(params) {
  return request({
    url: '/admin/reports/list',
    method: 'get',
    params
  });
}

/** 举报详情 */
export function fetchReportDetail(id) {
  return request({
    url: `/admin/reports/${id}`,
    method: 'get'
  });
}

/** 处理举报 */
export function processReport(id, data) {
  return request({
    url: `/admin/reports/${id}/process`,
    method: 'post',
    data
  });
}

/** 举报统计 */
export function fetchReportStats(params) {
  return request({
    url: '/admin/reports/stats',
    method: 'get',
    params
  });
}
