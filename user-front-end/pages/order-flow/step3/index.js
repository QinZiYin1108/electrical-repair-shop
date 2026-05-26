const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { fetchSelectionContext } = require("../../../api/userOrderFlow");

const LABELS = {
  step: "步骤 3 / 6",
  title: "选择师傅",
  currentService: "当前服务项",
  addressTitle: "服务地址（上门服务必选）",
  defaultTag: "默认",
  technicianTitle: "可选师傅",
  allTechnicians: "全部师傅",
  onsiteTip: "已按评分、接单数、距离综合排序",
  offlineTip: "线下维修按距离从近到远排序",
  recommendTag: "推荐",
  rating: "评分",
  orderCount: "接单",
  maxDistance: "最大服务",
  emptyTechnician: "当前没有满足条件的师傅，请返回上一步调整服务类型",
  prev: "上一步",
  next: "下一步",
  toastSelectServiceType: "请先选择服务类型",
  toastLoading: "加载中...",
  toastLoadTechniciansFail: "师傅列表加载失败",
  toastSelectTechnician: "请先选择师傅",
  toastSelectAddress: "请先选择服务地址"
};

function findById(list, id) {
  return (list || []).find((item) => item.id === id) || null;
}

function getServiceModeFallbackName(mode) {
  if (Number(mode) === 2) return "上门安装";
  if (Number(mode) === 3) return "线下维修";
  return "上门维修";
}

Page({
  data: {
    serviceMode: 1,
    serviceModeName: "",
    serviceTypeName: "",
    categoryPath: "",
    showAddressSection: true,
    addresses: [],
    selectedAddressId: "",
    technicianOptions: [],
    technicianTotal: 0,
    selectedTechnicianId: "",
    labels: LABELS
  },

  onLoad() {
    const draft = draftStore.clearEditingState();
    if (!draft.selectedServiceTypeId) {
      wx.showToast({
        title: LABELS.toastSelectServiceType,
        icon: "none"
      });
      flowNavigation.redirectTo(this, "/pages/order-flow/step2/index");
      return;
    }
    this._loaded = true;
    this.rebuild(draft);
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  onShow() {
    if (!this._loaded) {
      return;
    }
    const draft = draftStore.getDraft();
    if (draft.selectedServiceTypeId) {
      this.rebuild(draft);
    }
  },

  rebuild(sourceDraft) {
    const draft = sourceDraft || draftStore.getDraft();
    const params = {
      serviceMode: draft.serviceMode,
      serviceTypeId: draft.selectedServiceTypeId,
      addressId: draft.selectedAddressId || ""
    };

    wx.showLoading({
      title: LABELS.toastLoading
    });

    fetchSelectionContext(params)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("empty");
        }
        const data = res.data;
        const technicians = Array.isArray(data.technicians) ? data.technicians : [];
        let selectedTechnicianId = draft.selectedTechnicianId;
        if (!technicians.some((item) => item.id === selectedTechnicianId)) {
          selectedTechnicianId = technicians.length ? technicians[0].id : "";
        }

        this.setData({
          serviceMode: Number(data.serviceMode || draft.serviceMode || 1),
          serviceModeName: data.serviceModeName || getServiceModeFallbackName(draft.serviceMode),
          serviceTypeName: data.serviceTypeName || draft.selectedServiceTypeName || "",
          categoryPath: data.categoryPath || draft.selectedCategoryPath || "",
          showAddressSection: !!data.showAddressSection,
          addresses: Array.isArray(data.addresses) ? data.addresses : [],
          selectedAddressId: data.selectedAddressId || "",
          technicianOptions: technicians,
          technicianTotal: technicians.length,
          selectedTechnicianId
        });
      })
      .catch(() => {
        wx.showToast({
          title: LABELS.toastLoadTechniciansFail,
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onSelectAddress(e) {
    const selectedAddressId = e.currentTarget.dataset.id;
    const selectedAddress = findById(this.data.addresses, selectedAddressId);
    const draft = Object.assign({}, draftStore.getDraft(), {
      selectedAddressId,
      selectedAddressText: selectedAddress ? selectedAddress.detail : ""
    });
    draftStore.saveDraft(draft);
    this.rebuild(draft);
  },

  onViewAllTechnicians() {
    wx.navigateTo({
      url: "/pages/technician-browse/index"
    });
  },

  onSelectTechnician(e) {
    const selectedTechnicianId = e.currentTarget.dataset.id;
    this.setData({
      selectedTechnicianId
    });
  },

  onPrev() {
    flowNavigation.navigateBack(this);
  },

  onNext() {
    if (!this.data.selectedTechnicianId) {
      wx.showToast({
        title: LABELS.toastSelectTechnician,
        icon: "none"
      });
      return;
    }
    if (this.data.showAddressSection && !this.data.selectedAddressId) {
      wx.showToast({
        title: LABELS.toastSelectAddress,
        icon: "none"
      });
      return;
    }

    const selectedAddress = findById(this.data.addresses, this.data.selectedAddressId);
    const selectedTechnician = findById(this.data.technicianOptions, this.data.selectedTechnicianId);
    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        editingOrderId: "",
        editingMode: "",
        canModifyAppointment: true,
        selectedAddressId: this.data.showAddressSection ? this.data.selectedAddressId : "",
        selectedAddressText: selectedAddress ? selectedAddress.detail : "",
        selectedTechnicianId: this.data.selectedTechnicianId,
        selectedTechnicianName: selectedTechnician ? selectedTechnician.name : ""
      })
    );

    if (this.data.serviceMode === 2) {
      wx.navigateTo({
        url: "/pages/order-flow/step5/index"
      });
      return;
    }

    wx.navigateTo({
      url: "/pages/order-flow/step4/index"
    });
  }
});
