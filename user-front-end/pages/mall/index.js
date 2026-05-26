const router = require("../../utils/router");
const { fetchMallCategories, fetchMallProducts } = require("../../api/userMall");

const PRODUCT_TYPE_TABS = [
  {
    id: 1,
    title: "商城好物",
    desc: "精选家电与配件，支持按条件筛选和排序"
  },
  {
    id: 2,
    title: "二手优选",
    desc: "聚焦高性价比二手商品，先比价格再看配置"
  }
];

const SORT_OPTIONS = [
  { value: "default", label: "综合推荐" },
  { value: "priceAsc", label: "价格由低到高" },
  { value: "priceDesc", label: "价格由高到低" },
  { value: "salesDesc", label: "销量优先" },
  { value: "latest", label: "最近上新" }
];

const MARKETING_TAG_OPTIONS = [
  { value: "", label: "全部标签" },
  { value: "recommended", label: "店铺推荐" },
  { value: "hot", label: "热销优先" },
  { value: "new", label: "新品优先" }
];

const PRICE_RANGE_OPTIONS = [
  { value: "", label: "全部价格", min: "", max: "" },
  { value: "0-199", label: "199内", min: "0", max: "199" },
  { value: "200-499", label: "200-499", min: "200", max: "499" },
  { value: "500-999", label: "500-999", min: "500", max: "999" },
  { value: "1000+", label: "1000以上", min: "1000", max: "" }
];

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? "0.00" : amount.toFixed(2);
}

function sanitizePriceInput(value) {
  const text = String(value || "")
    .replace(/[^\d.]/g, "")
    .replace(/^\./, "");
  const segments = text.split(".");
  if (segments.length <= 2) {
    return text;
  }
  return `${segments[0]}.${segments.slice(1).join("")}`;
}

function trimToEmpty(value) {
  return String(value || "").trim();
}

function toPlainNumberText(value) {
  if (value === "" || value === null || value === undefined) {
    return "";
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return "";
  }
  return String(amount);
}

function normalizeProducts(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || "",
    name: item.name || "未命名商品",
    categoryPath: item.categoryPath || item.categoryName || "",
    brand: item.brand || "",
    model: item.model || "",
    mainImageUrl: item.mainImageUrl || "",
    sellingPriceText: formatPrice(item.sellingPrice),
    originalPriceText: formatPrice(item.originalPrice),
    stockQuantity: Number(item.stockQuantity || 0),
    salesCount: Number(item.salesCount || 0),
    isFreeShipping: Number(item.isFreeShipping || 0) === 1,
    isHot: Number(item.isHot || 0) === 1,
    isNew: Number(item.isNew || 0) === 1,
    isRecommended: Number(item.isRecommended || 0) === 1,
    productTypeText: item.productTypeText || ""
  }));
}

