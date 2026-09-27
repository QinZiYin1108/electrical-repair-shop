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
const API_BASE_URL = normalizeApiBaseUrl(localConfig.API_BASE_URL || DEFAULT_API_BASE_URL);

module.exports = {
  API_BASE_URL
};
