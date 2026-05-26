const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { fetchFaultOptions } = require("../../../api/userOrderFlow");

function normalizeFaultDetailMap(value) {
  const map = {};
  Object.keys(value || {}).forEach((key) => {
    const row = value[key] || {};
    map[key] = {
      description: row.description || "",
      images: Array.isArray(row.images) ? row.images : [],
      video: row.video || null
    };
  });
  return map;
}

function buildSelectedFaultDetailList(selectedFaultIds, faultOptions, faultDetailMap) {
  return selectedFaultIds.map((faultId) => {
    const fault = faultOptions.find((item) => item.id === faultId);
    const detail = faultDetailMap[faultId] || {
      description: "",
      images: [],
      video: null
    };
    return {
      id: faultId,
      name: fault ? fault.name : faultId,
      description: detail.description || "",
      images: detail.images || [],
      video: detail.video || null
    };
  });
}

function pickFileName(path, fallbackName) {
  if (!path) return fallbackName;
  const parts = String(path).split("/");
  const name = parts[parts.length - 1];
  return name || fallbackName;
}

function resolveImageMimeType(path) {
  const value = String(path || "").toLowerCase();
  if (value.endsWith(".png")) return "image/png";
  if (value.endsWith(".webp")) return "image/webp";
  if (value.endsWith(".gif")) return "image/gif";
  return "image/jpeg";
}

function buildLocalImage(faultId, tempFile, index) {
  const path = tempFile.path || tempFile.tempFilePath || "";
  return {
    id: `img_${faultId}_${Date.now()}_${index}`,
    name: pickFileName(path, `fault-${index + 1}.jpg`),
    localPath: path,
    url: path,
    mimeType: resolveImageMimeType(path),
    fileSize: tempFile.size || 0,
    width: tempFile.width || null,
    height: tempFile.height || null
  };
}

function buildLocalVideo(faultId, tempFile) {
  const path = tempFile.tempFilePath || tempFile.path || "";
  return {
    id: `video_${faultId}_${Date.now()}`,
    name: pickFileName(path, "fault-video.mp4"),
    localPath: path,
    url: path,
    mimeType: "video/mp4",
    fileSize: tempFile.size || 0,
    duration: tempFile.duration || 0,
    width: tempFile.width || null,
    height: tempFile.height || null,
    thumbnailUrl: tempFile.thumbTempFilePath || ""
  };
}

function resolveMediaPath(media) {
  if (!media || typeof media !== "object") return "";
  return media.url || media.localPath || media.tempFilePath || media.path || "";
}

