const userFundsApi = require('../../api/userFunds');

const PAYMENT_METHODS = [
  { id: 1, name: '微信支付', desc: '推荐使用微信完成充值', iconText: '微', activeClass: 'wechat-active' },
  { id: 2, name: '支付宝', desc: '适合使用支付宝快速充值', iconText: '支', activeClass: 'alipay-active' }
];

const QUICK_AMOUNTS = [50, 100, 200, 500, 1000, 2000];

function safeToNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function formatMoney(num) {
  return safeToNumber(num).toFixed(2);
}

function formatTimeText(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

function normalizeSummary(data) {
  const safeData = data || {};
  return {
    balance: formatMoney(safeData.balance),
    frozenBalance: formatMoney(safeData.frozenBalance),
    totalIncome: formatMoney(safeData.totalIncome),
    totalExpense: formatMoney(safeData.totalExpense)
  };
}

Page({
  data: {
    loading: false,
    submitting: false,
    summaryDisplay: normalizeSummary(),
    lastUpdatedText: '-',
    paymentMethods: PAYMENT_METHODS,
    quickAmounts: QUICK_AMOUNTS,
    selectedPaymentMethod: PAYMENT_METHODS[0].id,
    selectedAmount: QUICK_AMOUNTS[1],
    customAmount: ''
  },

  onShow() {
    this.reloadSummary();
  },

  async reloadSummary() {
    this.setData({ loading: true });
    try {
      const resp = await userFundsApi.getUserFundsSummary();
      if (resp && resp.code === 200) {
        this.setData({
          summaryDisplay: normalizeSummary(resp.data),
          lastUpdatedText: formatTimeText(Date.now())
        });
        return;
      }
      wx.showToast({
        title: (resp && resp.message) || '加载钱包失败',
        icon: 'none'
      });
    } catch (e) {
      wx.showToast({
        title: '加载钱包失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  onNavigateFlowPage() {
    wx.navigateTo({
      url: '/pages/funds-flow/index'
    });
  },

  onSelectPaymentMethod(e) {
    const paymentMethod = Number(e.currentTarget.dataset.id || 0);
    if (!paymentMethod || this.data.submitting) {
      return;
    }
    this.setData({ selectedPaymentMethod: paymentMethod });
  },

  onQuickAmountTap(e) {
    if (this.data.submitting) {
      return;
    }
    const amount = Number(e.currentTarget.dataset.amount || 0);
    if (!amount) {
      return;
    }
    this.setData({
      selectedAmount: amount,
      customAmount: ''
    });
  },

  onCustomAmountInput(e) {
    const value = (e.detail.value || '').replace(/[^\d.]/g, '');
    this.setData({
      customAmount: value,
      selectedAmount: 0
    });
  },

  resolveRechargeAmount() {
    if (this.data.customAmount) {
      return Number(this.data.customAmount);
    }
    return Number(this.data.selectedAmount || 0);
  },

  async onRechargeConfirm() {
    if (this.data.submitting) {
      return;
    }
    const amount = this.resolveRechargeAmount();
    if (!Number.isFinite(amount) || amount <= 0) {
      wx.showToast({
        title: '请输入正确的充值金额',
        icon: 'none'
      });
      return;
    }

    this.setData({ submitting: true });
    try {
      const resp = await userFundsApi.rechargeUserFunds({
        amount: amount.toFixed(2),
        paymentMethod: this.data.selectedPaymentMethod
      });
      if (!resp || resp.code !== 200) {
        wx.showToast({
          title: (resp && resp.message) || '充值失败',
          icon: 'none'
        });
        return;
      }
      this.setData({
        summaryDisplay: normalizeSummary(resp.data),
        lastUpdatedText: formatTimeText(Date.now())
      });
      wx.showToast({
        title: '充值成功',
        icon: 'success'
      });
    } catch (e) {
      wx.showToast({
        title: '充值失败',
        icon: 'none'
      });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
