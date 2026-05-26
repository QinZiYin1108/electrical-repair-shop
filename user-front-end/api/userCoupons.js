const request = require('./request');

function fetchUserCouponsList(data) {
  return request({
    url: '/user/coupons/list',
    method: 'GET',
    data: data || {}
  });
}

function fetchUserCouponDetail(id) {
  return request({
    url: '/user/coupons/detail',
    method: 'GET',
    data: { id }
  });
}

module.exports = {
  fetchUserCouponsList,
  fetchUserCouponDetail
};
