const router = require('../../utils/router');
const draftStore = require('../../utils/orderDraftStore');
const {
  fetchUserOrderDetail,
  fetchUserOrderDoorQr,
  payUserOrderTail,
  cancelUserOrder,
  confirmUserOrderCompletion,
  applyUserOrderAfterSales
} = require('../../api/userOrders');
const { getUserFundsSummary } = require('../../api/userFunds');

const PAYMENT_METHOD_WECHAT = 1;
const PAYMENT_METHOD_ALIPAY = 2;
const PAYMENT_METHOD_WALLET = 5;

const TEXTS = {
  loading: '正在加载订单详情...',
  loadFailed: '订单详情加载失败',
  retry: '重新加载',
  copyOrderNo: '复制订单号',
  copied: '已复制',
  doorQrButton: '上门码',
  doorQrTitle: '上门二维码',
  doorQrLoading: '上门码加载中...',
  doorQrClose: '关闭',
  doorQrExpireLabel: '失效时间',
  summaryTitle: '订单概览',
  serviceInfoTitle: '服务信息',
  deviceInfoTitle: '设备信息',
  faultInfoTitle: '故障信息',
  feeInfoTitle: '费用信息',
  progressTitle: '订单进度',
  extraInfoTitle: '其他信息',
  noFault: '暂无故障明细',
  noProgress: '暂无进度记录',
  phenomenonLabel: '故障现象',
  phenomenonDescLabel: '现象说明',
  userDescLabel: '补充描述',
  imageLabel: '图片',
  videoLabel: '视频',
  payButton: '支付',
  modifyButton: '修改订单',
  payTitle: '支付尾款',
  paySubtitle: '请先确认检查明细与支付方式',
  payIssueLabel: '问题判断',
  payPlanLabel: '维修方案',
  payMethodsTitle: '支付方式',
  payCancel: '取消',
  paySuccess: '支付成功',
  modifySuccess: '订单已更新',
  paying: '支付中...',
  walletInsufficient: '钱包余额不足，请先充值或更换支付方式',
  payableLabel: '待支付',
  paymentMethodWechat: '微信支付',
  paymentMethodWechatDesc: '推荐使用微信完成支付',
  paymentMethodAlipay: '支付宝',
  paymentMethodAlipayDesc: '适合支付宝用户快捷支付',
  paymentMethodWallet: '钱包支付',
  paymentMethodWalletDescPrefix: '可用余额 ￥',
  noInspectionSummary: '待师傅填写',
  statusUpdated: '订单状态已更新',
  doorQrConsumed: '上门码已核销',
  notPayable: '当前订单暂无需要支付的尾款'
,
  actionTitle: '订单操作',
  confirmCompletionButton: '确认完成',
  cancelButton: '取消订单',
  reviewButton: '去评价',
  viewReviewButton: '查看评价',
  afterSalesButton: '申请售后',
  afterSalesViewButton: '查看售后',
  actionProcessing: '提交中...',
  confirmCompletionTitle: '确认完成',
  confirmCompletionSubtitle: '确认后订单将直接完成，请确认服务已结束且无误后再操作',
  confirmCompletionSuccess: '订单已完成',
  cancelPopupTitle: '取消订单',
  cancelPopupSubtitle: '服务费未支付前可自主取消，未上门前会退还上门费。',
  afterSalesPopupTitle: '申请售后',
  afterSalesPopupSubtitle: '仅订单完成后7天内可申请售后，提交后由管理员审核是否退款。',
  reasonTitle: '选择原因',
  descriptionTitle: '补充说明',
  descriptionPlaceholder: '请补充具体情况，方便平台更快处理',
  submitCancel: '确认取消',
  submitAfterSales: '提交申请',
  reasonRequired: '请先选择或填写原因',
  cancelSuccess: '订单已取消',
  afterSalesSuccess: '售后申请已提交',
  afterSalesStatusLabel: '售后状态',
  afterSalesReasonLabel: '售后原因',
  afterSalesTimeLabel: '申请时间',
  afterSalesRemarkLabel: '处理备注'
};

