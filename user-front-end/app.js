App({
  onLaunch() {
    const token = wx.getStorageSync('userToken');
    this.globalData.isLogin = !!token;
  },
  globalData: {
    isLogin: false
  }
})
