const DEFAULT_API_BASE_URL = 'http://localhost:8081/api';

function normalizeApiBaseUrl(value) {
  const url = String(value || '').trim();
  return url.endsWith('/') ? url.slice(0, -1) : url;
}

function loadLocalConfig() {
  try {
    return require('./config.local');
  } catch (error) {
    return {};
  }
}

const localConfig = loadLocalConfig();
const envApiBaseUrl =
  typeof process !== 'undefined' && process.env ? process.env.VUE_APP_API_BASE_URL : '';

export const API_BASE_URL = normalizeApiBaseUrl(
  localConfig.API_BASE_URL || envApiBaseUrl || DEFAULT_API_BASE_URL
);
