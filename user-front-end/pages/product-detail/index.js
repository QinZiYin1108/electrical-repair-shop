const router = require("../../utils/router");
const { fetchMallProductDetail, toggleMallProductFavorite } = require("../../api/userMall");
const { addMallCart } = require("../../api/userMallOrder");
const userAddressApi = require("../../api/userAddress");

const SHEET_TITLES = {
  info: "商品信息",
  service: "服务保障",
  spec: "商品参数",
  address: "收货地址",
  review: "商品评价"
};

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? "0.00" : amount.toFixed(2);
}

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(value) {
  const time = Number(value || 0);
  if (!time) {
    return "";
  }
  const date = new Date(time);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function normalizeReviewList(list) {
  return (Array.isArray(list) ? list : []).map((item) => {
    const rating = Number(item.rating || 0);
    const images = Array.isArray(item.images)
      ? item.images
          .map((image) => ({
            id: image.id || image.url || "",
            url: image.url || "",
            thumbnailUrl: image.thumbnailUrl || image.url || ""
          }))
          .filter((image) => !!image.url)
      : [];
    const summaryTags = [];
    if (images.length) {
      summaryTags.push("有图");
    }
    if (rating >= 4) {
      summaryTags.push("好评");
    }
    if (item.replyContent) {
      summaryTags.push("已回复");
    }
    return {
      id: item.id || "",
      userDisplayName: item.userDisplayName || "匿名用户",
      rating,
      ratingText: rating > 0 ? rating.toFixed(1) : "0.0",
      stars: [1, 2, 3, 4, 5].map((value) => ({
        value,
        active: value <= rating
      })),
      content: item.content || "",
      createdTimeText: formatDateTime(item.createdTime),
      replyContent: item.replyContent || "",
      replyTimeText: formatDateTime(item.replyTime),
      images,
      summaryTags
    };
  });
}

function normalizeAddressList(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || "",
    contactName: item.contactName || "未填写联系人",
    contactPhone: item.contactPhone || "",
    addressTypeName: item.addressTypeName || "其他",
    fullAddress: item.fullAddress || item.detail || "",
    isDefault: Number(item.isDefault || 0) === 1
  }));
}

function normalizeDetail(data) {
  const galleryUrls = Array.isArray(data && data.galleryUrls) ? data.galleryUrls.filter(Boolean) : [];
  const videoUrls = Array.isArray(data && data.videoUrls) ? data.videoUrls.filter(Boolean) : [];
  const specifications = Array.isArray(data && data.specifications) ? data.specifications.filter((item) => item && (item.key || item.value)) : [];
  const stockQuantity = Number(data.stockQuantity || 0);
  const warrantyPeriod = Number(data.warrantyPeriod || 0);
  const sellingPrice = formatPrice(data.sellingPrice);
  const originalPrice = formatPrice(data.originalPrice);
  const favoriteCount = Number(data.favoriteCount || 0);
  const reviewCount = Number(data.reviewCount || 0);
  const reviewRating = Number(data.reviewRating || 0);
  const reviews = normalizeReviewList(data.reviews);
  const mediaItems = []
    .concat(galleryUrls.map((url) => ({ type: "image", url, isImage: true })))
    .concat(videoUrls.map((url) => ({ type: "video", url, isImage: false })));

  return {
    id: data.id || "",
    name: data.name || "未命名商品",
    productTypeText: data.productTypeText || "普通商品",
    categoryPath: data.categoryPath || data.categoryName || "",
    brandText: data.brand || "待补充",
    modelText: data.model || "待补充",
    descriptionText: data.description || "商家暂未补充商品描述",
    sellingPriceText: sellingPrice,
    originalPriceText: originalPrice,
    showOriginalPrice: Number(originalPrice) > 0 && originalPrice !== sellingPrice,
    stockStatusText: stockQuantity > 0 ? `现货 ${stockQuantity}` : "暂时缺货",
    warrantyText: warrantyPeriod > 0 ? `${warrantyPeriod} 个月` : "暂无说明",
    isHot: Number(data.isHot || 0) === 1,
    isNew: Number(data.isNew || 0) === 1,
    isRecommended: Number(data.isRecommended || 0) === 1,
    isFavorite: !!data.isFavorite,
    favoriteCount,
    reviewCount,
    reviewRating,
    reviewRatingText: reviewCount > 0 && reviewRating > 0 ? reviewRating.toFixed(1) : "暂无评分",
    reviews,
    reviewPreviewList: reviews.slice(0, 2),
    mediaItems,
    mediaCount: mediaItems.length,
    specifications,
    infoPreview: `${data.brand || "品牌待补充"} / ${data.model || "型号待补充"}`,
    servicePreview: `${stockQuantity > 0 ? "现货" : "缺货"} · ${warrantyPeriod > 0 ? `${warrantyPeriod}个月保修` : "暂无保修说明"}`,
    specPreview: specifications.length ? `共 ${specifications.length} 项参数` : "暂无详细参数",
    reviewPreview: reviewCount > 0 ? `${reviewRating > 0 ? reviewRating.toFixed(1) : "5.0"} 分 · ${reviewCount} 条评价` : "暂时还没有商品评价",
    favoriteText: favoriteCount > 0 ? `${favoriteCount} 人收藏` : "收藏商品"
  };
}