Page({
  data: {
    serviceMode: 1,
    serviceModeName: "",
    serviceTypeName: "",
    faultOptions: [],
    selectedFaultIds: [],
    faultDetailMap: {},
    selectedFaultDetailList: [],
    applianceBrand: "",
    applianceModel: "",
    purchaseDate: ""
  },

  onLoad() {
    const draft = draftStore.getDraft();
    if (Number(draft.serviceMode) === 2 && !draft.editingOrderId) {
      flowNavigation.redirectTo(this, "/pages/order-flow/step5/index");
      return;
    }
    if (!draft.selectedServiceTypeId) {
      wx.showToast({
        title: "请先选择服务类型",
        icon: "none"
      });
      flowNavigation.redirectTo(this, "/pages/order-flow/step2/index");
      return;
    }
    this.rebuild(draft);
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  rebuild(sourceDraft) {
    const draft = sourceDraft || draftStore.getDraft();

    wx.showLoading({
      title: "加载中..."
    });

    fetchFaultOptions(draft.selectedServiceTypeId)
      .then((res) => {
        const options = res && res.code === 200 && Array.isArray(res.data) ? res.data : [];
        const faultOptions = options.map((item) => ({ id: item.id, name: item.name }));
        const selectedFaultIds = (draft.selectedFaultIds || []).filter((id) =>
          faultOptions.some((item) => item.id === id)
        );
        const faultDetailMap = normalizeFaultDetailMap(draft.faultDetailMap || {});
        selectedFaultIds.forEach((id) => {
          if (!faultDetailMap[id]) {
            faultDetailMap[id] = {
              description: "",
              images: [],
              video: null
            };
          }
        });

        this.setData({
          serviceMode: Number(draft.serviceMode || 1),
          serviceModeName: draft.serviceMode === 2 ? "上门安装" : draft.serviceMode === 3 ? "线下维修" : "上门维修",
          serviceTypeName: draft.selectedServiceTypeName || "",
          faultOptions: faultOptions.map((item) => ({
            id: item.id,
            name: item.name,
            selected: selectedFaultIds.indexOf(item.id) !== -1
          })),
          selectedFaultIds,
          faultDetailMap,
          selectedFaultDetailList: buildSelectedFaultDetailList(selectedFaultIds, faultOptions, faultDetailMap),
          applianceBrand: draft.applianceBrand || "",
          applianceModel: draft.applianceModel || "",
          purchaseDate: draft.purchaseDate || ""
        });
      })
      .catch(() => {
        wx.showToast({
          title: "故障选项加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  syncFaultDetailList() {
    const selectedFaultDetailList = buildSelectedFaultDetailList(
      this.data.selectedFaultIds,
      this.data.faultOptions,
      this.data.faultDetailMap
    );
    this.setData({
      selectedFaultDetailList,
      faultOptions: this.data.faultOptions.map((item) =>
        Object.assign({}, item, {
          selected: this.data.selectedFaultIds.indexOf(item.id) !== -1
        })
      )
    });
  },

  onToggleFault(e) {
    const faultId = e.currentTarget.dataset.id;
    const selectedFaultIds = this.data.selectedFaultIds.slice();
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const idx = selectedFaultIds.indexOf(faultId);
    if (idx >= 0) {
      selectedFaultIds.splice(idx, 1);
      delete faultDetailMap[faultId];
    } else {
      selectedFaultIds.push(faultId);
      faultDetailMap[faultId] = {
        description: "",
        images: [],
        video: null
      };
    }
    this.setData(
      {
        selectedFaultIds,
        faultDetailMap
      },
      () => this.syncFaultDetailList()
    );
  },

  onFaultDescInput(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const value = e.detail.value || "";
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const row = Object.assign(
      {
        description: "",
        images: [],
        video: null
      },
      faultDetailMap[faultId] || {}
    );
    row.description = value;
    faultDetailMap[faultId] = row;
    this.setData(
      {
        faultDetailMap
      },
      () => this.syncFaultDetailList()
    );
  },

  onAddFaultImage(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const row = Object.assign(
      {
        description: "",
        images: [],
        video: null
      },
      faultDetailMap[faultId] || {}
    );
    const images = (row.images || []).slice();
    if (images.length >= 3) {
      wx.showToast({
        title: "每个故障最多3张图",
        icon: "none"
      });
      return;
    }

    wx.chooseImage({
      count: 3 - images.length,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const tempFiles = Array.isArray(res.tempFiles)
          ? res.tempFiles
          : (res.tempFilePaths || []).map((filePath) => ({ path: filePath }));
        tempFiles.forEach((tempFile) => {
          if (images.length >= 3) return;
          images.push(buildLocalImage(faultId, tempFile, images.length));
        });
        row.images = images;
        faultDetailMap[faultId] = row;
        this.setData(
          {
            faultDetailMap
          },
          () => this.syncFaultDetailList()
        );
      }
    });
  },

  onRemoveFaultImage(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const index = Number(e.currentTarget.dataset.index);
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const row = Object.assign(
      {
        description: "",
        images: [],
        video: null
      },
      faultDetailMap[faultId] || {}
    );
    const images = (row.images || []).slice();
    if (index >= 0 && index < images.length) {
      images.splice(index, 1);
    }
    row.images = images;
    faultDetailMap[faultId] = row;
    this.setData(
      {
        faultDetailMap
      },
      () => this.syncFaultDetailList()
    );
  },

  onPreviewFaultImage(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const index = Number(e.currentTarget.dataset.index);
    const row = this.data.faultDetailMap[faultId] || {};
    const urls = (row.images || []).map((item) => resolveMediaPath(item)).filter(Boolean);
    if (!urls.length) {
      return;
    }
    const current = urls[index] || urls[0];
    wx.previewImage({
      current,
      urls
    });
  },

  onAddFaultVideo(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const row = Object.assign(
      {
        description: "",
        images: [],
        video: null
      },
      faultDetailMap[faultId] || {}
    );

    wx.chooseVideo({
      sourceType: ["album", "camera"],
      compressed: true,
      maxDuration: 60,
      success: (res) => {
        row.video = buildLocalVideo(faultId, res || {});
        faultDetailMap[faultId] = row;
        this.setData(
          {
            faultDetailMap
          },
          () => this.syncFaultDetailList()
        );
      }
    });
  },

  onRemoveFaultVideo(e) {
    const faultId = e.currentTarget.dataset.faultid;
    const faultDetailMap = Object.assign({}, this.data.faultDetailMap);
    const row = Object.assign(
      {
        description: "",
        images: [],
        video: null
      },
      faultDetailMap[faultId] || {}
    );
    row.video = null;
    faultDetailMap[faultId] = row;
    this.setData(
      {
        faultDetailMap
      },
      () => this.syncFaultDetailList()
    );
  },

  onBrandInput(e) {
    this.setData({
      applianceBrand: e.detail.value || ""
    });
  },

  onModelInput(e) {
    this.setData({
      applianceModel: e.detail.value || ""
    });
  },

  onPurchaseDateChange(e) {
    this.setData({
      purchaseDate: e.detail.value
    });
  },

  onPrev() {
    flowNavigation.navigateBack(this);
  },

  onNext() {
    const faultNameMap = {};
    this.data.faultOptions.forEach((item) => {
      faultNameMap[item.id] = item.name;
    });

    const draft = draftStore.getDraft();
    draftStore.saveDraft(
      Object.assign({}, draft, {
        selectedFaultIds: this.data.selectedFaultIds,
        faultNameMap,
        faultDetailMap: this.data.faultDetailMap,
        applianceBrand: this.data.applianceBrand,
        applianceModel: this.data.applianceModel,
        purchaseDate: this.data.purchaseDate
      })
    );

    if (this.data.serviceMode === 1 || this.data.serviceMode === 2) {
      wx.navigateTo({
        url: "/pages/order-flow/step5/index"
      });
      return;
    }

    wx.navigateTo({
      url: "/pages/order-flow/step6/index"
    });
  }
});

