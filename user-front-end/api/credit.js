const request = require('./request');

/** 获取当前信用积分 */
function getCreditScore() {
  return request({
    url: '/user/credit/score',
    method: 'GET'
  });
}

/** 信用积分变动历史 */
function getCreditHistory(params) {
  return request({
    url: '/user/credit/history',
    method: 'GET',
    params
  });
}

module.exports = {
  getCreditScore,
  getCreditHistory
};
