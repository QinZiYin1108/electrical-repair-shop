<template>
  <view class="page worker-funds">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#0f172a" />
      </view>
      <view class="nav-center">
        <text class="nav-title">资金管理</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view scroll-y class="page-scroll">
      <view class="hero-card">
        <view class="hero-top">
          <view>
            <text class="hero-label">可提现余额</text>
            <text class="hero-value">￥{{ summaryDisplay.balance }}</text>
          </view>
          <view class="hero-badge">
            <u-icon name="red-packet-fill" size="15" color="#ffffff" />
            <text class="hero-badge-text">只读预览</text>
          </view>
        </view>
        <view class="hero-stats">
          <view class="hero-stat-item">
            <text class="hero-stat-label">冻结资金</text>
            <text class="hero-stat-value">￥{{ summaryDisplay.frozenBalance }}</text>
          </view>
          <view class="hero-stat-item">
            <text class="hero-stat-label">累计收入</text>
            <text class="hero-stat-value">￥{{ summaryDisplay.totalIncome }}</text>
          </view>
          <view class="hero-stat-item">
            <text class="hero-stat-label">累计支出</text>
            <text class="hero-stat-value">￥{{ summaryDisplay.totalExpense }}</text>
          </view>
        </view>
        <text class="hero-tip">订单完成满7天后金额才会转入可提现余额；提现能力待接入正式申请、审核和打款链路。</text>
      </view>

      <view class="card withdraw-card">
        <view class="section-head">
          <view>
            <text class="section-title">提现能力</text>
            <text class="section-subtitle">当前仅展示可提现余额，正式提现将在接入申请、审核和打款链路后开放</text>
          </view>
          <view class="section-icon soft-green">
            <u-icon name="red-packet-fill" size="18" color="#16a34a" />
          </view>
        </view>

        <view class="method-grid">
          <view
            v-for="item in withdrawMethods"
            :key="item.id"
            class="method-card"
            :class="{ 'method-card-active': selectedWithdrawMethod === item.id }"
            @click="selectedWithdrawMethod = item.id"
          >
            <view class="method-icon" :style="{ background: item.bg }">
              <u-icon :name="item.icon" size="18" :color="item.color" />
            </view>
            <view class="method-body">
              <text class="method-name">{{ item.name }}</text>
              <text class="method-desc">{{ item.desc }}</text>
            </view>
            <view class="method-check">
              <u-icon
                :name="selectedWithdrawMethod === item.id ? 'checkmark-circle-fill' : 'circle'"
                size="18"
                :color="selectedWithdrawMethod === item.id ? item.color : '#cbd5e1'"
              />
            </view>
          </view>
        </view>

        <view class="amount-panel">
          <text class="amount-label">提现金额</text>
          <view class="amount-input-row">
            <text class="currency">￥</text>
            <u-input
              v-model="withdrawAmount"
              type="number"
              placeholder="请输入提现金额"
              border="none"
              fontSize="34rpx"
              input-align="left"
              placeholderStyle="color: #94a3b8"
            />
            <view class="quick-action" @click="fillAll">
              <text class="quick-action-text">全部提现</text>
            </view>
          </view>
          <view class="amount-meta">
            <text class="amount-meta-text">可提现：￥{{ summaryDisplay.balance }}</text>
            <text class="amount-meta-text">方式：{{ currentMethod.name }}</text>
          </view>
        </view>

        <view class="cta-row">
          <u-button
            text="正式提现待接入"
            type="primary"
            shape="circle"
            @click="onWithdrawConfirm"
          />
        </view>
      </view>

      <view class="card flow-entry-card" @click="goFundFlows">
        <view class="flow-entry-main">
          <view class="section-icon soft-violet">
            <u-icon name="clock-fill" size="18" color="#7c3aed" />
          </view>
          <view class="flow-entry-body">
            <text class="section-title compact">资金流水</text>
            <text class="section-subtitle">点击查看收入、提现等全部明细记录</text>
          </view>
        </view>
        <view class="flow-entry-action">
          <text class="flow-entry-link">查看明细</text>
          <u-icon name="arrow-right" size="16" color="#64748b" />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getWorkerFundsSummary } from '@/api/workerFunds';
import { formatMoney, safeToNumber } from '@/utils/funds';

const WITHDRAW_METHODS = [
  {
    id: 'wechat',
    name: '微信',
    desc: '提现到微信零钱',
    icon: 'weixin-circle-fill',
    color: '#16a34a',
    bg: 'linear-gradient(135deg, rgba(22, 163, 74, 0.12) 0%, rgba(34, 197, 94, 0.22) 100%)'
  },
  {
    id: 'alipay',
    name: '支付宝',
    desc: '提现到支付宝账户',
    icon: 'zhifubao-circle-fill',
    color: '#1677ff',
    bg: 'linear-gradient(135deg, rgba(22, 119, 255, 0.12) 0%, rgba(56, 189, 248, 0.22) 100%)'
  }
];

