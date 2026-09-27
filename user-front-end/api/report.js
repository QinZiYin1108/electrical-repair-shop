const request = require('./request');

/** 提交举报 */
function submitReport(data) {
  return request({
    url: '/user/reports',
    method: 'POST',
    data
  });
}

/** 我的举报列表 */
function getMyReports(params) {
  return request({
    url: '/user/reports/my',
    method: 'GET',
    params
  });
}

/** 举报详情 */
function getReportDetail(id) {
  return request({
    url: `/user/reports/${id}`,
    method: 'GET'
  });
}

module.exports = {
  submitReport,
  getMyReports,
  getReportDetail
};
