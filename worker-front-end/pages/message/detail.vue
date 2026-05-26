<template>
  <view class="page worker-message-detail">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">{{ type === 'system' ? '系统消息' : sessionTitle }}</text>
      </view>
      <view class="nav-right" />
    </view>

    <view v-if="type === 'system'" class="message-list system-list">
      <view
        v-for="item in systemMessages"
        :key="item.id"
        class="message-item system-message-item"
        @click="onSystemMessageTap(item)"
      >
        <view class="system-meta">
          <text class="system-title">{{ item.title }}</text>
          <text class="system-time">{{ item.createdTimeText }}</text>
        </view>
        <view class="system-content">{{ item.content }}</view>
      </view>
      <view v-if="!loading && systemMessages.length === 0" class="empty">
        <text class="empty-text">暂无消息</text>
      </view>
    </view>

    <view v-else class="chat-container">
      <view v-if="!canSend" class="chat-tip">当前会话已结束，如需沟通请联系平台客服</view>
      <scroll-view scroll-y class="chat-scroll" :scroll-into-view="scrollIntoView">
        <view class="chat-list">
          <view v-for="msg in chatMessages" :key="msg.id">
            <view v-if="msg.showTimeDivider" class="time-divider">
              <text>{{ msg.timeDividerText }}</text>
            </view>
            <view
              :id="`msg-${msg.id}`"
              :class="['message-row', msg.isSelf ? 'message-row-self' : 'message-row-other']"
            >
              <view class="message-block">
                <view :class="['message-bubble', msg.bubbleType !== 'text' ? 'message-bubble-media' : '']">
                  <text v-if="msg.bubbleType === 'text'" class="bubble-text">{{ msg.content }}</text>
                  <image
                    v-else-if="msg.bubbleType === 'image'"
                    class="bubble-image"
                    :src="msg.imageUrl"
                    mode="widthFix"
                    @click="previewImage(msg.imageUrl)"
                  />
                  <video
                    v-else-if="msg.bubbleType === 'video'"
                    class="bubble-video"
                    :src="msg.videoUrl"
                    :poster="msg.thumbnailUrl"
                    controls
                    object-fit="cover"
                  />
                </view>
              </view>
            </view>
          </view>
          <view v-if="!loading && chatMessages.length === 0" class="empty">
            <text class="empty-text">暂无消息</text>
          </view>
        </view>
      </scroll-view>

      <view :class="['composer', !canSend ? 'composer-disabled' : '']">
        <view class="composer-tool" @click="chooseImage">
          <u-icon name="photo" size="22" color="#334155" />
        </view>
        <view class="composer-tool" @click="chooseVideo">
          <u-icon name="camera" size="22" color="#334155" />
        </view>
        <input
          v-model="messageInput"
          class="composer-input"
          maxlength="1000"
          placeholder="请输入消息"
          :disabled="!canSend || sending || uploadBusy"
          confirm-type="send"
          @confirm="sendTextMessage"
        />
        <view :class="['composer-send', messageInput.trim() && canSend ? 'composer-send-active' : '']" @click="sendTextMessage">
          {{ sending ? '发送中' : '发送' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import {
  fetchWorkerSystemMessages,
  markWorkerSystemAllRead,
  fetchWorkerChatMessages,
  sendWorkerChatMessage,
  uploadWorkerChatMedia
} from '@/api/workerMessages';
import { showUploadErrorModal } from '@/utils/uploadFeedback';

const ROLE_WORKER = 2;
const CONTENT_TYPE_TEXT = 1;
const CONTENT_TYPE_IMAGE = 2;
const CONTENT_TYPE_VIDEO = 5;
const TIME_DIVIDER_INTERVAL = 5 * 60 * 1000;

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDate(timestamp) {
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function formatDateTime(timestamp) {
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function isSameDay(leftTimestamp, rightTimestamp) {
  if (!leftTimestamp || !rightTimestamp) {
    return false;
  }
  const left = new Date(leftTimestamp);
  const right = new Date(rightTimestamp);
  return left.getFullYear() === right.getFullYear()
    && left.getMonth() === right.getMonth()
    && left.getDate() === right.getDate();
}

function formatTimeDivider(timestamp) {
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const yesterdayStart = todayStart - 24 * 60 * 60 * 1000;
  const timeText = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  if (timestamp >= todayStart) {
    return timeText;
  }
  if (timestamp >= yesterdayStart) {
    return `昨天 ${timeText}`;
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日 ${timeText}`;
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${timeText}`;
}

function shouldShowTimeDivider(previousSendTime, currentSendTime) {
  if (!currentSendTime) {
    return false;
  }
  if (!previousSendTime) {
    return true;
  }
  if (!isSameDay(previousSendTime, currentSendTime)) {
    return true;
  }
  return currentSendTime - previousSendTime >= TIME_DIVIDER_INTERVAL;
}

function normalizeExtraData(extraData) {
  return extraData && typeof extraData === 'object' ? extraData : {};
}

function mapChatMessage(item) {
  const extraData = normalizeExtraData(item && item.extraData);
  const contentType = Number((item && item.contentType) || CONTENT_TYPE_TEXT);
  const bubbleType = contentType === CONTENT_TYPE_IMAGE
    ? 'image'
    : (contentType === CONTENT_TYPE_VIDEO ? 'video' : 'text');
  return {
    id: (item && item.id) || `local-${Date.now()}`,
    senderType: Number((item && item.senderType) || 0),
    isSelf: Number((item && item.senderType) || 0) === ROLE_WORKER,
    content: (item && item.content) || '',
    contentType,
    bubbleType,
    imageUrl: extraData.url || (item && item.imageUrl) || '',
    videoUrl: extraData.url || (item && item.videoUrl) || '',
    thumbnailUrl: extraData.thumbnailUrl || (item && item.thumbnailUrl) || '',
    sendTime: item && item.sendTime ? Number(item.sendTime) : 0
  };
}

function buildChatMessages(items) {
  const list = Array.isArray(items) ? items.map(mapChatMessage) : [];
  return list.map((item, index) => {
    const previous = index > 0 ? list[index - 1] : null;
    const showTimeDivider = shouldShowTimeDivider(previous && previous.sendTime, item.sendTime);
    return {
      ...item,
      showTimeDivider,
      timeDividerText: showTimeDivider ? formatTimeDivider(item.sendTime) : ''
    };
  });
}

export default {
  data() {
    return {
      type: 'chat',
      sessionId: '',
      systemMessages: [],
      chatMessages: [],
      loading: true,
      sessionTitle: '会话',
      canSend: false,
      messageInput: '',
      sending: false,
      uploadBusy: false,
      scrollIntoView: ''
    };
  },
  onLoad(options) {
    this.type = options.type || 'chat';
    this.sessionId = options.sessionId || '';
    if (this.type === 'system') {
      this.loadSystemMessages();
    } else {
      this.loadChatMessages();
    }
  },
  onShow() {
    if (this.type === 'chat') {
      this.startAutoRefresh();
    }
  },
  onHide() {
    this.stopAutoRefresh();
  },
  onUnload() {
    this.stopAutoRefresh();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    startAutoRefresh() {
      if (this._timer || this.type !== 'chat' || !this.sessionId) {
        return;
      }
      this._timer = setInterval(() => {
        if (!this.sending && !this.uploadBusy) {
          this.loadChatMessages(true);
        }
      }, 4000);
    },
    stopAutoRefresh() {
      if (this._timer) {
        clearInterval(this._timer);
        this._timer = null;
      }
    },
    loadSystemMessages() {
      this.loading = true;
      fetchWorkerSystemMessages()
        .then((res) => {
          let list = [];
          if (res && res.code === 200 && Array.isArray(res.data)) {
            list = res.data.map((item) => ({
              ...item,
              createdTimeText: formatDate(item.createdTime)
            }));
          }
          this.systemMessages = list;
          this.loading = false;
          return markWorkerSystemAllRead();
        })
        .catch(() => {
          this.loading = false;
        });
    },
    loadChatMessages(silent = false) {
      if (!this.sessionId) {
        this.loading = false;
        return;
      }
      if (!silent) {
        this.loading = true;
      }
      fetchWorkerChatMessages(this.sessionId)
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || '加载消息失败');
          }
          const data = res.data || {};
          this.chatMessages = buildChatMessages(data.messages);
          this.sessionTitle = data.title || '会话';
          this.canSend = !!data.canSend;
          this.loading = false;
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        })
        .catch((error) => {
          if (!silent) {
            this.loading = false;
            uni.showToast({
              title: (error && error.message) || '加载消息失败',
              icon: 'none'
            });
          }
        });
    },
    scrollToBottom() {
      if (!this.chatMessages.length) {
        return;
      }
      this.scrollIntoView = `msg-${this.chatMessages[this.chatMessages.length - 1].id}`;
    },
    sendTextMessage() {
      const content = String(this.messageInput || '').trim();
      if (!content || this.sending || !this.canSend) {
        return;
      }
      this.sending = true;
      sendWorkerChatMessage({
        sessionId: this.sessionId,
        contentType: CONTENT_TYPE_TEXT,
        content
      })
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || '发送失败');
          }
          this.chatMessages = buildChatMessages(this.chatMessages.concat(mapChatMessage(res.data)));
          this.messageInput = '';
          this.sending = false;
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        })
        .catch((error) => {
          this.sending = false;
          uni.showToast({
              title: (error && error.message) || '发送失败',
            icon: 'none'
          });
          this.loadChatMessages(true);
        });
    },
    chooseImage() {
      if (this.uploadBusy || !this.canSend) {
        return;
      }
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = Array.isArray(res.tempFilePaths) ? res.tempFilePaths[0] : '';
          if (!filePath) {
            return;
          }
          this.uploadAndSendMedia(filePath, 'image', CONTENT_TYPE_IMAGE);
        },
        fail: (error) => {
          if (error && error.errMsg && error.errMsg.indexOf('cancel') >= 0) {
            return;
          }
          uni.showToast({
            title: '暂时无法选择图片',
            icon: 'none'
          });
        }
      });
    },
    chooseVideo() {
      if (this.uploadBusy || !this.canSend) {
        return;
      }
      uni.chooseMedia({
        count: 1,
        mediaType: ['video'],
        sourceType: ['album', 'camera'],
        maxDuration: 120,
        success: (res) => {
          const tempFile = res && Array.isArray(res.tempFiles) ? res.tempFiles[0] : null;
          const filePath = tempFile && tempFile.tempFilePath ? tempFile.tempFilePath : '';
          if (!filePath) {
            return;
          }
          this.uploadAndSendMedia(filePath, 'video', CONTENT_TYPE_VIDEO, {
            duration: tempFile.duration || 0,
            width: tempFile.width || 0,
            height: tempFile.height || 0,
            thumbnailUrl: tempFile.thumbTempFilePath || ''
          });
        },
        fail: (error) => {
          if (error && error.errMsg && error.errMsg.indexOf('cancel') >= 0) {
            return;
          }
          uni.showToast({
            title: '暂时无法选择视频',
            icon: 'none'
          });
        }
      });
    },
    uploadAndSendMedia(filePath, mediaType, contentType, extraData = {}) {
      this.uploadBusy = true;
      uni.showLoading({
        title: mediaType === 'video' ? '上传视频中...' : '上传图片中...',
        mask: true
      });
      uploadWorkerChatMedia(filePath, mediaType)
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || '上传失败');
          }
          return sendWorkerChatMessage({
            sessionId: this.sessionId,
            contentType,
            extraData: {
              ...res.data,
              ...extraData
            }
          });
        })
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || '发送失败');
          }
          this.chatMessages = buildChatMessages(this.chatMessages.concat(mapChatMessage(res.data)));
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        })
        .catch((error) => {
          showUploadErrorModal(error, {
            title: '发送失败',
            fallback: '发送失败'
          });
          this.loadChatMessages(true);
        })
        .finally(() => {
          uni.hideLoading();
          this.uploadBusy = false;
        });
    },
    previewImage(url) {
      if (!url) {
        return;
      }
      const urls = this.chatMessages
        .filter((item) => item.bubbleType === 'image' && item.imageUrl)
        .map((item) => item.imageUrl);
      if (!urls.length) {
        return;
      }
      uni.previewImage({
        current: url,
        urls
      });
    },
    onSystemMessageTap(item) {
      if (!item || !item.businessType) {
        return;
      }
      if (item.businessType === 'USER_REVIEW_NOTIFY_WORKER') {
        uni.navigateTo({
          url: '/pages/reviews/index'
        });
      }
    }
  }
};
</script>

