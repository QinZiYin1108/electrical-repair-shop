const request = require('./request');
const { API_BASE_URL } = require('./config');

const fetchUserOrderReview = (orderId) => {
  return request({
    url: '/user/reviews/order',
    method: 'GET',
    data: {
      orderId
    }
  });
};

const fetchUserProductOrderReview = (orderId) => {
  return request({
    url: '/user/reviews/product-order',
    method: 'GET',
    data: {
      orderId
    }
  });
};

const submitUserReview = (data) => {
  return request({
    url: '/user/reviews/submit',
    method: 'POST',
    data: data || {}
  });
};

const submitUserProductReview = (data) => {
  return request({
    url: '/user/reviews/product-submit',
    method: 'POST',
    data: data || {}
  });
};

const uploadUserReviewImage = (filePath) => {
  const token = wx.getStorageSync('userToken');
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + '/user/reviews/upload-image',
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        try {
          resolve(request.resolveUploadResponse(res, '评价图片上传失败'));
        } catch (error) {
          reject(error);
        }
      },
      fail(err) {
        reject(request.createRequestError((err && err.errMsg) || '评价图片上传失败', err));
      }
    });
  });
};

module.exports = {
  fetchUserOrderReview,
  fetchUserProductOrderReview,
  submitUserReview,
  submitUserProductReview,
  uploadUserReviewImage
};
