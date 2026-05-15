<template>
  <view class="page worker-reviews">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">服务评价</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view scroll-y class="reviews-scroll">
      <view class="summary-card">
        <text class="summary-title">用户评价与回复</text>
        <text class="summary-desc">这里展示用户对你服务订单的评价，可对每条评价回复一次。</text>
        <text class="summary-count">共 {{ reviews.length }} 条评价</text>
      </view>

      <view v-if="reviewsLoading" class="state-card">评价加载中...</view>
      <view v-else-if="!reviews.length" class="state-card">暂时还没有收到评价</view>

      <view
        v-for="item in reviews"
        :key="item.id"
        class="review-card"
      >
        <view class="review-card-head">
          <view class="review-main">
            <text class="review-user">{{ item.userDisplayName || '匿名用户' }}</text>
            <view class="review-stars">
              <text
                v-for="star in starNumbers"
                :key="star"
                class="review-star"
                :class="{ 'review-star-active': star <= item.rating }"
              >★</text>
            </view>
          </view>
          <view class="review-meta">
            <text
              v-if="item.statusText"
              class="review-status"
              :class="item.status === 2 ? 'review-status-hidden' : 'review-status-normal'"
            >{{ item.statusText }}</text>
            <text class="review-time">{{ formatDateTime(item.createdTime) }}</text>
          </view>
        </view>

        <view class="review-order-row">
          <text class="review-order-no">{{ item.orderNo || '-' }}</text>
          <text class="review-service">{{ item.serviceTypeName || '维修服务' }}</text>
        </view>

        <text v-if="item.content" class="review-content">{{ item.content }}</text>

        <view v-if="item.images && item.images.length" class="review-images">
          <image
            v-for="image in item.images"
            :key="image.id || image.url"
            class="review-image"
            :src="image.thumbnailUrl || image.url"
            mode="aspectFill"
            @click="previewReviewImages(item, image.url)"
          />
        </view>

        <view v-if="item.replyContent" class="review-reply">
          <text class="review-reply-title">我的回复</text>
          <text class="review-reply-content">{{ item.replyContent }}</text>
          <text v-if="item.replyTime" class="review-reply-time">{{ formatDateTime(item.replyTime) }}</text>
        </view>

        <view v-else-if="replyingReviewId === item.id" class="reply-editor">
          <u-textarea
            v-model="replyContent"
            placeholder="请输入回复内容"
            height="120"
            maxlength="200"
          />
          <view class="reply-actions">
            <u-button
              text="取消"
              shape="circle"
              @click="cancelReply"
            />
            <u-button
              text="提交回复"
              type="primary"
              shape="circle"
              :loading="replySubmitting"
              @click="submitReply(item)"
            />
          </view>
        </view>

        <view v-else class="review-card-foot">
          <u-button
            text="回复评价"
            type="primary"
            shape="circle"
            size="small"
            @click="startReply(item)"
          />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getWorkerReviews, replyWorkerReview } from '@/api/workerProfile';

export default {
  name: 'WorkerReviewsPage',
  data() {
    return {
      reviewsLoading: false,
      replySubmitting: false,
      replyingReviewId: '',
      replyContent: '',
      starNumbers: [1, 2, 3, 4, 5],
      reviews: []
    };
  },
  onShow() {
    this.loadReviews();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadReviews() {
      this.reviewsLoading = true;
      getWorkerReviews()
        .then((res) => {
          if (res && res.code === 200) {
            this.reviews = Array.isArray(res.data) ? res.data : [];
          } else {
            this.reviews = [];
          }
        })
        .catch(() => {
          this.reviews = [];
        })
        .finally(() => {
          this.reviewsLoading = false;
        });
    },
    formatDateTime(ts) {
      if (typeof ts !== 'number' || ts <= 0) return '-';
      const d = new Date(ts);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const hh = String(d.getHours()).padStart(2, '0');
      const mm = String(d.getMinutes()).padStart(2, '0');
      return `${y}-${m}-${day} ${hh}:${mm}`;
    },
    previewReviewImages(review, current) {
      const urls = Array.isArray(review && review.images)
        ? review.images.map((item) => item.url).filter(Boolean)
        : [];
      if (!urls.length || !current) return;
      uni.previewImage({
        current,
        urls
      });
    },
    startReply(review) {
      this.replyingReviewId = review && review.id ? review.id : '';
      this.replyContent = '';
    },
    cancelReply() {
      if (this.replySubmitting) return;
      this.replyingReviewId = '';
      this.replyContent = '';
    },
    submitReply(review) {
      const reviewId = review && review.id ? review.id : '';
      const replyContent = String(this.replyContent || '').trim();
      if (!reviewId || !replyContent || this.replySubmitting) {
        if (!replyContent) {
          uni.showToast({ title: '请输入回复内容', icon: 'none' });
        }
        return;
      }
      this.replySubmitting = true;
      replyWorkerReview(reviewId, { replyContent })
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.reviews = this.reviews.map((item) => (item.id === reviewId ? res.data : item));
            this.replyingReviewId = '';
            this.replyContent = '';
            uni.showToast({ title: '回复成功', icon: 'success' });
          } else {
            uni.showToast({ title: res?.message || '回复失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '回复失败', icon: 'none' });
        })
        .finally(() => {
          this.replySubmitting = false;
        });
    }
  }
};
</script>

<style scoped>
.worker-reviews {
  min-height: 100vh;
  background: linear-gradient(180deg, #f3f7fd 0%, #eef4fb 100%);
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  background-color: rgba(255, 255, 255, 0.96);
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

.reviews-scroll {
  height: calc(100vh - 88rpx - var(--status-bar-height));
  box-sizing: border-box;
}

.summary-card,
.state-card,
.review-card {
  margin: 20rpx 24rpx 0;
  border-radius: 24rpx;
  background-color: #ffffff;
  box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.06);
}

.summary-card {
  padding: 26rpx 24rpx;
}

.summary-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.summary-desc,
.summary-count,
.state-card,
.review-time,
.review-reply-time {
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.7;
}

.summary-desc {
  margin-top: 10rpx;
}

.summary-count {
  display: block;
  margin-top: 16rpx;
}

.state-card {
  padding: 28rpx 24rpx;
  text-align: center;
}

.review-card {
  padding: 24rpx 22rpx;
}

.review-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-user {
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.review-stars {
  margin-top: 10rpx;
  display: flex;
  gap: 8rpx;
}

.review-star {
  font-size: 30rpx;
  color: #d0d7de;
  line-height: 1;
}

.review-star-active {
  color: #f59e0b;
}

.review-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
}

.review-status {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.review-status-normal {
  background-color: rgba(22, 163, 74, 0.12);
  color: #15803d;
}

.review-status-hidden {
  background-color: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.review-order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding: 14rpx 16rpx;
  border-radius: 18rpx;
  background-color: #f8fafc;
}

.review-order-no,
.review-service {
  font-size: 22rpx;
  color: #475569;
}

.review-content,
.review-reply-content {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.review-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 18rpx;
  background-color: #e5e7eb;
}

.review-reply {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 18rpx;
  background-color: #f8fbff;
}

.review-reply-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #1677ff;
}

.reply-editor {
  margin-top: 18rpx;
}

.reply-actions,
.review-card-foot {
  margin-top: 16rpx;
  display: flex;
  gap: 16rpx;
}

.reply-actions :deep(.u-button),
.review-card-foot :deep(.u-button) {
  flex: 1;
}
</style>
