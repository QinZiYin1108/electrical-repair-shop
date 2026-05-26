const router = require("../../utils/router");
const { fetchHomePrivateData } = require("../../api/userHome");
const { getUserFundsSummary } = require("../../api/userFunds");
const userProfileApi = require("../../api/userProfile");

const SHARE_TITLE = "安修到家｜让上门维修更安心";
const SHARE_PATH = "/pages/home/index";
const SHARE_IMAGE = "/assets/logo-full.png";

const TEXTS = {
  userFallback: "用户",
  login: "登录",
  unsetName: "未设置昵称",
  notLoggedIn: "未登录",
  userSub: "点击编辑完善资料",
  guestSub: "登录后可查看订单、钱包和个人信息",
  wechatLogin: "立即登录",
  editProfile: "编辑资料",
  manageAddress: "地址管理",
  orderCenter: "订单中心",
  walletCenter: "钱包中心",
  latestOrderTitle: "最近订单",
  latestOrderEmpty: "还没有报修订单，去首页提交报修吧",
  quickToolsTitle: "常用功能",
  accountTitle: "账户管理",
  serviceTitle: "服务保障",
  fundsBalance: "钱包余额",
  frozenBalance: "冻结金额",
  totalIncome: "累计入账",
  totalExpense: "累计支出",
  guestCardTitle: "登录后，报修进度和账户信息都能一目了然",
  guestCardSub: "支持查看订单、地址、钱包和个人资料",
  goRepair: "去报修",
  safetyTip: "账户资料完整度",
  memberTag: "安心用户",
  guestTag: "游客模式",
  profile: "个人资料",
  settings: "设置",
  settingsDesc: "通知、隐私与账号安全",
  profileDesc: "完善昵称、联系方式和紧急联系人",
  moreAction: "查看更多",
  orders: "报修订单",
  ordersDesc: "查看维修记录与服务进度",
  mallOrders: "商城订单",
  mallOrdersDesc: "查看商品订单与物流状态",
  address: "服务地址",
  addressDesc: "管理上门地址和默认地址",
  wallet: "我的钱包",
  walletDesc: "查看余额、流水和充值记录",
  coupons: "优惠券",
  couponsDesc: "查看可用优惠券和使用状态",
  warrantyCards: "保修卡",
  warrantyCardsDesc: "查看商品保修期限和保障信息",
  favorites: "我的收藏",
  favoritesDesc: "查看收藏商品，回到详情或继续下单"
};

function createDefaultUser() {
  return {
    avatarUrl: "",
    username: "",
    phone: ""
  };
}

function createDefaultWalletSummary() {
  return {
    balance: "0.00",
    frozenBalance: "0.00",
    totalIncome: "0.00",
    totalExpense: "0.00"
  };
}

function buildDefaultOrderStats() {
  return [
    { key: "waiting", label: "待接单", count: 0, theme: "amber" },
    { key: "processing", label: "进行中", count: 0, theme: "blue" },
    { key: "to-pay", label: "待支付", count: 0, theme: "orange" },
    { key: "finished", label: "已完成", count: 0, theme: "green" }
  ];
}

function buildQuickEntries() {
  return [
    {
      key: "orders",
      icon: "orders-o",
      title: TEXTS.orders,
      subtitle: TEXTS.ordersDesc,
      theme: "blue"
    },
    {
      key: "mallOrders",
      icon: "gift-o",
      title: TEXTS.mallOrders,
      subtitle: TEXTS.mallOrdersDesc,
      theme: "green"
    },
    {
      key: "address",
      icon: "location-o",
      title: TEXTS.address,
      subtitle: TEXTS.addressDesc,
      theme: "orange"
    },
    {
      key: "funds",
      icon: "balance-o",
      title: TEXTS.wallet,
      subtitle: TEXTS.walletDesc,
      theme: "blue"
    },
    {
      key: "profile",
      icon: "contact-o",
      title: TEXTS.profile,
      subtitle: TEXTS.profileDesc,
      theme: "purple"
    },
    {
      key: "coupons",
      icon: "gift-o",
      title: TEXTS.coupons,
      subtitle: TEXTS.couponsDesc,
      theme: "orange"
    },
    {
      key: "warrantyCards",
      icon: "description",
      title: TEXTS.warrantyCards,
      subtitle: TEXTS.warrantyCardsDesc,
      theme: "green"
    },
    {
      key: "favorites",
      icon: "goods-collect-o",
      title: TEXTS.favorites,
      subtitle: TEXTS.favoritesDesc,
      theme: "purple"
    }
  ];
}

