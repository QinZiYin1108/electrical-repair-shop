const router = require('../../utils/router');
const { showUploadErrorModal } = require('../../utils/uploadFeedback');
const {
  fetchUserAfterSalesDetail,
  applyUserOrderAfterSales,
  cancelUserOrderAfterSales
} = require('../../api/userOrders');
const { uploadFaultMedia } = require('../../api/userOrderFlow');

const MAX_IMAGE_COUNT = 5;
const REASON_OPTIONS = [
  '维修后仍有问题',
  '服务体验不佳',
  '费用存在争议',
  '师傅未按约处理',
  '其他'
];

const TEXTS = {
  loading: '正在加载售后信息...',
  loadFailed: '售后信息加载失败',
  retry: '重新加载',
  title: '售后申请',
  orderNoLabel: '订单号',
  orderStatusLabel: '订单状态',
  serviceModeLabel: '服务方式',
  technicianLabel: '维修师傅',
  applyTipTitle: '申请说明',
  applyTipFallback: '仅订单完成后7天内可申请售后，提交后由管理员审核是否退款。',
  historyTitle: '售后记录',
  formTitle: '填写售后申请',
  reasonTitle: '售后原因',
  descTitle: '补充说明',
  descPlaceholder: '请补充问题表现、服务经过或退款诉求，便于平台更快处理',
  imageTitle: '图片证据',
  imageTip: '最多选择 5 张图片，提交时统一上传',
  videoTitle: '视频证据',
  videoTip: '最多选择 1 段视频，提交时统一上传',
  addImage: '添加图片',
  addVideo: '上传视频',
  submit: '提交售后申请',
  submitting: '提交中...',
  submitUploadLoading: '正在上传凭证...',
  uploadFailed: '上传失败，请稍后重试',
  reasonRequired: '请选择售后原因',
  submitSuccess: '售后申请已提交',
  cancelConfirmTitle: '取消售后申请',
  cancelConfirmContent: '确认取消当前售后申请吗？',
  cancelSuccess: '售后申请已取消',
  cancelAction: '取消售后',
  adminRemarkLabel: '处理备注',
  reasonLabel: '申请原因',
  descLabel: '问题说明',
  createdLabel: '申请时间',
  processedLabel: '处理时间',
  completedLabel: '完成时间',
  refundAmountLabel: '退款金额',
  noData: '暂无售后记录',
  viewOrder: '查看订单',
  mediaEmpty: '暂未上传',
  videoDurationSuffix: '秒',
  deleteText: '删除'
};

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

