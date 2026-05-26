const { markdownToHtml } = require('../../utils/markdown');

Page({
  data: {
    type: 'user',
    title: '协议内容',
    fileName: '',
    updatedTimeText: '',
    htmlContent: '',
    loading: true
  },
  onLoad(options) {
    const type = options && options.type ? options.type : 'user';
    this.setData({ type });
    this.loadProtocol(type);
  },
  loadProtocol(type) {
    const protocolApi = require('../../api/protocol');
    this.setData({ loading: true });
    protocolApi.fetchProtocol(type)
      .then((res) => {
        if (res.code !== 200 || !res.data) {
          wx.showToast({
            title: res.message || '加载协议失败',
            icon: 'none'
          });
          this.setData({ loading: false });
          return;
        }
        const title = res.data.title || '协议内容';
        wx.setNavigationBarTitle({ title });
        this.setData({
          title,
          fileName: res.data.fileName || '',
          updatedTimeText: this.formatTime(res.data.updatedTime),
          htmlContent: markdownToHtml(res.data.content || ''),
          loading: false
        });
      })
      .catch(() => {
        wx.showToast({
          title: '加载协议失败',
          icon: 'none'
        });
        this.setData({ loading: false });
      });
  },
  formatTime(value) {
    const timestamp = Number(value || 0);
    if (!timestamp) {
      return '';
    }
    const date = new Date(timestamp);
    const pad = (num) => String(num).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
});
