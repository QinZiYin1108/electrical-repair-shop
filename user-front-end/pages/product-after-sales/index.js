const router = require('../../utils/router');
const { showUploadErrorModal } = require('../../utils/uploadFeedback');
const {
  fetchUserProductAfterSalesDetail,
  applyUserProductAfterSales,
  cancelUserProductAfterSales
} = require('../../api/userProductOrders');
const { uploadFaultMedia } = require('../../api/userOrderFlow');

const MAX_IMAGE_COUNT = 5;
const REASON_OPTIONS = [
  '商品破损',
  '商品与描述不符',
  '发货太慢',
  '收到后不满意',
  '其他'
];

const TEXTS = {
  loading: '正在加载商品售后信息...',
  loadFailed: '商品售后信息加载失败',
  retry: '重新加载',
  title: '商品售后',
  orderNoLabel: '订单号',
  orderStatusLabel: '订单状态',
  paymentStatusLabel: '支付状态',
  deliveryStatusLabel: '物流状态',
  productLabel: '商品信息',
  applyTipTitle: '申请说明',
  applyTipFallback: '订单待发货可申请仅退款，已发货后可申请退货退款。',
  historyTitle: '售后记录',
  formTitle: '填写售后申请',
  typeTitle: '售后类型',
  reasonTitle: '售后原因',
  descTitle: '补充说明',
  descPlaceholder: '请补充退款原因、商品问题或当前情况，方便平台尽快处理',
  imageTitle: '图片凭证',
  imageTip: '最多选择 5 张图片，提交时统一上传',
  videoTitle: '视频凭证',
  videoTip: '最多选择 1 段视频，提交时统一上传',
  addImage: '添加图片',
  addVideo: '上传视频',
  submit: '提交售后申请',
  submitting: '提交中...',
  submitUploadLoading: '正在上传凭证...',
  uploadFailed: '上传失败，请稍后重试',
  typeRequired: '请选择售后类型',
  reasonRequired: '请选择售后原因',
  submitSuccess: '售后申请已提交',
  cancelConfirmTitle: '取消售后申请',
  cancelConfirmContent: '确认取消当前售后申请吗？',
  cancelSuccess: '售后申请已取消',
  cancelAction: '取消售后',
  adminRemarkLabel: '处理备注',
  reasonLabel: '申请原因',
  typeLabel: '售后类型',
  descLabel: '问题说明',
  createdLabel: '申请时间',
  processedLabel: '处理时间',
  completedLabel: '完成时间',
  refundAmountLabel: '退款金额',
  noData: '当前暂无售后记录',
  viewOrder: '查看订单',
  mediaEmpty: '暂未上传',
  videoDurationSuffix: '秒'
};

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) return '';
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function normalizeMoney(value) {
  const amount = Number(value || 0);
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00';
}

function mapImageItem(item) {
  return {
    id: item.id || item.url || '',
    url: item.url || '',
    thumbnailUrl: item.thumbnailUrl || item.url || '',
    name: item.name || '',
    mimeType: item.mimeType || 'image/jpeg',
    localPath: '',
    isLocal: false
  };
}

function mapVideoItem(item) {
  const duration = Number(item.duration || 0);
  return {
    id: item.id || item.url || '',
    url: item.url || '',
    thumbnailUrl: item.thumbnailUrl || '',
    name: item.name || '',
    mimeType: item.mimeType || 'video/mp4',
    duration,
    durationText: duration > 0 ? `${duration}${TEXTS.videoDurationSuffix}` : '',
    localPath: '',
    isLocal: false
  };
}

function mapApplication(application) {
  if (!application) return null;
  return {
    id: application.id || '',
    applicationTypeText: application.applicationTypeText || '',
    statusText: application.statusText || '',
    reason: application.reason || '',
    description: application.description || '',
    refundAmount: normalizeMoney(application.refundAmount),
    adminRemark: application.adminRemark || '',
    canCancel: !!application.canCancel,
    createdTimeText: formatDateTime(application.createdTime),
    processedTimeText: formatDateTime(application.processedTime),
    completedTimeText: formatDateTime(application.completedTime),
    evidenceImages: Array.isArray(application.evidenceImages) ? application.evidenceImages.map(mapImageItem) : [],
    evidenceVideos: Array.isArray(application.evidenceVideos) ? application.evidenceVideos.map(mapVideoItem) : []
  };
}

