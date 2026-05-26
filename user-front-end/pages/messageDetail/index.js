const messagesApi = require("../../api/messages");
const { showUploadErrorModal } = require("../../utils/uploadFeedback");

const ROLE_USER = 1;
const CONTENT_TYPE_TEXT = 1;
const CONTENT_TYPE_IMAGE = 2;
const CONTENT_TYPE_VIDEO = 5;
const TIME_DIVIDER_INTERVAL = 5 * 60 * 1000;

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDate(timestamp) {
  if (!timestamp) {
    return "";
  }
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function formatDateTime(timestamp) {
  if (!timestamp) {
    return "";
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
    return "";
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
  if (!extraData || typeof extraData !== "object") {
    return {};
  }
  return extraData;
}

function mapChatMessage(item) {
  const extraData = normalizeExtraData(item && item.extraData);
  const contentType = Number((item && item.contentType) || CONTENT_TYPE_TEXT);
  const bubbleType = contentType === CONTENT_TYPE_IMAGE
    ? "image"
    : (contentType === CONTENT_TYPE_VIDEO ? "video" : "text");
  return {
    id: (item && item.id) || `local-${Date.now()}`,
    senderType: Number((item && item.senderType) || 0),
    isSelf: Number((item && item.senderType) || 0) === ROLE_USER,
    contentType,
    bubbleType,
    content: (item && item.content) || "",
    imageUrl: extraData.url || (item && item.imageUrl) || "",
    videoUrl: extraData.url || (item && item.videoUrl) || "",
    thumbnailUrl: extraData.thumbnailUrl || (item && item.thumbnailUrl) || "",
    caption: bubbleType === "text" ? "" : ((item && item.content) || ""),
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
      timeDividerText: showTimeDivider ? formatTimeDivider(item.sendTime) : ""
    };
  });
}

