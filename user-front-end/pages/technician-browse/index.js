const draftStore = require("../../utils/orderDraftStore");
const { fetchTechnicians, toggleTechnicianFollow } = require("../../api/userOrderFlow");

const LABELS = {
  modeRepair: "上门维修",
  modeInstall: "上门安装",
  modeOffline: "线下维修",
  toastSelectServiceType: "请先选择服务类型",
  toastLoading: "加载中...",
  toastLoadFail: "师傅列表加载失败",
  toastFollowFail: "更新关注状态失败",
  toastSelectTechnician: "请先点击选中师傅",
  step: "全部师傅",
  title: "浏览可选师傅",
  currentFilter: "当前条件",
  serviceType: "服务类型",
  address: "服务地址",
  selectedTag: "已选",
  recommendTag: "推荐",
  rating: "评分",
  orderCount: "接单",
  distance: "距离",
  tapToSelect: "点击卡片即选中师傅",
  selectedHint: "已选中，点击下方确认",
  followIcon: "☆",
  followedIcon: "★",
  profileIcon: "⌂",
  empty: "当前没有满足条件的师傅",
  back: "返回",
  confirm: "确认选择",
  defaultInitial: "师"
};

function findById(list, id) {
  return (list || []).find((item) => item.id === id) || null;
}

function getServiceModeName(mode) {
  if (Number(mode) === 2) return LABELS.modeInstall;
  if (Number(mode) === 3) return LABELS.modeOffline;
  return LABELS.modeRepair;
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
    serviceMode: 1,
    serviceModeName: "",
    serviceTypeName: "",
    addressText: "",
    technicians: [],
    selectedTechnicianId: "",
    loading: false,
    labels: LABELS
  },

  onLoad() {
    const draft = draftStore.getDraft();
    if (!draft.selectedServiceTypeId) {
      wx.showToast({
        title: LABELS.toastSelectServiceType,
        icon: "none"
      });
      wx.redirectTo({
        url: "/pages/order-flow/step2/index"
      });
      return;
    }
    this.loadTechnicians(draft);
  },

  onShow() {
    if (!this._loaded) {
      return;
    }
    const draft = draftStore.getDraft();
    if (draft.selectedServiceTypeId) {
      this.loadTechnicians(draft, true);
    }
  },

  loadTechnicians(sourceDraft, silent) {
    const draft = sourceDraft || draftStore.getDraft();
    const params = {
      serviceMode: draft.serviceMode,
      serviceTypeId: draft.selectedServiceTypeId,
      addressId: draft.selectedAddressId || ""
    };

    if (!silent) {
      wx.showLoading({
        title: LABELS.toastLoading
      });
    }
    this.setData({ loading: true });

    fetchTechnicians(params)
      .then((res) => {
        if (!res || res.code !== 200 || !Array.isArray(res.data)) {
          throw new Error("empty");
        }
        const technicians = res.data;
        let selectedTechnicianId = draft.selectedTechnicianId;
        if (!technicians.some((item) => item.id === selectedTechnicianId)) {
          selectedTechnicianId = "";
        }
        this.setData({
          serviceMode: Number(draft.serviceMode || 1),
          serviceModeName: getServiceModeName(draft.serviceMode),
          serviceTypeName: draft.selectedServiceTypeName || "",
          addressText: draft.selectedAddressText || "",
          technicians,
          selectedTechnicianId
        });
        this._loaded = true;
      })
      .catch(() => {
        wx.showToast({
          title: LABELS.toastLoadFail,
          icon: "none"
        });
      })
      .finally(() => {
        if (!silent) {
          wx.hideLoading();
        }
        this.setData({ loading: false });
      });
  },

  onSelectTechnician(e) {
    const technicianId = e.currentTarget.dataset.id;
    if (!technicianId) {
      return;
    }
    this.setData({
      selectedTechnicianId: technicianId
    });
  },

  onConfirmChoose() {
    const technician = findById(this.data.technicians, this.data.selectedTechnicianId);
    if (!technician) {
      wx.showToast({
        title: LABELS.toastSelectTechnician,
        icon: "none"
      });
      return;
    }
    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        selectedTechnicianId: technician.id,
        selectedTechnicianName: technician.name || ""
      })
    );
    safeBack("/pages/order-flow/step3/index", 1);
  },

  onToggleFollow(e) {
    const technicianId = e.currentTarget.dataset.id;
    const technician = findById(this.data.technicians, technicianId);
    if (!technicianId || !technician) {
      return;
    }

    toggleTechnicianFollow({
      technicianId,
      follow: !technician.isFollowed
    })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error("fail");
        }
        const updated = this.data.technicians.map((item) => {
          if (item.id === technicianId) {
            return Object.assign({}, item, {
              isFollowed: !!res.data.isFollowed
            });
          }
          return item;
        });
        this.setData({ technicians: updated });
      })
      .catch(() => {
        wx.showToast({
          title: LABELS.toastFollowFail,
          icon: "none"
        });
      });
  },

  onViewProfile(e) {
    const technicianId = e.currentTarget.dataset.id;
    if (!technicianId) {
      return;
    }
    wx.navigateTo({
      url: `/pages/technician-detail/index?technicianId=${technicianId}`
    });
  },

  onBack() {
    safeBack("/pages/order-flow/step3/index", 1);
  }
});