function mapDetail(data) {
  const detail = data || {};
  return {
    orderId: detail.orderId || '',
    orderNo: detail.orderNo || '',
    orderStatusText: detail.orderStatusText || '',
    paymentStatusText: detail.paymentStatusText || '',
    deliveryStatusText: detail.deliveryStatusText || '',
    productSummary: detail.productSummary || '商品订单',
    canApplyAfterSales: !!detail.canApplyAfterSales,
    afterSalesTip: detail.afterSalesTip || '',
    applicationTypeOptions: Array.isArray(detail.applicationTypeOptions) ? detail.applicationTypeOptions : [],
    application: mapApplication(detail.application)
  };
}

function createLocalImageItem(file, index) {
  const path = file.path || file.tempFilePath || '';
  return {
    id: `local-image-${Date.now()}-${index}`,
    url: path,
    thumbnailUrl: path,
    name: `after-sales-image-${index + 1}.jpg`,
    mimeType: file.type || 'image/jpeg',
    localPath: path,
    isLocal: true
  };
}

function createLocalVideoItem(file) {
  const path = file.tempFilePath || '';
  return {
    id: `local-video-${Date.now()}`,
    url: path,
    thumbnailUrl: file.thumbTempFilePath || '',
    name: 'after-sales-video.mp4',
    mimeType: 'video/mp4',
    duration: Number(file.duration || 0),
    durationText: Number(file.duration || 0) > 0 ? `${Number(file.duration || 0)}${TEXTS.videoDurationSuffix}` : '',
    localPath: path,
    isLocal: true
  };
}

