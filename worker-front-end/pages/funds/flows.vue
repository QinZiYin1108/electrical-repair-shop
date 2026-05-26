<template>
  <view class="page worker-fund-flows">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#0f172a" />
      </view>
      <view class="nav-center">
        <text class="nav-title">资金流水</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view scroll-y class="page-scroll">
      <view class="overview-card">
        <view class="overview-icon">
          <u-icon name="clock-fill" size="20" color="#7c3aed" />
        </view>
        <view class="overview-body">
          <text class="overview-title">资金明细记录</text>
          <text class="overview-subtitle">查看收入、提现等全部资金变动</text>
        </view>
      </view>

      <view class="tabs-card">
        <view
          v-for="item in tabs"
          :key="item.key"
          class="tab-item"
          :class="{ 'tab-item-active': activeTab === item.key }"
          @click="activeTab = item.key"
        >
          <text class="tab-text">{{ item.label }}</text>
        </view>
      </view>

      <view class="list-card">
        <view class="list-head">
          <text class="list-title">流水明细</text>
          <text class="list-subtitle">当前 {{ filteredFlows.length }} 条</text>
        </view>

        <view v-if="filteredFlows.length" class="flow-list">
          <view v-for="item in filteredFlows" :key="item.id" class="flow-item">
            <view class="flow-icon" :class="safeToNumber(item.flowType) === 2 ? 'flow-icon-expense' : 'flow-icon-income'">
              <u-icon
                :name="safeToNumber(item.flowType) === 2 ? 'arrow-upward' : 'arrow-downward'"
                size="16"
                :color="safeToNumber(item.flowType) === 2 ? '#ef4444' : '#16a34a'"
              />
            </view>
            <view class="flow-main">
              <view class="flow-top">
                <text class="flow-title">{{ item.description || '资金变动' }}</text>
                <text class="flow-amount" :class="amountClass(item.flowType)">{{ formatAmount(item.flowType, item.amount) }}</text>
              </view>
              <view class="flow-bottom">
                <text class="flow-meta">{{ flowDirection(item.flowType) }}</text>
                <text class="flow-meta">{{ formatTime(item.createdTime) }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty-state">
          <u-icon name="calendar" size="28" color="#cbd5e1" />
          <text class="empty-text">暂无符合条件的流水记录</text>
        </view>

        <view class="load-more">
          <u-button
            :text="noMore ? '没有更多了' : '加载更多'"
            size="small"
            :disabled="noMore"
            :loading="loadingMore"
            @click="loadMore"
          />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { listWorkerFundFlows } from '@/api/workerFunds';
import {
  fundAmountClass,
  formatFundAmount,
  formatFundTime,
  fundFlowDirection,
  safeToNumber
} from '@/utils/funds';

export default {
  name: 'WorkerFundFlowsPage',
  data() {
    return {
      loadingMore: false,
      noMore: false,
      pageNo: 1,
      pageSize: 20,
      activeTab: 'all',
      tabs: [
        { key: 'all', label: '全部' },
        { key: 'income', label: '收入' },
        { key: 'expense', label: '支出' }
      ],
      flows: []
    };
  },
  computed: {
    filteredFlows() {
      if (this.activeTab === 'income') {
        return this.flows.filter((item) => safeToNumber(item.flowType) !== 2);
      }
      if (this.activeTab === 'expense') {
        return this.flows.filter((item) => safeToNumber(item.flowType) === 2);
      }
      return this.flows;
    }
  },
  onShow() {
    this.reloadAll();
  },
  methods: {
    safeToNumber,
    goBack() {
      uni.navigateBack();
    },
    async reloadAll() {
      this.pageNo = 1;
      this.noMore = false;
      await this.loadFlows(true);
    },
    async loadFlows(reset) {
      const pageNo = reset ? 1 : this.pageNo;
      const res = await listWorkerFundFlows({
        pageNo,
        pageSize: this.pageSize
      });
      if (!res || res.code !== 200) return;
      const page = res.data || {};
      const serverFlows = page.records || page.list || [];
      this.flows = reset ? serverFlows : this.flows.concat(serverFlows);
      this.noMore = serverFlows.length < this.pageSize;
      this.pageNo = pageNo + 1;
    },
    loadMore() {
      if (this.loadingMore || this.noMore) return;
      this.loadingMore = true;
      this.loadFlows(false)
        .catch(() => {})
        .finally(() => {
          this.loadingMore = false;
        });
    },
    formatTime(value) {
      return formatFundTime(value);
    },
    formatAmount(flowType, amount) {
      return formatFundAmount(flowType, amount);
    },
    amountClass(flowType) {
      return fundAmountClass(flowType);
    },
    flowDirection(flowType) {
      return fundFlowDirection(flowType);
    }
  }
};
</script>

<style scoped>
.worker-fund-flows {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(124, 58, 237, 0.08) 0, rgba(124, 58, 237, 0) 24%),
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.1) 0, rgba(59, 130, 246, 0) 28%),
    linear-gradient(180deg, #f3f6ff 0%, #f9fbff 42%, #f5f7fb 100%);
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  background: rgba(248, 251, 255, 0.92);
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(16rpx);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-left,
.nav-right {
  width: 120rpx;
  display: flex;
  align-items: center;
}

.nav-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.page-scroll {
  box-sizing: border-box;
  padding: 18rpx 20rpx 36rpx;
}

.overview-card,
.tabs-card,
.list-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 22rpx 20rpx;
}

.overview-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12) 0%, rgba(168, 85, 247, 0.22) 100%);
}

.overview-body {
  min-width: 0;
}

.overview-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.overview-subtitle,
.list-subtitle,
.flow-meta,
.empty-text {
  color: #64748b;
  font-size: 22rpx;
}

.overview-subtitle {
  display: block;
  margin-top: 6rpx;
  line-height: 1.5;
}

.tabs-card {
  margin-top: 18rpx;
  padding: 10rpx;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10rpx;
}

.tab-item {
  height: 74rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-item-active {
  background: linear-gradient(135deg, #155eef 0%, #0ea5e9 100%);
  box-shadow: 0 10rpx 22rpx rgba(21, 94, 239, 0.16);
}

.tab-text {
  color: #475569;
  font-size: 24rpx;
  font-weight: 600;
}

.tab-item-active .tab-text {
  color: #ffffff;
}

.list-card {
  margin-top: 18rpx;
  padding: 22rpx 20rpx 18rpx;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.list-title {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}

.flow-list {
  margin-top: 14rpx;
}

.flow-item {
  display: flex;
  gap: 14rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.8);
}

.flow-item:last-child {
  border-bottom: none;
}

.flow-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.flow-icon-income {
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.12) 0%, rgba(74, 222, 128, 0.22) 100%);
}

.flow-icon-expense {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.12) 0%, rgba(248, 113, 113, 0.22) 100%);
}

.flow-main {
  flex: 1;
  min-width: 0;
}

.flow-top,
.flow-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.flow-title {
  flex: 1;
  color: #0f172a;
  font-size: 26rpx;
  font-weight: 600;
}

.flow-amount {
  flex-shrink: 0;
  font-size: 28rpx;
  font-weight: 700;
}

.is-income {
  color: #16a34a;
}

.is-expense {
  color: #ef4444;
}

.flow-bottom {
  margin-top: 8rpx;
}

.empty-state {
  padding: 48rpx 0 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.load-more {
  margin-top: 14rpx;
}
</style>
