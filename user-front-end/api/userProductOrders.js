const request = require('./request');

const fetchUserProductOrders = (params) => {
  return request({
    url: '/user/product-orders/list',
    method: 'GET',
    data: params || {}
  });
};

const fetchUserProductOrderDetail = (orderId) => {
  return request({
    url: '/user/product-orders/detail',
    method: 'GET',
    data: {
      orderId
    }
  });
};

const fetchUserProductAfterSalesDetail = (orderId) => {
  return request({
    url: '/user/product-orders/after-sales/detail',
    method: 'GET',
    data: {
      orderId
    }
  });
};

const confirmUserProductOrderReceipt = (data) => {
  return request({
    url: '/user/product-orders/confirm-receipt',
    method: 'POST',
    data: data || {}
  });
};

const applyUserProductAfterSales = (data) => {
  return request({
    url: '/user/product-orders/after-sales/apply',
    method: 'POST',
    data: data || {}
  });
};

const cancelUserProductAfterSales = (data) => {
  return request({
    url: '/user/product-orders/after-sales/cancel',
    method: 'POST',
    data: data || {}
  });
};

module.exports = {
  fetchUserProductOrders,
  fetchUserProductOrderDetail,
  fetchUserProductAfterSalesDetail,
  confirmUserProductOrderReceipt,
  applyUserProductAfterSales,
  cancelUserProductAfterSales
};