Page({
  data: {
    texts: TEXTS,
    reasonOptions: REASON_OPTIONS,
    orderId: '',
    loading: true,
    loadError: '',
    detail: null,
    selectedType: '',
    selectedReason: '',
    description: '',
    uploadImages: [],
    uploadVideo: null,
    submitting: false
  },

  onLoad(options) {
    const orderId = options && options.orderId ? options.orderId : '';
    if (!orderId) {
      this.setData({ loading: false, loadError: TEXTS.loadFailed });
      return;
    }
    this.setData({ orderId });
    this.loadDetail();
  },

  loadDetail() {
    this.setData({ loading: true, loadError: '' });
    fetchUserProductAfterSalesDetail(this.data.orderId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) throw new Error((res && res.message) || TEXTS.loadFailed);
        const detail = mapDetail(res.data);
        const defaultType = detail.applicationTypeOptions.length === 1 ? Number(detail.applicationTypeOptions[0].value) : '';
        this.setData({
          loading: false,
          loadError: '',
          detail,
          selectedType: defaultType || '',
          selectedReason: '',
          description: '',
          uploadImages: [],
          uploadVideo: null,
          submitting: false
        });
      })
      .catch((err) => {
        this.setData({ loading: false, loadError: (err && err.message) || TEXTS.loadFailed });
      });
  },

  onRetryTap() {
    this.loadDetail();
  },

  onChooseType(e) {
    this.setData({ selectedType: Number(e.currentTarget.dataset.value || 0) || '' });
  },

  onChooseReason(e) {
    this.setData({ selectedReason: e.currentTarget.dataset.reason || '' });
  },

  onDescriptionInput(e) {
    this.setData({ description: e.detail.value || '' });
  },

  onChooseImages() {
    const remainCount = MAX_IMAGE_COUNT - this.data.uploadImages.length;
    if (remainCount <= 0) return;
    wx.chooseMedia({
      count: remainCount,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = Array.isArray(res.tempFiles) ? res.tempFiles : [];
        const nextImages = this.data.uploadImages.concat(files.map((file, index) => createLocalImageItem(file, this.data.uploadImages.length + index)));
        this.setData({ uploadImages: nextImages.slice(0, MAX_IMAGE_COUNT) });
      }
    });
  },

  onChooseVideo() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['video'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = Array.isArray(res.tempFiles) && res.tempFiles[0] ? res.tempFiles[0] : null;
        if (!file) return;
        this.setData({ uploadVideo: createLocalVideoItem(file) });
      }
    });
  },

  onDeleteImage(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (!Number.isInteger(index) || index < 0) return;
    const nextImages = this.data.uploadImages.slice();
    nextImages.splice(index, 1);
    this.setData({ uploadImages: nextImages });
  },

  onDeleteVideo() {
    this.setData({ uploadVideo: null });
  },

  onPreviewEvidenceImage(e) {
    const current = e.currentTarget.dataset.current || '';
    const detail = this.data.detail || {};
    const urls = ((detail.application && detail.application.evidenceImages) || []).map((item) => item.url).filter(Boolean);
    if (!current || !urls.length) return;
    wx.previewImage({ current, urls });
  },

  onPreviewUploadImage(e) {
    const current = e.currentTarget.dataset.current || '';
    const urls = (this.data.uploadImages || []).map((item) => item.url).filter(Boolean);
    if (!current || !urls.length) return;
    wx.previewImage({ current, urls });
  },

  uploadPendingMedia() {
    const imageTasks = (this.data.uploadImages || []).map((item) => {
      if (!item.isLocal || !item.localPath) return Promise.resolve({ url: item.url, name: item.name, mimeType: item.mimeType });
      return uploadFaultMedia(item.localPath, 'image').then((res) => {
        if (!res || res.code !== 200 || !res.data) throw new Error((res && res.message) || TEXTS.uploadFailed);
        return res.data;
      });
    });
    const videoTask = this.data.uploadVideo && this.data.uploadVideo.isLocal && this.data.uploadVideo.localPath
      ? uploadFaultMedia(this.data.uploadVideo.localPath, 'video').then((res) => {
        if (!res || res.code !== 200 || !res.data) throw new Error((res && res.message) || TEXTS.uploadFailed);
        return res.data;
      })
      : Promise.resolve(this.data.uploadVideo ? { url: this.data.uploadVideo.url, name: this.data.uploadVideo.name, mimeType: this.data.uploadVideo.mimeType } : null);

    return Promise.all([Promise.all(imageTasks), videoTask]).then(([images, video]) => ({ images, video }));
  },

  onSubmitTap() {
    const detail = this.data.detail || {};
    if (!detail.orderId || !detail.canApplyAfterSales || this.data.submitting) return;
    const applicationType = Number(this.data.selectedType || 0);
    const reason = (this.data.selectedReason || '').trim();
    if (!applicationType) {
      wx.showToast({ title: TEXTS.typeRequired, icon: 'none' });
      return;
    }
    if (!reason) {
      wx.showToast({ title: TEXTS.reasonRequired, icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: TEXTS.submitUploadLoading, mask: true });
    this.uploadPendingMedia()
      .then(({ images, video }) => applyUserProductAfterSales({
        orderId: detail.orderId,
        applicationType,
        reason,
        description: (this.data.description || '').trim(),
        images,
        video
      }))
      .then((res) => {
        if (!res || res.code !== 200) throw new Error((res && res.message) || TEXTS.loadFailed);
        wx.showToast({ title: TEXTS.submitSuccess, icon: 'success' });
        this.loadDetail();
      })
      .catch((err) => {
        this.setData({ submitting: false });
        showUploadErrorModal(err, {
          title: '提交失败',
          fallback: TEXTS.uploadFailed
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onCancelAfterSalesTap() {
    const detail = this.data.detail || {};
    const application = detail.application || {};
    if (!detail.orderId || !application.canCancel || this.data.submitting) return;
    wx.showModal({
      title: TEXTS.cancelConfirmTitle,
      content: TEXTS.cancelConfirmContent,
      success: ({ confirm }) => {
        if (!confirm) return;
        this.setData({ submitting: true });
        cancelUserProductAfterSales({ orderId: detail.orderId })
          .then((res) => {
            if (!res || res.code !== 200) throw new Error((res && res.message) || TEXTS.loadFailed);
            wx.showToast({ title: TEXTS.cancelSuccess, icon: 'success' });
            this.loadDetail();
          })
          .catch((err) => {
            this.setData({ submitting: false });
            wx.showToast({ title: (err && err.message) || TEXTS.loadFailed, icon: 'none' });
          });
      }
    });
  },

  onViewOrderTap() {
    if (!this.data.orderId) return;
    router.navigateTo({ url: `/pages/product-order-detail/index?orderId=${this.data.orderId}` });
  }
});
