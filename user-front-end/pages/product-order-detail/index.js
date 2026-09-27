const router = require('../../utils/router');
const {
  fetchUserProductOrderDetail,
  confirmUserProductOrderReceipt,
  cancelUserProductOrder
} = require('../../api/userProductOrders');

const TEXTS = {
  loading: "正在加载商品订单详情...",
  loadFailed: "商品订单详情加载失败",
  retry: "重新加载",
  copyOrderNo: "复制订单号",
  copied: "已复制",
  afterSalesButton: "申请售后",
  afterSalesViewButton: "查看售后",
  reviewButton: "去评价",
  viewReviewButton: "查看评价",
  confirmButton: "确认收货",
  confirming: "确认中...",
  confirmSuccess: "已确认收货",
  goMall: "继续逛商城",
  cancelButton: "取消订单",
  cancelPopupTitle: "取消订单",
  cancelPopupSubtitle: "待发货的商品订单可取消。如遇商家问题请走售后通道。",
  reasonTitle: "选择原因",
  reasonRequired: "请先选择或填写原因",
  remarkPlaceholder: "补充说明（选填）",
  remarkLabel: "补充说明",
  submitCancel: "确认取消",
  cancelSuccess: "订单已取消",
  cancelSubmitting: "提交中..."
};

const CANCEL_REASON_OPTIONS = [
  { code: 'temp_busy', label: '临时有事' },
  { code: 'no_need', label: '不想修了' },
  { code: 'price_high', label: '价格不合适' },
  { code: 'duplicate', label: '重复下单' },
  { code: 'reschedule', label: '改约其他时间' },
  { code: 'other', label: '其他原因' }
];

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) {
    return '';
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

function normalizeThirdPartyNo(value) {
  const text = String(value || '').trim();
  if (!text) {
    return '';
  }
  if (!text.startsWith('MOCK-')) {
    return text;
  }
  return text.replace(/^MOCK-/, '').replace(/-/g, '');
}

function buildInfoRows(detail) {
  return {
    shippingRows: [
      { label: "收货人", value: detail.deliveryName || "-" },
      { label: "联系电话", value: detail.deliveryPhone || "-" },
      { label: "收货地址", value: detail.deliveryAddress || "-" },
      { label: "快递公司", value: detail.deliveryCompany || "待发货" },
      { label: "快递单号", value: detail.deliveryNo || "待发货" }
    ],
    amountRows: [
      { label: "商品金额", value: `￥${detail.productAmountText}` },
      { label: "运费", value: `￥${detail.shippingFeeText}` },
      { label: "优惠金额", value: `￥${detail.discountAmountText}` },
      { label: "订单合计", value: `￥${detail.totalAmountText}`, strong: true },
      { label: "实付金额", value: `￥${detail.actualAmountText}`, strong: true }
    ],
    extraRows: [
      { label: "支付方式", value: detail.paymentMethodText || "-" },
      { label: "支付单号", value: detail.paymentNo || "-" },
      { label: "第三方流水", value: normalizeThirdPartyNo(detail.thirdPartyNo) || "-" },
      { label: "支付备注", value: detail.paymentRemark || "暂无" },
      { label: "订单备注", value: detail.remark || "暂无" },
      { label: "下单时间", value: detail.createdTimeText || "-" },
      { label: "支付时间", value: detail.paymentTimeText || "-" },
      { label: "发货时间", value: detail.deliveryTimeText || "-" },
      { label: "收货时间", value: detail.receiveTimeText || "-" },
      { label: "完成时间", value: detail.completionTimeText || "-" }
    ]
  };
}

function mapDetail(data) {
  const detail = Object.assign({}, data || {});
  detail.id = detail.id || "";
  detail.orderNo = detail.orderNo || "";
  detail.orderStatusText = detail.orderStatusText || "订单状态";
  detail.paymentStatusText = detail.paymentStatusText || "";
  detail.deliveryStatusText = detail.deliveryStatusText || "";
  detail.productSummary = detail.productSummary || "商品订单";
  detail.itemCount = Number(detail.itemCount || 0);
  detail.productAmountText = formatMoney(detail.productAmount);
  detail.shippingFeeText = formatMoney(detail.shippingFee);
  detail.discountAmountText = formatMoney(detail.discountAmount);
  detail.totalAmountText = formatMoney(detail.totalAmount);
  detail.actualAmountText = formatMoney(detail.actualAmount);
  detail.thirdPartyNo = normalizeThirdPartyNo(detail.thirdPartyNo);
  detail.createdTimeText = formatDateTime(detail.createdTime);
  detail.paymentTimeText = formatDateTime(detail.paymentTime);
  detail.deliveryTimeText = formatDateTime(detail.deliveryTime);
  detail.receiveTimeText = formatDateTime(detail.receiveTime);
  detail.completionTimeText = formatDateTime(detail.completionTime);
  detail.canConfirmReceipt = !!detail.canConfirmReceipt;
  detail.canReview = !!detail.canReview;
  detail.hasReview = !!detail.hasReview;
  detail.reviewId = detail.reviewId || "";
  detail.canApplyAfterSales = !!detail.canApplyAfterSales;
  detail.hasAfterSalesEntry = !!detail.hasAfterSalesEntry;
  detail.afterSalesTip = detail.afterSalesTip || "";
  detail.afterSalesApplication = detail.afterSalesApplication || null;
  detail.hasReviewEntry = detail.canReview || detail.hasReview;
  detail.canCancel = Number(detail.orderStatus) === 2;
  detail.cancelReason = detail.cancelReason || '';
  detail.cancelTime = detail.cancelTime || 0;
  detail.items = (Array.isArray(detail.items) ? detail.items : []).map((item) => ({
    id: item.id || "",
    productId: item.productId || "",
    productName: item.productName || "商品信息待补全",
    productImage: item.productImage || "",
    productPriceText: formatMoney(item.productPrice),
    totalPriceText: formatMoney(item.totalPrice),
    quantity: Number(item.quantity || 0)
  }));
  return detail;
}

Page({
  data: {
    texts: TEXTS,
    orderId: '',
    loading: true,
    loadError: '',
    detail: null,
    shippingRows: [],
    amountRows: [],
    extraRows: [],
    confirming: false,
    cancelPopupVisible: false,
    cancelReasonOptions: CANCEL_REASON_OPTIONS,
    selectedCancelReason: '',
    cancelRemark: '',
    cancelSubmitting: false
  },

  onLoad(options) {
    const orderId = options && options.orderId ? options.orderId : '';
    if (!orderId) {
      this.setData({
        loading: false,
        loadError: TEXTS.loadFailed
      });
      return;
    }
    this.setData({ orderId });
    this.loadDetail();
  },

  onPullDownRefresh() {
    this.loadDetail(true);
  },

  onShow() {
    if (this.data.orderId && !this.data.loading) {
      this.loadDetail();
    }
  },

  applyDetail(detail) {
    const rows = buildInfoRows(detail);
    this.setData({
      detail,
      shippingRows: rows.shippingRows,
      amountRows: rows.amountRows,
      extraRows: rows.extraRows,
      loading: false,
      loadError: ''
    });
  },

  loadDetail(fromPullDown) {
    this.setData({
      loading: true,
      loadError: ''
    });
    fetchUserProductOrderDetail(this.data.orderId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.applyDetail(mapDetail(res.data));
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || TEXTS.loadFailed
        });
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onRetryTap() {
    this.loadDetail();
  },

  onCopyOrderNoTap() {
    const detail = this.data.detail || {};
    if (!detail.orderNo) {
      return;
    }
    wx.setClipboardData({
      data: detail.orderNo,
      success: () => {
        wx.showToast({
          title: TEXTS.copied,
          icon: 'none'
        });
      }
    });
  },

  onProductTap(e) {
    const { productId } = e.currentTarget.dataset;
    if (!productId) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-detail/index?id=${productId}`
    });
  },

  onGoMallTap() {
    wx.switchTab({
      url: '/pages/mall/index'
    });
  },

  onReviewTap() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.hasReviewEntry) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-order-review/index?orderId=${detail.id}`
    });
  },

  onAfterSalesTap() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.hasAfterSalesEntry) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-after-sales/index?orderId=${detail.id}`
    });
  },

  onConfirmTap() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.canConfirmReceipt || this.data.confirming) {
      return;
    }
    wx.showModal({
      title: "确认收货",
      content: "确认后订单将变为已完成状态，是否继续？",
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.submitConfirm();
      }
    });
  },
  submitConfirm() {
    const detail = this.data.detail || {};
    if (!detail.id) {
      return;
    }
    this.setData({ confirming: true });
    confirmUserProductOrderReceipt({
      orderId: detail.id
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.applyDetail(mapDetail(res.data));
        wx.showToast({
          title: TEXTS.confirmSuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ confirming: false });
      });
  },

  onCancelTap() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.canCancel || this.data.cancelSubmitting) {
      return;
    }
    this.setData({
      cancelPopupVisible: true,
      selectedCancelReason: '',
      cancelRemark: '',
      cancelSubmitting: false
    });
  },

  onCloseCancelPopup() {
    if (this.data.cancelSubmitting) {
      return;
    }
    this.setData({ cancelPopupVisible: false });
  },

  onSelectCancelReason(e) {
    this.setData({
      selectedCancelReason: e.currentTarget.dataset.reason || ''
    });
  },

  onCancelRemarkInput(e) {
    this.setData({ cancelRemark: e.detail.value || '' });
  },

  onCancelConfirm() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.canCancel || this.data.cancelSubmitting) {
      return;
    }
    const selectedCode = this.data.selectedCancelReason;
    const selectedOption = CANCEL_REASON_OPTIONS.find(o => o.code === selectedCode);
    const reasonLabel = selectedOption ? selectedOption.label : '';
    const userRemark = (this.data.cancelRemark || '').trim();
    if (!selectedCode) {
      wx.showToast({ title: TEXTS.reasonRequired, icon: 'none' });
      return;
    }

    this.setData({ cancelSubmitting: true });
    cancelUserProductOrder({
      orderId: detail.id,
      reasonCode: selectedCode,
      reasonLabel: reasonLabel,
      userRemark: userRemark
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.applyDetail(mapDetail(res.data));
        this.setData({
          cancelPopupVisible: false,
          cancelSubmitting: false,
          selectedCancelReason: '',
          cancelRemark: ''
        });
        wx.showToast({ title: TEXTS.cancelSuccess, icon: 'success' });
      })
      .catch((err) => {
        this.setData({ cancelSubmitting: false });
        wx.showToast({ title: (err && err.message) || TEXTS.loadFailed, icon: 'none' });
      });
  },

  onPopupInnerTap() {}
});


