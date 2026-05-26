const { fetchUserCouponsList } = require('../../api/userCoupons');

const TAB_LIST = [
  { key: 'all', label: '全部' },
  { key: 'unused', label: '未使用' },
  { key: 'used', label: '已使用' },
  { key: 'expired', label: '已过期' }
];

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) {
    return '暂无';
  }
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function normalizeTab(value) {
  const target = String(value || '').trim().toLowerCase();
  return TAB_LIST.some((item) => item.key === target) ? target : 'all';
}

function mapCouponItem(item) {
  return {
    id: item.id || '',
    name: item.name || '优惠券',
    typeText: item.typeText || '优惠',
    statusText: item.statusText || '未使用',
    discountText: item.discountText || '待优惠',
    minAmountText: Number(item.minAmount || 0).toFixed(2),
    applicableTypeText: item.applicableTypeText || '当前订单可用',
    disabledReason: item.disabledReason || '',
    expireTimeText: formatDateTime(item.expireTime),
    receiveTimeText: formatDateTime(item.receiveTime),
    status: Number(item.status || 1)
  };
}

Page({
  data: {
    tabs: TAB_LIST.map((item, index) => ({ ...item, active: index === 0 })),
    activeTab: 'all',
    loading: true,
    loadError: '',
    couponList: []
  },

  onLoad(options) {
    const activeTab = normalizeTab(options && options.status);
    this.setActiveTab(activeTab);
    this.loadList();
  },

  onPullDownRefresh() {
    this.loadList(true);
  },

  setActiveTab(activeTab) {
    this.setData({
      activeTab,
      tabs: TAB_LIST.map((item) => ({ ...item, active: item.key === activeTab }))
    });
  },

  loadList(fromPullDown) {
    this.setData({ loading: true, loadError: '' });
    fetchUserCouponsList({
      status: this.data.activeTab === 'all' ? undefined : this.data.activeTab
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data || !Array.isArray(res.data.items)) {
          throw new Error((res && res.message) || '优惠券加载失败');
        }
        this.setData({
          loading: false,
          loadError: '',
          couponList: res.data.items.map(mapCouponItem)
        });
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || '优惠券加载失败',
          couponList: []
        });
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onTabTap(e) {
    const key = normalizeTab(e.currentTarget.dataset.key);
    if (key === this.data.activeTab) {
      return;
    }
    this.setActiveTab(key);
    this.loadList();
  },

  onRetryTap() {
    this.loadList();
  }
});
