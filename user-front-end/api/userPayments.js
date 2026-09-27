const request = require('./request');

function createOrderPaymentIntent(data) {
  return request({
    url: '/user/payments/intents',
    method: 'POST',
    data: Object.assign({ orderType: 1 }, data || {})
  });
}

function getOrderPaymentStatus(paymentNo) {
  return request({
    url: `/user/payments/${encodeURIComponent(paymentNo)}`,
    method: 'GET'
  });
}

module.exports = {
  createOrderPaymentIntent,
  getOrderPaymentStatus
};
