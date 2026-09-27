const { getMyReports } = require("../../api/report");

const STATUS_MAP = {
  1: { text: '待处理', color: 'warning' },
  2: { text: '处理中', color: 'primary' },
  3: { text: '已成立', color: 'success' },
  4: { text: '已驳回', color: 'danger' }
};

const TARGET_TYPE_MAP = {
  1: '账号',
  2: '门店',
  3: '订单',
  4: '商品'
};

Page({
  data: {
    reports: [],
    loading: false,
    page: 1,
    size: 20,
    total: 0,
    hasMore: false
  },

  onShow() {
    this.setData({ page: 1, reports: [] });
    this.loadReports();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadReports();
    }
  },

  loadReports() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    getMyReports({ page: this.data.page, size: this.data.size })
      .then(res => {
        if (res && res.code === 200 && res.data) {
          const records = (res.data.records || []).map(r => ({
            ...r,
            statusText: STATUS_MAP[r.status]?.text || '未知',
            statusColor: STATUS_MAP[r.status]?.color || '',
            targetTypeText: TARGET_TYPE_MAP[r.targetType] || '未知',
            createdTimeText: this.formatTime(r.createdTime)
          }));
          this.setData({
            reports: this.data.page === 1 ? records : [...this.data.reports, ...records],
            total: res.data.total || 0,
            hasMore: (this.data.page * this.data.size) < (res.data.total || 0),
            page: this.data.page + 1
          });
        }
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  formatTime(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${day} ${h}:${min}`;
  }
});
