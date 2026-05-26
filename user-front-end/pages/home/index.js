const router = require("../../utils/router");
const { fetchHomePublicData, fetchHomePrivateData } = require("../../api/userHome");
const { fetchMallProducts } = require("../../api/userMall");

const TEXTS = {
  noticeTag: "公告",
  emptyBannerTitle: "当前无公告",
  waiting: "待接单",
  processing: "进行中",
  toPay: "待支付",
  finished: "已完成",
  myOrders: "我的订单",
  moreService: "更多服务",
  quickPlaceOrder: "立即报修",
  quickPlaceOrderSub: "一键提交故障，快速派单上门",
  viewAll: "查看全部",
  noLatestOrder: "暂无订单进度，去下单看看",
  loginTip: "登录后可查看订单流程和实时状态",
  goLogin: "去登录",
  followedWorkersTitle: "关注师傅动态",
  noFollowedWorkers: "暂无关注师傅，去服务分类看看",
  hotCategoriesTitle: "热门维修分类",
  noHotCategories: "暂无热门分类",
  orderNoPrefix: "订单 ",
  scorePrefix: "评分 ",
  defaultCategoryName: "维修分类",
  defaultCategoryDesc: "专业维修服务",
  defaultLatestAppliance: "维修订单",
  defaultWorkerName: "维修师傅",
  defaultSkill: "电器维修",
  recommendedProductsTitle: "推荐商品",
  recommendedProductsSub: "从商城里挑了几件更适合家庭常用的好物",
  recommendedProductsEmpty: "当前暂无推荐商品，稍后再来看看",
  recommendedProductsGuest: "登录后可查看推荐商品与商城活动",
  goMall: "逛商城",
  goLoginMall: "登录后查看",
  offline: "离线",
  supportPending: "客服中心（待接入）",
  bannerFallback: "当前无公告"
};

const DEFAULT_STEPS = [
  { text: "待接单" },
  { text: "待上门" },
  { text: "待检查" },
  { text: "待支付" },
  { text: "服务中" },
  { text: "已完成" }
];

const EMPTY_BANNER = {
  id: "EMPTY_BANNER",
  tag: TEXTS.noticeTag,
  title: TEXTS.emptyBannerTitle,
  subtitle: "",
  imageUrl: "",
  emoji: "",
  contentType: 2
};

const EMPTY_PRIVATE = {
  orderSummary: [
    { key: "waiting", label: TEXTS.waiting, count: 0 },
    { key: "processing", label: TEXTS.processing, count: 0 },
    { key: "to-pay", label: TEXTS.toPay, count: 0 },
    { key: "finished", label: TEXTS.finished, count: 0 }
  ],
  latestOrder: null,
  followedWorkers: []
};

const EMPTY_RECOMMEND = {
  recommendedFeatured: null,
  recommendedSecondary: []
};

const SHARE_TITLE = "安修到家｜家电维修、安装、清洗，一站到家";
const SHARE_PATH = "/pages/home/index";
const SHARE_IMAGE = "/assets/logo-full.png";

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? "0.00" : amount.toFixed(2);
}