const CANCEL_REASON_OPTIONS = ['计划有变', '价格不合适', '预约时间不合适', '问题已解决'];
const AFTER_SALES_REASON_OPTIONS = ['维修后仍有问题', '服务体验不佳', '费用存在争议', '其他'];

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(timestamp, emptyText) {
  const value = Number(timestamp || 0);
  if (!value) {
    return emptyText || '暂无';
  }
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDate(timestamp, emptyText) {
  const value = Number(timestamp || 0);
  if (!value) {
    return emptyText || '未填写';
  }
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
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

function buildServiceTitle(detail) {
  const parts = [];
  if (detail.serviceCategoryName) {
    parts.push(detail.serviceCategoryName);
  }
  if (detail.serviceTypeName) {
    parts.push(detail.serviceTypeName);
  }
  return parts.join(' ') || detail.serviceTypeName || '服务订单';
}

function buildContactText(name, phone) {
  return [name, phone].filter(Boolean).join(' / ') || '暂无';
}

function buildTechnicianText(detail) {
  return [detail.technicianName, detail.technicianPhone].filter(Boolean).join(' / ') || '暂未分配';
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
  if (value === 7) return '已取消';
  if (value === 8) return '已退款';
  return '订单状态';
}

function resolveServiceModeText(serviceMode, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(serviceMode || 0);
  if (value === 1) return '上门维修';
  if (value === 2) return '上门安装';
  if (value === 3) return '线下维修';
  return '服务方式';
}

function resolveAfterSalesStatusText(status, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(status || 0);
  if (value === 1) return '待审核';
  if (value === 2) return '审核通过';
  if (value === 3) return '审核拒绝';
  if (value === 4) return '处理中';
  if (value === 5) return '已完成';
  if (value === 6) return '已取消';
  return '售后处理中';
}


function mapAfterSalesApplication(application) {
  if (!application || typeof application !== 'object') {
    return null;
  }
  return {
    id: application.id || '',
    status: typeof application.status === 'number' ? application.status : null,
    statusText: resolveAfterSalesStatusText(application.status, application.statusText),
    reason: application.reason || '',
    description: application.description || '',
    refundAmount: formatMoney(application.refundAmount),
    adminRemark: application.adminRemark || '',
    createdTime: application.createdTime || 0,
    updatedTime: application.updatedTime || 0
  };
}

function buildSubmitReason(selectedReason, description) {
  const selected = String(selectedReason || '').trim();
  const text = String(description || '').trim();
  if (selected && text && text !== selected) {
    return `${selected}：${text}`;
  }
  return text || selected;
}


function calculateRemainingAmount(detail) {
  const totalAmount = normalizeMoney(detail && detail.totalAmount);
  const paidAmount = normalizeMoney(detail && detail.paidAmount);
  const remaining = totalAmount - paidAmount;
  return remaining > 0 ? remaining : 0;
}

function resolvePaymentStatusText(detail) {
  const fallback = detail && detail.paymentStatusText;
  if (!isBrokenText(fallback)) {
    return fallback;
  }
  const paidAmount = normalizeMoney(detail && detail.paidAmount);
  const remainingAmount = calculateRemainingAmount(detail);
  if (remainingAmount <= 0 && paidAmount > 0) {
    return '已支付';
  }
  if (paidAmount > 0) {
    return '待补尾款';
  }
  return '待支付';
}

function resolveDisplayStatusText(detail) {
  if (detail && detail.canConfirmCompletion && Number(detail.status) === 5) {
    return '待确认完成';
  }
  const fallback = detail && detail.statusText;
  const baseText = resolveOrderStatusText(detail && detail.status, fallback);
  if (Number(detail && detail.status) === 4 && calculateRemainingAmount(detail) <= 0 && normalizeMoney(detail && detail.paidAmount) > 0) {
    return '已支付';
  }
  return baseText;
}

function mapDetail(data) {
  const detail = Object.assign({}, data || {});
  detail.id = detail.id || '';
  detail.orderNo = detail.orderNo || '';
  detail.status = typeof detail.status === 'number' ? detail.status : null;
  detail.statusText = resolveOrderStatusText(detail.status, detail.statusText);
  detail.serviceTitle = buildServiceTitle(detail);
  detail.serviceModeText = resolveServiceModeText(detail.serviceMode, detail.serviceModeText);
  detail.doorFee = formatMoney(detail.doorFee);
  detail.distanceFee = formatMoney(detail.distanceFee);
  detail.serviceFee = formatMoney(detail.serviceFee);
  detail.materialFee = formatMoney(detail.materialFee);
  detail.overtimeFee = formatMoney(detail.overtimeFee);
  detail.totalAmount = formatMoney(detail.totalAmount);
  detail.paidAmount = formatMoney(detail.paidAmount);
  detail.remainingAmount = formatMoney(calculateRemainingAmount(detail));
  detail.paymentStatusText = resolvePaymentStatusText(detail);
  detail.displayStatusText = resolveDisplayStatusText(detail);
  detail.inspectionDiagnosis = detail.inspectionDiagnosis || '';
  detail.repairPlan = detail.repairPlan || '';
  detail.hasDoorQr = !!detail.hasDoorQr;
  detail.updatedTime = detail.updatedTime || 0;
  detail.canPayTail = Number(detail.status) === 4 && calculateRemainingAmount(detail) > 0.0001;
  detail.canCancel = !!detail.canCancel;
  detail.canModifyOrder = !!detail.canModifyOrder;
  detail.canModifyAppointment = !!detail.canModifyAppointment;
  detail.hasModifyEntry = detail.canModifyOrder || detail.canModifyAppointment;
  detail.cancelTip = detail.cancelTip || '';
  detail.cancelRefundAmount = formatMoney(detail.cancelRefundAmount);
  detail.canConfirmCompletion = !!detail.canConfirmCompletion;
  detail.confirmCompletionTip = detail.confirmCompletionTip || '';
  detail.canApplyAfterSales = !!detail.canApplyAfterSales;
  detail.afterSalesTip = detail.afterSalesTip || '';
  detail.canReview = !!detail.canReview;
  detail.hasReview = !!detail.hasReview;
  detail.reviewId = detail.reviewId || '';
  detail.afterSalesApplication = mapAfterSalesApplication(detail.afterSalesApplication);
  detail.hasAfterSalesEntry = detail.canApplyAfterSales || !!detail.afterSalesApplication;
  detail.hasReviewEntry = detail.canReview || detail.hasReview;
  detail.hasActionCard = detail.canCancel || detail.hasModifyEntry || detail.canConfirmCompletion || detail.hasAfterSalesEntry || detail.hasReviewEntry;
  detail.displayStatusText = resolveDisplayStatusText(detail);
  return detail;
}


function buildRows(detail) {
  const summaryRows = [
    { label: '订单号', value: detail.orderNo || '-' },
    { label: '服务项目', value: detail.serviceTitle },
    { label: '订单状态', value: detail.displayStatusText || '暂无' }
  ];

  const serviceRows = [
    { label: '预约时间', value: formatDateTime(detail.appointmentTime, '暂未安排') },
    { label: '服务方式', value: detail.serviceModeText || '暂无' },
    { label: '服务地址', value: detail.serviceAddress || '线下服务无需地址' },
    { label: '联系人', value: buildContactText(detail.contactName, detail.contactPhone) },
    { label: '维修师傅', value: buildTechnicianText(detail) },
    { label: '下单时间', value: formatDateTime(detail.createdTime) },
    { label: '最新更新', value: formatDateTime(detail.updatedTime) }
  ];

  const deviceRows = [];
  if (detail.applianceBrand) {
    deviceRows.push({ label: '品牌', value: detail.applianceBrand });
  }
  if (detail.applianceModel) {
    deviceRows.push({ label: '型号', value: detail.applianceModel });
  }
  if (detail.purchaseDate) {
    deviceRows.push({ label: '购买日期', value: formatDate(detail.purchaseDate) });
  }

  const feeRows = [
    { label: '支付状态', value: detail.paymentStatusText || '暂无' },
    { label: '上门费', value: `￥${detail.doorFee}` },
    { label: '距离费', value: `￥${detail.distanceFee}` },
    { label: '服务费', value: `￥${detail.serviceFee}` },
    { label: '材料费', value: `￥${detail.materialFee}` },
    { label: '加班费', value: `￥${detail.overtimeFee}` },
    { label: '订单合计', value: `￥${detail.totalAmount}`, strong: true },
    { label: '已支付', value: `￥${detail.paidAmount}` }
  ];

  const extraRows = [];
  if (detail.remark) {
    extraRows.push({ label: '备注', value: detail.remark });
  }
  if (detail.cancelReason) {
    extraRows.push({ label: '取消原因', value: detail.cancelReason });
  }
  if (detail.cancelTime) {
    extraRows.push({ label: '取消时间', value: formatDateTime(detail.cancelTime) });
  }
  if (detail.refundReason) {
    extraRows.push({ label: '退款说明', value: detail.refundReason });
  }
  if (detail.refundTime) {
    extraRows.push({ label: '退款时间', value: formatDateTime(detail.refundTime) });
  }
  if (normalizeMoney(detail.refundAmount) > 0) {
    extraRows.push({ label: '退款金额', value: `￥${formatMoney(detail.refundAmount)}` });
  }
  if (detail.afterSalesApplication) {
    extraRows.push({ label: TEXTS.afterSalesStatusLabel, value: detail.afterSalesApplication.statusText || '处理中' });
    if (detail.afterSalesApplication.reason) {
      extraRows.push({ label: TEXTS.afterSalesReasonLabel, value: detail.afterSalesApplication.reason });
    }
    if (detail.afterSalesApplication.createdTime) {
      extraRows.push({ label: TEXTS.afterSalesTimeLabel, value: formatDateTime(detail.afterSalesApplication.createdTime) });
    }
    if (detail.afterSalesApplication.adminRemark) {
      extraRows.push({ label: TEXTS.afterSalesRemarkLabel, value: detail.afterSalesApplication.adminRemark });
    }
  }

  return {
    summaryRows,
    serviceRows,
    deviceRows,
    feeRows,
    extraRows
  };
}


function buildTailPaymentRows(detail) {
  return [
    { label: '上门费', value: `￥${detail.doorFee}` },
    { label: '距离费', value: `￥${detail.distanceFee}` },
    { label: '服务费', value: `￥${detail.serviceFee}` },
    { label: '材料费', value: `￥${detail.materialFee}` },
    { label: '加班费', value: `￥${detail.overtimeFee}` },
    { label: '订单合计', value: `￥${detail.totalAmount}` },
    { label: '已支付', value: `￥${detail.paidAmount}` },
    { label: TEXTS.payableLabel, value: `￥${detail.remainingAmount}`, strong: true }
  ];
}

function buildPaymentMethodOptions(walletBalanceText) {
  return [
    {
      id: PAYMENT_METHOD_WECHAT,
      label: TEXTS.paymentMethodWechat,
      desc: TEXTS.paymentMethodWechatDesc
    },
    {
      id: PAYMENT_METHOD_ALIPAY,
      label: TEXTS.paymentMethodAlipay,
      desc: TEXTS.paymentMethodAlipayDesc
    },
    {
      id: PAYMENT_METHOD_WALLET,
      label: TEXTS.paymentMethodWallet,
      desc: `${TEXTS.paymentMethodWalletDescPrefix}${walletBalanceText}`
    }
  ];
}

function buildPayPopupState(detail, walletBalanceText) {
  const remainingAmount = detail ? formatMoney(calculateRemainingAmount(detail)) : '0.00';
  return {
    paymentRows: detail ? buildTailPaymentRows(detail) : [],
    payButtonText: `支付 ￥${remainingAmount}`,
    paymentMethodOptions: buildPaymentMethodOptions(walletBalanceText),
    walletInsufficient: normalizeMoney(walletBalanceText) + 0.0001 < normalizeMoney(remainingAmount)
  };
}

function mapFaultList(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || '',
    phenomenonName: item.faultPhenomenonName || '未知故障',
    phenomenonDescription: item.faultPhenomenonDescription || '',
    faultDescription: item.faultDescription || '',
    images: Array.isArray(item.images)
      ? item.images
          .map((image) => ({
            id: image.id || '',
            url: image.url || '',
            thumbnailUrl: image.thumbnailUrl || image.url || ''
          }))
          .filter((image) => !!image.url)
      : [],
    videos: Array.isArray(item.videos)
      ? item.videos
          .map((video) => ({
            id: video.id || '',
            url: video.url || '',
            thumbnailUrl: video.thumbnailUrl || '',
            name: video.name || '',
            durationText: video.duration ? `${video.duration}s` : ''
          }))
          .filter((video) => !!video.url)
      : []
  })).map((item) => ({
    ...item,
    previewUrls: item.images.map((image) => image.url)
  }));
}

