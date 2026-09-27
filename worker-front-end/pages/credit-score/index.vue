<template>
  <view class="page">
    <view class="header">
      <view class="header-left" @click="goBack"><u-icon name="arrow-left" size="20" /></view>
      <view class="header-center"><text class="header-title">信用积分</text></view>
      <view class="header-right"></view>
    </view>

    <view class="score-card" :class="scoreClass">
      <text class="score-value">{{ creditScore !== null ? creditScore : '--' }}</text>
      <text class="score-label">当前信用积分</text>
      <text class="score-status">{{ scoreStatusText }}</text>
    </view>

    <view class="card tip-card">
      <view class="tip-row"
        ><view class="tip-dot tip-dot-normal" /><text class="tip-text"
          >≥60 分：正常使用全部功能</text
        ></view
      >
      <view class="tip-row"
        ><view class="tip-dot tip-dot-warning" /><text class="tip-text"
          >40-59 分：部分功能受限</text
        ></view
      >
      <view class="tip-row"
        ><view class="tip-dot tip-dot-danger" /><text class="tip-text"
          >&lt;40 分：账号已限制</text
        ></view
      >
    </view>

    <view class="history-section">
      <text class="section-title">积分变动记录</text>
      <view v-if="historyList.length > 0">
        <view v-for="item in historyList" :key="item.id" class="card history-card">
          <view class="history-header">
            <u-tag
              :text="changeTypeText(item.changeType)"
              :type="changeTypeTag(item.changeType)"
              size="mini"
            />
            <text :class="item.scoreChange >= 0 ? 'positive' : 'negative'" class="change-value">
              {{ item.scoreChange >= 0 ? '+' : '' }}{{ item.scoreChange }}
            </text>
          </view>
          <view class="history-row">
            <text class="h-label">变动前：</text>
            <text class="h-value">{{ item.scoreBefore }}</text>
            <text class="h-arrow">→</text>
            <text class="h-value" :class="scoreAfterClass(item.scoreAfter)">{{
              item.scoreAfter
            }}</text>
          </view>
          <view v-if="item.reason" class="history-row">
            <text class="h-label">原因：</text><text class="h-value">{{ item.reason }}</text>
          </view>
          <view class="history-time"
            ><text class="time-text">{{ formatTime(item.createdTime) }}</text></view
          >
        </view>
        <view v-if="historyLoadingMore" class="loading-more"><u-loading-icon size="20" /></view>
        <view v-else-if="!historyHasMore" class="no-more">— 没有更多了 —</view>
      </view>
      <view v-else-if="historyLoading" class="loading-area"><u-loading-icon size="30" /></view>
    </view>
  </view>
</template>

<script>
import { getCreditScore, getCreditHistory } from '@/api/workerCredit';

