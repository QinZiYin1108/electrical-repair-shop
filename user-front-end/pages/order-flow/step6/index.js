const draftStore = require("../../../utils/orderDraftStore");
const flowNavigation = require("../../../utils/orderFlowNavigation");
const { showUploadErrorModal } = require("../../../utils/uploadFeedback");
const { fetchFeePreview, submitOrder, uploadFaultMedia } = require("../../../api/userOrderFlow");
const { updateUserOrder } = require("../../../api/userOrders");
const { getUserFundsSummary } = require("../../../api/userFunds");

const PAYMENT_METHOD_WECHAT = 1;
const PAYMENT_METHOD_ALIPAY = 2;
const PAYMENT_METHOD_WALLET = 5;

function buildFaultSummaryList(selectedFaultIds, faultNameMap, faultDetailMap) {
  return (selectedFaultIds || []).map((faultId) => {
    const row = (faultDetailMap || {})[faultId] || {};
    const images = Array.isArray(row.images) ? row.images : [];
    return {
      id: faultId,
      name: (faultNameMap || {})[faultId] || faultId,
      description: row.description || "",
      imageCount: images.length,
      hasVideo: !!row.video
    };
  });
}

function pickFileName(path, fallbackName) {
  if (!path) return fallbackName;
  const parts = String(path).split("/");
  return parts[parts.length - 1] || fallbackName;
}

function isRemoteUrl(url) {
  return /^https?:\/\//i.test(String(url || "").trim());
}

function resolveLocalPath(media) {
  if (!media || typeof media !== "object") return "";
  return media.localPath || media.tempFilePath || media.path || "";
}

function isEditingDraft(draft) {
  return !!(draft && draft.editingOrderId);
}

