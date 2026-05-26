const { API_BASE_URL } = require('./config');

function parseResponseBody(data) {
  if (typeof data !== 'string') {
    return data;
  }
  const text = data.trim();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return text;
  }
}

function extractMessage(payload, fallback = '请求失败，请稍后重试') {
  if (payload && typeof payload === 'object' && typeof payload.message === 'string' && payload.message.trim()) {
    return payload.message.trim();
  }
  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim();
  }
  return fallback;
}

function createRequestError(message, extra = {}) {
  const error = new Error(message || '请求失败，请稍后重试');
  if (extra && typeof extra === 'object') {
    Object.keys(extra).forEach((key) => {
      error[key] = extra[key];
    });
  }
  return error;
}

function resolveUploadResponse(res, fallback = '上传失败，请稍后重试') {
  const body = parseResponseBody(res && res.data);
  const statusCode = Number((res && res.statusCode) || 0);
  if (statusCode !== 200) {
    throw createRequestError(extractMessage(body, fallback), { response: res, body, statusCode });
  }
  if (!body || typeof body !== 'object') {
    throw createRequestError(fallback, { response: res, body, statusCode });
  }
  if (Number(body.code) !== 200) {
    throw createRequestError(extractMessage(body, fallback), { response: res, body, statusCode });
  }
  return body;
}

const request = (options) => {
  const token = wx.getStorageSync('userToken');
  return new Promise((resolve, reject) => {
    wx.request({
      url: API_BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else {
          const body = parseResponseBody(res.data);
          reject(createRequestError(extractMessage(body), { response: res, body, statusCode: res.statusCode }));
        }
      },
      fail(err) {
        reject(createRequestError((err && err.errMsg) || '网络请求失败，请稍后重试', err));
      }
    });
  });
};

module.exports = request;
module.exports.parseResponseBody = parseResponseBody;
module.exports.extractMessage = extractMessage;
module.exports.createRequestError = createRequestError;
module.exports.resolveUploadResponse = resolveUploadResponse;
module.exports.API_BASE_URL = API_BASE_URL;
