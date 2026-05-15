<template>
  <view class="page worker-settings">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">设置</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <view class="row clickable" @click="goCertification">
        <text class="label">实名认证</text>
        <view class="right">
          <text class="status">{{ certificationStatusText }}</text>
          <u-icon name="arrow-right" size="16" color="#c0c4cc" />
        </view>
      </view>
      <view class="row clickable" @click="goWorkTimePage">
        <text class="label">工作时间设置</text>
        <view class="right">
          <u-icon name="arrow-right" size="16" color="#c0c4cc" />
        </view>
      </view>
      <view class="row clickable" @click="goSkillPage">
        <text class="label">技能管理</text>
        <view class="right">
          <u-icon name="arrow-right" size="16" color="#c0c4cc" />
        </view>
      </view>
    </view>

    <view class="card">
      <view v-if="canceling" class="row">
        <text class="label danger-text">账号注销中</text>
        <view class="right">
          <text class="status danger-text">{{ cancelDeadlineText }}</text>
        </view>
      </view>
      <view v-if="canceling" class="row clickable danger" @click="revokeCancel">
        <text class="label">撤销注销</text>
        <view class="right">
          <u-icon name="arrow-right" size="16" color="#f56c6c" />
        </view>
      </view>
      <view v-else class="row clickable danger" @click="applyCancel">
        <text class="label">注销账号</text>
        <view class="right">
          <u-icon name="arrow-right" size="16" color="#f56c6c" />
        </view>
      </view>
      <view class="row clickable danger" @click="logout">
        <text class="label">退出登录</text>
        <view class="right">
          <u-icon name="arrow-right" size="16" color="#f56c6c" />
        </view>
      </view>
    </view>

    <view class="brand-card">
      <view class="brand-head">
        <image class="brand-logo" src="/static/logo.png" mode="aspectFit" />
        <view class="brand-meta">
          <text class="brand-title">安修到家师傅端</text>
          <text class="brand-sub">面向维修师傅的接单与服务协同工具</text>
        </view>
      </view>
      <text class="brand-copy">支持接单、定位、服务进度、评价与资金管理，让服务过程更清楚，协作更顺畅。</text>
      <view class="brand-tags">
        <text class="brand-tag">接单更高效</text>
        <text class="brand-tag">服务更清楚</text>
        <text class="brand-tag">售后可追踪</text>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getWorkerAccountInfo,
  fetchWorkerAccountCancelStatus,
  applyWorkerAccountCancel,
  revokeWorkerAccountCancel
} from '@/api/workerAccount';

