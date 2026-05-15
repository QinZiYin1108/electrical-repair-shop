const request = require('./request');

function fetchUserWarrantyCardList(data) {
  return request({
    url: '/user/warranty-cards/list',
    method: 'GET',
    data: data || {}
  });
}

function fetchUserWarrantyCardDetail(id) {
  return request({
    url: '/user/warranty-cards/detail',
    method: 'GET',
    data: { id }
  });
}

function applyUserWarrantyCardUsage(data) {
  return request({
    url: '/user/warranty-cards/usage/apply',
    method: 'POST',
    data: data || {}
  });
}

module.exports = {
  fetchUserWarrantyCardList,
  fetchUserWarrantyCardDetail,
  applyUserWarrantyCardUsage
};
