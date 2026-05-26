<template>
  <view class="protocol-page">
    <view class="protocol-nav">
      <text class="protocol-back" @click="goBack">返回</text>
      <text class="protocol-nav-title">协议内容</text>
      <text class="protocol-nav-placeholder"></text>
    </view>

    <scroll-view scroll-y class="protocol-scroll">
      <view class="protocol-card">
        <view class="protocol-title">{{ title }}</view>
        <view v-if="fileName" class="protocol-meta">文件：{{ fileName }}</view>
        <view v-if="updatedTimeText" class="protocol-meta">最近更新：{{ updatedTimeText }}</view>
        <view v-if="loading" class="protocol-placeholder">加载中...</view>
        <rich-text v-else class="protocol-rich-text" :nodes="htmlContent" />
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { fetchProtocol } from '@/api/protocol';
import { markdownToHtml } from '@/utils/markdown';

export default {
  data() {
    return {
      type: 'user',
      title: '协议内容',
      fileName: '',
      updatedTimeText: '',
      htmlContent: '',
      loading: true
    };
  },
  onLoad(options) {
    this.type = options && options.type ? options.type : 'user';
    this.loadProtocol();
  },
  methods: {
    loadProtocol() {
      this.loading = true;
      this.htmlContent = '';
      fetchProtocol(this.type)
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            uni.showToast({
              title: (res && res.message) || '加载协议失败',
              icon: 'none'
            });
            return;
          }
          this.title = res.data.title || '协议内容';
          this.fileName = res.data.fileName || '';
          this.updatedTimeText = this.formatTime(res.data.updatedTime);
          this.htmlContent = markdownToHtml(res.data.content || '');
        })
        .catch(() => {
          uni.showToast({
            title: '加载协议失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    formatTime(value) {
      const timestamp = Number(value || 0);
      if (!timestamp) {
        return '';
      }
      const date = new Date(timestamp);
      const pad = (num) => String(num).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
    goBack() {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        uni.navigateBack();
        return;
      }
      uni.reLaunch({
        url: '/pages/login/index'
      });
    }
  }
};
</script>

<style scoped>
.protocol-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f8fb 0%, #ffffff 100%);
}

.protocol-nav {
  padding: 88rpx 32rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.protocol-back,
.protocol-nav-title,
.protocol-nav-placeholder {
  width: 120rpx;
  font-size: 28rpx;
}

.protocol-back {
  color: #3c9cff;
}

.protocol-nav-title {
  width: auto;
  font-weight: 600;
  color: #173247;
}

.protocol-nav-placeholder {
  color: transparent;
}

.protocol-scroll {
  height: calc(100vh - 160rpx);
}

.protocol-card {
  margin: 0 24rpx 24rpx;
  padding: 32rpx 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(31, 65, 94, 0.08);
}

.protocol-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #173247;
}

.protocol-meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #7b8d9c;
}

.protocol-placeholder {
  margin-top: 32rpx;
  font-size: 28rpx;
  color: #909399;
}

.protocol-rich-text {
  margin-top: 32rpx;
}
</style>
