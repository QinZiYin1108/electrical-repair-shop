<template>
  <view class="page">
    <view class="header">
      <view class="header-left" @click="goBack"><u-icon name="arrow-left" size="20" /></view>
      <view class="header-center"><text class="header-title">我的举报</text></view>
      <view class="header-right"></view>
    </view>

    <view v-if="loading && list.length === 0" class="loading-area">
      <u-loading-icon size="30" />
    </view>

    <block v-else-if="list.length > 0">
      <view v-for="item in list" :key="item.id" class="card report-card">
        <view class="report-header">
          <view class="report-target">
            <text class="target-label">{{ targetTypeText(item.targetType) }}</text>
            <text class="target-id">{{ item.targetId }}</text>
          </view>
          <u-tag :text="statusText(item.status)" :type="statusTag(item.status)" size="mini" />
        </view>
        <view class="report-body">
          <view class="info-row"
            ><text class="info-label">原因：</text
            ><text class="info-value">{{ item.reasonCategory }}</text></view
          >
          <view v-if="item.description" class="info-row"
            ><text class="info-label">说明：</text
            ><text class="info-value">{{ item.description }}</text></view
          >
          <view v-if="item.result" class="info-row"
            ><text class="info-label">结果：</text
            ><text class="info-value result-text">{{ item.result }}</text></view
          >
        </view>
        <view class="report-footer"
          ><text class="time-text">{{ formatTime(item.createdTime) }}</text></view
        >
      </view>
      <view v-if="loadingMore" class="loading-more"><u-loading-icon size="20" /></view>
      <view v-else-if="!hasMore" class="no-more">— 没有更多了 —</view>
    </block>

    <u-empty v-else text="暂无举报记录" mode="list" />
  </view>
</template>

<script>
import { getMyReports } from '@/api/workerReport';

export default {
  name: 'WorkerMyReports',
  data() {
    return {
      list: [],
      loading: false,
      loadingMore: false,
      page: 1,
      size: 20,
      total: 0,
      hasMore: false
    };
  },
  onShow() {
    this.page = 1;
    this.list = [];
    this.loadList();
  },
  onReachBottom() {
    if (this.hasMore && !this.loadingMore) this.loadList();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadList() {
      if (this.loading) return;
      this.loading = true;
      this.loadingMore = this.page > 1;
      getMyReports({ page: this.page, size: this.size })
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.list =
              this.page === 1
                ? res.data.records || []
                : [...this.list, ...(res.data.records || [])];
            this.total = res.data.total || 0;
            this.hasMore = this.page * this.size < this.total;
            this.page++;
          }
        })
        .finally(() => {
          this.loading = false;
          this.loadingMore = false;
        });
    },
    statusText(s) {
      return s === 1
        ? '待处理'
        : s === 2
          ? '处理中'
          : s === 3
            ? '已成立'
            : s === 4
              ? '已驳回'
              : '未知';
    },
    statusTag(s) {
      return s === 1
        ? 'warning'
        : s === 2
          ? 'primary'
          : s === 3
            ? 'success'
            : s === 4
              ? 'error'
              : '';
    },
    targetTypeText(t) {
      return t === 1 ? '账号' : t === 2 ? '门店' : t === 3 ? '订单' : t === 4 ? '商品' : '未知';
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
.card {
  margin: 16rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 20rpx;
}
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.report-target {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.target-label {
  font-size: 24rpx;
  color: #909399;
}
.target-id {
  font-size: 26rpx;
  color: #303133;
  font-weight: 500;
}
.report-body {
  padding: 16rpx 0;
}
.info-row {
  display: flex;
  margin-bottom: 8rpx;
}
.info-label {
  font-size: 26rpx;
  color: #909399;
  flex-shrink: 0;
}
.info-value {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
  word-break: break-all;
}
.result-text {
  color: #67c23a;
}
.report-footer {
  padding-top: 12rpx;
  border-top: 1rpx solid #f0f0f0;
}
.time-text {
  font-size: 24rpx;
  color: #c0c4cc;
}
</style>
