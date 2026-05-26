const request = require("./request");
const { API_BASE_URL } = require("./config");

const fetchServiceModes = () => {
  return request({
    url: "/user/order-flow/service-modes",
    method: "GET"
  });
};

const fetchCategoryTree = (keyword) => {
  return request({
    url: "/user/order-flow/categories",
    method: "GET",
    data: {
      keyword: keyword || ""
    }
  });
};

const fetchCategoryDetail = (categoryId) => {
  return request({
    url: "/user/order-flow/category-detail",
    method: "GET",
    data: {
      categoryId
    }
  });
};

const fetchServiceTypes = (params) => {
  return request({
    url: "/user/order-flow/service-types",
    method: "GET",
    data: params || {}
  });
};

const fetchSelectionContext = (params) => {
  return request({
    url: "/user/order-flow/selection-context",
    method: "GET",
    data: params || {}
  });
};

const fetchAllTechnicians = (params) => {
  return request({
    url: "/user/order-flow/all-technicians",
    method: "GET",
    data: params || {}
  });
};

const fetchFaultOptions = (serviceTypeId) => {
  return request({
    url: "/user/order-flow/fault-options",
    method: "GET",
    data: {
      serviceTypeId
    }
  });
};

const fetchTechnicians = (params) => {
  return request({
    url: "/user/order-flow/technicians",
    method: "GET",
    data: params || {}
  });
};

const fetchTechnicianDetail = (technicianId) => {
  return request({
    url: "/user/order-flow/technician-detail",
    method: "GET",
    data: {
      technicianId
    }
  });
};

const toggleTechnicianFollow = (data) => {
  return request({
    url: "/user/order-flow/technician-follow",
    method: "POST",
    data: data || {}
  });
};

const fetchAppointmentSlots = (params) => {
  return request({
    url: "/user/order-flow/appointment-slots",
    method: "GET",
    data: params || {}
  });
};

const fetchFeePreview = (params) => {
  return request({
    url: "/user/order-flow/fee-preview",
    method: "GET",
    data: params || {}
  });
};

const submitOrder = (data) => {
  return request({
    url: "/user/order-flow/submit",
    method: "POST",
    data: data || {}
  });
};

const uploadFaultMedia = (filePath, mediaType) => {
  const token = wx.getStorageSync("userToken");
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + "/user/order-flow/upload-fault-media",
      filePath,
      name: "file",
      formData: {
        mediaType: mediaType || ""
      },
      header: {
        ...(token ? { Authorization: "Bearer " + token } : {})
      },
      success(res) {
        try {
          resolve(request.resolveUploadResponse(res, mediaType === 'video' ? '视频上传失败' : '图片上传失败'));
        } catch (error) {
          reject(error);
        }
      },
      fail(err) {
        reject(request.createRequestError((err && err.errMsg) || '上传失败，请稍后重试', err));
      }
    });
  });
};

module.exports = {
  fetchServiceModes,
  fetchCategoryTree,
  fetchCategoryDetail,
  fetchServiceTypes,
  fetchSelectionContext,
  fetchAllTechnicians,
  fetchFaultOptions,
  fetchTechnicians,
  fetchTechnicianDetail,
  toggleTechnicianFollow,
  fetchAppointmentSlots,
  fetchFeePreview,
  submitOrder,
  uploadFaultMedia
};