export default {
  name: 'WorkerFundsPage',
  data() {
    return {
      loading: false,
      withdrawAmount: '',
      selectedWithdrawMethod: 'wechat',
      withdrawMethods: WITHDRAW_METHODS,
      summaryDisplay: {
        balance: '0.00',
        frozenBalance: '0.00',
        totalIncome: '0.00',
        totalExpense: '0.00'
      }
    };
  },
  computed: {
    currentMethod() {
      return this.withdrawMethods.find((item) => item.id === this.selectedWithdrawMethod) || this.withdrawMethods[0];
    }
  },
  onShow() {
    this.reloadSummary();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    goFundFlows() {
      uni.navigateTo({
        url: '/pages/funds/flows'
      });
    },
    async reloadSummary() {
      this.loading = true;
      try {
        const res = await getWorkerFundsSummary();
        if (res && res.code === 200) {
          const data = res.data || {};
          this.summaryDisplay = {
            balance: formatMoney(data.balance || '0.00'),
            frozenBalance: formatMoney(data.frozenBalance || '0.00'),
            totalIncome: formatMoney(data.totalIncome || '0.00'),
            totalExpense: formatMoney(data.totalExpense || '0.00')
          };
        }
      } finally {
        this.loading = false;
      }
    },
    availableBalanceNumber() {
      return safeToNumber(this.summaryDisplay.balance);
    },
    fillAll() {
      this.withdrawAmount = formatMoney(this.availableBalanceNumber());
    },
    onWithdrawConfirm() {
      uni.showToast({
        title: '正式提现待接入',
        icon: 'none'
      });
    }
  }
};
</script>

<style scoped>
.worker-funds {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(22, 163, 74, 0.1) 0, rgba(22, 163, 74, 0) 28%),
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12) 0, rgba(59, 130, 246, 0) 32%),
    linear-gradient(180deg, #f1f6ff 0%, #f8fbff 40%, #f5f7fb 100%);
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

.hero-card,
.card {
  border-radius: 28rpx;
  box-sizing: border-box;
}

.hero-card {
  padding: 28rpx 24rpx;
  background: linear-gradient(135deg, #155eef 0%, #0ea5e9 48%, #14b8a6 100%);
  box-shadow: 0 18rpx 40rpx rgba(21, 94, 239, 0.18);
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.hero-label,
.hero-tip,
.hero-stat-label,
.hero-badge-text {
  color: rgba(255, 255, 255, 0.8);
}

.hero-label {
  display: block;
  font-size: 24rpx;
}

.hero-value {
  display: block;
  margin-top: 12rpx;
  font-size: 54rpx;
  font-weight: 700;
  line-height: 1;
  color: #ffffff;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
}

.hero-badge-text {
  font-size: 22rpx;
}

.hero-stats {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.hero-stat-item {
  padding: 18rpx 16rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.16);
}

.hero-stat-label {
  display: block;
  font-size: 20rpx;
}

.hero-stat-value {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #ffffff;
  line-height: 1.35;
}

.hero-tip {
  display: block;
  margin-top: 18rpx;
  font-size: 22rpx;
  line-height: 1.5;
}

.card {
  margin-top: 18rpx;
  padding: 24rpx 22rpx;
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14rpx;
}

.section-title {
  display: block;
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}

.section-title.compact {
  font-size: 28rpx;
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.5;
}

.section-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.soft-green {
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.12) 0%, rgba(74, 222, 128, 0.22) 100%);
}

.soft-violet {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12) 0%, rgba(168, 85, 247, 0.22) 100%);
}

.method-grid {
  display: grid;
  gap: 14rpx;
  margin-top: 22rpx;
}

.method-card {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx 18rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.96);
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
}

.method-card-active {
  border-color: rgba(59, 130, 246, 0.45);
  box-shadow: 0 10rpx 24rpx rgba(59, 130, 246, 0.08);
}

.method-icon {
  width: 66rpx;
  height: 66rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.method-body {
  flex: 1;
  min-width: 0;
}

.method-name {
  display: block;
  color: #0f172a;
  font-size: 26rpx;
  font-weight: 600;
}

.method-desc {
  display: block;
  margin-top: 4rpx;
  color: #64748b;
  font-size: 22rpx;
}

.method-check {
  flex-shrink: 0;
}

.amount-panel {
  margin-top: 20rpx;
  padding: 20rpx 18rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
}

.amount-label {
  display: block;
  color: #475569;
  font-size: 22rpx;
}

.amount-input-row {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.currency {
  color: #0f172a;
  font-size: 38rpx;
  font-weight: 700;
}

.quick-action {
  flex-shrink: 0;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(22, 119, 255, 0.08);
}

.quick-action-text {
  color: #1677ff;
  font-size: 22rpx;
  font-weight: 600;
}

.amount-meta {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx 18rpx;
}

.amount-meta-text {
  color: #64748b;
  font-size: 22rpx;
}

.cta-row {
  margin-top: 22rpx;
}

.flow-entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.flow-entry-main {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-width: 0;
}

.flow-entry-body {
  min-width: 0;
}

.flow-entry-action {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
}

.flow-entry-link {
  color: #475569;
  font-size: 22rpx;
  font-weight: 600;
}
</style>
