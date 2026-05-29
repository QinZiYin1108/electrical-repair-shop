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

      <view class="binding-info">
        <text class="info-label">门店ID</text>
        <text class="info-value">{{ binding.storeId }}</text>
      </view>

      <view v-if="binding.invitedTime" class="binding-info">
        <text class="info-label">邀请时间</text>
        <text class="info-value">{{ formatTime(binding.invitedTime) }}</text>
      </view>

      <view class="binding-actions">
        <u-button v-if="binding.status === 1" type="primary" shape="circle" @click="handleAccept">接受邀请</u-button>
        <u-button v-if="binding.status === 1" type="default" shape="circle" style="margin-top:16rpx" @click="handleReject">拒绝</u-button>
        <u-button v-if="binding.status === 2" type="warning" shape="circle" @click="handleRequestUnbind">申请解绑</u-button>
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
import { getWorkerBindingStatus, acceptBinding, rejectBinding, requestUnbind } from '@/api/workerBinding';

export default {
  name: 'StoreBindingPage',
  data() {
    return { binding: null, loading: true };
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
    bindingStatusText() {
      if (!this.binding) return '未绑定';
      if (this.binding.status === 1) return '待确认';
      if (this.binding.status === 2) return '已绑定';
      if (this.binding.status === 3) return '待解绑';
      return '';
    }
  },
  onShow() { this.loadStatus(); },
  methods: {
    goBack() { uni.navigateBack(); },
    async loadStatus() {
      this.loading = true;
      try {
        const res = await getWorkerBindingStatus();
        this.binding = res?.data || null;
      } catch (e) { this.binding = null; }
      finally { this.loading = false; }
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
          } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }); }
        }
      });
    },
    async handleReject() {
      try {
        await rejectBinding();
        uni.showToast({ title: '已拒绝', icon: 'none' });
        this.loadStatus();
      } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }); }
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
          } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }); }
        }
      });
    },
    formatTime(ts) {
      if (!ts) return '';
      const d = new Date(ts);
      const pad = (n) => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
  }
};
</script>

<style scoped>
.binding-page { min-height: 100vh; background-color: #f5f5f5; }
.nav-bar { height: calc(88rpx + var(--status-bar-height)); padding: var(--status-bar-height) 24rpx 0; background:#fff; display:flex; align-items:center; justify-content:space-between; }
.nav-left,.nav-right { width:120rpx; display:flex; align-items:center; }
.nav-center { flex:1; display:flex; align-items:center; justify-content:center; }
.nav-title { font-size:32rpx; font-weight:600; color:#303133; }
.loading-wrap { text-align:center; padding:100rpx 0; color:#909399; }
.card { margin:16rpx 24rpx; padding:24rpx; border-radius:20rpx; background:#fff; }
.binding-header { margin-bottom:24rpx; }
.binding-title { display:block; font-size:32rpx; font-weight:600; color:#303133; }
.binding-desc { display:block; margin-top:8rpx; font-size:24rpx; color:#909399; }
.binding-info { display:flex; justify-content:space-between; padding:12rpx 0; border-bottom:1rpx solid #f0f0f0; }
.info-label { font-size:26rpx; color:#909399; }
.info-value { font-size:26rpx; color:#303133; }
.binding-actions { margin-top:32rpx; }
</style>
