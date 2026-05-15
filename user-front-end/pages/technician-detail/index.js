const draftStore = require("../../utils/orderDraftStore");
const { fetchTechnicianDetail, toggleTechnicianFollow } = require("../../api/userOrderFlow");

const LABELS = {
  toastMissing: "师傅信息缺失",
  toastLoading: "加载中...",
  toastLoadFail: "师傅信息加载失败",
  toastFollowFail: "更新关注状态失败",
  toastLocationMissing: "该师傅暂未设置服务地址",
  step: "师傅主页",
  title: "师傅详情",
  rating: "评分",
  orderCount: "接单",
  completedOrderCount: "总完成单数",
  follow: "关注",
  followed: "已关注",
  workYears: "工作年限",
  specialties: "擅长方向",
  certificates: "证书",
  education: "学历",
  location: "服务地址",
  intro: "个人介绍",
  reviewTitle: "用户评价",
  reviewEmpty: "暂时还没有评价",
  anonymousUser: "匿名用户",
  replyTitle: "师傅回复：",
  yearSuffix: " 年",
  completedOrderSuffix: " 单",
  defaultInitial: "师",
  back: "返回",
  chooseTechnician: "选择师傅"
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

function mapReviewList(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || '',
    userDisplayName: item.userDisplayName || LABELS.anonymousUser,
    rating: Number(item.rating || 0),
    stars: [1, 2, 3, 4, 5].map((value) => ({
      value,
      active: value <= Number(item.rating || 0)
    })),
    content: item.content || '',
    createdTimeText: formatDateTime(item.createdTime),
    replyContent: item.replyContent || '',
    replyTimeText: formatDateTime(item.replyTime),
    images: Array.isArray(item.images)
      ? item.images
          .map((image) => ({
            id: image.id || image.url || '',
            url: image.url || '',
            thumbnailUrl: image.thumbnailUrl || image.url || ''
          }))
          .filter((image) => !!image.url)
      : []
  }));
}

function mapDetail(data) {
  const detail = data || {};
  return Object.assign({}, detail, {
    completedOrderCount: detail.completedOrderCount != null
      ? Number(detail.completedOrderCount)
      : Number(detail.orderCount || 0),
    reviews: mapReviewList(detail.reviews)
  });
}

function safeBack(fallbackUrl, delta) {
  const pages = getCurrentPages();
  const backDelta = Number(delta) > 0 ? Number(delta) : 1;
  if (pages.length > backDelta) {
    wx.navigateBack({ delta: backDelta });
    return;
  }
  wx.redirectTo({
    url: fallbackUrl
  });
}

Page({
  data: {
    technicianId: "",
    detail: null,
    loading: true,
    labels: LABELS
  },

  onLoad(options) {
    const technicianId = options && options.technicianId;
    if (!technicianId) {
      wx.showToast({
        title: LABELS.toastMissing,
        icon: "none"
      });
      safeBack("/pages/order-flow/step3/index", 1);
      return;
    }
    this.setData({ technicianId });
    this.loadDetail(technicianId);
  },

  loadDetail(technicianId) {
    this.setData({ loading: true });
    wx.showLoading({
      title: LABELS.toastLoading
    });

    fetchTechnicianDetail(technicianId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("empty");
        }
        this.setData({
          detail: mapDetail(res.data)
        });
      })
      .catch(() => {
        wx.showToast({
          title: LABELS.toastLoadFail,
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  onToggleFollow() {
    const detail = this.data.detail;
    if (!detail || !detail.id) {
      return;
    }

    toggleTechnicianFollow({
      technicianId: detail.id,
      follow: !detail.isFollowed
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("fail");
        }
        this.setData({
          detail: Object.assign({}, detail, {
            isFollowed: !!res.data.isFollowed
          })
        });
      })
      .catch(() => {
        wx.showToast({
          title: LABELS.toastFollowFail,
          icon: "none"
        });
      });
  },

  onOpenLocation() {
    const detail = this.data.detail;
    const latitude = Number(detail && detail.latitude);
    const longitude = Number(detail && detail.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      wx.showToast({
        title: LABELS.toastLocationMissing,
        icon: "none"
      });
      return;
    }
    wx.openLocation({
      latitude,
      longitude,
      name: detail && detail.name ? detail.name : "维修师傅",
      address: (detail && detail.locationAddress) || "",
      scale: 16
    });
  },

  onChooseTechnician() {
    const detail = this.data.detail;
    if (!detail || !detail.id) {
      return;
    }
    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        selectedTechnicianId: detail.id,
        selectedTechnicianName: detail.name || ""
      })
    );

    const pages = getCurrentPages();
    if (pages.length >= 3 && pages[pages.length - 2].route === "pages/technician-browse/index") {
      safeBack("/pages/order-flow/step3/index", 2);
      return;
    }
    safeBack("/pages/order-flow/step3/index", 1);
  },

  onBack() {
    safeBack("/pages/order-flow/step3/index", 1);
  },

  onPreviewReviewImage(e) {
    const reviewIndex = Number(e.currentTarget.dataset.reviewIndex);
    const current = e.currentTarget.dataset.current || '';
    const detail = this.data.detail || {};
    const reviews = Array.isArray(detail.reviews) ? detail.reviews : [];
    const review = reviews[reviewIndex];
    if (!review || !current) {
      return;
    }
    const urls = (review.images || []).map((item) => item.url).filter(Boolean);
    if (!urls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  }
});