Page({
  data: {
    type: "chat",
    conversationId: "",
    systemMessages: [],
    chatMessages: [],
    loading: true,
    sessionTitle: "会话",
    canSend: false,
    messageInput: "",
    sending: false,
    uploadBusy: false,
    scrollIntoView: ""
  },

  onLoad(options) {
    const type = options.type || "chat";
    const conversationId = options.conversationId || "";
    this.setData({
      type,
      conversationId
    });
    if (type === "system") {
      wx.setNavigationBarTitle({ title: "系统消息" });
      this.loadSystemMessages();
    } else {
      wx.setNavigationBarTitle({ title: "聊天" });
      this.loadChatMessages();
    }
  },

  onShow() {
    if (this.data.type === "chat") {
      this.startAutoRefresh();
    }
  },

  onHide() {
    this.stopAutoRefresh();
  },

  onUnload() {
    this.stopAutoRefresh();
  },

  startAutoRefresh() {
    if (this._timer || this.data.type !== "chat" || !this.data.conversationId) {
      return;
    }
    this._timer = setInterval(() => {
      if (!this.data.sending && !this.data.uploadBusy) {
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
    this.setData({ loading: true });
    messagesApi
      .fetchSystemMessages()
      .then((resp) => {
        const list = resp && resp.code === 200 && Array.isArray(resp.data)
          ? resp.data.map((item) => ({
              ...item,
              createdTimeText: formatDate(item.createdTime)
            }))
          : [];
        this.setData({
          systemMessages: list,
          loading: false
        });
        return messagesApi.markAllSystemRead();
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  loadChatMessages(silent) {
    const sessionId = this.data.conversationId;
    if (!sessionId) {
      this.setData({ loading: false });
      return;
    }
    if (!silent) {
      this.setData({ loading: true });
    }
    messagesApi
      .fetchChatMessages(sessionId)
      .then((resp) => {
        if (!resp || resp.code !== 200 || !resp.data) {
          throw new Error((resp && resp.message) || "加载消息失败");
        }
        const data = resp.data || {};
        const list = buildChatMessages(data.messages);
        const title = data.title || "会话";
        this.setData(
          {
            chatMessages: list,
            sessionTitle: title,
            canSend: !!data.canSend,
            loading: false
          },
          () => {
            wx.setNavigationBarTitle({ title });
            this.scrollToBottom();
          }
        );
      })
      .catch((err) => {
        if (!silent) {
          this.setData({ loading: false });
          wx.showToast({
            title: (err && err.message) || "加载消息失败",
            icon: "none"
          });
        }
      });
  },

  scrollToBottom() {
    const list = this.data.chatMessages || [];
    if (!list.length) {
      return;
    }
    const last = list[list.length - 1];
    this.setData({
      scrollIntoView: `msg-${last.id}`
    });
  },

  onMessageInput(e) {
    this.setData({
      messageInput: (e.detail && e.detail.value) || ""
    });
  },

  onSendTap() {
    const content = String(this.data.messageInput || "").trim();
    if (!content || this.data.sending || !this.data.canSend) {
      return;
    }
    this.setData({ sending: true });
    messagesApi
      .sendUserChatMessage({
        sessionId: this.data.conversationId,
        contentType: CONTENT_TYPE_TEXT,
        content
      })
      .then((resp) => {
        if (!resp || resp.code !== 200 || !resp.data) {
          throw new Error((resp && resp.message) || "发送失败");
        }
        const chatMessages = buildChatMessages(this.data.chatMessages.concat(mapChatMessage(resp.data)));
        this.setData(
          {
            chatMessages,
            messageInput: "",
            sending: false
          },
          () => {
            this.scrollToBottom();
          }
        );
      })
      .catch((err) => {
        this.setData({ sending: false });
        wx.showToast({
          title: (err && err.message) || "发送失败",
          icon: "none"
        });
        this.loadChatMessages(true);
      });
  },

  onChooseImage() {
    if (this.data.uploadBusy || !this.data.canSend) {
      return;
    }
    wx.chooseImage({
      count: 1,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const filePath = res && res.tempFilePaths && res.tempFilePaths[0];
        if (!filePath) {
          return;
        }
        this.uploadAndSendMedia(filePath, "image", CONTENT_TYPE_IMAGE);
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.indexOf("cancel") >= 0) {
          return;
        }
        wx.showToast({
          title: "暂时无法选择图片",
          icon: "none"
        });
      }
    });
  },

  onChooseVideo() {
    if (this.data.uploadBusy || !this.data.canSend) {
      return;
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ["video"],
      sourceType: ["album", "camera"],
      maxDuration: 120,
      success: (res) => {
        const tempFile = res && Array.isArray(res.tempFiles) ? res.tempFiles[0] : null;
        const filePath = tempFile && tempFile.tempFilePath ? tempFile.tempFilePath : "";
        if (!filePath) {
          return;
        }
        this.uploadAndSendMedia(filePath, "video", CONTENT_TYPE_VIDEO, {
          duration: tempFile.duration || 0,
          width: tempFile.width || 0,
          height: tempFile.height || 0,
          thumbnailUrl: tempFile.thumbTempFilePath || ""
        });
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.indexOf("cancel") >= 0) {
          return;
        }
        wx.showToast({
          title: "暂时无法选择视频",
          icon: "none"
        });
      }
    });
  },

  uploadAndSendMedia(filePath, mediaType, contentType, extraData) {
    this.setData({ uploadBusy: true });
    wx.showLoading({
      title: mediaType === "video" ? "上传视频中..." : "上传图片中...",
      mask: true
    });
    messagesApi
      .uploadUserChatMedia(filePath, mediaType)
      .then((resp) => {
        if (!resp || resp.code !== 200 || !resp.data) {
          throw new Error((resp && resp.message) || "上传失败");
        }
        const payload = Object.assign({}, resp.data, extraData || {});
        return messagesApi.sendUserChatMessage({
          sessionId: this.data.conversationId,
          contentType,
          extraData: payload
        });
      })
      .then((resp) => {
        if (!resp || resp.code !== 200 || !resp.data) {
          throw new Error((resp && resp.message) || "发送失败");
        }
        const chatMessages = buildChatMessages(this.data.chatMessages.concat(mapChatMessage(resp.data)));
        this.setData({ chatMessages }, () => this.scrollToBottom());
      })
      .catch((err) => {
        showUploadErrorModal(err, {
          title: "发送失败",
          fallback: "发送失败"
        });
        this.loadChatMessages(true);
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ uploadBusy: false });
      });
  },

  onPreviewImage(e) {
    const current = e.currentTarget.dataset.url || "";
    const urls = (this.data.chatMessages || [])
      .filter((item) => item.bubbleType === "image" && item.imageUrl)
      .map((item) => item.imageUrl);
    if (!current || !urls.length) {
      return;
    }
    wx.previewImage({
      current,
      urls
    });
  }
});
