const router = require("../../utils/router");
const {
  fetchMallFavoriteProducts,
  toggleMallProductFavorite
} = require("../../api/userMall");

const TEXTS = {
  heroTitle: "我的收藏",
  heroSubtitle: "把想回看的商品先收进这里，需要时直接回到详情或继续下单。",
  itemUnit: "件",
  tipPullDown: "支持下拉刷新",
  tipCancel: "可直接取消收藏",
  tipSort: "自动按收藏时间排序",
  loading: "收藏商品加载中...",
  loadFailedTitle: "加载失败",
  retry: "重新加载",
  favoriteAtPrefix: "收藏于",
  emptyImage: "暂无图片",
  recommend: "推荐",
  hot: "热销",
  new: "新品",
  viewDetail: "查看详情",
  removing: "处理中...",
  cancelFavorite: "取消收藏",
  emptyTitle: "还没有收藏商品",
  emptyDesc: "去商城逛逛，看到想回看的商品时点一下收藏，这里会自动帮你整理好。",
  goMall: "去商城看看",
  justFavorited: "刚刚收藏",
  unnamedProduct: "未命名商品",
  outOfStock: "暂时缺货",
  stockPrefix: "库存",
  productFallback: "商品",
  brandFallback: "品牌待补充",
  modelFallback: "型号待补充",
  freeShipping: "包邮",
  needShipping: "需运费",
  soldPrefix: "已售",
  loadFailed: "收藏商品加载失败",
  cancelFailed: "取消收藏失败",
  cancelSuccess: "已取消收藏"
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
    return TEXTS.justFavorited;
  }
  const date = new Date(time);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function mapFavoriteItem(item) {
  const sellingPriceText = formatPrice(item.sellingPrice);
  const originalPriceText = formatPrice(item.originalPrice);
  const stockQuantity = Number(item.stockQuantity || 0);
  return {
    id: item.id || "",
    name: item.name || TEXTS.unnamedProduct,
    categoryPath: item.categoryPath || item.categoryName || "",
    brandText: item.brand || TEXTS.brandFallback,
    modelText: item.model || TEXTS.modelFallback,
    mainImageUrl: item.mainImageUrl || "",
    sellingPriceText,
    originalPriceText,
    sellingPriceLabel: `￥${sellingPriceText}`,
    originalPriceLabel: `￥${originalPriceText}`,
    showOriginalPrice: Number(originalPriceText) > 0 && originalPriceText !== sellingPriceText,
    stockQuantity,
    stockText: stockQuantity > 0 ? `${TEXTS.stockPrefix} ${stockQuantity}` : TEXTS.outOfStock,
    salesCount: Number(item.salesCount || 0),
    salesText: `${TEXTS.soldPrefix} ${Number(item.salesCount || 0)}`,
    isFreeShipping: Number(item.isFreeShipping || 0) === 1,
    isHot: Number(item.isHot || 0) === 1,
    isNew: Number(item.isNew || 0) === 1,
    isRecommended: Number(item.isRecommended || 0) === 1,
    productTypeText: item.productTypeText || TEXTS.productFallback,
    shippingText: Number(item.isFreeShipping || 0) === 1 ? TEXTS.freeShipping : TEXTS.needShipping,
    favoriteTimeLabel: `${TEXTS.favoriteAtPrefix} ${formatDateTime(item.favoriteTime)}`
  };
}

Page({
  data: {
    texts: TEXTS,
    loading: true,
    loadError: "",
    removingId: "",
    favorites: []
  },

  onLoad() {
    this.loadList();
  },

  onShow() {
    if (!this.data.loading) {
      this.loadList();
    }
  },

  onPullDownRefresh() {
    this.loadList(true);
  },

  loadList(fromPullDown) {
    this.setData({
      loading: true,
      loadError: ""
    });

    fetchMallFavoriteProducts()
      .then((res) => {
        if (!res || res.code !== 200 || !Array.isArray(res.data)) {
          throw new Error((res && res.message) || TEXTS.loadFailed);
        }
        this.setData({
          loading: false,
          loadError: "",
          favorites: res.data.map(mapFavoriteItem)
        });
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || TEXTS.loadFailed,
          favorites: []
        });
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onRetryTap() {
    this.loadList();
  },

  onGoMallTap() {
    wx.switchTab({
      url: "/pages/mall/index"
    });
  },

  onProductTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-detail/index?id=${id}`
    });
  },

  onCancelFavorite(e) {
    const id = e.currentTarget.dataset.id;
    if (!id || this.data.removingId) {
      return;
    }

    this.setData({ removingId: id });
    toggleMallProductFavorite(id, false)
      .then((res) => {
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || TEXTS.cancelFailed);
        }
        this.setData({
          favorites: this.data.favorites.filter((item) => item.id !== id)
        });
        wx.showToast({
          title: TEXTS.cancelSuccess,
          icon: "none"
        });
      })
      .catch((err) => {
        wx.showToast({
          title: (err && err.message) || TEXTS.cancelFailed,
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({ removingId: "" });
      });
  }
});