function formatProgressDescription(value) {
  const text = String(value || '').trim();
  if (!text) {
    return '';
  }
  return text.replace(/[?;]/g, '\n');
}

function mapProgressList(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || '',
    statusText: resolveOrderStatusText(item.status, item.statusText),
    description: formatProgressDescription(item.description),
    operatorName: item.operatorName || '',
    createdTimeText: formatDateTime(item.createdTime)
  }));
}

function buildDraftFromDetail(detail, mode) {
  const faultList = Array.isArray(detail && detail.faultList) ? detail.faultList : [];
  const selectedFaultIds = [];
  const faultNameMap = {};
  const faultDetailMap = {};

  faultList.forEach((item) => {
    const faultId = item.faultPhenomenonId || item.id || '';
    if (!faultId) {
      return;
    }
    selectedFaultIds.push(faultId);
    faultNameMap[faultId] = item.phenomenonName || item.faultPhenomenonName || '';
    faultDetailMap[faultId] = {
      description: item.faultDescription || '',
      images: Array.isArray(item.images)
        ? item.images.map((image) => ({
            id: image.id || image.url || '',
            url: image.url || '',
            thumbnailUrl: image.thumbnailUrl || image.url || '',
            name: image.name || '',
            mimeType: image.mimeType || 'image/jpeg'
          }))
        : [],
      video: Array.isArray(item.videos) && item.videos.length
        ? {
            id: item.videos[0].id || item.videos[0].url || '',
            url: item.videos[0].url || '',
            thumbnailUrl: item.videos[0].thumbnailUrl || '',
            name: item.videos[0].name || '',
            mimeType: item.videos[0].mimeType || 'video/mp4',
            duration: item.videos[0].duration || 0
          }
        : null
    };
  });

  return {
    serviceMode: detail.serviceMode || 1,
    selectedCategoryId: detail.serviceCategoryId || '',
    selectedCategoryPath: detail.serviceCategoryPath || '',
    selectedServiceTypeId: detail.serviceTypeId || '',
    selectedServiceTypeName: detail.serviceTypeName || '',
    selectedAddressId: detail.serviceAddressId || '',
    selectedAddressText: detail.serviceAddress || '',
    selectedTechnicianId: detail.technicianId || '',
    selectedTechnicianName: detail.technicianName || '',
    selectedFaultIds,
    faultNameMap,
    faultDetailMap,
    applianceBrand: detail.applianceBrand || '',
    applianceModel: detail.applianceModel || '',
    purchaseDate: detail.purchaseDate ? formatDate(detail.purchaseDate, '') : '',
    selectedAppointmentId: detail.appointmentTime ? String(detail.appointmentTime) : '',
    selectedAppointmentLabel: formatDateTime(detail.appointmentTime, ''),
    selectedAppointmentTime: detail.appointmentTime || null,
    canModifyAppointment: !!detail.canModifyAppointment,
    editingOrderId: detail.id || '',
    editingMode: mode || ''
  };
}

