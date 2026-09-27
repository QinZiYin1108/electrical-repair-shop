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
    url: '/user/payments/intents',
    method: 'POST',
    data: Object.assign({ orderType: 3 }, data || {})
  });
}

function getPaymentStatus(paymentNo) {
  return request({
    url: `/user/payments/${encodeURIComponent(paymentNo)}`,
    method: 'GET'
  });
}

module.exports = {
  getUserFundsSummary,
  listUserFundFlows,
  rechargeUserFunds,
  getPaymentStatus
};
