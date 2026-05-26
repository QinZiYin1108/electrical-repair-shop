const router = require('../../utils/router');
const { fetchUserWarrantyCardDetail } = require('../../api/userWarrantyCards');

function buildInfoRows(detail) {
  return [
    { label: '保修卡号', value: detail.cardNo || '-' },
    { label: '商品名称', value: detail.productName || '-' },
    { label: '商品型号', value: detail.productModel || '-' },
    { label: '购买日期', value: detail.purchaseDate || '-' },
    { label: '保修开始', value: detail.warrantyStartDate || '-' },
    { label: '保修截止', value: detail.warrantyEndDate || '-' },
    { label: '保修类型', value: detail.warrantyTypeText || '-' },
    { label: '当前状态', value: detail.warrantyStatusText || '-' },
    { label: '维修次数', value: String(detail.repairCount || 0) },
    { label: '最近维修', value: detail.lastRepairDate || '暂无' },
    { label: '剩余天数', value: `${detail.remainingDays || 0} 天` },
    { label: '待处理申请', value: String(detail.pendingUsageCount || 0) }
  ];
}

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(value) {
  const timestamp = Number(value || 0);
  if (!timestamp) {
    return '暂无';
  }
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function mapUsageRecord(item) {
  return {
    id: item.id || '',
    issueDescription: item.issueDescription || '暂无描述',
    contactName: item.contactName || '-',
    contactPhone: item.contactPhone || '-',
    statusText: item.statusText || '待处理',
    processRemark: item.processRemark || '',
    applyTimeText: formatDateTime(item.applyTime),
    processTimeText: item.processTime ? formatDateTime(item.processTime) : '暂无'
  };
}

Page({
  data: {
    loading: true,
    loadError: '',
    detail: null,
    infoRows: [],
    usageRecords: []
  },

  onLoad(options) {
    const id = options && options.id ? options.id : '';
    if (!id) {
      this.setData({ loading: false, loadError: '保修卡 ID 不存在' });
      return;
    }
    this.cardId = id;
    this.hasLoaded = false;
    this.loadDetail();
  },

  onPullDownRefresh() {
    this.loadDetail(true);
  },

  onShow() {
    if (this.cardId && this.hasLoaded) {
      this.loadDetail();
    }
  },

  loadDetail(fromPullDown) {
    this.setData({ loading: true, loadError: '' });
    fetchUserWarrantyCardDetail(this.cardId)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || '保修卡详情加载失败');
        }
        this.setData({
          loading: false,
          loadError: '',
          detail: res.data,
          infoRows: buildInfoRows(res.data),
          usageRecords: Array.isArray(res.data.usageRecords) ? res.data.usageRecords.map(mapUsageRecord) : []
        });
        this.hasLoaded = true;
      })
      .catch((err) => {
        this.setData({
          loading: false,
          loadError: (err && err.message) || '保修卡详情加载失败',
          detail: null,
          infoRows: [],
          usageRecords: []
        });
        this.hasLoaded = true;
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onRetryTap() {
    this.loadDetail();
  },

  onApplyTap() {
    if (!this.cardId || !this.data.detail || !this.data.detail.canApplyUsage) {
      return;
    }
    router.navigateTo({
      url: `/pages/warranty-card-usage-apply/index?id=${this.cardId}`
    });
  }
});
