const needLoginPages = [
  'pages/follow-workers/index',
  'pages/product-checkout/index',
  'pages/message/index',
  'pages/messageDetail/index',
  'pages/cart/index',
  'pages/mine/index',
  'pages/profile/index',
  'pages/settings/index',
  'pages/address-list/index',
  'pages/address-edit/index',
  'pages/order-list/index',
  'pages/order-detail/index',
  'pages/product-order-list/index',
  'pages/product-order-detail/index',
  'pages/favorite-products/index',
  'pages/order-review/index',
  'pages/after-sales/index'
];

const checkNeedLogin = (url) => {
  return needLoginPages.some((page) => url.indexOf(page) !== -1);
};

const isLoggedIn = () => {
  const app = typeof getApp === 'function' ? getApp() : null;
  const token = wx.getStorageSync('userToken');
  return !!token && !!(app && app.globalData && app.globalData.isLogin);
};

const redirectToLogin = (url) => {
  if (url) {
    wx.setStorageSync('redirectUrl', url);
  }
  wx.navigateTo({
    url: '/pages/login/index'
  });
  return false;
};

const requireLogin = (url) => {
  if (isLoggedIn()) {
    return true;
  }
  return redirectToLogin(url);
};

const navigateWithAuth = (options) => {
  const url = options.url || '';
  if (checkNeedLogin(url) && !requireLogin(url)) {
    return false;
  }
  return true;
};

const navigateTo = (options) => {
  if (navigateWithAuth(options)) {
    wx.navigateTo(options);
    return true;
  }
  return false;
};

const switchTab = (options) => {
  if (navigateWithAuth(options)) {
    wx.switchTab(options);
    return true;
  }
  return false;
};

const redirectTo = (options) => {
  if (navigateWithAuth(options)) {
    wx.redirectTo(options);
    return true;
  }
  return false;
};

const reLaunch = (options) => {
  if (navigateWithAuth(options)) {
    wx.reLaunch(options);
    return true;
  }
  return false;
};

const navigateBack = (options = {}) => {
  wx.navigateBack({
    delta: 1,
    ...options
  });
  return true;
};

module.exports = {
  navigateTo,
  switchTab,
  redirectTo,
  reLaunch,
  navigateBack,
  checkNeedLogin,
  isLoggedIn,
  requireLogin,
  redirectToLogin
};
