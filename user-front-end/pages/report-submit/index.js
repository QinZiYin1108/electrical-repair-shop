const router = require("../../utils/router");
const { submitReport } = require("../../api/report");

const TARGET_TYPES = [
  { value: 1, label: '账号' },
  { value: 2, label: '门店' },
  { value: 3, label: '订单' },
  { value: 4, label: '商品' }
];

const REASON_CATEGORIES = [
  { value: '违规内容', label: '违规内容' },
  { value: '虚假信息', label: '虚假信息' },
  { value: '骚扰辱骂', label: '骚扰辱骂' },
  { value: '侵权', label: '侵权' },
  { value: '欺诈', label: '欺诈' },
  { value: '其他', label: '其他' }
];

Page({
  data: {
    targetType: null,
    targetTypeLabel: '请选择',
    targetTypeOptions: TARGET_TYPES.map(t => t.label),
    targetId: '',
    targetField: '',
    reasonCategory: null,
    reasonCategoryLabel: '请选择',
    reasonCategoryOptions: REASON_CATEGORIES.map(r => r.label),
    description: '',
    evidenceImages: [],
    submitting: false,
    showTargetType: false,
    showReasonCategory: false
  },

  onSelectTargetType() {
    this.setData({ showTargetType: true });
  },

  onCloseTargetType() {
    this.setData({ showTargetType: false });
  },

  onTargetTypeSelect(e) {
    const index = e.detail.index;
    this.setData({
      targetType: TARGET_TYPES[index].value,
      targetTypeLabel: TARGET_TYPES[index].label,
      showTargetType: false
    });
  },

  onSelectReasonCategory() {
    this.setData({ showReasonCategory: true });
  },

  onCloseReasonCategory() {
    this.setData({ showReasonCategory: false });
  },

  onReasonCategorySelect(e) {
    const index = e.detail.index;
    this.setData({
      reasonCategory: REASON_CATEGORIES[index].value,
      reasonCategoryLabel: REASON_CATEGORIES[index].label,
      showReasonCategory: false
    });
  },

  onTargetIdInput(e) {
    this.setData({ targetId: e.detail.value });
  },

  onTargetFieldInput(e) {
    this.setData({ targetField: e.detail.value });
  },

  onDescriptionInput(e) {
    this.setData({ description: e.detail.value });
  },

  onUploadEvidence(e) {
    const { fileList } = e.detail;
    this.setData({ evidenceImages: fileList });
  },

  onSubmit() {
    const { targetType, targetId, reasonCategory, description } = this.data;
    if (!targetType) {
      wx.showToast({ title: '请选择举报对象类型', icon: 'none' });
      return;
    }
    if (!targetId.trim()) {
      wx.showToast({ title: '请输入举报对象ID', icon: 'none' });
      return;
    }
    if (!reasonCategory) {
      wx.showToast({ title: '请选择举报原因', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    const data = {
      targetType,
      targetId: targetId.trim(),
      targetField: this.data.targetField.trim() || null,
      reasonCategory,
      description: description.trim() || null,
      evidenceImages: this.data.evidenceImages.length > 0
        ? JSON.stringify(this.data.evidenceImages.map(f => f.url || f.path))
        : null
    };

    submitReport(data)
      .then(res => {
        if (res && res.code === 200) {
          wx.showToast({ title: '举报提交成功', icon: 'success' });
          setTimeout(() => { router.navigateBack(); }, 1200);
        } else {
          wx.showToast({ title: (res && res.message) || '提交失败', icon: 'none' });
        }
      })
      .catch(err => {
        wx.showToast({ title: err.message || '提交失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  }
});
