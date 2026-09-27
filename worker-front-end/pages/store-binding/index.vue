<template>
  <view class="page binding-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center"><text class="nav-title">门店绑定</text></view>
      <view class="nav-right" />
    </view>

    <view v-if="loading" class="loading-wrap"><text>加载中...</text></view>

    <view v-else-if="binding" class="card">
      <view class="binding-header">
        <text class="binding-title">{{ statusTitle }}</text>
        <text class="binding-desc">{{ statusDesc }}</text>
      </view>

      <!-- 门店详情（绑定成功后展示） -->
      <view v-if="storeDetail" class="store-detail-block">
        <view class="store-name-row">
          <text class="store-name">{{ storeDetail.name }}</text>
          <view :class="['status-chip', storeDetail.isOnline === 1 ? 'chip-open' : 'chip-closed']">
            <text class="status-dot-text">{{
              storeDetail.isOnline === 1 ? '营业中' : '已打烊'
            }}</text>
          </view>
        </view>
        <view v-if="todayHours" class="today-hours-row">
          <text class="hours-label">今日营业</text>
          <text class="hours-value">{{ todayHoursText }}</text>
        </view>
        <view v-if="storeDetail.address" class="store-addr-row">
          <text class="addr-label">地址</text>
          <text class="addr-value">{{ storeDetail.address }}</text>
        </view>
      </view>

      <view class="binding-info">
        <text class="info-label">绑定时间</text>
        <text class="info-value">{{ formatTime(binding.invitedTime) }}</text>
      </view>

      <view class="binding-actions">
        <u-button v-if="binding.status === 1" type="primary" shape="circle" @click="handleAccept"
          >接受邀请</u-button
        >
        <u-button
          v-if="binding.status === 1"
          type="default"
          shape="circle"
          style="margin-top: 16rpx"
          @click="handleReject"
          >拒绝</u-button
        >
        <u-button
          v-if="binding.status === 2"
          type="warning"
          shape="circle"
          @click="handleRequestUnbind"
          >申请解绑</u-button
        >
      </view>
    </view>

    <view v-else class="card">
      <view class="binding-header">
        <text class="binding-title">未绑定门店</text>
        <text class="binding-desc">当前没有门店邀请或绑定记录</text>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getWorkerBindingStatus,
  acceptBinding,
  rejectBinding,
  requestUnbind,
  fetchPublicStoreDetail
} from '@/api/workerBinding';

export default {
  name: 'StoreBindingPage',
  data() {
    return { binding: null, storeDetail: null, loading: true };
  },
  computed: {
    statusTitle() {
      if (this.binding?.status === 1) return '待确认邀请';
      if (this.binding?.status === 2) return '已绑定';
      if (this.binding?.status === 3) return '解绑申请中';
      return '';
    },
    statusDesc() {
      if (this.binding?.status === 1) return '门店已向您发出绑定邀请，请确认';
      if (this.binding?.status === 2) return '您已绑定门店，可申请解绑';
      if (this.binding?.status === 3) return '解绑申请已提交，等待门店确认';
      return '';
    },
    todayHours() {
      if (!this.storeDetail?.businessHours?.length) return null;
      const dow = new Date().getDay();
      const dayOfWeek = dow === 0 ? 7 : dow;
      return this.storeDetail.businessHours.find((h) => h.dayOfWeek === dayOfWeek) || null;
    },
    todayHoursText() {
      const h = this.todayHours;
      if (!h) return '';
      if (h.isAvailable === 0) return '今日休息';
      return `${h.startTime} - ${h.endTime}`;
    }
  },
  onShow() {
    this.loadStatus();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    async loadStatus() {
      this.loading = true;
      this.storeDetail = null;
      try {
        const res = await getWorkerBindingStatus();
        this.binding = res?.data || null;
        if (this.binding?.storeId) {
          this.loadStoreDetail(this.binding.storeId);
        }
      } catch (e) {
        this.binding = null;
      } finally {
        this.loading = false;
      }
    },
    async loadStoreDetail(storeId) {
      try {
        const res = await fetchPublicStoreDetail(storeId);
        if (res?.code === 200 && res?.data) {
          this.storeDetail = res.data;
        }
      } catch (e) {
        /* 静默处理 */
      }
    },
    async handleAccept() {
      uni.showModal({
        title: '确认绑定',
        content: '接受后您将正式加入该门店',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await acceptBinding();
            uni.showToast({ title: '绑定成功', icon: 'success' });
            this.loadStatus();
          } catch (e) {
            uni.showToast({ title: '操作失败', icon: 'none' });
          }
        }
      });
    },
    async handleReject() {
      try {
        await rejectBinding();
        uni.showToast({ title: '已拒绝', icon: 'none' });
        this.loadStatus();
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' });
      }
    },
    async handleRequestUnbind() {
      uni.showModal({
        title: '申请解绑',
        content: '提交申请后需等待门店管理员确认',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await requestUnbind();
            uni.showToast({ title: '已提交申请', icon: 'success' });
            this.loadStatus();
          } catch (e) {
            uni.showToast({ title: '操作失败', icon: 'none' });
          }
        }
      });
    },
    formatTime(ts) {
      if (!ts) return '';
      const d = new Date(ts);
      const pad = (n) => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
  }
};
</script>

<style scoped>
.binding-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}
.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  background: #fff;
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
.loading-wrap {
  text-align: center;
  padding: 100rpx 0;
  color: #909399;
}
.card {
  margin: 16rpx 24rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #fff;
}
.binding-header {
  margin-bottom: 24rpx;
}
.binding-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}
.binding-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}
.binding-info {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.info-label {
  font-size: 26rpx;
  color: #909399;
}
.info-value {
  font-size: 26rpx;
  color: #303133;
}
.binding-actions {
  margin-top: 32rpx;
}

/* 门店详情块 */
.store-detail-block {
  background: #f8fafc;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.store-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.store-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
  margin-right: 12rpx;
}
.status-chip {
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 500;
}
.chip-open {
  background: #f0fdf4;
  color: #16a34a;
}
.chip-closed {
  background: #fef2f2;
  color: #dc2626;
}
.status-dot-text {
  font-size: 22rpx;
}
.today-hours-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}
.hours-label {
  font-size: 24rpx;
  color: #64748b;
  width: 80rpx;
}
.hours-value {
  font-size: 24rpx;
  color: #1e293b;
  font-weight: 500;
}
.store-addr-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}
.addr-label {
  font-size: 24rpx;
  color: #64748b;
  width: 80rpx;
  flex-shrink: 0;
}
.addr-value {
  font-size: 24rpx;
  color: #475569;
  line-height: 1.5;
  flex: 1;
}
</style>
