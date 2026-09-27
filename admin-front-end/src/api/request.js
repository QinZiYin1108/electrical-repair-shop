import axios from 'axios';
import { clearAuth, getToken } from '../utils/auth';

const apiBaseURL = process.env.VUE_APP_API_BASE_URL || '/api';

const service = axios.create({
  baseURL: apiBaseURL,
  timeout: 10000
});

function redirectToLogin() {
  if (window.location.pathname === '/login') return;
  const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`);
}

service.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      if (!config.headers) {
        config.headers = {};
      }
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  (response) => {
    const body = response.data;
    const code = Number(body?.code || 200);
    if (code === 401) {
      clearAuth();
      redirectToLogin();
      const error = new Error(body?.message || '登录状态已失效，请重新登录');
      error.code = code;
      error.authError = true;
      return Promise.reject(error);
    }
    if (code === 403) {
      const error = new Error(body?.message || '当前账号无权执行此操作');
      error.code = code;
      error.forbidden = true;
      return Promise.reject(error);
    }
    return body;
  },
  (error) => {
    const responseMessage = error?.response?.data?.message;
    const status = Number(error?.response?.status || 0);
    if (status === 401) {
      clearAuth();
      redirectToLogin();
    }
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      error.message = responseMessage.trim();
    } else if (status === 413) {
      error.message = '上传文件过大，请检查图片或视频大小是否超出限制';
    } else if (!error?.message) {
      error.message = '请求失败，请稍后重试';
    }
    return Promise.reject(error);
  }
);

export default service;
