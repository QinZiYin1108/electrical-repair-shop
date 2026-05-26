const router = require('../../utils/router');
const {
  fetchUserProductOrders,
  confirmUserProductOrderReceipt
} = require('../../api/userProductOrders');

const TAB_LIST = [
  { key: 'all', label: '全部' },
  { key: 'pending-delivery', label: '待发货' },
  { key: 'pending-receipt', label: '待收货' },
  { key: 'finished', label: '已完成' },
  { key: 'closed', label: '已关闭' }
];

const TEXTS = {
  loading: '正在加载商品订单...',
  loadFailed: '商品订单加载失败',
  retry: '重新加载',
  emptyTitle: '还没有商城订单',
  emptyDesc: '去商城挑选心仪商品，下单后会在这里查看物流和状态',
  goMall: '去逛商城',
  afterSalesButton: '申请售后',
  afterSalesViewButton: '查看售后',
  reviewButton: '去评价',
  viewReviewButton: '查看评价',
  confirmButton: '确认收货',
  confirming: '确认中...',
  confirmSuccess: '已确认收货'
};

function normalizeTab(tab) {
  const keys = TAB_LIST.map((item) => item.key);
  return keys.includes(tab) ? tab : 'all';
}

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) {
    return '暂无';
  }
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatMoney(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function mapOrderItem(item) {
  const order = Object.assign({}, item || {});
  return {
    id: order.id || '',
    orderNo: order.orderNo || '',
    statusText: order.orderStatusText || '订单状态',
    paymentStatusText: order.paymentStatusText || '',
    deliveryStatusText: order.deliveryStatusText || '',
    firstProductImage: order.firstProductImage || '',
    productSummary: order.productSummary || '商品订单',
    itemCount: Number(order.itemCount || 0),
    totalAmountText: formatMoney(order.totalAmount),
    actualAmountText: formatMoney(order.actualAmount),
    createdTimeText: formatDateTime(order.createdTime),
    deliveryTimeText: order.deliveryTime ? formatDateTime(order.deliveryTime) : '暂未发货',
    canConfirmReceipt: !!order.canConfirmReceipt,
    canReview: !!order.canReview,
    hasReview: !!order.hasReview,
    reviewId: order.reviewId || '',
    canApplyAfterSales: !!order.canApplyAfterSales,
    hasAfterSalesEntry: !!order.hasAfterSalesEntry,
    afterSalesTip: order.afterSalesTip || '',
    afterSalesApplication: order.afterSalesApplication || null
  };
}

Page({
  data: {
    texts: TEXTS,
    tabs: TAB_LIST.map((item, index) => ({
      ...item,
      active: index === 0
    })),
    activeTab: 'all',
    loading: true,
    loadError: '',
    orderList: [],
    confirmingOrderId: ''
  },

  onLoad(options) {
    const activeTab = normalizeTab(options && options.tab);
    this.setActiveTab(activeTab);
    this.loadOrders();
  },

  onPullDownRefresh() {
    this.loadOrders(true);
  },

  onShow() {
    if (!this.data.loading) {
      this.loadOrders();
    }
  },

  setActiveTab(activeTab) {
    this.setData({
      activeTab,
      tabs: TAB_LIST.map((item) => ({
        ...item,
        active: item.key === activeTab
      }))
    });
  },

  loadOrders(fromPullDown) {
    this.setData({
      loading: true,
      loadError: ''
    });

    fetchUserProductOrders({
      tab: this.data.activeTab
    })
      .then((res) => {
        if (!res || res.code !== 200 || !Array.isArray(res.data)) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.setData({
          orderList: res.data.map(mapOrderItem),
          loading: false,
          loadError: ''
        });
      })
      .catch((err) => {
        this.setData({
          loading: false,
          orderList: [],
          loadError: (err && err.message) || TEXTS.loadFailed
        });
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onTabTap(e) {
    const tab = normalizeTab(e.currentTarget.dataset.key);
    if (tab === this.data.activeTab) {
      return;
    }
    this.setActiveTab(tab);
    this.loadOrders();
  },

  onRetryTap() {
    this.loadOrders();
  },

  onGoMallTap() {
    wx.switchTab({
      url: '/pages/mall/index'
    });
  },

  onOrderTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-order-detail/index?orderId=${id}`
    });
  },

  onReviewTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-order-review/index?orderId=${id}`
    });
  },

  onAfterSalesTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-after-sales/index?orderId=${id}`
    });
  },

  onConfirmTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id || this.data.confirmingOrderId) {
      return;
    }
    wx.showModal({
      title: '确认收货',
      content: '确认后订单会变为已完成状态，是否继续？',
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.submitConfirm(id);
      }
    });
  },

  submitConfirm(orderId) {
    this.setData({ confirmingOrderId: orderId });
    confirmUserProductOrderReceipt({
      orderId
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        wx.showToast({
          title: TEXTS.confirmSuccess,
          icon: 'success'
        });
        this.loadOrders();
      })
      .catch((err) => {
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ confirmingOrderId: '' });
      });
  }
});
