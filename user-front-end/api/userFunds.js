const request = require('./request');

function getUserFundsSummary() {
  return request({
    url: '/user/funds/summary',
    method: 'GET'
  });
}

function listUserFundFlows(params) {
  return request({
    url: '/user/funds/flows',
    method: 'GET',
    data: params || {}
  });
}

function rechargeUserFunds(data) {
  return request({
    url: '/user/funds/recharge',
    method: 'POST',
    data: data || {}
  });
}

module.exports = {
  getUserFundsSummary,
  listUserFundFlows,
  rechargeUserFunds
};
