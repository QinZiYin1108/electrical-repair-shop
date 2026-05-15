const request = require("./request");

const fetchUserOrders = (params) => {
  return request({
    url: "/user/orders/list",
    method: "GET",
    data: params || {}
  });
};

const fetchUserOrderDetail = (orderId) => {
  return request({
    url: "/user/orders/detail",
    method: "GET",
    data: {
      orderId
    }
  });
};

const updateUserOrder = (data) => {
  return request({
    url: "/user/orders/update",
    method: "POST",
    data: data || {}
  });
};

const fetchUserOrderDoorQr = (orderId) => {
  return request({
    url: "/user/orders/door-qr",
    method: "GET",
    data: {
      orderId
    }
  });
};

const payUserOrderTail = (data) => {
  return request({
    url: "/user/orders/pay-tail",
    method: "POST",
    data: data || {}
  });
};

const cancelUserOrder = (data) => {
  return request({
    url: "/user/orders/cancel",
    method: "POST",
    data: data || {}
  });
};

const confirmUserOrderCompletion = (data) => {
  return request({
    url: "/user/orders/confirm-completion",
    method: "POST",
    data: data || {}
  });
};

const applyUserOrderAfterSales = (data) => {
  return request({
    url: "/user/orders/after-sales/apply",
    method: "POST",
    data: data || {}
  });
};

const fetchUserAfterSalesDetail = (orderId) => {
  return request({
    url: "/user/orders/after-sales/detail",
    method: "GET",
    data: {
      orderId
    }
  });
};

const cancelUserOrderAfterSales = (applicationId) => {
  return request({
    url: "/user/orders/after-sales/cancel",
    method: "POST",
    data: {
      applicationId
    }
  });
};

module.exports = {
  fetchUserOrders,
  fetchUserOrderDetail,
  updateUserOrder,
  fetchUserOrderDoorQr,
  payUserOrderTail,
  cancelUserOrder,
  confirmUserOrderCompletion,
  applyUserOrderAfterSales,
  fetchUserAfterSalesDetail,
  cancelUserOrderAfterSales
};
