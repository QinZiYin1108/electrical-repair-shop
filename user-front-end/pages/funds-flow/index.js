const userFundsApi = require('../../api/userFunds');

function safeToNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function formatMoney(num) {
  return safeToNumber(num).toFixed(2);
}

function formatTimeText(value) {
  if (!value) return '';
  const ts = safeToNumber(value);
  if (!ts) return '';
  const date = new Date(ts);
  if (Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

function buildFlowDisplay(flow) {
  const safeFlow = flow || {};
  const flowType = safeToNumber(safeFlow.flowType);
  const amount = formatMoney(safeFlow.amount);
  return {
    ...safeFlow,
    createdTimeText: formatTimeText(safeFlow.createdTime),
    amountText: `${flowType === 2 ? '-' : '+'}${amount}`
  };
}

Page({
  data: {
    loading: false,
    loadingMore: false,
    noMore: false,
    pageNo: 1,
    pageSize: 20,
    flows: []
  },

  onShow() {
    this.reloadList();
  },

  async reloadList() {
    this.setData({
      loading: true,
      pageNo: 1,
      noMore: false,
      flows: []
    });
    try {
      await this.loadFlows(true);
    } finally {
      this.setData({
        loading: false
      });
    }
  },

  async loadFlows(reset) {
    if (reset) {
      this.setData({
        pageNo: 1,
        noMore: false
      });
    }

    const pageNo = reset ? 1 : this.data.pageNo;
    const resp = await userFundsApi.listUserFundFlows({
      pageNo,
      pageSize: this.data.pageSize
    });
    if (!resp || resp.code !== 200) {
      wx.showToast({
        title: (resp && resp.message) || '加载流水失败',
        icon: 'none'
      });
      return;
    }

    const page = resp.data || {};
    const serverFlows = (page.records || page.list || []).map(buildFlowDisplay);
    const nextFlows = reset ? serverFlows : this.data.flows.concat(serverFlows);

    this.setData({
      flows: nextFlows,
      pageNo: pageNo + 1,
      noMore: serverFlows.length < this.data.pageSize
    });
  },

  loadMore() {
    if (this.data.loadingMore || this.data.noMore) return;
    this.setData({ loadingMore: true });
    this.loadFlows(false)
      .catch(() => {
        wx.showToast({
          title: '加载流水失败',
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ loadingMore: false });
      });
  }
});