function buildServicePromises() {
  return [
    {
      title: "极速派单",
      subtitle: "附近师傅快速响应",
      theme: "blue"
    },
    {
      title: "价格透明",
      subtitle: "费用明细清晰可查",
      theme: "orange"
    },
    {
      title: "售后保障",
      subtitle: "服务完成后可追踪",
      theme: "green"
    }
  ];
}

function safeToNumber(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num : 0;
}

function formatMoney(value) {
  return safeToNumber(value).toFixed(2);
}

function calculateProfileCompletion(user) {
  const info = user || {};
  const total = 4;
  let completed = 0;
  if (info.avatarUrl) completed += 1;
  if (info.username) completed += 1;
  if (info.phone) completed += 1;
  if (info.email) completed += 1;
  return Math.round((completed / total) * 100);
}

function mapOrderSummary(list) {
  const countMap = {};
  (Array.isArray(list) ? list : []).forEach((item) => {
    countMap[item.key] = safeToNumber(item.count);
  });
  return buildDefaultOrderStats().map((item) => Object.assign({}, item, {
    count: countMap[item.key] || 0
  }));
}

function resolveLatestOrderTone(statusText) {
  const text = String(statusText || "");
  if (text.indexOf("完成") !== -1) return "finished";
  if (text.indexOf("支付") !== -1) return "warning";
  if (text.indexOf("进行") !== -1 || text.indexOf("服务") !== -1 || text.indexOf("上门") !== -1) {
    return "processing";
  }
  return "pending";
}

function mapLatestOrder(data) {
  if (!data || !data.orderNo) {
    return null;
  }
  return {
    orderId: data.orderId || "",
    orderNo: data.orderNo || "",
    appliance: data.appliance || "维修订单",
    statusText: data.statusText || "订单进行中",
    tone: resolveLatestOrderTone(data.statusText)
  };
}

