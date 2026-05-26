const DRAFT_KEY = "order_flow_draft";

function createDefaultDraft() {
  return {
    serviceMode: 1,
    level1Keyword: "",
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
    applianceBrand: "",
    applianceModel: "",
    purchaseDate: "",
    editingOrderId: "",
    editingMode: "",
    canModifyAppointment: true,
    selectedAppointmentId: "",
    selectedAppointmentLabel: "",
    selectedAppointmentTime: null
  };
}

function normalizeFaultDetailMap(value) {
  if (!value || typeof value !== "object") return {};
  const next = {};
  Object.keys(value).forEach((key) => {
    const row = value[key] || {};
    next[key] = {
      description: row.description || "",
      images: Array.isArray(row.images) ? row.images : [],
      video: row.video || null
    };
  });
  return next;
}

function normalizeNameMap(value) {
  if (!value || typeof value !== "object") return {};
  const next = {};
  Object.keys(value).forEach((key) => {
    next[key] = value[key] || "";
  });
  return next;
}

function normalizeDraft(raw) {
  const defaults = createDefaultDraft();
  const draft = Object.assign({}, defaults, raw || {});
  if (!Array.isArray(draft.selectedFaultIds)) {
    draft.selectedFaultIds = [];
  }
  draft.faultNameMap = normalizeNameMap(draft.faultNameMap);
  draft.faultDetailMap = normalizeFaultDetailMap(draft.faultDetailMap);
  if (!draft.selectedAppointmentTime) {
    draft.selectedAppointmentTime = null;
  } else {
    draft.selectedAppointmentTime = Number(draft.selectedAppointmentTime);
  }
  return draft;
}

function saveDraft(draft) {
  const normalized = normalizeDraft(draft);
  wx.setStorageSync(DRAFT_KEY, normalized);
  return normalized;
}

function getDraft() {
  const cached = wx.getStorageSync(DRAFT_KEY);
  if (!cached || typeof cached !== "object") {
    return saveDraft(createDefaultDraft());
  }
  return saveDraft(cached);
}

function patchDraft(patch) {
  const current = getDraft();
  const next = Object.assign({}, current, patch || {});
  return saveDraft(next);
}

function resetDraft() {
  wx.removeStorageSync(DRAFT_KEY);
  return saveDraft(createDefaultDraft());
}

function clearEditingState() {
  return patchDraft({
    editingOrderId: "",
    editingMode: "",
    canModifyAppointment: true
  });
}

module.exports = {
  getDraft,
  saveDraft,
  patchDraft,
  resetDraft,
  clearEditingState
};
