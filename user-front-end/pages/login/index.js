const userAuth = require('../../api/userAuth');

const PHONE_PATTERN = /^1[3-9]\d{9}$/;

Page({
  data: {
    loginMode: 'wechat',
    agreed: false,
    timeText: '',
    loginSubmitting: false,
    phone: '',
    password: '',
    showBindPhone: false,
    bindPhone: '',
    bindCode: '',
    bindSending: false,
    bindCountdown: 0,
    bindSubmitting: false
  },

  onUnload() {
    if (this._timer) {
      clearInterval(this._timer);
      this._timer = null;
    }
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
      this.doPhonePasswordLogin(false);
      return;
    }
    this.doWxLogin(false);
  },

  doPhonePasswordLogin(confirmCancel) {
    const phone = this.normalizePhone(this.data.phone);
    const password = this.data.password || '';
    if (!phone) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      });
      return;
    }
    if (!PHONE_PATTERN.test(phone)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      });
      return;
    }
    if (!password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      });
      return;
    }

    this.setData({
      loginSubmitting: true,
      phone
    });
    userAuth
      .userPhonePasswordLogin(phone, password, confirmCancel)
      .then((resp) => {
        this.handleLoginResponse(resp, {
          retry: () => this.doPhonePasswordLogin(true)
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
    if (data.needBindPhone) {
      this.setData({ showBindPhone: true });
      return;
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

  normalizePhone(value) {
    if (value === null || value === undefined) {
      return '';
    }
    return String(value).trim();
  },

  // ---- 手机号绑定弹窗 ----

  onCloseBindPhone() {
    this.setData({ showBindPhone: false });
  },

  onBindPhoneInput(e) {
    this.setData({ bindPhone: e.detail.value || '' });
  },

  onBindCodeInput(e) {
    this.setData({ bindCode: e.detail.value || '' });
  },

  onBindSendCode() {
    if (this.data.bindSending || this.data.bindCountdown > 0) {
      return;
    }
    const phone = (this.data.bindPhone || '').trim();
    if (!phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!PHONE_PATTERN.test(phone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' });
      return;
    }
    this.setData({ bindSending: true });
    userAuth.sendBindPhoneCode(phone)
      .then(() => {
        wx.showToast({ title: '验证码已发送', icon: 'none' });
        this.startBindCountdown();
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '发送失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ bindSending: false });
      });
  },

  startBindCountdown() {
    this.setData({ bindCountdown: 60 });
    this._timer = setInterval(() => {
      const next = this.data.bindCountdown - 1;
      if (next <= 0) {
        clearInterval(this._timer);
        this._timer = null;
        this.setData({ bindCountdown: 0 });
      } else {
        this.setData({ bindCountdown: next });
      }
    }, 1000);
  },

  onBindSubmit() {
    if (this.data.bindSubmitting) {
      return;
    }
    const phone = (this.data.bindPhone || '').trim();
    const code = (this.data.bindCode || '').trim();
    if (!phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!PHONE_PATTERN.test(phone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' });
      return;
    }
    if (!code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }
    this.setData({ bindSubmitting: true });
    userAuth.bindPhone(phone, code)
      .then(() => {
        wx.showToast({ title: '绑定成功', icon: 'success' });
        this.setData({ showBindPhone: false });
        setTimeout(() => {
          this.finishLoginRedirect();
        }, 600);
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '绑定失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ bindSubmitting: false });
      });
  }
});
