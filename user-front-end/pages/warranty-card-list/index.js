const router = require('../../utils/router');
const { fetchUserWarrantyCardList } = require('../../api/userWarrantyCards');

const TAB_LIST = [
  { key: 'all', label: '全部' },
  { key: 'active', label: '保障中' },
  { key: 'expired', label: '已过期' }
];

function normalizeTab(value) {
  const target = String(value || '').trim().toLowerCase();
  return TAB_LIST.some((item) => item.key === target) ? target : 'all';
}

function mapCardItem(item) {
  const remainingDays = Number(item.remainingDays || 0);
  const expired = Number(item.warrantyStatus || 1) === 2;
  return {
    id: item.id || '',
    cardNo: item.cardNo || '',
    productName: item.productName || '商品信息待补充',
    productModel: item.productModel || '-',
    warrantyEndDate: item.warrantyEndDate || '-',
    statusText: item.warrantyStatusText || '保障中',
    remainingDays,
    badgeText: expired ? '已过期' : `剩余 ${remainingDays} 天`
  };
}

Page({
  data: {
    tabs: TAB_LIST.map((item, index) => ({ ...item, active: index === 0 })),
    activeTab: 'all',
    loading: true,
    loadError: '',
    cardList: []
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
    fetchUserWarrantyCardList({
      status: this.data.activeTab === 'all' ? undefined : this.data.activeTab
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data || !Array.isArray(res.data.items)) {
          throw new Error((res && res.message) || '保修卡加载失败');
        }
        this.setData({
          loading: false,
          loadError: '',
          cardList: res.data.items.map(mapCardItem)
        });
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || '保修卡加载失败',
          cardList: []
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
  },

  onCardTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/warranty-card-detail/index?id=${id}`
    });
  }
});
