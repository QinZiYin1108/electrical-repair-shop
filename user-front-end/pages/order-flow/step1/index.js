const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { fetchServiceModes } = require("../../../api/userOrderFlow");

Page({
  data: {
    serviceModes: [],
    selectedServiceMode: 1
  },

  onLoad() {
    draftStore.clearEditingState();
    this.loadServiceModes();
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  loadServiceModes() {
    const draft = draftStore.getDraft();
    wx.showLoading({
      title: "加载中..."
    });
    fetchServiceModes()
      .then((res) => {
        const list = res && res.code === 200 && Array.isArray(res.data) ? res.data : [];
        const selectedFromDraft = Number(draft.serviceMode || 1);
        const selectedServiceMode = list.some((item) => Number(item.id) === selectedFromDraft)
          ? selectedFromDraft
          : (list.length ? Number(list[0].id) : 1);
        this.setData({
          serviceModes: list,
          selectedServiceMode
        });
      })
      .catch(() => {
        wx.showToast({
          title: "服务方式加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onSelectMode(e) {
    const mode = Number(e.currentTarget.dataset.mode);
    this.setData({
      selectedServiceMode: mode
    });
  },

  onNext() {
    const mode = Number(this.data.selectedServiceMode);
    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        serviceMode: mode,
        editingOrderId: "",
        editingMode: "",
        canModifyAppointment: true,
        selectedLevel1Id: "",
        selectedLevel2Id: "",
        selectedCategoryId: "",
        selectedCategoryPath: "",
        selectedServiceTypeId: "",
        selectedServiceTypeName: "",
        selectedAddressId: "",
        selectedAddressText: "",
        selectedTechnicianId: "",
        selectedTechnicianName: "",
        selectedFaultIds: [],
        faultNameMap: {},
        faultDetailMap: {},
        selectedAppointmentId: "",
        selectedAppointmentLabel: "",
        selectedAppointmentTime: null
      })
    );
    wx.navigateTo({
      url: "/pages/order-flow/step2/index"
    });
  }
});
