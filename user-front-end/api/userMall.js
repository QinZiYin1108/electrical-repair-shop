const request = require('./request');

function buildParams(params) {
  const source = params || {};
  const result = {};
  Object.keys(source).forEach((key) => {
    const value = source[key];
    if (value === undefined || value === null || value === "") {
      return;
    }
    result[key] = value;
  });
  return result;
}

function fetchMallCategories(params) {
  return request({
    url: '/user/mall/categories',
    method: 'GET',
    data: buildParams(params)
  });
}

function fetchMallProducts(params) {
  return request({
    url: '/user/mall/products',
    method: 'GET',
    data: buildParams(params)
  });
}

function fetchMallProductDetail(id) {
  return request({
    url: `/user/mall/products/${id}`,
    method: 'GET'
  });
}

function fetchMallFavoriteProducts() {
  return request({
    url: '/user/mall/favorites',
    method: 'GET'
  });
}

function toggleMallProductFavorite(id, favorite) {
  return request({
    url: `/user/mall/products/${id}/favorite`,
    method: 'POST',
    data: {
      favorite: !!favorite
    }
  });
}

module.exports = {
  fetchMallCategories,
  fetchMallProducts,
  fetchMallFavoriteProducts,
  fetchMallProductDetail,
  toggleMallProductFavorite
};
