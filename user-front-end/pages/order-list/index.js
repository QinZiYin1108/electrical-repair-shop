const router = require('../../utils/router');
const { fetchUserOrders, fetchUserOrderDoorQr } = require('../../api/userOrders');

const TAB_LIST = [
  { key: 'all', label: '全部' },
  { key: 'waiting', label: '待接单' },
  { key: 'processing', label: '进行中' },
  { key: 'to-pay', label: '待支付' },
  { key: 'finished', label: '已完成' },
  { key: 'closed', label: '已关闭' }
];

const TEXTS = {
  loading: '正在加载订单...',
  loadFailed: '订单列表加载失败',
  retry: '重新加载',
  emptyTitle: '还没有匹配的订单',
  emptyDesc: '可以从首页或下面按钮去下单',
  createOrder: '去下单',
  doorQrButton: '上门码',
  confirmCompletionButton: '确认完成',
  cancelButton: '取消订单',
  afterSalesButton: '申请售后',
  afterSalesViewButton: '查看售后',
  payButton: '支付',
  doorQrTitle: '上门二维码',
  doorQrLoading: '上门码加载中...',
  doorQrClose: '关闭',
  doorQrExpireLabel: '失效时间',
  addressLabel: '服务地址',
  appointmentLabel: '预约时间',
  technicianLabel: '维修师傅',
  faultLabel: '故障信息',
  priceLabel: '订单金额'
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
    return '暂未安排';
  }
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function normalizeMoney(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) {
    return 0;
  }
  return amount;
}

function formatMoney(value) {
  return normalizeMoney(value).toFixed(2);
}

function buildServiceTitle(item) {
  const parts = [];
  if (item.serviceCategoryName) {
    parts.push(item.serviceCategoryName);
  }
  if (item.serviceTypeName) {
    parts.push(item.serviceTypeName);
  }
  return parts.join(' ') || item.serviceTypeName || '服务订单';
}

function buildApplianceText(item) {
  const parts = [];
  if (item.applianceBrand) {
    parts.push(item.applianceBrand);
  }
  if (item.applianceModel) {
    parts.push(item.applianceModel);
  }
  return parts.join(' ');
}

function isBrokenText(value) {
  if (value == null) return true;
  const text = String(value).trim();
  if (!text) return true;
  return /^[?？]+$/.test(text) || text.includes('???');
}

function resolveOrderStatusText(status, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(status || 0);
  if (value === 1) return '待接单';
  if (value === 2) return '待上门';
  if (value === 3) return '待检查';
  if (value === 4) return '待支付';
  if (value === 5) return '服务中';
  if (value === 6) return '已完成';
  if (value === 7) return '已关闭';
  if (value === 8) return '已退款';
  return '订单状态';
}

function calculateRemainingAmount(item) {
  const remaining = normalizeMoney(item && item.totalAmount) - normalizeMoney(item && item.paidAmount);
  return remaining > 0 ? remaining : 0;
}

function resolvePaymentStatusText(item) {
  if (!isBrokenText(item.paymentStatusText)) {
    return item.paymentStatusText;
  }
  const paidAmount = normalizeMoney(item.paidAmount);
  const remainingAmount = calculateRemainingAmount(item);
  if (remainingAmount <= 0 && paidAmount > 0) {
    return '已支付';
  }
  if (paidAmount > 0) {
    return '待补尾款';
  }
  return '待支付';
}

function resolveDisplayStatusText(item) {
  if (item && item.canConfirmCompletion && Number(item.status) === 5) {
    return '待确认完成';
  }
  const statusText = resolveOrderStatusText(item.status, item.statusText);
  if (Number(item.status) === 4 && calculateRemainingAmount(item) <= 0 && normalizeMoney(item.paidAmount) > 0) {
    return '已支付';
  }
  return statusText;
}

function canCancelOrder(item) {
  const status = Number((item && item.status) || 0);
  return status >= 1 && status <= 4 && calculateRemainingAmount(item) > 0.0001;
}