function normalizeMoney(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function buildServiceTitle(detail) {
  const parts = [];
  if (detail.serviceCategoryName) {
    parts.push(detail.serviceCategoryName);
  }
  if (detail.serviceTypeName) {
    parts.push(detail.serviceTypeName);
  }
  return parts.join(' ') || detail.serviceTypeName || '维修订单';
}

function getFileNameFromPath(filePath, fallback) {
  const path = String(filePath || '');
  if (!path) {
    return fallback;
  }
  const parts = path.split('/');
  return parts[parts.length - 1] || fallback;
}

function mapImageItem(item) {
  return {
    id: item.id || item.url || '',
    url: item.url || '',
    thumbnailUrl: item.thumbnailUrl || item.url || '',
    name: item.name || '',
    mimeType: item.mimeType || 'image/jpeg',
    fileSize: item.fileSize || 0,
    width: item.width || 0,
    height: item.height || 0,
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
    fileSize: item.fileSize || 0,
    width: item.width || 0,
    height: item.height || 0,
    duration,
    durationText: duration > 0 ? `${duration}${TEXTS.videoDurationSuffix}` : '',
    localPath: '',
    isLocal: false
  };
}

function mapApplication(application) {
  if (!application) {
    return null;
  }
  return {
    id: application.id || '',
    status: application.status || 0,
    statusText: application.statusText || '',
    reason: application.reason || '',
    description: application.description || '',
    refundAmount: normalizeMoney(application.refundAmount),
    adminRemark: application.adminRemark || '',
    contactPhone: application.contactPhone || '',
    contactAddress: application.contactAddress || '',
    canCancel: !!application.canCancel,
    createdTimeText: formatDateTime(application.createdTime),
    processedTimeText: formatDateTime(application.processedTime),
    completedTimeText: formatDateTime(application.completedTime),
    evidenceImages: Array.isArray(application.evidenceImages)
      ? application.evidenceImages.map(mapImageItem).filter((item) => !!item.url)
      : [],
    evidenceVideos: Array.isArray(application.evidenceVideos)
      ? application.evidenceVideos.map(mapVideoItem).filter((item) => !!item.url)
      : []
  };
}

function mapDetail(data) {
  const detail = data || {};
  return {
    orderId: detail.orderId || '',
    orderNo: detail.orderNo || '',
    orderStatusText: detail.orderStatusText || '',
    serviceTypeName: detail.serviceTypeName || '',
    serviceCategoryName: detail.serviceCategoryName || '',
    serviceModeText: detail.serviceModeText || '',
    technicianName: detail.technicianName || '',
    canApplyAfterSales: !!detail.canApplyAfterSales,
    afterSalesTip: detail.afterSalesTip || '',
    serviceTitle: buildServiceTitle(detail),
    application: mapApplication(detail.application)
  };
}

function createLocalImageItem(file, index) {
  const path = file.path || file.tempFilePath || '';
  return {
    id: `local-image-${Date.now()}-${index}`,
    url: path,
    thumbnailUrl: path,
    name: getFileNameFromPath(path, `after-sales-image-${index + 1}.jpg`),
    mimeType: file.type || 'image/jpeg',
    fileSize: file.size || 0,
    width: file.width || 0,
    height: file.height || 0,
    localPath: path,
    isLocal: true
  };
}

function createLocalVideoItem(file) {
  const path = file.tempFilePath || '';
  const duration = Number(file.duration || 0);
  return {
    id: `local-video-${Date.now()}`,
    url: path,
    thumbnailUrl: file.thumbTempFilePath || '',
    name: getFileNameFromPath(path, 'after-sales-video.mp4'),
    mimeType: 'video/mp4',
    fileSize: file.size || 0,
    width: file.width || 0,
    height: file.height || 0,
    duration,
    durationText: duration > 0 ? `${duration}${TEXTS.videoDurationSuffix}` : '',
    localPath: path,
    isLocal: true
  };
}

function createUploadedImagePayload(item, uploaded, index) {
  return {
    url: uploaded.url || '',
    name: uploaded.name || item.name || `after-sales-image-${index + 1}.jpg`,
    fileSize: uploaded.fileSize || item.fileSize || 0,
    mimeType: uploaded.mimeType || item.mimeType || 'image/jpeg',
    width: uploaded.width || item.width || 0,
    height: uploaded.height || item.height || 0
  };
}

function createUploadedVideoPayload(item, uploaded) {
  return {
    url: uploaded.url || '',
    name: uploaded.name || item.name || 'after-sales-video.mp4',
    fileSize: uploaded.fileSize || item.fileSize || 0,
    mimeType: uploaded.mimeType || item.mimeType || 'video/mp4',
    duration: uploaded.duration || item.duration || 0,
    width: uploaded.width || item.width || 0,
    height: uploaded.height || item.height || 0,
    thumbnailUrl: uploaded.thumbnailUrl || item.thumbnailUrl || ''
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
    selectedReason: '',
    description: '',
    uploadImages: [],
    uploadVideo: null,
    submitting: false
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

  loadDetail() {
    this.setData({
      loading: true,
      loadError: ''
    });

    fetchUserAfterSalesDetail(this.data.orderId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.setData({
          loading: false,
          loadError: '',
          detail: mapDetail(res.data),
          selectedReason: '',
          description: '',
          uploadImages: [],
          uploadVideo: null,
          submitting: false
        });
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || TEXTS.loadFailed
        });
      });
  },

  onRetryTap() {
    this.loadDetail();
  },

  onChooseReason(e) {
    this.setData({
      selectedReason: e.currentTarget.dataset.reason || ''
    });
  },

  onDescriptionInput(e) {
    this.setData({
      description: e.detail.value || ''
    });
  },

  onChooseImages() {
    if (this.data.submitting) {
      return;
    }
    const remainCount = MAX_IMAGE_COUNT - this.data.uploadImages.length;
    if (remainCount <= 0) {
      wx.showToast({
        title: TEXTS.imageTip,
        icon: 'none'
      });
      return;
    }

    wx.chooseImage({
      count: remainCount,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFiles = Array.isArray(res.tempFiles) && res.tempFiles.length
          ? res.tempFiles
          : (res.tempFilePaths || []).map((path) => ({ path }));
        if (!tempFiles.length) {
          return;
        }
        const nextImages = this.data.uploadImages.concat(
          tempFiles
            .slice(0, remainCount)
            .map((item, index) => createLocalImageItem(item, this.data.uploadImages.length + index))
        );
        this.setData({
          uploadImages: nextImages
        });
      }
    });
  },

  onDeleteImage(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (Number.isNaN(index)) {
      return;
    }
    const uploadImages = this.data.uploadImages.slice();
    uploadImages.splice(index, 1);
    this.setData({ uploadImages });
  },

  onPreviewUploadImage(e) {
    const current = e.currentTarget.dataset.current || '';
    const urls = this.data.uploadImages.map((item) => item.url).filter(Boolean);
    if (!current || !urls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  },

  onPreviewEvidenceImage(e) {
    const current = e.currentTarget.dataset.current || '';
    const detail = this.data.detail || {};
    const application = detail.application || {};
    const urls = (application.evidenceImages || []).map((item) => item.url).filter(Boolean);
    if (!current || !urls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  },

  onChooseVideo() {
    if (this.data.submitting || this.data.uploadVideo) {
      return;
    }

    wx.chooseVideo({
      sourceType: ['album', 'camera'],
      compressed: true,
      maxDuration: 60,
      success: (res) => {
        if (!res || !res.tempFilePath) {
          return;
        }
        this.setData({
          uploadVideo: createLocalVideoItem(res)
        });
      }
    });
  },

  onDeleteVideo() {
    this.setData({
      uploadVideo: null
    });
  },

  uploadSingleMedia(filePath, mediaType) {
    return uploadFaultMedia(filePath, mediaType).then((res) => {
      if (!res || res.code !== 200 || !res.data) {
        throw new Error((res && res.message) || TEXTS.uploadFailed);
      }
      return res.data;
    });
  },

  uploadPendingMedia() {
    let imageChain = Promise.resolve([]);
    this.data.uploadImages.forEach((item, index) => {
      imageChain = imageChain.then((images) => {
        if (!item || !item.localPath) {
          return images;
        }
        return this.uploadSingleMedia(item.localPath, 'image').then((uploaded) => {
          images.push(createUploadedImagePayload(item, uploaded, index));
          return images;
        });
      });
    });

    return imageChain.then((images) => {
      if (!this.data.uploadVideo || !this.data.uploadVideo.localPath) {
        return {
          images,
          video: null
        };
      }
      return this.uploadSingleMedia(this.data.uploadVideo.localPath, 'video').then((uploaded) => ({
        images,
        video: createUploadedVideoPayload(this.data.uploadVideo, uploaded)
      }));
    });
  },

  onCancelAfterSalesTap() {
    const detail = this.data.detail || {};
    const application = detail.application || null;
    if (!application || !application.id || !application.canCancel || this.data.submitting) {
      return;
    }

    wx.showModal({
      title: TEXTS.cancelConfirmTitle,
      content: TEXTS.cancelConfirmContent,
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        this.setData({ submitting: true });
        cancelUserOrderAfterSales(application.id)
          .then((result) => {
            if (!result || result.code !== 200 || !result.data) {
              throw new Error((result && result.message) || TEXTS.loadFailed);
            }
            this.setData({
              detail: mapDetail(result.data),
              submitting: false
            });
            wx.showToast({
              title: TEXTS.cancelSuccess,
              icon: 'success'
            });
          })
          .catch((err) => {
            this.setData({ submitting: false });
            wx.showToast({
              title: (err && err.message) || TEXTS.loadFailed,
              icon: 'none'
            });
          });
      }
    });
  },

  onSubmitTap() {
    const detail = this.data.detail || {};
    if (!detail.orderId || !detail.canApplyAfterSales || this.data.submitting) {
      return;
    }

    const reason = (this.data.selectedReason || '').trim();
    if (!reason) {
      wx.showToast({
        title: TEXTS.reasonRequired,
        icon: 'none'
      });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({
      title: TEXTS.submitUploadLoading,
      mask: true
    });

    this.uploadPendingMedia()
      .then(({ images, video }) => applyUserOrderAfterSales({
        orderId: detail.orderId,
        reason,
        description: (this.data.description || '').trim(),
        images,
        video
      }))
      .then((res) => {
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        wx.showToast({
          title: TEXTS.submitSuccess,
          icon: 'success'
        });
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

  onViewOrderTap() {
    if (!this.data.orderId) {
      return;
    }
    router.navigateTo({
      url: `/pages/order-detail/index?orderId=${this.data.orderId}`
    });
  }
});
