<template>
  <view class="page store-address-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">门店地址</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <text class="card-title">设置门店地址</text>
      <text class="card-desc">请前往门店所在地，获取当前位置作为门店地址</text>

      <view class="locate-btn" @click="locateCurrentPosition">
        <u-icon name="map" size="20" color="#ffffff" />
        <text class="locate-text">获取当前位置</text>
      </view>

      <view v-if="loading" class="loading-text">正在获取位置...</view>

      <view v-if="selectedAddress" class="selected-info">
        <text class="addr-label">当前位置</text>
        <text class="addr-value">{{ selectedAddress }}</text>
        <text class="addr-coord">{{ latitude.toFixed(6) }}, {{ longitude.toFixed(6) }}</text>
      </view>
      <view v-else-if="!loading" class="empty-info">
        <text class="empty-text">点击上方按钮获取当前门店位置</text>
      </view>
    </view>

    <view class="submit-wrap">
      <u-button
        type="primary"
        :loading="saving"
        :disabled="!selectedAddress"
        shape="circle"
        @click="handleSave"
      >
        保存为门店地址
      </u-button>
    </view>
  </view>
</template>

<script>
import { setStoreAddress } from '@/api/workerLocation';

export default {
  name: 'StoreAddressPage',
  data() {
    return {
      latitude: 0,
      longitude: 0,
      selectedAddress: '',
      loading: false,
      saving: false
    };
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },

    locateCurrentPosition() {
      this.loading = true;
      this.selectedAddress = '';
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.latitude = res.latitude;
          this.longitude = res.longitude;
          // 不调第三方地图API，直接显示坐标，等后端保存后才解析地址
          this.selectedAddress = res.latitude.toFixed(6) + ', ' + res.longitude.toFixed(6);
          this.loading = false;
        },
        fail: () => {
          this.loading = false;
          uni.showToast({ title: '定位失败，请检查定位权限', icon: 'none' });
        }
      });
    },

    async handleSave() {
      if (!this.latitude || !this.longitude) {
        uni.showToast({ title: '请先获取当前位置', icon: 'none' });
        return;
      }
      this.saving = true;
      try {
        const res = await setStoreAddress({
          latitude: this.latitude,
          longitude: this.longitude,
          coordType: 'gcj02ll'
        });
        if (res && res.code === 200) {
          // 后端返回了逆地理编码后的地址
          const addr = res.data && res.data.address;
          uni.showToast({ title: '门店地址已保存', icon: 'success' });
          setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        } else {
          uni.showToast({ title: (res && res.message) || '保存失败', icon: 'none' });
        }
      } catch (e) {
        uni.showToast({ title: '保存失败，请重试', icon: 'none' });
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>

<style scoped>
.store-address-page {
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
  padding: 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
}

.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}

.locate-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 32rpx 0;
  padding: 24rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #1677ff, #409eff);
  box-shadow: 0 8rpx 24rpx rgba(22, 119, 255, 0.3);
}

.locate-text {
  margin-left: 12rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
}

.loading-text {
  text-align: center;
  font-size: 26rpx;
  color: #909399;
  margin-top: 16rpx;
}

.selected-info {
  margin-top: 20rpx;
  padding: 16rpx;
  background-color: #f0f9eb;
  border-radius: 12rpx;
}

.addr-label {
  font-size: 22rpx;
  color: #67c23a;
}

.addr-value {
  display: block;
  margin-top: 6rpx;
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
}

.addr-coord {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #909399;
}

.empty-info {
  margin-top: 20rpx;
  padding: 16rpx;
  background-color: #fdf6ec;
  border-radius: 12rpx;
}

.empty-text {
  font-size: 24rpx;
  color: #e6a23c;
}

.submit-wrap {
  margin: 40rpx 24rpx;
}
</style>