Page({
  data: {
    loading: true,
    addressLoading: false,
    favoriteLoading: false,
    actionLoading: false,
    productId: "",
    detail: null,
    currentMediaIndex: 0,
    showSheet: false,
    activeSheet: "",
    sheetTitle: "",
    addressList: [],
    selectedAddressId: "",
    selectedAddress: null
  },

  onLoad(options) {
    const productId = options && options.id ? String(options.id) : "";
    if (!productId) {
      wx.showToast({
        title: "商品信息缺失",
        icon: "none"
      });
      setTimeout(() => {
        wx.navigateBack({ delta: 1 });
      }, 500);
      return;
    }
    this.setData({ productId });
    this.loadPageData();
  },

  onShow() {
    if (this.data.productId && router.isLoggedIn()) {
      this.loadAddressList();
      return;
    }
    this.resetAddressState();
  },

  onPullDownRefresh() {
    this.loadPageData(true);
  },

  loadPageData(fromPullDown) {
    this.setData({ loading: true });
    const tasks = [this.loadDetail()];
    if (router.isLoggedIn()) {
      tasks.push(this.loadAddressList());
    } else {
      this.resetAddressState();
    }
    Promise.allSettled(tasks)
      .catch(() => {})
      .finally(() => {
        this.setData({ loading: false });
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  resetAddressState() {
    this.setData({
      addressLoading: false,
      addressList: [],
      selectedAddressId: "",
      selectedAddress: null
    });
  },

  loadDetail() {
    return fetchMallProductDetail(this.data.productId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("empty");
        }
        this.setData({
          detail: normalizeDetail(res.data),
          currentMediaIndex: 0
        });
      })
      .catch(() => {
        wx.showToast({
          title: "商品详情加载失败",
          icon: "none"
        });
      });
  },

  loadAddressList() {
    if (!router.isLoggedIn()) {
      this.resetAddressState();
      return Promise.resolve();
    }
    this.setData({ addressLoading: true });
    return userAddressApi
      .listUserAddresses()
      .then((res) => {
        if (!res || res.code !== 200) {
          this.setData({
            addressList: [],
            selectedAddressId: "",
            selectedAddress: null
          });
          return;
        }
        const addressList = normalizeAddressList(res.data);
        const currentSelected = addressList.find((item) => item.id === this.data.selectedAddressId) || null;
        const fallbackSelected = currentSelected || addressList.find((item) => item.isDefault) || addressList[0] || null;
        this.setData({
          addressList,
          selectedAddressId: fallbackSelected ? fallbackSelected.id : "",
          selectedAddress: fallbackSelected
        });
      })
      .catch(() => {
        this.setData({
          addressList: [],
          selectedAddressId: "",
          selectedAddress: null
        });
      })
      .finally(() => {
        this.setData({ addressLoading: false });
      });
  },

  onSwiperChange(e) {
    this.setData({
      currentMediaIndex: Number(e.detail.current || 0)
    });
  },

  onPreviewImage(e) {
    const current = e.currentTarget.dataset.url || "";
    const detail = this.data.detail;
    const imageUrls = (detail && detail.mediaItems || [])
      .filter((item) => item.type === "image" && item.url)
      .map((item) => item.url);
    if (!current || !imageUrls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls: imageUrls
    });
  },

  onPreviewReviewImage(e) {
    const reviewIndex = Number(e.currentTarget.dataset.reviewIndex);
    const current = e.currentTarget.dataset.current || "";
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
  },

  onOpenSheet(e) {
    const type = e.currentTarget.dataset.type || "";
    if (!type) {
      return;
    }
    this.setData({
      showSheet: true,
      activeSheet: type,
      sheetTitle: SHEET_TITLES[type] || "详情"
    });
  },

  onCloseSheet() {
    this.setData({
      showSheet: false,
      activeSheet: "",
      sheetTitle: ""
    });
  },

  onSelectAddress(e) {
    const addressId = e.currentTarget.dataset.id || "";
    const selectedAddress = this.data.addressList.find((item) => item.id === addressId) || null;
    if (!selectedAddress) {
      return;
    }
    this.setData({
      selectedAddressId: selectedAddress.id,
      selectedAddress,
      showSheet: false,
      activeSheet: "",
      sheetTitle: ""
    });
  },

  onManageAddress() {
    this.onCloseSheet();
    router.navigateTo({
      url: "/pages/address-list/index"
    });
  },

  onAddAddress() {
    this.onCloseSheet();
    router.navigateTo({
      url: "/pages/address-edit/index"
    });
  },

  onAddCartTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id || this.data.actionLoading) {
      return;
    }
    if (!router.isLoggedIn()) {
      router.requireLogin(`/pages/product-detail/index?id=${detail.id}`);
      return;
    }
    this.setData({ actionLoading: true });
    addMallCart({
      productId: detail.id,
      quantity: 1
    })
      .then((res) => {
        if (!res || res.code !== 200) {
          wx.showToast({
            title: (res && res.message) || "加入购物车失败",
            icon: "none"
          });
          return;
        }
        wx.showToast({
          title: "已加入购物车",
          icon: "success"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "加入购物车失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({ actionLoading: false });
      });
  },

  onBuyNowTap() {
    const detail = this.data.detail;
    if (!detail || !detail.id || this.data.actionLoading) {
      return;
    }
    wx.setStorageSync("mallCheckoutPayload", {
      scene: "buyNow",
      items: [
        {
          productId: detail.id,
          quantity: 1
        }
      ]
    });
    router.navigateTo({
      url: "/pages/product-checkout/index?scene=buyNow"
    });
  },

  onToggleFavorite() {
    const detail = this.data.detail;
    if (!detail || !detail.id || this.data.favoriteLoading) {
      return;
    }
    if (!router.isLoggedIn()) {
      router.requireLogin(`/pages/product-detail/index?id=${detail.id}`);
      return;
    }
    const nextFavorite = !detail.isFavorite;
    this.setData({ favoriteLoading: true });
    toggleMallProductFavorite(detail.id, nextFavorite)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("fail");
        }
        const favoriteCount = Number(res.data.favoriteCount || 0);
        this.setData({
          detail: Object.assign({}, this.data.detail, {
            isFavorite: !!res.data.isFavorite,
            favoriteCount,
            favoriteText: favoriteCount > 0 ? `${favoriteCount} 人收藏` : "收藏商品"
          })
        });
        wx.showToast({
          title: res.data.isFavorite ? "已收藏" : "已取消收藏",
          icon: "none"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "收藏状态更新失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({ favoriteLoading: false });
      });
  }
});
