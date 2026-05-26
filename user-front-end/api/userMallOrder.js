const request = require("./request");

function fetchMallCart() {
  return request({
    url: "/user/mall/cart",
    method: "GET"
  });
}

function addMallCart(data) {
  return request({
    url: "/user/mall/cart/add",
    method: "POST",
    data: data || {}
  });
}

function updateMallCartQuantity(data) {
  return request({
    url: "/user/mall/cart/update-quantity",
    method: "POST",
    data: data || {}
  });
}

function toggleMallCartSelected(data) {
  return request({
    url: "/user/mall/cart/toggle-selected",
    method: "POST",
    data: data || {}
  });
}

function toggleMallCartAll(data) {
  return request({
    url: "/user/mall/cart/toggle-all",
    method: "POST",
    data: data || {}
  });
}

function removeMallCartItems(data) {
  return request({
    url: "/user/mall/cart/remove",
    method: "POST",
    data: data || {}
  });
}

function fetchAvailableMallCoupons(data) {
  return request({
    url: "/user/mall/orders/available-coupons",
    method: "POST",
    data: data || {}
  });
}

function submitMallOrder(data) {
  return request({
    url: "/user/mall/orders/submit",
    method: "POST",
    data: data || {}
  });
}

module.exports = {
  fetchMallCart,
  addMallCart,
  updateMallCartQuantity,
  toggleMallCartSelected,
  toggleMallCartAll,
  removeMallCartItems,
  fetchAvailableMallCoupons,
  submitMallOrder
};
