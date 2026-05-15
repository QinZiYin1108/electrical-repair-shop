const request = require('./request');

const userWxLogin = (code, confirmCancel) => {
  return request({
    url: '/pass/auth/user/login',
    method: 'POST',
    data: { code, confirmCancel: !!confirmCancel }
  });
};

const userEmailPasswordLogin = (email, password, confirmCancel) => {
  return request({
    url: '/pass/auth/user/login/password',
    method: 'POST',
    data: {
      email,
      password,
      confirmCancel: !!confirmCancel
    }
  });
};

module.exports = {
  userWxLogin,
  userEmailPasswordLogin
};
