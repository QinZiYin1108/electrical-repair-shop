const userAuth = require('../../api/userAuth');

const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

Page({
  data: {
    loginMode: 'wechat',
    agreed: false,
    timeText: '',
    loginSubmitting: false,
    email: '',
    password: ''
  },

  onLoad() {
    const date = new Date();
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    this.setData({
      timeText: `${h}:${m}`
    });
  },

  onModeTap(e) {
    const mode = e.currentTarget.dataset.mode;
    if (!mode || mode === this.data.loginMode) {
      return;
    }
    this.setData({
      loginMode: mode
    });
  },

  onToggleAgree() {
    this.setData({
      agreed: !this.data.agreed
    });
  },

  onProtocolTap(e) {
    const type = e.currentTarget.dataset.type || 'user';
    wx.navigateTo({
      url: `/pages/protocol/index?type=${type}`
    });
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    if (!field) {
      return;
    }
    this.setData({
      [field]: e.detail
    });
  },

  ensureAgreement() {
    if (this.data.agreed) {
      return true;
    }
    wx.showModal({
      title: '\u63d0\u793a',
      content: '\u8bf7\u5148\u52fe\u9009\u5e76\u540c\u610f\u300a\u7528\u6237\u534f\u8bae\u300b\u548c\u300a\u9690\u79c1\u534f\u8bae\u300b\u3002',
      showCancel: false,
      confirmText: '\u6211\u77e5\u9053\u4e86'
    });
    return false;
  },

  onLoginTap() {
    if (this.data.loginSubmitting) {
      return;
    }
    if (!this.ensureAgreement()) {
      return;
    }
    if (this.data.loginMode === 'password') {
      this.doEmailPasswordLogin(false);
      return;
    }
    this.doWxLogin(false);
  },

  doEmailPasswordLogin(confirmCancel) {
    const email = this.normalizeEmail(this.data.email);
    const password = this.data.password || '';
    if (!email) {
      wx.showToast({
        title: '\u8bf7\u8f93\u5165\u90ae\u7bb1',
        icon: 'none'
      });
      return;
    }
    if (!EMAIL_PATTERN.test(email)) {
      wx.showToast({
        title: '\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e',
        icon: 'none'
      });
      return;
    }
    if (!password) {
      wx.showToast({
        title: '\u8bf7\u8f93\u5165\u5bc6\u7801',
        icon: 'none'
      });
      return;
    }

    this.setData({
      loginSubmitting: true,
      email
    });
    userAuth
      .userEmailPasswordLogin(email, password, confirmCancel)
      .then((resp) => {
        this.handleLoginResponse(resp, {
          retry: () => this.doEmailPasswordLogin(true)
        });
      })
      .catch((error) => {
        wx.showToast({
          title: (error && error.message) || '\u767b\u5f55\u5931\u8d25',
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ loginSubmitting: false });
      });
  },

  doWxLogin(confirmCancel) {
    this.setData({ loginSubmitting: true });
    wx.login({
      success: (res) => {
        if (!res.code) {
          wx.showToast({
            title: '\u5fae\u4fe1\u767b\u5f55\u5931\u8d25',
            icon: 'none'
          });
          this.setData({ loginSubmitting: false });
          return;
        }
        userAuth
          .userWxLogin(res.code, confirmCancel)
          .then((resp) => {
            this.handleLoginResponse(resp, {
              retry: () => this.doWxLogin(true)
            });
          })
          .catch((error) => {
            wx.showToast({
              title: (error && error.message) || '\u767b\u5f55\u5931\u8d25',
              icon: 'none'
            });
          })
          .finally(() => {
            this.setData({ loginSubmitting: false });
          });
      },
      fail: () => {
        wx.showToast({
          title: '\u5fae\u4fe1\u767b\u5f55\u5931\u8d25',
          icon: 'none'
        });
        this.setData({ loginSubmitting: false });
      }
    });
  },

  handleLoginResponse(resp, options) {
    if (!resp || resp.code !== 200 || !resp.data) {
      wx.showToast({
        title: (resp && resp.message) || '\u767b\u5f55\u5931\u8d25',
        icon: 'none'
      });
      return;
    }

    const data = resp.data;
    if (data.needCancelConfirm) {
      const deadline = Number(data.cancelDeadlineTime || 0);
      const content = deadline
        ? `\u8be5\u8d26\u53f7\u5df2\u7533\u8bf7\u6ce8\u9500\uff0c\u5c06\u4e8e ${this.formatDateTime(deadline)} \u81ea\u52a8\u6ce8\u9500\u3002\u7ee7\u7eed\u767b\u5f55\u4f1a\u64a4\u9500\u6ce8\u9500\u7533\u8bf7\uff0c\u662f\u5426\u7ee7\u7eed\uff1f`
        : '\u8be5\u8d26\u53f7\u5df2\u7533\u8bf7\u6ce8\u9500\u3002\u7ee7\u7eed\u767b\u5f55\u4f1a\u64a4\u9500\u6ce8\u9500\u7533\u8bf7\uff0c\u662f\u5426\u7ee7\u7eed\uff1f';
      wx.showModal({
        title: '\u6ce8\u9500\u53cd\u6094\u671f\u63d0\u793a',
        content,
        confirmText: '\u7ee7\u7eed\u767b\u5f55',
        cancelText: '\u6682\u4e0d\u767b\u5f55',
        success: ({ confirm }) => {
          if (confirm && options && typeof options.retry === 'function') {
            options.retry();
          }
        }
      });
      return;
    }

    if (data.token) {
      wx.setStorageSync('userToken', data.token);
    }
    if (data.cancelRevoked) {
      wx.showToast({
        title: '\u5df2\u64a4\u9500\u6ce8\u9500\u7533\u8bf7',
        icon: 'none'
      });
    }
    this.finishLoginRedirect();
  },

  finishLoginRedirect() {
    const app = getApp();
    app.globalData.isLogin = true;
    const redirectUrl = wx.getStorageSync('redirectUrl') || '/pages/home/index';
    wx.removeStorageSync('redirectUrl');
    if (
      redirectUrl.indexOf('/pages/home/index') === 0 ||
      redirectUrl.indexOf('/pages/mall/index') === 0 ||
      redirectUrl.indexOf('/pages/message/index') === 0 ||
      redirectUrl.indexOf('/pages/cart/index') === 0 ||
      redirectUrl.indexOf('/pages/mine/index') === 0
    ) {
      wx.switchTab({
        url: redirectUrl
      });
      return;
    }
    wx.redirectTo({
      url: redirectUrl
    });
  },

  formatDateTime(timestamp) {
    const value = Number(timestamp || 0);
    if (!value) {
      return '';
    }
    const date = new Date(value);
    const pad = (num) => (num < 10 ? `0${num}` : `${num}`);
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  },

  normalizeEmail(value) {
    if (value === null || value === undefined) {
      return '';
    }
    return String(value).trim();
  }
});