export default {
  name: 'WorkerSettingsPage',
  data() {
    return {
      accountStatus: null,
      canceling: false,
      cancelApplyTime: null,
      cancelDeadlineTime: null
    };
  },
  computed: {
    certificationStatusText() {
      if (this.accountStatus === 1) return '已认证';
      if (this.accountStatus === 2) return '未认证';
      if (this.accountStatus === 3) return '已冻结';
      if (this.accountStatus === 4) return '已离职';
      return '未知状态';
    },
    cancelDeadlineText() {
      if (!this.canceling || !this.cancelDeadlineTime) return '';
      return `反悔期至 ${this.formatTimestamp(this.cancelDeadlineTime)}`;
    }
  },
  onShow() {
    this.loadAccountInfo();
    this.loadCancelStatus();
  },
  methods: {
    formatTimestamp(ts) {
      if (!ts) return '';
      const d = new Date(ts);
      const pad = (n) => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    },
    goBack() {
      uni.navigateBack();
    },
    loadAccountInfo() {
      getWorkerAccountInfo()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.accountStatus = res.data.accountStatus ?? null;
          }
        })
        .catch(() => {});
    },
    loadCancelStatus() {
      fetchWorkerAccountCancelStatus()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.canceling = !!res.data.canceling;
            this.cancelApplyTime = res.data.cancelApplyTime ?? null;
            this.cancelDeadlineTime = res.data.cancelDeadlineTime ?? null;
          }
        })
        .catch(() => {});
    },
    applyCancel() {
      uni.showModal({
        title: '注销账号',
        content: '注销后将进入7天反悔期。\n7天内再次登录将取消注销，7天后账号将自动注销。\n确定要继续吗？',
        confirmText: '申请注销',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return;
          applyWorkerAccountCancel()
            .then((r) => {
              if (r && r.code === 200) {
                uni.showToast({
                  title: '已提交注销申请',
                  icon: 'success'
                });
                const app = getApp();
                if (app && app.globalData) {
                  app.globalData.workerIsLogin = false;
                  app.globalData.workerInfo = null;
                }
                uni.removeStorageSync('workerToken');
                uni.reLaunch({
                  url: '/pages/login/index'
                });
              } else {
                uni.showToast({
                  title: (r && r.message) || '提交失败',
                  icon: 'none'
                });
              }
            })
            .catch(() => {
              uni.showToast({
                title: '提交失败',
                icon: 'none'
              });
            });
        }
      });
    },
    revokeCancel() {
      uni.showModal({
        title: '撤销注销',
        content: '确定要撤销注销申请吗？撤销后账号恢复正常使用。',
        confirmText: '撤销',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return;
          revokeWorkerAccountCancel()
            .then((r) => {
              if (r && r.code === 200) {
                uni.showToast({
                  title: '已撤销注销',
                  icon: 'success'
                });
                this.loadCancelStatus();
              } else {
                uni.showToast({
                  title: (r && r.message) || '撤销失败',
                  icon: 'none'
                });
              }
            })
            .catch(() => {
              uni.showToast({
                title: '撤销失败',
                icon: 'none'
              });
            });
        }
      });
    },
    goCertification() {
      uni.navigateTo({
        url: '/pages/certification/index'
      });
    },
    goWorkTimePage() {
      uni.navigateTo({
        url: '/pages/work-time/index'
      });
    },
    goSkillPage() {
      uni.navigateTo({
        url: '/pages/skills/index'
      });
    },
    logout() {
      const app = getApp();
      uni.showModal({
        title: '提示',
        content: '确定要退出当前账号吗？',
        confirmText: '退出登录',
        cancelText: '取消',
        success(res) {
          if (!res.confirm) return;
          if (app && app.globalData) {
            app.globalData.workerIsLogin = false;
            app.globalData.workerInfo = null;
          }
          uni.removeStorageSync('workerToken');
          uni.reLaunch({
            url: '/pages/login/index'
          });
        }
      });
    }
  }
};
</script>

<style scoped>
.worker-settings {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  background-color: #ffffff;
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
  color: #303133;
}

.card {
  margin: 16rpx 24rpx 0;
  padding: 0 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  box-sizing: border-box;
}

.row {
  min-height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #f0f0f0;
}

.card .row:last-child {
  border-bottom: none;
}

.clickable {
  cursor: pointer;
}

.label {
  font-size: 28rpx;
  color: #303133;
}

.right {
  display: flex;
  align-items: center;
}

.status {
  margin-right: 10rpx;
  font-size: 24rpx;
  color: #909399;
}

.danger .label {
  color: #f56c6c;
}

.danger-text {
  color: #f56c6c;
}

.brand-card {
  margin: 20rpx 24rpx 0;
  padding: 28rpx 24rpx 30rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ffffff 0%, #f5fbff 100%);
  box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.05);
}

.brand-head {
  display: flex;
  align-items: center;
}

.brand-logo {
  width: 88rpx;
  height: 88rpx;
  flex-shrink: 0;
}

.brand-meta {
  margin-left: 18rpx;
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #12355b;
}

.brand-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #5f7b99;
}

.brand-copy {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #4b5563;
}

.brand-tags {
  margin-top: 18rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.brand-tag {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background-color: #eaf3ff;
  font-size: 20rpx;
  color: #1677ff;
}
</style>
