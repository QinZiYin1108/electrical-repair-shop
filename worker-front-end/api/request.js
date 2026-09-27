import { API_BASE_URL } from './config';

export const BASE_URL = API_BASE_URL;

export function parseResponseBody(data) {
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

export function extractMessage(payload, fallback = '请求失败，请稍后重试') {
  if (
    payload &&
    typeof payload === 'object' &&
    typeof payload.message === 'string' &&
    payload.message.trim()
  ) {
    return payload.message.trim();
  }
  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim();
  }
  return fallback;
}

export function createRequestError(message, extra = {}) {
  const error = new Error(message || '请求失败，请稍后重试');
  if (extra && typeof extra === 'object') {
    Object.keys(extra).forEach((key) => {
      error[key] = extra[key];
    });
  }
  return error;
}

function handleAuthError(responseBody) {
  const app = getApp();
  uni.removeStorageSync('workerToken');
  if (app && app.globalData) {
    app.globalData.workerIsLogin = false;
    app.globalData.workerInfo = null;
  }
  uni.reLaunch({
    url: '/pages/login/index'
  });
  const body = responseBody || {};
  return createRequestError(body.message || '登录状态已失效，请重新登录', {
    code: body.code,
    authError: true
  });
}

export function resolveUploadResponse(res, fallback = '上传失败，请稍后重试') {
  const body = parseResponseBody(res?.data);
  const statusCode = Number(res?.statusCode || 0);
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

export default function request(options) {
  const token = uni.getStorageSync('workerToken');
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        if (res.statusCode === 401) {
          reject(handleAuthError(parseResponseBody(res.data)));
          return;
        }

        if (res.statusCode === 200) {
          const body = parseResponseBody(res.data);
          const code = body && body.code ? Number(body.code) : 200;
          if (code === 401) {
            reject(handleAuthError(body));
            return;
          }
          if (code === 403) {
            reject(
              createRequestError(extractMessage(body, '当前账号无权执行此操作'), {
                body,
                code,
                forbidden: true
              })
            );
            return;
          }
          resolve(res.data);
        } else {
          const body = parseResponseBody(res.data);
          reject(
            createRequestError(extractMessage(body), {
              response: res,
              body,
              statusCode: res.statusCode
            })
          );
        }
      },
      fail(err) {
        reject(createRequestError(err?.errMsg || '网络请求失败，请稍后重试', err));
      }
    });
  });
}
