Page({
  data: {
    cancelInfo: null,
    cancelCellValue: '7天反悔期',
    loadingCancel: false
  },

  onShow() {
    this.loadCancelStatus();
  },

  loadCancelStatus() {
    const userProfile = require('../../api/userProfile');
    this.setData({ loadingCancel: true });
    userProfile
      .fetchAccountCancelStatus()
      .then((res) => {
        if (res && res.code === 200 && res.data) {
          const cancelInfo = res.data;
          this.setData({
            cancelInfo,
            cancelCellValue: this.buildCancelCellValue(cancelInfo)
          });
        }
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ loadingCancel: false });
      });
  },

  onNavigateProfile() {
    const router = require("../../utils/router");
    router.navigateTo({
      url: "/pages/profile/index"
    });
  },

  formatDateTime(timestamp) {
    const value = Number(timestamp || 0);
    if (!value) return '';
    const date = new Date(value);
    const pad = (v) => (v < 10 ? `0${v}` : `${v}`);
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  },

  buildCancelCellValue(info) {
    const status = info && typeof info.status === 'number' ? info.status : 0;
    if (status === 3) {
      const deadline = Number(info.cancelDeadlineTime || 0);
      return deadline ? `反悔期至 ${this.formatDateTime(deadline)}` : '注销申请中';
    }
    if (status === 4) {
      return '已注销';
    }
    return '7天反悔期';
  },

  onAccountCancelTap() {
    const info = this.data.cancelInfo || null;
    const status = info && typeof info.status === 'number' ? info.status : 0;

    if (status === 3) {
      wx.showModal({
        title: '撤销注销申请',
        content: '撤销后账号将恢复正常状态。确认撤销吗？',
        confirmText: '撤销',
        cancelText: '取消',
        success: ({ confirm }) => {
          if (!confirm) return;
          const userProfile = require('../../api/userProfile');
          userProfile
            .revokeAccountCancel()
            .then((res) => {
              if (res && res.code === 200) {
                wx.showToast({
                  title: '已撤销注销申请',
                  icon: 'success'
                });
                this.loadCancelStatus();
              } else {
                wx.showToast({
                  title: (res && res.message) || '撤销失败',
                  icon: 'none'
                });
              }
            })
            .catch(() => {
              wx.showToast({
                title: '撤销失败',
                icon: 'none'
              });
            });
        }
      });
      return;
    }

    if (status === 4) {
      wx.showModal({
        title: '账号已注销',
        content: '该账号已注销，无法再次操作。',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }

    wx.showModal({
      title: '注销账号',
      content: '确认提交注销申请吗？提交后进入7天反悔期，7天内再次登录或撤销将取消注销，到期后将自动注销。',
      confirmText: '提交注销',
      cancelText: '取消',
      success: ({ confirm }) => {
        if (!confirm) return;
        const userProfile = require('../../api/userProfile');
        userProfile
          .applyAccountCancel('用户主动注销')
          .then((res) => {
            if (res && res.code === 200) {
              const app = getApp();
              app.globalData.isLogin = false;
              wx.removeStorageSync('userToken');
              wx.removeStorageSync('redirectUrl');
              wx.showToast({
                title: '已提交注销申请',
                icon: 'success'
              });
              wx.switchTab({
                url: '/pages/home/index'
              });
            } else {
              wx.showToast({
                title: (res && res.message) || '提交失败',
                icon: 'none'
              });
            }
          })
          .catch(() => {
            wx.showToast({
              title: '提交失败',
              icon: 'none'
            });
          });
      }
    });
  },

  onLogoutTap() {
    const app = getApp();
    wx.showModal({
      title: "提示",
      content: "确定要退出当前账号吗？",
      confirmText: "退出登录",
      cancelText: "取消",
      success(res) {
        if (!res.confirm) {
          return;
        }
        app.globalData.isLogin = false;
        wx.removeStorageSync("userToken");
        wx.removeStorageSync("redirectUrl");
        wx.showToast({
          title: "已退出登录",
          icon: "success"
        });
        wx.switchTab({
          url: "/pages/home/index"
        });
      }
    });
  }
});
