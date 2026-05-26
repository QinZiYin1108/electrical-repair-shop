const router = require('../../utils/router');
const { showUploadErrorModal } = require('../../utils/uploadFeedback');
const { fetchUserOrderDetail } = require('../../api/userOrders');
const {
  fetchUserOrderReview,
  submitUserReview,
  uploadUserReviewImage
} = require('../../api/userReviews');

const MAX_IMAGE_COUNT = 5;

const TEXTS = {
  loading: '正在加载评价信息...',
  loadFailed: '评价信息加载失败',
  retry: '重新加载',
  title: '服务评价',
  viewOrder: '查看订单',
  serviceTitleLabel: '服务项目',
  technicianLabel: '服务师傅',
  serviceModeLabel: '服务方式',
  orderNoLabel: '订单号',
  completedLabel: '完成时间',
  formTitle: '请为本次服务打分',
  scoreTitle: '服务评分',
  contentTitle: '评价内容',
  contentPlaceholder: '可以描述服务态度、维修效果、沟通体验等，帮助其他用户更好地了解师傅',
  imageTitle: '评价图片',
  imageTip: '最多上传 5 张图片，提交时统一上传',
  anonymousLabel: '匿名评价',
  anonymousTip: '开启后，其他用户和师傅看到的将是匿名用户',
  addImage: '添加图片',
  submit: '提交评价',
  submitting: '提交中...',
  uploadLoading: '正在上传评价图片...',
  submitSuccess: '评价提交成功',
  contentRequired: '请填写评价内容或上传图片',
  noReviewTip: '当前订单暂不可评价',
  historyTitle: '我的评价',
  replyTitle: '师傅回复',
  deleteText: '删除',
  statusLabel: '状态'
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

function mapOrderDetail(detail) {
  const item = detail || {};
  return {
    id: item.id || '',
    orderNo: item.orderNo || '',
    technicianName: item.technicianName || '',
    serviceModeText: item.serviceModeText || '',
    serviceTitle: buildServiceTitle(item),
    completionTimeText: formatDateTime(item.completionTime || item.updatedTime || 0),
    canReview: !!item.canReview,
    hasReview: !!item.hasReview
  };
}

function mapReview(review) {
  if (!review) {
    return null;
  }
  return {
    id: review.id || '',
    rating: Number(review.rating || 0),
    content: review.content || '',
    isAnonymous: Number(review.isAnonymous || 0) === 1,
    statusText: review.statusText || '',
    createdTimeText: formatDateTime(review.createdTime),
    replyContent: review.replyContent || '',
    replyTimeText: formatDateTime(review.replyTime),
    images: Array.isArray(review.images)
      ? review.images
          .map((item) => ({
            id: item.id || item.url || '',
            url: item.url || '',
            thumbnailUrl: item.thumbnailUrl || item.url || ''
          }))
          .filter((item) => !!item.url)
      : []
  };
}

function createLocalImageItem(file, index) {
  const path = file.path || file.tempFilePath || '';
  return {
    id: `review-image-${Date.now()}-${index}`,
    url: path,
    thumbnailUrl: path,
    localPath: path
  };
}

function createUploadedImagePayload(item, uploaded, index) {
  return {
    url: uploaded.url || '',
    name: uploaded.name || `review-image-${index + 1}.jpg`,
    fileSize: uploaded.fileSize || 0,
    mimeType: uploaded.mimeType || 'image/jpeg',
    width: uploaded.width || 0,
    height: uploaded.height || 0
  };
}

Page({
  data: {
    texts: TEXTS,
    orderId: '',
    loading: true,
    loadError: '',
    detail: null,
    review: null,
    rating: 5,
    content: '',
    isAnonymous: false,
    uploadImages: [],
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
    this.loadPage();
  },

  loadPage() {
    this.setData({
      loading: true,
      loadError: ''
    });

    Promise.all([
      fetchUserOrderDetail(this.data.orderId),
      fetchUserOrderReview(this.data.orderId)
    ])
      .then(([detailRes, reviewRes]) => {
        if (!detailRes || detailRes.code !== 200 || !detailRes.data) {
          throw new Error((detailRes && detailRes.message) || TEXTS.loadFailed);
        }
        const detail = mapOrderDetail(detailRes.data);
        const review =
          reviewRes && reviewRes.code === 200 && reviewRes.data
            ? mapReview(reviewRes.data)
            : null;
        this.setData({
          loading: false,
          loadError: '',
          detail,
          review,
          rating: review ? Number(review.rating || 5) : 5,
          content: '',
          isAnonymous: false,
          uploadImages: [],
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
    this.loadPage();
  },

  onChooseRating(e) {
    const value = Number(e.currentTarget.dataset.value || 5);
    this.setData({
      rating: value >= 1 && value <= 5 ? value : 5
    });
  },

  onContentInput(e) {
    this.setData({
      content: e.detail.value || ''
    });
  },

  onToggleAnonymous() {
    this.setData({
      isAnonymous: !this.data.isAnonymous
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
        this.setData({
          uploadImages: this.data.uploadImages.concat(
            tempFiles
              .slice(0, remainCount)
              .map((item, index) => createLocalImageItem(item, this.data.uploadImages.length + index))
          )
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

  onPreviewReviewImage(e) {
    const current = e.currentTarget.dataset.current || '';
    const review = this.data.review || {};
    const urls = (review.images || []).map((item) => item.url).filter(Boolean);
    if (!current || !urls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  },

  uploadPendingImages() {
    let chain = Promise.resolve([]);
    this.data.uploadImages.forEach((item, index) => {
      chain = chain.then((images) => {
        if (!item || !item.localPath) {
          return images;
        }
        return uploadUserReviewImage(item.localPath).then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || TEXTS.loadFailed);
          }
          images.push(createUploadedImagePayload(item, res.data, index));
          return images;
        });
      });
    });
    return chain;
  },

  onSubmitTap() {
    const detail = this.data.detail || {};
    if (!detail.id || !detail.canReview || this.data.review || this.data.submitting) {
      return;
    }
    if (!String(this.data.content || '').trim() && !this.data.uploadImages.length) {
      wx.showToast({
        title: TEXTS.contentRequired,
        icon: 'none'
      });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({
      title: TEXTS.uploadLoading,
      mask: true
    });

    this.uploadPendingImages()
      .then((images) => submitUserReview({
        orderId: detail.id,
        rating: this.data.rating,
        content: String(this.data.content || '').trim(),
        isAnonymous: this.data.isAnonymous ? 1 : 0,
        images
      }))
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        const review = mapReview(res.data);
        this.setData({
          review,
          detail: Object.assign({}, detail, {
            canReview: false,
            hasReview: true
          }),
          uploadImages: [],
          content: '',
          submitting: false
        });
        wx.showToast({
          title: TEXTS.submitSuccess,
          icon: 'success'
        });
      })
      .catch((err) => {
        this.setData({ submitting: false });
        showUploadErrorModal(err, {
          title: '提交失败',
          fallback: TEXTS.loadFailed
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
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack({ delta: 1 });
      return;
    }
    router.redirectTo({
      url: `/pages/order-detail/index?orderId=${this.data.orderId}`
    });
  }
});
