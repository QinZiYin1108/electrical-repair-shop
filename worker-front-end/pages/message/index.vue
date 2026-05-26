<template>
  <view class="page worker-message">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">消息中心</text>
      </view>
      <view class="nav-right" />
    </view>
    <view class="conversation-list">
      <view
        v-for="item in conversations"
        :key="item.id"
        class="conversation-item"
        @click="onConversationTap(item)"
      >
        <view class="conversation-avatar">
          <view class="avatar-circle">
            {{ item.type === 'system' ? '系' : '聊' }}
          </view>
        </view>
        <view class="conversation-main">
          <view class="conversation-title-row">
            <text class="conversation-title">{{ item.title }}</text>
            <view
              v-if="item.unreadCount > 0"
              class="conversation-unread-badge"
            >
              <text class="conversation-unread-text">
                {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
              </text>
            </view>
          </view>
          <view class="conversation-subtitle-row">
            <text class="conversation-subtitle">{{ item.subtitle }}</text>
          </view>
        </view>
      </view>
      <view
        v-if="!loading && conversations.length === 0"
        class="empty"
      >
        <text class="empty-text">暂无消息</text>
      </view>
    </view>
  </view>
</template>

<script>
import {
  fetchWorkerConversations,
  fetchWorkerUnreadFlag
} from '@/api/workerMessages';

export default {
  name: 'WorkerMessagePage',
  data() {
    return {
      loading: true,
      conversations: []
    };
  },
  onShow() {
    this.loadConversations();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadConversations() {
      this.loading = true;
      fetchWorkerConversations()
        .then((res) => {
          let list = [];
          if (res && res.code === 200 && Array.isArray(res.data)) {
            list = res.data.map((item) => {
              return {
                id: item.id,
                type: item.type || 'chat',
                title: item.title || '',
                subtitle: item.subtitle || '',
                avatarUrl: item.avatarUrl || '',
                unreadCount: item.unreadCount || 0
              };
            });
          }
          const systemItem = {
            id: 'SYSTEM',
            type: 'system',
            title: '系统消息',
            subtitle: '查看平台通知与账号提醒',
            avatarUrl: '',
            unreadCount: 0
          };
          fetchWorkerUnreadFlag()
            .then((flagRes) => {
              let hasUnread = false;
              if (flagRes && flagRes.code === 200 && flagRes.data) {
                if (
                  typeof flagRes.data.systemUnreadCount === 'number' &&
                  flagRes.data.systemUnreadCount > 0
                ) {
                  systemItem.unreadCount = flagRes.data.systemUnreadCount;
                }
                if (
                  typeof flagRes.data.totalUnreadCount === 'number' &&
                  flagRes.data.totalUnreadCount > 0
                ) {
                  hasUnread = true;
                } else {
                  hasUnread =
                    systemItem.unreadCount > 0 ||
                    list.some((c) => c.unreadCount > 0);
                }
              } else {
                hasUnread =
                  systemItem.unreadCount > 0 ||
                  list.some((c) => c.unreadCount > 0);
              }
              const finalList = [systemItem].concat(list);
              this.conversations = finalList;
              this.loading = false;
              const app = getApp();
              if (app && app.globalData) {
                app.globalData.workerHasUnread = hasUnread;
              }
            })
            .catch(() => {
              const hasUnread =
                systemItem.unreadCount > 0 ||
                list.some((c) => c.unreadCount > 0);
              const finalList = [systemItem].concat(list);
              this.conversations = finalList;
              this.loading = false;
              const app = getApp();
              if (app && app.globalData) {
                app.globalData.workerHasUnread = hasUnread;
              }
            });
        })
        .catch(() => {
          this.loading = false;
        });
    },
    onConversationTap(item) {
      if (!item || !item.id || !item.type) {
        return;
      }
      if (item.type === 'system') {
        uni.navigateTo({
          url: '/pages/message/detail?type=system'
        });
      } else {
        uni.navigateTo({
          url:
            '/pages/message/detail?type=chat&sessionId=' +
            encodeURIComponent(item.id)
        });
      }
    }
  }
};
</script>

<style scoped>
.worker-message {
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

.nav-right {
  justify-content: flex-end;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}

.conversation-list {
  padding-top: 0.40rem;
}

.conversation-item {
  display: flex;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  background-color: #ffffff;
}

.conversation-avatar {
  width: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-circle {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background-color: #3c9cff;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.conversation-main {
  flex: 1;
  margin-left: 16rpx;
}

.conversation-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conversation-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #303133;
}

.conversation-unread-badge {
  min-width: 32rpx;
  padding: 0 8rpx;
  height: 32rpx;
  border-radius: 16rpx;
  background-color: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
}

.conversation-unread-text {
  font-size: 22rpx;
  color: #ffffff;
}

.conversation-subtitle-row {
  margin-top: 8rpx;
}

.conversation-subtitle {
  font-size: 24rpx;
  color: #909399;
}

.empty {
  margin-top: 80rpx;
  align-items: center;
  justify-content: center;
  display: flex;
}

.empty-text {
  font-size: 26rpx;
  color: #909399;
}
</style>