Page({
  data: {
    texts: TEXTS,
    isLogin: false,
    refreshing: false,
    hasNotice: false,
    noticeText: "",
    banners: [EMPTY_BANNER],
    notices: [],
    quickActions: [
      { key: "order-list", text: "我的订单", icon: "orders-o" },
      { key: "follow-workers", text: "关注师傅", icon: "friends-o" },
      { key: "service-categories", text: "服务分类", icon: "apps-o" },
      { key: "support", text: "客服中心", icon: "service-o" }
    ],
    orderSummary: EMPTY_PRIVATE.orderSummary,
    latestOrder: EMPTY_PRIVATE.latestOrder,
    followedWorkers: EMPTY_PRIVATE.followedWorkers,
    hotCategories: [],
    recommendedFeatured: EMPTY_RECOMMEND.recommendedFeatured,
    recommendedSecondary: EMPTY_RECOMMEND.recommendedSecondary
  },

  onLoad() {
    this.refreshPromise = null;
    this.pendingRefresh = false;
    this.pendingPullDown = false;
    this.syncLoginState();
    this.refreshAllData();
  },

  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
    this.syncLoginState();
    this.refreshAllData();
  },

  onPullDownRefresh() {
    this.refreshAllData(true);
  },

  onRefresherRefresh() {
    this.refreshAllData(true);
  },

  onShareAppMessage() {
    return {
      title: SHARE_TITLE,
      path: SHARE_PATH,
      imageUrl: SHARE_IMAGE
    };
  },

  onShareTimeline() {
    return {
      title: SHARE_TITLE,
      query: "from=timeline",
      imageUrl: SHARE_IMAGE
    };
  },

  syncLoginState() {
    const app = getApp();
    const isLogin = !!((app && app.globalData && app.globalData.isLogin) || wx.getStorageSync("userToken"));
    this.setData({ isLogin });
    return isLogin;
  },

  refreshAllData(fromPullDown) {
    if (this.refreshPromise) {
      this.pendingRefresh = true;
      this.pendingPullDown = this.pendingPullDown || !!fromPullDown;
      return this.refreshPromise;
    }

    const isLogin = this.data.isLogin;
    this.setData({ refreshing: true });

    const tasks = [this.loadPublicHomeData()];
    if (isLogin) {
      tasks.push(this.loadPrivateHomeData());
      tasks.push(this.loadRecommendedProducts());
    } else {
      this.resetPrivateData();
      this.resetRecommendedProducts();
    }

    this.refreshPromise = Promise.allSettled(tasks).finally(() => {
      const needRefreshAgain = this.pendingRefresh;
      const nextFromPullDown = this.pendingPullDown;
      this.refreshPromise = null;
      this.pendingRefresh = false;
      this.pendingPullDown = false;
      this.setData({ refreshing: false });
      if (needRefreshAgain) {
        return this.refreshAllData(nextFromPullDown);
      }
      if (fromPullDown || nextFromPullDown) {
        wx.stopPullDownRefresh();
      }
      return null;
    });

    return this.refreshPromise;
  },

  refreshNoticeText(notices) {
    const texts = (notices || [])
      .map((item) => (typeof item === "string" ? item : item && item.text))
      .filter((item) => !!item);
    this.setData({
      hasNotice: texts.length > 0,
      noticeText: texts.join("  |  ")
    });
  },

  getDefaultBannerList() {
    return [{ ...EMPTY_BANNER }];
  },

  normalizePublicData(data) {
    const banners = Array.isArray(data && data.banners)
      ? data.banners
          .map((item) => ({
            id: item.id,
            tag: item.tag || TEXTS.noticeTag,
            title: item.title || "",
            subtitle: item.subtitle || "",
            imageUrl: item.imageUrl || "",
            emoji: item.emoji || "",
            contentType: Number(item.contentType || 0)
          }))
          .filter((item) => !(item.contentType === 1 && !item.imageUrl))
      : [];

    const notices = Array.isArray(data && data.notices)
      ? data.notices.map((item) => ({
          id: item.id,
          text: item.text || ""
        }))
      : [];

    const hotCategories = Array.isArray(data && data.hotCategories)
      ? data.hotCategories.map((item) => ({
          id: item.id,
          name: item.name || TEXTS.defaultCategoryName,
          desc: item.desc || TEXTS.defaultCategoryDesc,
          iconUrl: item.iconUrl || ""
        }))
      : [];

    return {
      banners: banners.length ? banners : this.getDefaultBannerList(),
      notices,
      hotCategories
    };
  },

  loadPublicHomeData() {
    return fetchHomePublicData()
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          const emptyData = this.normalizePublicData(null);
          this.setData(emptyData);
          this.refreshNoticeText(emptyData.notices);
          return;
        }
        const parsed = this.normalizePublicData(res.data);
        this.setData(parsed);
        this.refreshNoticeText(parsed.notices);
      })
      .catch(() => {
        const emptyData = this.normalizePublicData(null);
        this.setData(emptyData);
        this.refreshNoticeText(emptyData.notices);
      });
  },

  loadPrivateHomeData() {
    return fetchHomePrivateData()
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          return;
        }
        const data = res.data;

        const orderSummary = Array.isArray(data.orderSummary) && data.orderSummary.length
          ? data.orderSummary.map((item) => ({
              key: item.key,
              label: item.label || "",
              count: Number(item.count || 0)
            }))
          : EMPTY_PRIVATE.orderSummary;

        const latestOrder = data.latestOrder && data.latestOrder.orderNo
          ? {
              orderId: data.latestOrder.orderId || "",
              orderNo: data.latestOrder.orderNo,
              appliance: data.latestOrder.appliance || TEXTS.defaultLatestAppliance,
              statusText: data.latestOrder.statusText || "",
              stepActive: Number(data.latestOrder.stepActive || 0),
              steps: Array.isArray(data.latestOrder.steps) && data.latestOrder.steps.length
                ? data.latestOrder.steps
                : DEFAULT_STEPS
            }
          : null;

        const followedWorkers = Array.isArray(data.followedWorkers) && data.followedWorkers.length
          ? data.followedWorkers.map((item) => ({
              id: item.id,
              name: item.name || TEXTS.defaultWorkerName,
              initial: item.initial || (item.name ? item.name.substring(0, 1) : "师"),
              skill: item.skill || TEXTS.defaultSkill,
              score: item.score || "--",
              statusText: item.statusText || TEXTS.offline,
              statusType: item.statusType || "primary",
              avatarUrl: item.avatarUrl || ""
            }))
          : [];

        this.setData({
          orderSummary,
          latestOrder,
          followedWorkers
        });
      })
      .catch(() => {
        this.resetPrivateData();
      });
  },

  resetPrivateData() {
    this.setData({
      orderSummary: EMPTY_PRIVATE.orderSummary,
      latestOrder: EMPTY_PRIVATE.latestOrder,
      followedWorkers: EMPTY_PRIVATE.followedWorkers
    });
  },

  normalizeRecommendedProducts(list) {
    return (Array.isArray(list) ? list : [])
      .map((item) => ({
        id: item.id || "",
        name: item.name || "未命名商品",
        categoryPath: item.categoryPath || item.categoryName || "",
        brand: item.brand || "",
        model: item.model || "",
        mainImageUrl: item.mainImageUrl || "",
        sellingPriceText: formatPrice(item.sellingPrice),
        originalPriceText: formatPrice(item.originalPrice),
        salesCount: Number(item.salesCount || 0),
        isFreeShipping: Number(item.isFreeShipping || 0) === 1,
        isHot: Number(item.isHot || 0) === 1,
        isNew: Number(item.isNew || 0) === 1,
        isRecommended: Number(item.isRecommended || 0) === 1
      }))
      .filter((item) => !!item.id)
      .slice(0, 3);
  },

  loadRecommendedProducts() {
    return fetchMallProducts({
      productType: 1,
      marketingTag: "recommended",
      sortBy: "default"
    })
      .then((res) => {
        if (!res || res.code !== 200) {
          this.resetRecommendedProducts();
          return;
        }
        const products = this.normalizeRecommendedProducts(res.data);
        this.setData({
          recommendedFeatured: products[0] || null,
          recommendedSecondary: products.slice(1)
        });
      })
      .catch(() => {
        this.resetRecommendedProducts();
      });
  },

  resetRecommendedProducts() {
    this.setData({
      recommendedFeatured: EMPTY_RECOMMEND.recommendedFeatured,
      recommendedSecondary: EMPTY_RECOMMEND.recommendedSecondary
    });
  },

  navigateToOrderList(tab) {
    if (!this.data.isLogin) {
      this.onLoginTap();
      return;
    }
    const url = tab && tab !== "all"
      ? `/pages/order-list/index?tab=${tab}`
      : "/pages/order-list/index";
    router.navigateTo({ url });
  },

  onPlaceOrderTap() {
    router.navigateTo({
      url: "/pages/order-flow/step1/index"
    });
  },

  onBannerTap(e) {
    const index = Number(e.currentTarget.dataset.index || 0);
    const banner = this.data.banners[index];
    wx.showToast({
      title: (banner && (banner.title || banner.tag)) || TEXTS.bannerFallback,
      icon: "none"
    });
  },

  onQuickActionTap(e) {
    const key = e.currentTarget.dataset.key;
    if (key === "order-list") {
      this.navigateToOrderList("all");
      return;
    }
    if (key === "follow-workers") {
      router.navigateTo({
        url: "/pages/follow-workers/index"
      });
      return;
    }
    if (key === "service-categories") {
      router.navigateTo({
        url: "/pages/category/index"
      });
      return;
    }
    wx.showToast({
      title: TEXTS.supportPending,
      icon: "none"
    });
  },

  onOrderSummaryTap(e) {
    const key = e.currentTarget.dataset.key || "all";
    this.navigateToOrderList(key);
  },

  onLatestOrderTap() {
    if (!this.data.isLogin) {
      this.onLoginTap();
      return;
    }
    const latestOrder = this.data.latestOrder;
    if (latestOrder && latestOrder.orderId) {
      router.navigateTo({
        url: `/pages/order-detail/index?orderId=${latestOrder.orderId}`
      });
      return;
    }
    this.navigateToOrderList("all");
  },

  onFollowedWorkerTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/technician-detail/index?technicianId=${id}`
    });
  },

  onCategoryTap(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name || "";
    const query = [];
    if (id) {
      query.push(`focusCategoryId=${encodeURIComponent(id)}`);
    }
    if (name) {
      query.push(`focusCategoryName=${encodeURIComponent(name)}`);
    }
    router.navigateTo({
      url: `/pages/category-detail/index${query.length ? `?${query.join("&")}` : ""}`
    });
  },

  onLoginTap() {
    wx.navigateTo({
      url: "/pages/login/index"
    });
  },

  onGoMallTap() {
    router.switchTab({
      url: "/pages/mall/index"
    });
  },

  onRecommendedProductTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-detail/index?id=${id}`
    });
  }
});
