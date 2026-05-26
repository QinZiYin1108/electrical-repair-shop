const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { fetchCategoryTree, fetchServiceTypes } = require("../../../api/userOrderFlow");

function findFirstId(list) {
  return list && list.length ? list[0].id : "";
}

function findById(list, id) {
  return (list || []).find((item) => item.id === id) || null;
}

Page({
  data: {
    serviceModeName: "",
    level1Keyword: "",
    level1List: [],
    selectedLevel1Id: "",
    level2List: [],
    selectedLevel2Id: "",
    level3List: [],
    selectedCategoryId: "",
    selectedCategoryPath: "",
    serviceTypeOptions: [],
    selectedServiceTypeId: ""
  },

  onLoad() {
    const draft = draftStore.clearEditingState();
    this.rebuildView(draft.level1Keyword || "", draft);
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  rebuildView(keyword, sourceDraft) {
    const draft = sourceDraft || draftStore.getDraft();
    const requestId = Date.now();
    this._rebuildRequestId = requestId;

    wx.showLoading({
      title: "加载中..."
    });

    fetchCategoryTree(keyword)
      .then((res) => {
        if (this._rebuildRequestId !== requestId) return;

        const level1List = res && res.code === 200 && Array.isArray(res.data) ? res.data : [];
        const selectedLevel1Id = level1List.some((item) => item.id === draft.selectedLevel1Id)
          ? draft.selectedLevel1Id
          : findFirstId(level1List);
        const selectedLevel1 = findById(level1List, selectedLevel1Id);
        const level2List = selectedLevel1 && Array.isArray(selectedLevel1.children)
          ? selectedLevel1.children
          : [];

        const selectedLevel2Id = level2List.some((item) => item.id === draft.selectedLevel2Id)
          ? draft.selectedLevel2Id
          : findFirstId(level2List);
        const selectedLevel2 = findById(level2List, selectedLevel2Id);
        const level3List = selectedLevel2 && Array.isArray(selectedLevel2.children)
          ? selectedLevel2.children
          : [];

        const selectedCategoryId = level3List.some((item) => item.id === draft.selectedCategoryId)
          ? draft.selectedCategoryId
          : findFirstId(level3List);
        const selectedLevel3 = findById(level3List, selectedCategoryId);

        const selectedCategoryPath = selectedLevel1 && selectedLevel2 && selectedLevel3
          ? `${selectedLevel1.name} / ${selectedLevel2.name} / ${selectedLevel3.name}`
          : "";

        this.setData({
          serviceModeName: draft.serviceMode === 2 ? "上门安装" : draft.serviceMode === 3 ? "线下维修" : "上门维修",
          level1Keyword: keyword,
          level1List,
          selectedLevel1Id,
          level2List,
          selectedLevel2Id,
          level3List,
          selectedCategoryId,
          selectedCategoryPath,
          serviceTypeOptions: [],
          selectedServiceTypeId: ""
        });

        if (!selectedCategoryId) {
          return null;
        }

        return fetchServiceTypes({
          serviceMode: draft.serviceMode,
          categoryId: selectedCategoryId
        }).then((serviceTypeRes) => {
          if (this._rebuildRequestId !== requestId) return;
          const serviceTypeOptions = serviceTypeRes && serviceTypeRes.code === 200 && Array.isArray(serviceTypeRes.data)
            ? serviceTypeRes.data
            : [];
          const selectedServiceTypeId = serviceTypeOptions.some((item) => item.id === draft.selectedServiceTypeId)
            ? draft.selectedServiceTypeId
            : findFirstId(serviceTypeOptions);

          this.setData({
            serviceTypeOptions,
            selectedServiceTypeId
          });
        });
      })
      .catch(() => {
        if (this._rebuildRequestId !== requestId) return;
        wx.showToast({
          title: "分类加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        if (this._rebuildRequestId === requestId) {
          wx.hideLoading();
        }
      });
  },

  onKeywordInput(e) {
    const keyword = (e.detail.value || "").trim();
    const draft = Object.assign({}, draftStore.getDraft(), {
      level1Keyword: keyword,
      selectedLevel1Id: "",
      selectedLevel2Id: "",
      selectedCategoryId: "",
      selectedServiceTypeId: ""
    });
    this.rebuildView(keyword, draft);
  },

  onSelectLevel1(e) {
    const selectedLevel1Id = e.currentTarget.dataset.id;
    const draft = Object.assign({}, draftStore.getDraft(), {
      selectedLevel1Id,
      selectedLevel2Id: "",
      selectedCategoryId: "",
      selectedServiceTypeId: ""
    });
    this.rebuildView(this.data.level1Keyword, draft);
  },

  onSelectLevel2(e) {
    const selectedLevel2Id = e.currentTarget.dataset.id;
    const draft = Object.assign({}, draftStore.getDraft(), {
      selectedLevel1Id: this.data.selectedLevel1Id,
      selectedLevel2Id,
      selectedCategoryId: "",
      selectedServiceTypeId: ""
    });
    this.rebuildView(this.data.level1Keyword, draft);
  },

  onSelectCategory(e) {
    const selectedCategoryId = e.currentTarget.dataset.id;
    const draft = Object.assign({}, draftStore.getDraft(), {
      selectedLevel1Id: this.data.selectedLevel1Id,
      selectedLevel2Id: this.data.selectedLevel2Id,
      selectedCategoryId,
      selectedServiceTypeId: ""
    });
    this.rebuildView(this.data.level1Keyword, draft);
  },

  onSelectServiceType(e) {
    const selectedServiceTypeId = e.currentTarget.dataset.id;
    this.setData({
      selectedServiceTypeId
    });
  },

  onPrev() {
    flowNavigation.navigateBack(this);
  },

  onNext() {
    if (!this.data.selectedCategoryId) {
      wx.showToast({
        title: "请先选择三级分类",
        icon: "none"
      });
      return;
    }
    if (!this.data.selectedServiceTypeId) {
      wx.showToast({
        title: "请先选择服务类型",
        icon: "none"
      });
      return;
    }

    const selectedServiceType = findById(this.data.serviceTypeOptions, this.data.selectedServiceTypeId);
    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        editingOrderId: "",
        editingMode: "",
        canModifyAppointment: true,
        level1Keyword: this.data.level1Keyword,
        selectedLevel1Id: this.data.selectedLevel1Id,
        selectedLevel2Id: this.data.selectedLevel2Id,
        selectedCategoryId: this.data.selectedCategoryId,
        selectedCategoryPath: this.data.selectedCategoryPath,
        selectedServiceTypeId: this.data.selectedServiceTypeId,
        selectedServiceTypeName: selectedServiceType ? selectedServiceType.name : "",
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
      url: "/pages/order-flow/step3/index"
    });
  }
});