Page({
  data: {
    draft: null,
    serviceMode: 1,
    serviceModeName: "",
    categoryPath: "",
    serviceTypeName: "",
    technicianName: "",
    addressText: "",
    appointmentText: "",
    applianceBrand: "",
    applianceModel: "",
    purchaseDate: "",
    faultSummaryList: [],
    feePreview: null,
    submitAmountText: "0.00",
    submitAmountLabel: "本次预估",
    selectedPaymentMethod: PAYMENT_METHOD_WECHAT,
    walletBalanceText: "0.00",
    submitButtonText: "提交订单",
    contentPaddingBottomPx: 160,
    submitting: false
  },

  onLoad() {
    const draft = draftStore.getDraft();
    if (!draft.selectedServiceTypeId || !draft.selectedTechnicianId) {
      wx.showToast({
        title: "请先完成前序步骤",
        icon: "none"
      });
      flowNavigation.redirectTo(this, "/pages/order-flow/step1/index");
      return;
    }
    this.buildView(draft);
  },

  onUnload() {
    flowNavigation.handleUnload(this);
  },

  onReady() {
    this.updateFlowBottomPadding();
  },

  buildView(draft) {
    const serviceMode = Number(draft.serviceMode || 1);
    const editing = isEditingDraft(draft);
    const appointmentOnly = editing && draft.editingMode === "appointment";
    const faultSummaryList = buildFaultSummaryList(draft.selectedFaultIds, draft.faultNameMap, draft.faultDetailMap);

    const baseData = {
      draft,
      serviceMode,
      serviceModeName: serviceMode === 2 ? "上门安装" : serviceMode === 3 ? "线下维修" : "上门维修",
      categoryPath: draft.selectedCategoryPath || "",
      serviceTypeName: draft.selectedServiceTypeName || "",
      technicianName: draft.selectedTechnicianName || "",
      addressText: serviceMode === 3 ? "线下维修无需地址" : (draft.selectedAddressText || ""),
      appointmentText: serviceMode === 3 ? "线下维修无需预约" : (draft.selectedAppointmentLabel || ""),
      applianceBrand: draft.applianceBrand || "未填写",
      applianceModel: draft.applianceModel || "未填写",
      purchaseDate: draft.purchaseDate || "未填写",
      faultSummaryList,
      feePreview: null,
      submitAmountText: editing ? "按原订单结算" : (serviceMode === 3 ? "到店后确认" : "¥0.00"),
      submitAmountLabel: editing ? "当前状态" : (serviceMode === 3 ? "费用提示" : "需支付"),
      submitButtonText: appointmentOnly ? "确认修改预约" : (editing ? "确认修改订单" : (serviceMode === 3 ? "提交订单" : "支付并提交"))
    };

    this.setData(baseData, () => {
      this.updateFlowBottomPadding();
    });

    if (editing || serviceMode === 3) {
      return;
    }

    this.loadWalletSummary();

    fetchFeePreview({
      serviceMode,
      serviceTypeId: draft.selectedServiceTypeId,
      technicianId: draft.selectedTechnicianId,
      addressId: draft.selectedAddressId || ""
    })
      .then((res) => {
        const feePreview = res && res.code === 200 ? res.data : null;
        if (!feePreview) return;
        this.setData(
          {
            feePreview,
            submitAmountText: `¥${feePreview.totalAmount || "0.00"}`,
            submitAmountLabel: "需支付"
          },
          () => this.updateFlowBottomPadding()
        );
      })
      .catch(() => {
        wx.showToast({
          title: "费用预览加载失败",
          icon: "none"
        });
      });
  },

  loadWalletSummary() {
    getUserFundsSummary()
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          return;
        }
        this.setData({
          walletBalanceText: res.data.balance || "0.00"
        });
      })
      .catch(() => {});
  },

  getFallbackBottomPaddingPx() {
    let windowWidth = 375;
    try {
      if (wx.getWindowInfo) {
        windowWidth = wx.getWindowInfo().windowWidth || 375;
      } else {
        windowWidth = wx.getSystemInfoSync().windowWidth || 375;
      }
    } catch (e) {}
    return Math.ceil((windowWidth * 188) / 750);
  },

  updateFlowBottomPadding() {
    const query = this.createSelectorQuery();
    query.select("#submitFooter").boundingClientRect();
    query.exec((res) => {
      const rect = res && res[0];
      const fallback = this.getFallbackBottomPaddingPx();
      const nextPadding = rect && rect.height ? Math.ceil(rect.height + 12) : fallback;
      if (nextPadding !== this.data.contentPaddingBottomPx) {
        this.setData({
          contentPaddingBottomPx: nextPadding
        });
      }
    });
  },

  onPrev() {
    flowNavigation.navigateBack(this);
  },

  onRestart() {
    draftStore.resetDraft();
    flowNavigation.redirectTo(this, "/pages/order-flow/step1/index");
  },

  onSelectPaymentMethod(e) {
    const method = Number(e.currentTarget.dataset.method || PAYMENT_METHOD_WECHAT);
    this.setData({
      selectedPaymentMethod: method
    });
  },

  getSelectedPaymentMethodText() {
    const method = Number(this.data.selectedPaymentMethod || PAYMENT_METHOD_WECHAT);
    if (method === PAYMENT_METHOD_WALLET) return "钱包";
    if (method === PAYMENT_METHOD_ALIPAY) return "支付宝";
    return "微信";
  },

  getCurrentPayAmount() {
    const text = String((this.data.feePreview && this.data.feePreview.totalAmount) || "0");
    const amount = Number(text);
    return Number.isFinite(amount) ? amount : 0;
  },

  uploadImageForSubmit(image, index) {
    if (!image || typeof image !== "object") {
      return Promise.resolve(null);
    }

    const localPath = resolveLocalPath(image);
    if (!localPath) {
      const remoteUrl = String(image.url || "").trim();
      if (!isRemoteUrl(remoteUrl)) {
        return Promise.resolve(null);
      }
      return Promise.resolve({
        url: remoteUrl,
        name: image.name || `fault-image-${index + 1}.jpg`,
        fileSize: image.fileSize || 0,
        mimeType: image.mimeType || "image/jpeg",
        width: image.width || null,
        height: image.height || null
      });
    }

    return uploadFaultMedia(localPath, "image").then((res) => {
      if (!res || res.code !== 200 || !res.data || !res.data.url) {
        throw new Error((res && res.message) || "图片上传失败");
      }
      const uploaded = res.data;
      return {
        url: uploaded.url,
        name: uploaded.name || image.name || pickFileName(localPath, `fault-image-${index + 1}.jpg`),
        fileSize: uploaded.fileSize || image.fileSize || 0,
        mimeType: uploaded.mimeType || image.mimeType || "image/jpeg",
        width: uploaded.width || image.width || null,
        height: uploaded.height || image.height || null
      };
    });
  },

  uploadVideoForSubmit(video) {
    if (!video || typeof video !== "object") {
      return Promise.resolve(null);
    }

    const localPath = resolveLocalPath(video);
    if (!localPath) {
      const remoteUrl = String(video.url || "").trim();
      if (!isRemoteUrl(remoteUrl)) {
        return Promise.resolve(null);
      }
      return Promise.resolve({
        url: remoteUrl,
        name: video.name || "fault-video.mp4",
        fileSize: video.fileSize || 0,
        mimeType: video.mimeType || "video/mp4",
        duration: video.duration || 0,
        width: video.width || null,
        height: video.height || null,
        thumbnailUrl: video.thumbnailUrl || ""
      });
    }

    return uploadFaultMedia(localPath, "video").then((res) => {
      if (!res || res.code !== 200 || !res.data || !res.data.url) {
        throw new Error((res && res.message) || "视频上传失败");
      }
      const uploaded = res.data;
      return {
        url: uploaded.url,
        name: uploaded.name || video.name || pickFileName(localPath, "fault-video.mp4"),
        fileSize: uploaded.fileSize || video.fileSize || 0,
        mimeType: uploaded.mimeType || video.mimeType || "video/mp4",
        duration: uploaded.duration || video.duration || 0,
        width: uploaded.width || video.width || null,
        height: uploaded.height || video.height || null,
        thumbnailUrl: uploaded.thumbnailUrl || video.thumbnailUrl || ""
      };
    });
  },

  buildSubmitFaultList(draft) {
    const selectedFaultIds = draft.selectedFaultIds || [];
    const faultNameMap = draft.faultNameMap || {};
    const faultDetailMap = draft.faultDetailMap || {};

    return Promise.all(
      selectedFaultIds.map((faultId) => {
        const detail = faultDetailMap[faultId] || {};
        const images = Array.isArray(detail.images) ? detail.images : [];

        return Promise.all(images.map((image, index) => this.uploadImageForSubmit(image, index)))
          .then((uploadedImages) => uploadedImages.filter((item) => !!item))
          .then((imageList) => this.uploadVideoForSubmit(detail.video).then((videoItem) => ({ imageList, videoItem })))
          .then(({ imageList, videoItem }) => ({
            faultId,
            faultName: faultNameMap[faultId] || "",
            faultDescription: detail.description || "",
            images: imageList,
            video: videoItem
          }));
      })
    );
  },

  onSubmit() {
    if (this.data.submitting) {
      return;
    }

    const needPay = !isEditingDraft(this.data.draft) && this.data.serviceMode !== 3 && this.getCurrentPayAmount() > 0;
    if (
      needPay &&
      Number(this.data.selectedPaymentMethod) === PAYMENT_METHOD_WALLET &&
      Number(this.data.walletBalanceText || 0) < this.getCurrentPayAmount()
    ) {
      wx.showToast({
        title: "钱包余额不足，请更换支付方式",
        icon: "none"
      });
      return;
    }

    if (needPay) {
      wx.showModal({
        title: "确认支付",
        content: `将使用${this.getSelectedPaymentMethodText()}支付上门服务费 ¥${this.data.feePreview.totalAmount || "0.00"}，确认后提交订单。`,
        confirmText: "确认支付",
        cancelText: "取消",
        success: (res) => {
          if (!res.confirm) {
            return;
          }
          this.doSubmit();
        }
      });
      return;
    }

    this.doSubmit();
  },

  doSubmit() {
    if (this.data.submitting) {
      return;
    }

    const draft = this.data.draft || draftStore.getDraft();
    this.setData({ submitting: true });

    wx.showLoading({
      title: "上传素材中..."
    });

    this.buildSubmitFaultList(draft)
      .then((faultList) => {
        const editing = isEditingDraft(draft);
        const appointmentOnly = editing && draft.editingMode === "appointment";
        const payload = editing
          ? {
              orderId: draft.editingOrderId,
              appointmentOnly,
              appointmentTime: draft.selectedAppointmentTime || null,
              applianceBrand: draft.applianceBrand || "",
              applianceModel: draft.applianceModel || "",
              purchaseDate: draft.purchaseDate || "",
              faultList
            }
          : {
              serviceMode: draft.serviceMode,
              categoryId: draft.selectedCategoryId,
              serviceTypeId: draft.selectedServiceTypeId,
              technicianId: draft.selectedTechnicianId,
              serviceAddressId: draft.selectedAddressId || "",
              appointmentTime: draft.selectedAppointmentTime || null,
              applianceBrand: draft.applianceBrand || "",
              applianceModel: draft.applianceModel || "",
              purchaseDate: draft.purchaseDate || "",
              paymentMethod: this.data.serviceMode === 3 ? null : this.data.selectedPaymentMethod,
              faultList
            };

        wx.showLoading({
          title: editing ? "更新订单中..." : "提交订单中..."
        });
        return editing ? updateUserOrder(payload) : submitOrder(payload);
      })
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          wx.showToast({
            title: (res && res.message) || "提交失败",
            icon: "none"
          });
          return;
        }
        wx.showModal({
          title: isEditingDraft(draft) ? "修改成功" : "下单成功",
          content: `订单号：${(res.data && res.data.orderNo) || "-"}`,
          showCancel: false,
          success: () => {
            draftStore.resetDraft();
            if (isEditingDraft(draft)) {
              wx.redirectTo({
                url: `/pages/order-detail/index?orderId=${(res.data && res.data.id) || draft.editingOrderId}`
              });
              return;
            }
            flowNavigation.switchTabHome(this);
          }
        });
      })
      .catch((err) => {
        showUploadErrorModal(err, {
          title: "提交失败",
          fallback: "提交失败"
        });
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ submitting: false });
      });
  }
});