Page({
  data: {
    texts: TEXTS,
    isLogin: false,
    user: createDefaultUser(),
    profileCompletion: 0,
    orderStats: buildDefaultOrderStats(),
    walletSummary: createDefaultWalletSummary(),
    latestOrder: null,
    quickEntries: buildQuickEntries(),
    servicePromises: buildServicePromises()
  },

  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 4 });
    }
    const app = getApp();
    const isLogin = app.globalData.isLogin || !!wx.getStorageSync("userToken");
    this.setData({ isLogin });

    if (!isLogin) {
      this.resetUserInfo();
      this.resetOverviewData();
      return;
    }

    Promise.allSettled([
      this.loadUserInfo(),
      this.loadOverviewData()
    ]);
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
      query: "from=mine",
      imageUrl: SHARE_IMAGE
    };
  },

  resetUserInfo() {
    this.setData({
      user: createDefaultUser(),
      profileCompletion: 0
    });
  },

  resetOverviewData() {
    this.setData({
      orderStats: buildDefaultOrderStats(),
      walletSummary: createDefaultWalletSummary(),
      latestOrder: null
    });
  },

  loadUserInfo() {
    return userProfileApi
      .getUserProfileDetail()
      .then((resp) => {
        if (resp && resp.code === 200 && resp.data) {
          const user = {
            avatarUrl: resp.data.avatarUrl || "",
            username: resp.data.username || "",
            phone: resp.data.phone || "",
            email: resp.data.email || ""
          };
          this.setData({
            user,
            profileCompletion: calculateProfileCompletion(user)
          });
          return;
        }
        this.resetUserInfo();
      })
      .catch(() => {
        this.resetUserInfo();
      });
  },

  loadOverviewData() {
    return Promise.allSettled([
      fetchHomePrivateData(),
      getUserFundsSummary()
    ]).then((results) => {
      const homeResult = results[0];
      const fundsResult = results[1];

      if (
        homeResult &&
        homeResult.status === "fulfilled" &&
        homeResult.value &&
        homeResult.value.code === 200 &&
        homeResult.value.data
      ) {
        const data = homeResult.value.data;
        this.setData({
          orderStats: mapOrderSummary(data.orderSummary),
          latestOrder: mapLatestOrder(data.latestOrder)
        });
      } else {
        this.setData({
          orderStats: buildDefaultOrderStats(),
          latestOrder: null
        });
      }

      if (
        fundsResult &&
        fundsResult.status === "fulfilled" &&
        fundsResult.value &&
        fundsResult.value.code === 200 &&
        fundsResult.value.data
      ) {
        const summary = fundsResult.value.data;
        this.setData({
          walletSummary: {
            balance: formatMoney(summary.balance),
            frozenBalance: formatMoney(summary.frozenBalance),
            totalIncome: formatMoney(summary.totalIncome),
            totalExpense: formatMoney(summary.totalExpense)
          }
        });
      } else {
        this.setData({
          walletSummary: createDefaultWalletSummary()
        });
      }
    });
  },

  ensureLogin() {
    if (this.data.isLogin) {
      return true;
    }
    this.onLoginTap();
    return false;
  },

  onLoginTap() {
    router.navigateTo({
      url: "/pages/login/index"
    });
  },

  onGoRepairTap() {
    router.navigateTo({
      url: "/pages/order-flow/step1/index"
    });
  },

  onHeaderTap() {
    if (!this.ensureLogin()) {
      return;
    }
    this.onNavigateProfile();
  },

  onNavigateProfile() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/profile/index"
    });
  },

  onNavigateOrders(tab) {
    if (!this.ensureLogin()) {
      return;
    }
    const nextTab = typeof tab === "string" ? tab : "all";
    router.navigateTo({
      url: nextTab === "all"
        ? "/pages/order-list/index"
        : `/pages/order-list/index?tab=${nextTab}`
    });
  },

  onNavigateProductOrders(tab) {
    if (!this.ensureLogin()) {
      return;
    }
    const nextTab = typeof tab === "string" ? tab : "all";
    router.navigateTo({
      url: nextTab === "all"
        ? "/pages/product-order-list/index"
        : `/pages/product-order-list/index?tab=${nextTab}`
    });
  },

  onNavigateAddress() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/address-list/index"
    });
  },

  onNavigateSettings() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/settings/index"
    });
  },

  onNavigateFunds() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/funds/index"
    });
  },

  onNavigateCoupons() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/coupon-list/index"
    });
  },

  onNavigateWarrantyCards() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/warranty-card-list/index"
    });
  },

  onNavigateFavorites() {
    if (!this.ensureLogin()) {
      return;
    }
    router.navigateTo({
      url: "/pages/favorite-products/index"
    });
  },

  onLatestOrderTap() {
    if (!this.ensureLogin()) {
      return;
    }
    const latestOrder = this.data.latestOrder;
    if (latestOrder && latestOrder.orderId) {
      router.navigateTo({
        url: `/pages/order-detail/index?orderId=${latestOrder.orderId}`
      });
      return;
    }
    this.onNavigateOrders("all");
  },

  onOrderStatTap(e) {
    const tab = e.currentTarget.dataset.tab || "all";
    this.onNavigateOrders(tab);
  },

  onQuickEntryTap(e) {
    const key = e.currentTarget.dataset.key;
    if (key === "orders") {
      this.onNavigateOrders("all");
      return;
    }
    if (key === "mallOrders") {
      this.onNavigateProductOrders("all");
      return;
    }
    if (key === "address") {
      this.onNavigateAddress();
      return;
    }
    if (key === "funds") {
      this.onNavigateFunds();
      return;
    }
    if (key === "profile") {
      this.onNavigateProfile();
      return;
    }
    if (key === "coupons") {
      this.onNavigateCoupons();
      return;
    }
    if (key === "warrantyCards") {
      this.onNavigateWarrantyCards();
      return;
    }
    if (key === "favorites") {
      this.onNavigateFavorites();
    }
  }
});