Page({
  data: {
    loading: true,
    refreshing: false,
    productTypeTabs: PRODUCT_TYPE_TABS,
    activeProductType: 1,
    activeTypeTitle: PRODUCT_TYPE_TABS[0].title,
    activeTypeDesc: PRODUCT_TYPE_TABS[0].desc,
    sortOptions: SORT_OPTIONS,
    marketingTagOptions: MARKETING_TAG_OPTIONS,
    priceRangeOptions: PRICE_RANGE_OPTIONS,
    inputKeyword: "",
    keyword: "",
    categories: [],
    selectedCategoryId: "",
    selectedCategoryName: "",
    onlyInStock: false,
    onlyFreeShipping: false,
    activeMarketingTag: "",
    activeSort: "default",
    activePriceRange: "",
    minPrice: "",
    maxPrice: "",
    activeFilterItems: [],
    resultSummaryText: "可按分类、价格、包邮、库存和标签组合筛选",
    showFilterDrawer: false,
    draftSelectedCategoryId: "",
    draftSelectedCategoryName: "",
    draftOnlyInStock: false,
    draftOnlyFreeShipping: false,
    draftActiveMarketingTag: "",
    draftActiveSort: "default",
    draftActivePriceRange: "",
    draftInputMinPrice: "",
    draftInputMaxPrice: "",
    products: []
  },

  onLoad() {
    this.syncFilterSummary();
    this.refreshPage();
  },

  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
  },

  onPullDownRefresh() {
    this.refreshPage(true);
  },

  refreshPage(fromPullDown) {
    this.setData({
      loading: true,
      refreshing: !!fromPullDown
    });

    this.loadCategories(this.data.activeProductType)
      .then(() => this.loadProducts(this.buildQueryParams()))
      .catch(() => {
        wx.showToast({
          title: "商城数据加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({
          loading: false,
          refreshing: false
        });
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  loadCategories(productType) {
    return fetchMallCategories({ productType }).then((res) => {
      const categories = res && res.code === 200 && Array.isArray(res.data) ? res.data : [];
      const selectedCategory = categories.find((item) => item.id === this.data.selectedCategoryId) || null;
      const selectedCategoryId = selectedCategory ? selectedCategory.id : "";
      const selectedCategoryName = selectedCategory ? selectedCategory.name || "" : "";
      this.setData({
        categories,
        selectedCategoryId,
        selectedCategoryName
      });
      if (!this.data.showFilterDrawer) {
        this.syncFilterSummary();
      }
    });
  },

  loadProducts(params) {
    return fetchMallProducts(params).then((res) => {
      const products = res && res.code === 200 ? normalizeProducts(res.data) : [];
      this.setData({ products });
    });
  },

  buildQueryParams() {
    return {
      productType: this.data.activeProductType,
      keyword: this.data.keyword || undefined,
      categoryId: this.data.selectedCategoryId || undefined,
      minPrice: this.data.minPrice || undefined,
      maxPrice: this.data.maxPrice || undefined,
      onlyInStock: this.data.onlyInStock || undefined,
      onlyFreeShipping: this.data.onlyFreeShipping || undefined,
      marketingTag: this.data.activeMarketingTag || undefined,
      sortBy: this.data.activeSort || undefined
    };
  },

  syncFilterSummary() {
    const items = [];
    const {
      keyword,
      selectedCategoryName,
      onlyInStock,
      onlyFreeShipping,
      activeMarketingTag,
      activeSort,
      minPrice,
      maxPrice
    } = this.data;

    if (keyword) {
      items.push({ id: "keyword", text: `搜索 ${keyword}` });
    }
    if (selectedCategoryName) {
      items.push({ id: "category", text: selectedCategoryName });
    }
    if (minPrice || maxPrice) {
      let priceText = "价格 ";
      if (minPrice && maxPrice) {
        priceText += `${minPrice}-${maxPrice}`;
      } else if (minPrice) {
        priceText += `${minPrice}+`;
      } else {
        priceText += `0-${maxPrice}`;
      }
      items.push({ id: "price", text: priceText });
    }
    if (onlyInStock) {
      items.push({ id: "stock", text: "仅看有货" });
    }
    if (onlyFreeShipping) {
      items.push({ id: "shipping", text: "只看包邮" });
    }
    if (activeMarketingTag) {
      const tagItem = MARKETING_TAG_OPTIONS.find((item) => item.value === activeMarketingTag);
      if (tagItem) {
        items.push({ id: "tag", text: tagItem.label });
      }
    }
    if (activeSort && activeSort !== "default") {
      const sortItem = SORT_OPTIONS.find((item) => item.value === activeSort);
      if (sortItem) {
        items.push({ id: "sort", text: sortItem.label });
      }
    }

    this.setData({
      activeFilterItems: items,
      resultSummaryText: items.length
        ? `当前已使用 ${items.length} 个筛选条件`
        : "可按分类、价格、包邮、库存和标签组合筛选"
    });
  },

  onSwitchProductType(e) {
    const productType = Number(e.currentTarget.dataset.type || 1);
    if (productType === this.data.activeProductType) {
      return;
    }
    const activeTab = PRODUCT_TYPE_TABS.find((item) => item.id === productType) || PRODUCT_TYPE_TABS[0];
    this.setData({
      activeProductType: productType,
      activeTypeTitle: activeTab.title,
      activeTypeDesc: activeTab.desc,
      selectedCategoryId: "",
      selectedCategoryName: "",
      onlyInStock: false,
      onlyFreeShipping: false,
      activeMarketingTag: "",
      activeSort: "default",
      activePriceRange: "",
      minPrice: "",
      maxPrice: "",
      products: [],
      categories: [],
      showFilterDrawer: false
    });
    this.syncFilterSummary();
    this.refreshPage();
  },

  onSearchInput(e) {
    this.setData({
      inputKeyword: e.detail.value || ""
    });
  },

  onSearchConfirm() {
    this.setData({
      keyword: trimToEmpty(this.data.inputKeyword)
    });
    this.syncFilterSummary();
    this.refreshPage();
  },

  onClearSearch() {
    this.setData({
      inputKeyword: "",
      keyword: ""
    });
    this.syncFilterSummary();
    this.refreshPage();
  },

  openFilterDrawer() {
    this.setData({
      showFilterDrawer: true,
      draftSelectedCategoryId: this.data.selectedCategoryId,
      draftSelectedCategoryName: this.data.selectedCategoryName,
      draftOnlyInStock: this.data.onlyInStock,
      draftOnlyFreeShipping: this.data.onlyFreeShipping,
      draftActiveMarketingTag: this.data.activeMarketingTag,
      draftActiveSort: this.data.activeSort,
      draftActivePriceRange: this.data.activePriceRange,
      draftInputMinPrice: this.data.minPrice,
      draftInputMaxPrice: this.data.maxPrice
    });
  },

  closeFilterDrawer() {
    this.setData({
      showFilterDrawer: false
    });
  },

  onDrawerCategoryTap(e) {
    const categoryId = e.currentTarget.dataset.id || "";
    const categoryName = e.currentTarget.dataset.name || "";
    const nextCategoryId = categoryId === this.data.draftSelectedCategoryId ? "" : categoryId;
    this.setData({
      draftSelectedCategoryId: nextCategoryId,
      draftSelectedCategoryName: nextCategoryId ? categoryName : ""
    });
  },

  onDrawerSwitchTap(e) {
    const key = e.currentTarget.dataset.key;
    if (!key) {
      return;
    }
    this.setData({
      [key]: !this.data[key]
    });
  },

  onDrawerSortSelect(e) {
    const value = e.currentTarget.dataset.value || "default";
    this.setData({
      draftActiveSort: value
    });
  },

  onDrawerMarketingTagSelect(e) {
    const value = e.currentTarget.dataset.value || "";
    this.setData({
      draftActiveMarketingTag: value === this.data.draftActiveMarketingTag ? "" : value
    });
  },

  onDrawerPriceRangeSelect(e) {
    const value = e.currentTarget.dataset.value || "";
    const min = e.currentTarget.dataset.min || "";
    const max = e.currentTarget.dataset.max || "";
    this.setData({
      draftActivePriceRange: value,
      draftInputMinPrice: min,
      draftInputMaxPrice: max
    });
  },

  onDrawerMinPriceInput(e) {
    this.setData({
      draftActivePriceRange: "custom",
      draftInputMinPrice: sanitizePriceInput(e.detail.value)
    });
  },

  onDrawerMaxPriceInput(e) {
    this.setData({
      draftActivePriceRange: "custom",
      draftInputMaxPrice: sanitizePriceInput(e.detail.value)
    });
  },

  normalizeDraftPrice() {
    const minText = trimToEmpty(this.data.draftInputMinPrice);
    const maxText = trimToEmpty(this.data.draftInputMaxPrice);
    if (!minText && !maxText) {
      return {
        activePriceRange: "",
        minPrice: "",
        maxPrice: "",
        inputMinPrice: "",
        inputMaxPrice: ""
      };
    }

    const minValue = minText ? Number(minText) : null;
    const maxValue = maxText ? Number(maxText) : null;
    if ((minText && Number.isNaN(minValue)) || (maxText && Number.isNaN(maxValue))) {
      wx.showToast({
        title: "价格格式不正确",
        icon: "none"
      });
      return null;
    }
    if ((minValue !== null && minValue < 0) || (maxValue !== null && maxValue < 0)) {
      wx.showToast({
        title: "价格不能小于0",
        icon: "none"
      });
      return null;
    }
    if (minValue !== null && maxValue !== null && minValue > maxValue) {
      wx.showToast({
        title: "最低价不能高于最高价",
        icon: "none"
      });
      return null;
    }

    const normalizedMin = toPlainNumberText(minValue);
    const normalizedMax = toPlainNumberText(maxValue);
    return {
      activePriceRange: normalizedMin || normalizedMax ? "custom" : "",
      minPrice: normalizedMin,
      maxPrice: normalizedMax,
      inputMinPrice: normalizedMin,
      inputMaxPrice: normalizedMax
    };
  },

  onDrawerReset() {
    this.setData({
      draftSelectedCategoryId: "",
      draftSelectedCategoryName: "",
      draftOnlyInStock: false,
      draftOnlyFreeShipping: false,
      draftActiveMarketingTag: "",
      draftActiveSort: "default",
      draftActivePriceRange: "",
      draftInputMinPrice: "",
      draftInputMaxPrice: ""
    });
  },

  onApplyDrawerFilters() {
    const normalizedPrice = this.normalizeDraftPrice();
    if (!normalizedPrice) {
      return;
    }
    this.setData({
      showFilterDrawer: false,
      selectedCategoryId: this.data.draftSelectedCategoryId,
      selectedCategoryName: this.data.draftSelectedCategoryName,
      onlyInStock: this.data.draftOnlyInStock,
      onlyFreeShipping: this.data.draftOnlyFreeShipping,
      activeMarketingTag: this.data.draftActiveMarketingTag,
      activeSort: this.data.draftActiveSort,
      activePriceRange: normalizedPrice.activePriceRange,
      minPrice: normalizedPrice.minPrice,
      maxPrice: normalizedPrice.maxPrice,
      draftInputMinPrice: normalizedPrice.inputMinPrice,
      draftInputMaxPrice: normalizedPrice.inputMaxPrice
    });
    this.syncFilterSummary();
    this.refreshPage();
  },

  onResetFilters() {
    this.setData({
      inputKeyword: "",
      keyword: "",
      selectedCategoryId: "",
      selectedCategoryName: "",
      onlyInStock: false,
      onlyFreeShipping: false,
      activeMarketingTag: "",
      activeSort: "default",
      activePriceRange: "",
      minPrice: "",
      maxPrice: "",
      showFilterDrawer: false
    });
    this.syncFilterSummary();
    this.refreshPage();
  },

  onProductTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-detail/index?id=${id}`
    });
  }
});
