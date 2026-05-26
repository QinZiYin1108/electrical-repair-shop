const draftStore = require("../../utils/orderDraftStore");
const {
  fetchCategoryDetail,
  fetchServiceTypes
} = require("../../api/userOrderFlow");

const DEFAULT_SERVICE_MODES = [
  { id: 1, name: "上门维修" },
  { id: 2, name: "上门安装" },
  { id: 3, name: "线下维修" }
];

function decodeText(value) {
  if (!value) {
    return "";
  }
  try {
    return decodeURIComponent(value);
  } catch (error) {
    return value;
  }
}

function getTypeTagText(mode) {
  if (Number(mode) === 2) {
    return "安装";
  }
  if (Number(mode) === 3) {
    return "到店";
  }
  return "维修";
}

Page({
  data: {
    loading: true,
    categoryId: "",
    categoryName: "",
    categoryDetail: null,
    serviceModes: DEFAULT_SERVICE_MODES,
    selectedServiceMode: 1,
    serviceTypeOptions: [],
    selectedServiceTypeId: ""
  },

  onLoad(options) {
    const categoryId = (options && options.focusCategoryId) || "";
    const categoryName = decodeText(options && options.focusCategoryName);
    if (!categoryId) {
      wx.showToast({
        title: "分类信息缺失",
        icon: "none"
      });
      setTimeout(() => {
        wx.navigateBack({
          delta: 1
        });
      }, 500);
      return;
    }
    this.setData({
      categoryId,
      categoryName
    });
    this.loadPageData();
  },

  loadPageData() {
    this.setData({ loading: true });

    fetchCategoryDetail(this.data.categoryId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("empty");
        }
        const detail = res.data;
        this.setData({
          categoryDetail: detail,
          categoryName: detail.name || this.data.categoryName || ""
        });
        return this.loadServiceTypes(this.data.selectedServiceMode);
      })
      .catch(() => {
        wx.showToast({
          title: "分类详情加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  loadServiceTypes(serviceMode) {
    return fetchServiceTypes({
      serviceMode,
      categoryId: this.data.categoryId
    })
      .then((res) => {
        const list = res && res.code === 200 && Array.isArray(res.data)
          ? res.data.map((item) => Object.assign({}, item, {
              typeTagText: getTypeTagText(serviceMode)
            }))
          : [];
        const selectedServiceTypeId = list.length ? list[0].id : "";
        this.setData({
          selectedServiceMode: Number(serviceMode || 1),
          serviceTypeOptions: list,
          selectedServiceTypeId
        });
      })
      .catch(() => {
        this.setData({
          selectedServiceMode: Number(serviceMode || 1),
          serviceTypeOptions: [],
          selectedServiceTypeId: ""
        });
        wx.showToast({
          title: "服务类型加载失败",
          icon: "none"
        });
      });
  },

  onSelectServiceMode(e) {
    const serviceMode = Number(e.currentTarget.dataset.mode || 1);
    if (serviceMode === this.data.selectedServiceMode) {
      return;
    }
    this.loadServiceTypes(serviceMode);
  },

  onSelectServiceType(e) {
    const serviceTypeId = e.currentTarget.dataset.id || "";
    if (!serviceTypeId) {
      return;
    }
    this.setData({
      selectedServiceTypeId: serviceTypeId
    });
  },

  onPlaceOrder() {
    const categoryDetail = this.data.categoryDetail;
    const selectedServiceType = (this.data.serviceTypeOptions || []).find(
      (item) => item.id === this.data.selectedServiceTypeId
    );

    if (!categoryDetail || !selectedServiceType) {
      wx.showToast({
        title: "请先选择服务类型",
        icon: "none"
      });
      return;
    }

    draftStore.saveDraft({
      serviceMode: this.data.selectedServiceMode,
      level1Keyword: "",
      selectedLevel1Id: categoryDetail.level1Id || "",
      selectedLevel2Id: categoryDetail.level2Id || "",
      selectedCategoryId: categoryDetail.id || this.data.categoryId,
      selectedCategoryPath: categoryDetail.pathText || this.data.categoryName || "",
      selectedServiceTypeId: selectedServiceType.id,
      selectedServiceTypeName: selectedServiceType.name || "",
      selectedAddressId: "",
      selectedAddressText: "",
      selectedTechnicianId: "",
      selectedTechnicianName: "",
      selectedFaultIds: [],
      faultNameMap: {},
      faultDetailMap: {},
      applianceBrand: "",
      applianceModel: "",
      purchaseDate: "",
      selectedAppointmentId: "",
      selectedAppointmentLabel: "",
      selectedAppointmentTime: null
    });

    const app = getApp();
    const isLogin = app.globalData.isLogin || !!wx.getStorageSync("userToken");
    if (!isLogin) {
      wx.setStorageSync("redirectUrl", "/pages/order-flow/step3/index");
      wx.navigateTo({
        url: "/pages/login/index"
      });
      return;
    }

    wx.navigateTo({
      url: "/pages/order-flow/step3/index"
    });
  }
});
