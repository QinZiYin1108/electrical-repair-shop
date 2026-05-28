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
      <text class="card-desc">请在地图上选择门店所在位置</text>

      <!-- 搜索栏 -->
      <view class="search-box">
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="搜索地址关键词"
          @input="onSearchInput"
        />
        <view v-if="suggestions.length" class="suggestions-dropdown">
          <view
            v-for="(item, index) in suggestions"
            :key="index"
            class="suggestion-item"
            @click="selectSuggestion(item)"
          >
            <text class="sug-name">{{ item.name }}</text>
            <text class="sug-addr">{{ item.address }}</text>
          </view>
        </view>
      </view>

      <!-- 定位按钮 -->
      <view class="locate-btn" @click="locateCurrentPosition">
        <u-icon name="map" size="18" color="#1677ff" />
        <text class="locate-text">使用当前位置</text>
      </view>

      <!-- 地图 -->
      <map
        id="storeMap"
        class="store-map"
        :latitude="currentLat"
        :longitude="currentLng"
        :scale="16"
        :markers="markers"
        show-location
        @tap="onMapTap"
      />

      <!-- 已选地址信息 -->
      <view v-if="selectedAddress" class="selected-info">
        <text class="addr-label">已选位置</text>
        <text class="addr-value">{{ selectedAddress }}</text>
        <text class="addr-coord">{{ currentLat.toFixed(6) }}, {{ currentLng.toFixed(6) }}</text>
      </view>
      <view v-else class="empty-info">
        <text class="empty-text">请点击地图选择门店位置</text>
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
        保存门店地址
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
      searchKeyword: '',
      suggestions: [],
      currentLat: 39.915,
      currentLng: 116.404,
      selectedAddress: '',
      saving: false
    };
  },
  computed: {
    markers() {
      return [
        {
          id: 1,
          latitude: this.currentLat,
          longitude: this.currentLng,
          width: 30,
          height: 30,
          iconPath: '/static/logo.png'
        }
      ];
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },

    onSearchInput() {
      const keyword = this.searchKeyword?.trim();
      if (!keyword || keyword.length < 2) {
        this.suggestions = [];
        return;
      }
      // 使用 getLocation 搜索地址
      uni.request({
        url: 'https://apis.map.qq.com/ws/place/v1/suggestion',
        data: {
          keyword: keyword,
          region: '全国',
          key: 'WZLBZ-SBHKW-KY7R7-YRA4W-F6XUH-DZBBg'
        },
        success: (res) => {
          if (res.data && res.data.data) {
            this.suggestions = res.data.data.map(item => ({
              name: item.title,
              address: item.address,
              lat: item.location.lat,
              lng: item.location.lng
            }));
          }
        }
      });
    },

    selectSuggestion(item) {
      this.suggestions = [];
      this.searchKeyword = item.name;
      this.currentLat = item.lat;
      this.currentLng = item.lng;
      this.selectedAddress = item.name + ' ' + item.address;
    },

    locateCurrentPosition() {
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.currentLat = res.latitude;
          this.currentLng = res.longitude;
          this.selectedAddress = '';
          // 逆地理编码
          uni.request({
            url: 'https://apis.map.qq.com/ws/geocoder/v1/',
            data: {
              location: res.latitude + ',' + res.longitude,
              key: 'WZLBZ-SBHKW-KY7R7-YRA4W-F6XUH-DZBBg'
            },
            success: (geoRes) => {
              if (geoRes.data && geoRes.data.result) {
                this.selectedAddress = geoRes.data.result.address;
              } else {
                this.selectedAddress = res.latitude.toFixed(6) + ', ' + res.longitude.toFixed(6);
              }
            },
            fail: () => {
              this.selectedAddress = res.latitude.toFixed(6) + ', ' + res.longitude.toFixed(6);
            }
          });
        },
        fail: () => {
          uni.showToast({ title: '定位失败，请检查定位权限', icon: 'none' });
        }
      });
    },

    onMapTap(e) {
      this.currentLat = e.detail.latitude;
      this.currentLng = e.detail.longitude;
      this.selectedAddress = '';
      // 逆地理编码
      uni.request({
        url: 'https://apis.map.qq.com/ws/geocoder/v1/',
        data: {
          location: e.detail.latitude + ',' + e.detail.longitude,
          key: 'WZLBZ-SBHKW-KY7R7-YRA4W-F6XUH-DZBBg'
        },
        success: (geoRes) => {
          if (geoRes.data && geoRes.data.result) {
            this.selectedAddress = geoRes.data.result.address;
          } else {
            this.selectedAddress = e.detail.latitude.toFixed(6) + ', ' + e.detail.longitude.toFixed(6);
          }
        },
        fail: () => {
          this.selectedAddress = e.detail.latitude.toFixed(6) + ', ' + e.detail.longitude.toFixed(6);
        }
      });
    },

    async handleSave() {
      if (!this.selectedAddress) {
        uni.showToast({ title: '请选择门店位置', icon: 'none' });
        return;
      }
      this.saving = true;
      try {
        const res = await setStoreAddress({
          latitude: this.currentLat,
          longitude: this.currentLng,
          coordType: 'gcj02ll'
        });
        if (res && res.code === 200) {
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

.search-box {
  position: relative;
  margin-top: 24rpx;
}

.search-input {
  height: 72rpx;
  padding: 0 24rpx;
  border: 1rpx solid #e4e7ed;
  border-radius: 12rpx;
  font-size: 28rpx;
  background-color: #f8fafc;
}

.suggestions-dropdown {
  position: absolute;
  top: 80rpx;
  left: 0;
  right: 0;
  max-height: 300rpx;
  overflow-y: auto;
  background: #fff;
  border: 1rpx solid #e4e7ed;
  border-radius: 12rpx;
  z-index: 100;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
}

.suggestion-item {
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.sug-name {
  display: block;
  font-size: 28rpx;
  color: #303133;
}

.sug-addr {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #909399;
}

.locate-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 20rpx 0;
  padding: 16rpx;
  border-radius: 12rpx;
  background-color: #eaf3ff;
}

.locate-text {
  margin-left: 8rpx;
  font-size: 26rpx;
  color: #1677ff;
}

.store-map {
  width: 100%;
  height: 400rpx;
  border-radius: 12rpx;
  margin-top: 8rpx;
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