export default {
  name: 'WorkerCreditScore',
  data() {
    return {
      creditScore: null,
      scoreStatus: '',
      historyList: [],
      historyLoading: false,
      historyLoadingMore: false,
      historyPage: 1,
      historySize: 20,
      historyTotal: 0,
      historyHasMore: false
    };
  },
  computed: {
    scoreClass() {
      return this.scoreStatus === 'normal'
        ? 'score-normal'
        : this.scoreStatus === 'warning'
          ? 'score-warning'
          : 'score-danger';
    },
    scoreStatusText() {
      return this.scoreStatus === 'normal'
        ? '正常'
        : this.scoreStatus === 'warning'
          ? '风险观察'
          : '已限制';
    }
  },
  onShow() {
    this.loadScore();
    this.historyPage = 1;
    this.historyList = [];
    this.loadHistory();
  },
  onReachBottom() {
    if (this.historyHasMore && !this.historyLoadingMore) this.loadHistory();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadScore() {
      getCreditScore().then((res) => {
        if (res && res.code === 200 && res.data) {
          this.creditScore = res.data.creditScore;
          this.scoreStatus = res.data.status;
        }
      });
    },
    loadHistory() {
      if (this.historyLoading) return;
      this.historyLoading = true;
      this.historyLoadingMore = this.historyPage > 1;
      getCreditHistory({ page: this.historyPage, size: this.historySize })
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.historyList =
              this.historyPage === 1
                ? res.data.records || []
                : [...this.historyList, ...(res.data.records || [])];
            this.historyTotal = res.data.total || 0;
            this.historyHasMore = this.historyPage * this.historySize < this.historyTotal;
            this.historyPage++;
          }
        })
        .finally(() => {
          this.historyLoading = false;
          this.historyLoadingMore = false;
        });
    },
    changeTypeText(t) {
      return t === 1
        ? '违规扣分'
        : t === 2
          ? '自动恢复'
          : t === 3
            ? '举报奖励'
            : t === 4
              ? '完单恢复'
              : t === 5
                ? '申诉恢复'
                : '未知';
    },
    changeTypeTag(t) {
      return t === 1
        ? 'error'
        : t === 2
          ? 'success'
          : t === 3
            ? 'warning'
            : t === 4
              ? 'success'
              : t === 5
                ? 'primary'
                : '';
    },
    scoreAfterClass(s) {
      return s >= 60 ? 'after-normal' : s >= 40 ? 'after-warning' : 'after-danger';
    },
    formatTime(ts) {
      if (!ts) return '—';
      const d = new Date(ts);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}
.header {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-sizing: border-box;
}
.header-left,
.header-right {
  width: 120rpx;
  display: flex;
  align-items: center;
}
.header-center {
  flex: 1;
  text-align: center;
}
.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}
.loading-area {
  display: flex;
  justify-content: center;
  padding: 200rpx;
}
.loading-more {
  display: flex;
  justify-content: center;
  padding: 20rpx;
}
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #c0c4cc;
}
.score-card {
  margin: 24rpx;
  padding: 48rpx 32rpx;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}
.score-normal {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}
.score-warning {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}
.score-danger {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}
.score-value {
  font-size: 80rpx;
  font-weight: 700;
}
.score-label {
  font-size: 28rpx;
  opacity: 0.85;
  margin-top: 8rpx;
}
.score-status {
  font-size: 24rpx;
  opacity: 0.7;
  margin-top: 12rpx;
  padding: 6rpx 24rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999rpx;
}
.card {
  margin: 16rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 20rpx;
}
.tip-card {
  margin-top: 0;
}
.tip-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.tip-row:last-child {
  margin-bottom: 0;
}
.tip-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 12rpx;
  flex-shrink: 0;
}
.tip-dot-normal {
  background-color: #67c23a;
}
.tip-dot-warning {
  background-color: #e6a23c;
}
.tip-dot-danger {
  background-color: #f56c6c;
}
.tip-text {
  font-size: 24rpx;
  color: #606266;
}
.history-section {
  margin: 16rpx 24rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16rpx;
  display: block;
}
.history-card {
  margin: 0 0 16rpx 0;
}
.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.change-value {
  font-size: 36rpx;
  font-weight: 700;
}
.positive {
  color: #67c23a;
}
.negative {
  color: #f56c6c;
}
.history-row {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}
.h-label {
  font-size: 26rpx;
  color: #909399;
  flex-shrink: 0;
}
.h-value {
  font-size: 26rpx;
  color: #303133;
}
.h-arrow {
  margin: 0 8rpx;
  color: #c0c4cc;
}
.after-normal {
  color: #67c23a;
  font-weight: 600;
}
.after-warning {
  color: #e6a23c;
  font-weight: 600;
}
.after-danger {
  color: #f56c6c;
  font-weight: 600;
}
.history-time {
  padding-top: 12rpx;
  margin-top: 12rpx;
  border-top: 1rpx solid #f0f0f0;
}
.time-text {
  font-size: 24rpx;
  color: #c0c4cc;
}
</style>