<style scoped>
.worker-message-detail {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fbff 36%, #f5f7fb 100%);
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

.nav-right {
  justify-content: flex-end;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.system-list {
  padding: 24rpx;
}

.message-item {
  margin-top: 16rpx;
}

.system-message-item {
  padding: 18rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.06);
}

.system-meta {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

.system-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
}

.system-time,
.chat-tip,
.empty-text {
  font-size: 22rpx;
  color: #64748b;
}

.system-content {
  font-size: 26rpx;
  line-height: 1.7;
  color: #334155;
}

.chat-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-tip {
  margin: 20rpx 24rpx 0;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background-color: rgba(255, 255, 255, 0.88);
}

.chat-scroll {
  flex: 1;
  min-height: 0;
}

.chat-list {
  padding: 16rpx 24rpx 24rpx;
}

.time-divider {
  margin: 18rpx 0 10rpx;
  text-align: center;
  font-size: 22rpx;
  color: #94a3b8;
}

.message-row {
  display: flex;
  margin-top: 18rpx;
}

.message-row-self {
  justify-content: flex-end;
}

.message-row-other {
  justify-content: flex-start;
}

.message-block {
  max-width: 76%;
}

.message-bubble {
  padding: 18rpx 22rpx;
  border-radius: 24rpx;
  background-color: #ffffff;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.06);
}

.message-row-self .message-bubble {
  background: linear-gradient(135deg, #1677ff 0%, #2aa7ff 100%);
}

.message-bubble-media {
  padding: 10rpx;
}

.bubble-text {
  font-size: 28rpx;
  line-height: 1.6;
  color: #0f172a;
  word-break: break-word;
}

.message-row-self .bubble-text {
  color: #ffffff;
}

.bubble-image,
.bubble-video {
  width: 360rpx;
  max-width: 100%;
  border-radius: 18rpx;
  overflow: hidden;
  background-color: #dbeafe;
}

.bubble-video {
  height: 360rpx;
}

.empty {
  padding: 80rpx 0;
  display: flex;
  justify-content: center;
}

.composer {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom));
  background-color: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 20rpx rgba(15, 23, 42, 0.06);
}

.composer-disabled {
  opacity: 0.82;
}

.composer-tool,
.composer-send {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}

.composer-tool {
  background-color: #e2e8f0;
  color: #334155;
}

.composer-input {
  flex: 1;
  min-width: 0;
  height: 72rpx;
  padding: 0 22rpx;
  border-radius: 18rpx;
  background-color: #f8fafc;
  font-size: 26rpx;
  color: #0f172a;
}

.composer-send {
  background-color: #cbd5e1;
  color: #ffffff;
}

.composer-send-active {
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
}
</style>