function mapOrderItem(item) {
  const order = Object.assign({}, item || {});
  const serviceAddress = order.serviceAddressShort || order.serviceAddress || '';
  order.statusText = resolveDisplayStatusText(order);
  order.totalAmount = formatMoney(order.totalAmount);
  order.paidAmount = formatMoney(order.paidAmount);
  order.remainingAmount = formatMoney(calculateRemainingAmount(order));
  order.paymentStatusText = resolvePaymentStatusText(order);
  order.canPayTail = Number(order.status) === 4 && calculateRemainingAmount(order) > 0.0001;
  order.canConfirmCompletion = !!order.canConfirmCompletion;
  order.statusText = resolveDisplayStatusText(order);

  return {
    id: order.id || '',
    orderNo: order.orderNo || '',
    statusText: order.statusText,
    serviceTitle: buildServiceTitle(order),
    applianceText: buildApplianceText(order),
    serviceAddress,
    appointmentText: formatDateTime(order.appointmentTime),
    technicianText: order.technicianName || '暂未分配',
    faultSummary: order.faultSummary || '',
    amountText: `￥${order.totalAmount}`,
    paymentStatusText: order.paymentStatusText || '',
    hasDoorQr: !!order.hasDoorQr,
    canPayTail: order.canPayTail,
    canConfirmCompletion: order.canConfirmCompletion,
    canCancel: canCancelOrder(order),
    canApplyAfterSales: !!order.canApplyAfterSales,
    hasAfterSalesEntry: !!order.hasAfterSalesEntry,
    afterSalesButtonText: order.hasAfterSalesEntry && !order.canApplyAfterSales
      ? TEXTS.afterSalesViewButton
      : TEXTS.afterSalesButton
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
    autoRefreshing: false,
    doorQrPopupVisible: false,
    doorQrPopupLoading: false,
    doorQrPopupTitle: '',
    doorQrData: null
  },

  onLoad(options) {
    const activeTab = normalizeTab(options && options.tab);
    this.setActiveTab(activeTab);
    this.loadOrders();
  },

  onShow() {
    this.startAutoRefresh();
  },

  onHide() {
    this.stopAutoRefresh();
  },

  onUnload() {
    this.stopAutoRefresh();
  },

  onPullDownRefresh() {
    this.loadOrders(true);
  },

  startAutoRefresh() {
    if (this._autoRefreshTimer) return;
    this._autoRefreshTimer = setInterval(() => {
      this.refreshOrdersSilently();
    }, 8000);
  },

  stopAutoRefresh() {
    if (this._autoRefreshTimer) {
      clearInterval(this._autoRefreshTimer);
      this._autoRefreshTimer = null;
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

    fetchUserOrders({
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

  refreshOrdersSilently() {
    if (this.data.loading || this.data.autoRefreshing) return;
    this.setData({ autoRefreshing: true });

    fetchUserOrders({
      tab: this.data.activeTab
    })
      .then((res) => {
        if (!res || res.code !== 200 || !Array.isArray(res.data)) {
          return;
        }
        this.setData({
          orderList: res.data.map(mapOrderItem)
        });
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ autoRefreshing: false });
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

  onOrderTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-detail/index?orderId=${id}`
    });
  },

  onRetryTap() {
    this.loadOrders();
  },

  onCreateOrderTap() {
    router.navigateTo({
      url: '/pages/order-flow/step1/index'
    });
  },

  onQuickPayTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-detail/index?orderId=${id}&openPay=1`
    });
  },

  onQuickConfirmCompletionTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-detail/index?orderId=${id}&openConfirm=1`
    });
  },

  onQuickCancelTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-detail/index?orderId=${id}&openCancel=1`
    });
  },

  onQuickAfterSalesTap(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/after-sales/index?orderId=${id}`
    });
  },

  onDoorQrTap(e) {
    const { id, orderNo } = e.currentTarget.dataset;
    if (!id) {
      return;
    }

    this.setData({
      doorQrPopupVisible: true,
      doorQrPopupLoading: true,
      doorQrPopupTitle: orderNo || '',
      doorQrData: null
    });

    fetchUserOrderDoorQr(id)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.setData({
          doorQrPopupLoading: false,
          doorQrData: {
            qrImageUrl: res.data.qrImageUrl || '',
            statusText: res.data.statusText || '',
            expireTimeText: formatDateTime(res.data.expireTime)
          }
        });
      })
      .catch((err) => {
        this.setData({
          doorQrPopupLoading: false,
          doorQrData: {
            qrImageUrl: '',
            statusText: (err && err.message) || TEXTS.loadFailed,
            expireTimeText: ''
          }
        });
      });
  },

  onCloseDoorQrPopup() {
    this.setData({
      doorQrPopupVisible: false,
      doorQrPopupLoading: false,
      doorQrPopupTitle: '',
      doorQrData: null
    });
  },

  onPopupInnerTap() {}
});
