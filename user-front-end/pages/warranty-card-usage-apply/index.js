const router = require('../../utils/router');
const { applyUserWarrantyCardUsage } = require('../../api/userWarrantyCards');

Page({
  data: {
    cardId: '',
    issueDescription: '',
    issueDescriptionLength: 0,
    contactName: '',
    contactPhone: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({
      cardId: options && options.id ? options.id : ''
    });
  },

  onIssueInput(e) {
    const value = e.detail.value || '';
    this.setData({
      issueDescription: value,
      issueDescriptionLength: value.length
    });
  },

  onContactNameInput(e) {
    this.setData({ contactName: e.detail.value || '' });
  },

  onContactPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value || '' });
  },

  onSubmit() {
    const cardId = this.data.cardId;
    const issueDescription = String(this.data.issueDescription || '').trim();
    const contactName = String(this.data.contactName || '').trim();
    const contactPhone = String(this.data.contactPhone || '').trim();

    if (!cardId) {
      wx.showToast({ title: '保修卡不存在', icon: 'none' });
      return;
    }
    if (!issueDescription) {
      wx.showToast({ title: '请填写故障描述', icon: 'none' });
      return;
    }
    if (!contactName) {
      wx.showToast({ title: '请填写联系人', icon: 'none' });
      return;
    }
    if (!contactPhone) {
      wx.showToast({ title: '请填写联系电话', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    applyUserWarrantyCardUsage({
      warrantyCardId: cardId,
      issueDescription,
      contactName,
      contactPhone
    })
      .then((res) => {
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || '提交申请失败');
        }
        wx.showToast({ title: '申请已提交', icon: 'success' });
        setTimeout(() => {
          router.navigateBack();
        }, 600);
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '提交申请失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  }
});
