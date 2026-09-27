const { getCreditScore, getCreditHistory } = require("../../api/credit");

const CHANGE_TYPE_MAP = {
  1: { text: '违规扣分', color: 'danger' },
  2: { text: '自动恢复', color: 'success' },
  3: { text: '举报奖励', color: 'warning' },
  4: { text: '完单恢复', color: 'success' },
  5: { text: '申诉恢复', color: 'primary' }
};

Page({
  data: {
    creditScore: null,
    scoreStatus: '',
    scoreStatusText: '',
    scoreClass: '',
    historyList: [],
    loading: false,
    historyLoading: false,
    page: 1,
    size: 20,
    total: 0,
    hasMore: false
  },

  onShow() {
    this.loadScore();
    this.setData({ page: 1, historyList: [] });
    this.loadHistory();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.historyLoading) {
      this.loadHistory();
    }
  },

  loadScore() {
    getCreditScore()
      .then(res => {
        if (res && res.code === 200 && res.data) {
          const score = res.data.creditScore;
          const status = res.data.status;
          this.setData({
            creditScore: score,
            scoreStatus: status,
            scoreStatusText: status === 'normal' ? '正常' : status === 'warning' ? '风险观察' : '已限制',
            scoreClass: status === 'normal' ? 'score-normal' : status === 'warning' ? 'score-warning' : 'score-danger'
          });
        }
      })
      .catch(() => {});
  },

  loadHistory() {
    if (this.data.historyLoading) return;
    this.setData({ historyLoading: true });

    getCreditHistory({ page: this.data.page, size: this.data.size })
      .then(res => {
        if (res && res.code === 200 && res.data) {
          const records = (res.data.records || []).map(r => ({
            ...r,
            changeTypeText: CHANGE_TYPE_MAP[r.changeType]?.text || '未知',
            changeTypeColor: CHANGE_TYPE_MAP[r.changeType]?.color || '',
            timeText: this.formatTime(r.createdTime),
            isPositive: r.scoreChange >= 0
          }));
          this.setData({
            historyList: this.data.page === 1 ? records : [...this.data.historyList, ...records],
            total: res.data.total || 0,
            hasMore: (this.data.page * this.data.size) < (res.data.total || 0),
            page: this.data.page + 1
          });
        }
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ historyLoading: false });
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