Page({
  data: {
    texts: TEXTS,
    orderId: '',
    pendingOpenPay: false,
    pendingOpenConfirm: false,
    pendingOpenCancel: false,
    pendingOpenAfterSales: false,
    loading: true,
    loadError: '',
    detail: null,
    autoRefreshing: false,
    doorQrPopupVisible: false,
    doorQrPopupLoading: false,
    doorQrData: null,
    summaryRows: [],
    serviceRows: [],
    deviceRows: [],
    feeRows: [],
    extraRows: [],
    faultList: [],
    progressList: [],
    collapseState: {
      summary: false,
      service: false,
      device: false,
      fault: true,
      fee: true,
      extra: false,
      progress: true
    },
    payPopupVisible: false,
    paymentRows: [],
    paymentMethodOptions: buildPaymentMethodOptions('0.00'),
    selectedPaymentMethod: PAYMENT_METHOD_WECHAT,
    walletBalanceText: '0.00',
    walletInsufficient: false,
    payButtonText: '支付 ￥0.00',
    paying: false,
    cancelPopupVisible: false,
    afterSalesPopupVisible: false,
    cancelReasonOptions: CANCEL_REASON_OPTIONS,
    afterSalesReasonOptions: AFTER_SALES_REASON_OPTIONS,
    selectedCancelReason: '',
    selectedAfterSalesReason: '',
    cancelRemark: '',
    afterSalesDescription: '',
    actionSubmitting: false
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
    this.setData({
      orderId,
      pendingOpenPay: options && String(options.openPay || '') === '1',
      pendingOpenConfirm: options && String(options.openConfirm || '') === '1',
      pendingOpenCancel: options && String(options.openCancel || '') === '1',
      pendingOpenAfterSales: options && String(options.openAfterSales || '') === '1'
    });
    this.loadDetail();
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

  startAutoRefresh() {
    if (!this.data.orderId) return;
    if (this._autoRefreshTimer) return;
    this._autoRefreshTimer = setInterval(() => {
      this.refreshDetailSilently();
    }, 4000);
  },

  stopAutoRefresh() {
    if (this._autoRefreshTimer) {
      clearInterval(this._autoRefreshTimer);
      this._autoRefreshTimer = null;
    }
  },

  buildDetailViewState(detail) {
    const rows = buildRows(detail);
    const payState = buildPayPopupState(detail, this.data.walletBalanceText);
    return {
      detail,
      summaryRows: rows.summaryRows,
      serviceRows: rows.serviceRows,
      deviceRows: rows.deviceRows,
      feeRows: rows.feeRows,
      extraRows: rows.extraRows,
      faultList: mapFaultList(detail.faultList),
      progressList: mapProgressList(detail.progressList),
      paymentRows: payState.paymentRows,
      payButtonText: payState.payButtonText,
      paymentMethodOptions: payState.paymentMethodOptions,
      walletInsufficient: payState.walletInsufficient
    };
  },

  applyDetail(detail, extraData, callback) {
    const nextData = Object.assign({}, this.buildDetailViewState(detail), extraData || {});
    if (!detail.canPayTail && (this.data.payPopupVisible || nextData.payPopupVisible)) {
      nextData.payPopupVisible = false;
      nextData.paying = false;
    }
    if (!detail.canCancel && (this.data.cancelPopupVisible || nextData.cancelPopupVisible)) {
      nextData.cancelPopupVisible = false;
      nextData.actionSubmitting = false;
    }
    if (!detail.hasAfterSalesEntry && (this.data.afterSalesPopupVisible || nextData.afterSalesPopupVisible)) {
      nextData.afterSalesPopupVisible = false;
      nextData.actionSubmitting = false;
    }
    this.setData(nextData, callback);
  },

  loadDetail() {
    this.setData({
      loading: true,
      loadError: ''
    });

    fetchUserOrderDetail(this.data.orderId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const detail = mapDetail(res.data);
        this.applyDetail(
          detail,
          {
            loading: false,
            loadError: ''
          },
          () => {
            this.tryOpenPendingPayPopup(detail);
            this.tryOpenPendingActionPopup(detail);
          }
        );
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || TEXTS.loadFailed
        });
      });
  },

  refreshDetailSilently() {
    if (!this.data.orderId || this.data.autoRefreshing) return;
    this.setData({ autoRefreshing: true });

    fetchUserOrderDetail(this.data.orderId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          return;
        }
        const detail = mapDetail(res.data);
        const prevDetail = this.data.detail || {};
        const changed =
          Number(detail.updatedTime || 0) !== Number(prevDetail.updatedTime || 0) ||
          detail.displayStatusText !== (prevDetail.displayStatusText || '') ||
          detail.paymentStatusText !== (prevDetail.paymentStatusText || '') ||
          detail.remainingAmount !== (prevDetail.remainingAmount || '') ||
          detail.hasDoorQr !== !!prevDetail.hasDoorQr ||
          detail.canConfirmCompletion !== !!prevDetail.canConfirmCompletion ||
          detail.canCancel !== !!prevDetail.canCancel ||
          detail.canApplyAfterSales !== !!prevDetail.canApplyAfterSales ||
          ((detail.afterSalesApplication && detail.afterSalesApplication.statusText) || '') !== (((prevDetail.afterSalesApplication || {}).statusText) || '');

        if (!changed) {
          if (this.data.doorQrPopupVisible) {
            this.refreshDoorQrPopupSilently();
          }
          return;
        }

        this.applyDetail(detail, {}, () => {
          this.tryOpenPendingPayPopup(detail);
          this.tryOpenPendingActionPopup(detail);
        });

        if (prevDetail.displayStatusText && detail.displayStatusText && prevDetail.displayStatusText !== detail.displayStatusText) {
          wx.showToast({
            title: TEXTS.statusUpdated,
            icon: 'none'
          });
        } else if (!!prevDetail.hasDoorQr && !detail.hasDoorQr) {
          wx.showToast({
            title: TEXTS.doorQrConsumed,
            icon: 'none'
          });
        }

        if (this.data.doorQrPopupVisible) {
          this.refreshDoorQrPopupSilently();
        }
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ autoRefreshing: false });
      });
  },

  tryOpenPendingPayPopup(detail) {
    if (!this.data.pendingOpenPay) {
      return;
    }
    this.setData({ pendingOpenPay: false });
    if (!detail || !detail.canPayTail) {
      wx.showToast({
        title: TEXTS.notPayable,
        icon: 'none'
      });
      return;
    }
    this.openPayPopup();
  },

  tryOpenPendingActionPopup(detail) {
    if (this.data.pendingOpenConfirm) {
      this.setData({ pendingOpenConfirm: false });
      if (detail && detail.canConfirmCompletion) {
        this.onConfirmCompletionTap();
      } else if (detail && detail.confirmCompletionTip) {
        wx.showToast({
          title: detail.confirmCompletionTip,
          icon: 'none'
        });
      }
    }
    if (this.data.pendingOpenCancel) {
      this.setData({ pendingOpenCancel: false });
      if (detail && detail.canCancel) {
        this.onCancelOrderTap();
      }
    }
    if (this.data.pendingOpenAfterSales) {
      this.setData({ pendingOpenAfterSales: false });
      if (detail && detail.hasAfterSalesEntry) {
        this.onAfterSalesTap();
      }
    }
  },

  refreshDoorQrPopupSilently() {
    const detail = this.data.detail;
    if (!detail || !detail.id) return;
    fetchUserOrderDoorQr(detail.id)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) return;
        this.setData({
          doorQrData: {
            qrImageUrl: res.data.qrImageUrl || '',
            statusText: res.data.statusText || '',
            expireTimeText: formatDateTime(res.data.expireTime)
          }
        });
      })
      .catch(() => {});
  },

  loadWalletSummary() {
    getUserFundsSummary()
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          return;
        }
        const walletBalanceText = formatMoney(res.data.balance);
        const detail = this.data.detail;
        const payState = buildPayPopupState(detail, walletBalanceText);
        this.setData({
          walletBalanceText,
          paymentMethodOptions: payState.paymentMethodOptions,
          walletInsufficient: payState.walletInsufficient
        });
      })
      .catch(() => {});
  },

  openPayPopup() {
    const detail = this.data.detail;
    if (!detail || !detail.canPayTail) {
      wx.showToast({
        title: TEXTS.notPayable,
        icon: 'none'
      });
      return;
    }
    const payState = buildPayPopupState(detail, this.data.walletBalanceText);
    this.setData({
      payPopupVisible: true,
      selectedPaymentMethod: PAYMENT_METHOD_WECHAT,
      paymentRows: payState.paymentRows,
      payButtonText: payState.payButtonText,
      paymentMethodOptions: payState.paymentMethodOptions,
      walletInsufficient: payState.walletInsufficient,
      paying: false
    });
    this.loadWalletSummary();
  },

  onRetryTap() {
    this.loadDetail();
  },

  onToggleSection(e) {
    const key = e.currentTarget.dataset.key;
    if (!key) {
      return;
    }
    const collapseState = Object.assign({}, this.data.collapseState, {
      [key]: !this.data.collapseState[key]
    });
    this.setData({ collapseState });
  },

  onCopyOrderNoTap() {
    const orderNo = this.data.detail && this.data.detail.orderNo;
    if (!orderNo) {
      return;
    }
    wx.setClipboardData({
      data: orderNo,
      success: () => {
        wx.showToast({
          title: TEXTS.copied,
          icon: 'none'
        });
      }
    });
  },

  onPreviewImageTap(e) {
    const urls = e.currentTarget.dataset.urls || [];
    const current = e.currentTarget.dataset.current || '';
    if (!urls.length || !current) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  },

  onImageError(e) {
    const faultIndex = Number(e.currentTarget.dataset.faultIndex);
    const imageIndex = Number(e.currentTarget.dataset.imageIndex);
    if (Number.isNaN(faultIndex) || Number.isNaN(imageIndex)) {
      return;
    }
    const faultList = (this.data.faultList || []).map((item) => ({
      ...item,
      images: Array.isArray(item.images) ? item.images.slice() : []
    }));
    if (!faultList[faultIndex] || !faultList[faultIndex].images[imageIndex]) {
      return;
    }
    faultList[faultIndex].images.splice(imageIndex, 1);
    faultList[faultIndex].previewUrls = faultList[faultIndex].images.map((item) => item.url);
    this.setData({ faultList });
  },

  onVideoError(e) {
    const faultIndex = Number(e.currentTarget.dataset.faultIndex);
    const videoIndex = Number(e.currentTarget.dataset.videoIndex);
    if (Number.isNaN(faultIndex) || Number.isNaN(videoIndex)) {
      return;
    }
    const faultList = (this.data.faultList || []).map((item) => ({
      ...item,
      videos: Array.isArray(item.videos) ? item.videos.slice() : []
    }));
    if (!faultList[faultIndex] || !faultList[faultIndex].videos[videoIndex]) {
      return;
    }
    faultList[faultIndex].videos.splice(videoIndex, 1);
    this.setData({ faultList });
  },

  onDoorQrTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id) {
      return;
    }
    this.setData({
      doorQrPopupVisible: true,
      doorQrPopupLoading: true,
      doorQrData: null
    });
    fetchUserOrderDoorQr(detail.id)
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
      doorQrData: null
    });
  },

  onConfirmCompletionTap() {
    const detail = this.data.detail;
    if (!detail || this.data.actionSubmitting) {
      return;
    }
    if (!detail.canConfirmCompletion) {
      if (detail && detail.confirmCompletionTip) {
        wx.showToast({
          title: detail.confirmCompletionTip,
          icon: 'none'
        });
      }
      return;
    }
    wx.showModal({
      title: TEXTS.confirmCompletionTitle,
      content: detail.confirmCompletionTip || TEXTS.confirmCompletionSubtitle,
      confirmText: TEXTS.confirmCompletionButton,
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.submitConfirmCompletion();
      }
    });
  },

  submitConfirmCompletion() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.canConfirmCompletion || this.data.actionSubmitting) {
      return;
    }
    this.setData({ actionSubmitting: true });
    confirmUserOrderCompletion({
      orderId: detail.id
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const nextDetail = mapDetail(res.data);
        this.applyDetail(nextDetail, {
          actionSubmitting: false
        });
        wx.showToast({
          title: TEXTS.confirmCompletionSuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        this.setData({ actionSubmitting: false });
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      });
  },


  onCancelOrderTap() {
    const detail = this.data.detail;
    if (!detail || !detail.canCancel || this.data.actionSubmitting) {
      return;
    }
    this.setData({
      cancelPopupVisible: true,
      selectedCancelReason: '',
      cancelRemark: '',
      actionSubmitting: false
    });
  },

  onAfterSalesTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.hasAfterSalesEntry) {
      if (detail && detail.afterSalesTip) {
        wx.showToast({
          title: detail.afterSalesTip,
          icon: 'none'
        });
      }
      return;
    }
    router.navigateTo({
      url: `/pages/after-sales/index?orderId=${detail.id}`
    });
  },

  onReviewTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.hasReviewEntry) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-review/index?orderId=${detail.id}`
    });
  },

  onModifyOrderTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.hasModifyEntry) {
      return;
    }
    if (detail.canModifyOrder) {
      draftStore.saveDraft(buildDraftFromDetail(detail, 'order'));
      wx.navigateTo({
        url: '/pages/order-flow/step4/index'
      });
      return;
    }
    draftStore.saveDraft(buildDraftFromDetail(detail, 'appointment'));
    wx.navigateTo({
      url: '/pages/order-flow/step5/index'
    });
  },

  onCloseCancelPopup() {
    if (this.data.actionSubmitting) {
      return;
    }
    this.setData({ cancelPopupVisible: false });
  },

  onCloseAfterSalesPopup() {
    if (this.data.actionSubmitting) {
      return;
    }
    this.setData({ afterSalesPopupVisible: false });
  },

  onSelectCancelReason(e) {
    this.setData({
      selectedCancelReason: e.currentTarget.dataset.reason || ''
    });
  },

  onSelectAfterSalesReason(e) {
    this.setData({
      selectedAfterSalesReason: e.currentTarget.dataset.reason || ''
    });
  },

  onCancelRemarkInput(e) {
    this.setData({ cancelRemark: e.detail.value || '' });
  },

  onAfterSalesDescriptionInput(e) {
    this.setData({ afterSalesDescription: e.detail.value || '' });
  },

  onCancelConfirm() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.canCancel || this.data.actionSubmitting) {
      return;
    }
    const reason = buildSubmitReason(this.data.selectedCancelReason, this.data.cancelRemark);
    if (!reason) {
      wx.showToast({
        title: TEXTS.reasonRequired,
        icon: 'none'
      });
      return;
    }

    this.setData({ actionSubmitting: true });
    cancelUserOrder({
      orderId: detail.id,
      reason
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const nextDetail = mapDetail(res.data);
        this.applyDetail(nextDetail, {
          cancelPopupVisible: false,
          actionSubmitting: false,
          selectedCancelReason: '',
          cancelRemark: ''
        });
        wx.showToast({
          title: TEXTS.cancelSuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        this.setData({ actionSubmitting: false });
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      });
  },

  onAfterSalesConfirm() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.canApplyAfterSales || this.data.actionSubmitting) {
      return;
    }
    const reason = buildSubmitReason(this.data.selectedAfterSalesReason, this.data.afterSalesDescription);
    if (!reason) {
      wx.showToast({
        title: TEXTS.reasonRequired,
        icon: 'none'
      });
      return;
    }

    this.setData({ actionSubmitting: true });
    applyUserOrderAfterSales({
      orderId: detail.id,
      reason,
      description: (this.data.afterSalesDescription || '').trim()
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const nextDetail = mapDetail(res.data);
        this.applyDetail(nextDetail, {
          afterSalesPopupVisible: false,
          actionSubmitting: false,
          selectedAfterSalesReason: '',
          afterSalesDescription: ''
        });
        wx.showToast({
          title: TEXTS.afterSalesSuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        this.setData({ actionSubmitting: false });
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      });
  },

  onPayTap() {
    this.openPayPopup();
  },

  onSelectPaymentMethod(e) {
    const method = Number(e.currentTarget.dataset.method || PAYMENT_METHOD_WECHAT);
    this.setData({
      selectedPaymentMethod: method
    });
  },

  onClosePayPopup() {
    if (this.data.paying) {
      return;
    }
    this.setData({
      payPopupVisible: false
    });
  },

  onPayConfirm() {
    const detail = this.data.detail;
    if (!detail || !detail.id || !detail.canPayTail || this.data.paying) {
      return;
    }

    const payAmount = calculateRemainingAmount(detail);
    if (this.data.selectedPaymentMethod === PAYMENT_METHOD_WALLET && normalizeMoney(this.data.walletBalanceText) + 0.0001 < payAmount) {
      wx.showToast({
        title: TEXTS.walletInsufficient,
        icon: 'none'
      });
      return;
    }

    this.setData({ paying: true });
    payUserOrderTail({
      orderId: detail.id,
      paymentMethod: this.data.selectedPaymentMethod
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const nextDetail = mapDetail(res.data);
        this.applyDetail(nextDetail, {
          payPopupVisible: false,
          paying: false
        });
        wx.showToast({
          title: TEXTS.paySuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        this.setData({ paying: false });
        wx.showToast({
          title: (err && err.message) || TEXTS.loadFailed,
          icon: 'none'
        });
      });
  },

  onPopupInnerTap() {}
});
