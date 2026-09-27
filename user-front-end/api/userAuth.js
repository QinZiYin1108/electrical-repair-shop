const request = require('./request');

const userWxLogin = (code, confirmCancel) => {
  return request({
    url: '/pass/auth/user/login',
    method: 'POST',
    data: { code, confirmCancel: !!confirmCancel }
  });
};

const userPhonePasswordLogin = (phone, password, confirmCancel) => {
  return request({
    url: '/pass/auth/user/login/password',
    method: 'POST',
    data: {
      phone,
      password,
      confirmCancel: !!confirmCancel
    }
  });
};

const sendBindPhoneCode = (phone) => {
  return request({
    url: '/pass/auth/user/code/send',
    method: 'POST',
    data: { phone }
  });
};

const bindPhone = (phone, code) => {
  return request({
    url: '/pass/auth/user/bind-phone',
    method: 'POST',
    data: { phone, code }
  });
};

module.exports = {
  userWxLogin,
  userPhonePasswordLogin,
  sendBindPhoneCode,
  bindPhone
};
