const messagesApi = require("../../api/messages");
const router = require("../../utils/router");

Page({
  data: {
    loading: true,
    conversations: [],
    hasUnread: false
  },
  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
    this.loadConversations();
  },
  loadConversations() {
    this.setData({
      loading: true
    });
    messagesApi
      .fetchUserConversations()
      .then((resp) => {
        let conversations = [];
        if (resp && resp.code === 200 && Array.isArray(resp.data)) {
          conversations = resp.data.map((item) => {
            return {
              id: item.id,
              type: item.type || "chat",
              title: item.title || "",
              subtitle: item.subtitle || "",
              avatarUrl: item.avatarUrl || "",
              unreadCount: item.unreadCount || 0
            };
          });
        }
        const systemConversation = {
          id: "SYSTEM",
          type: "system",
          title: "系统消息",
          subtitle: "查看平台通知与账号提醒",
          avatarUrl: "",
          unreadCount: 0
        };
        return messagesApi
          .fetchUserUnreadFlag()
          .then((flagResp) => {
            let hasUnread = false;
            if (flagResp && flagResp.code === 200 && flagResp.data) {
              if (
                typeof flagResp.data.systemUnreadCount === "number" &&
                flagResp.data.systemUnreadCount > 0
              ) {
                systemConversation.unreadCount =
                  flagResp.data.systemUnreadCount;
              }
              if (
                typeof flagResp.data.totalUnreadCount === "number" &&
                flagResp.data.totalUnreadCount > 0
              ) {
                hasUnread = true;
              } else {
                hasUnread =
                  systemConversation.unreadCount > 0 ||
                  conversations.some((c) => c.unreadCount > 0);
              }
            } else {
              hasUnread =
                systemConversation.unreadCount > 0 ||
                conversations.some((c) => c.unreadCount > 0);
            }
            const list = [systemConversation].concat(conversations);
            this.setData({
              conversations: list,
              hasUnread,
              loading: false
            });
            if (typeof this.getTabBar === "function" && this.getTabBar()) {
              const tabBar = this.getTabBar();
              if (tabBar.updateMessageBadge) {
                tabBar.updateMessageBadge(hasUnread);
              }
            }
          })
          .catch(() => {
            const hasUnread =
              systemConversation.unreadCount > 0 ||
              conversations.some((c) => c.unreadCount > 0);
            const list = [systemConversation].concat(conversations);
            this.setData({
              conversations: list,
              hasUnread,
              loading: false
            });
            if (typeof this.getTabBar === "function" && this.getTabBar()) {
              const tabBar = this.getTabBar();
              if (tabBar.updateMessageBadge) {
                tabBar.updateMessageBadge(hasUnread);
              }
            }
          });
      })
      .catch(() => {
        const systemConversation = {
          id: "SYSTEM",
          type: "system",
          title: "系统消息",
          subtitle: "查看平台通知与账号提醒",
          avatarUrl: "",
          unreadCount: 0
        };
        const hasUnread = systemConversation.unreadCount > 0;
        this.setData({
          conversations: [systemConversation],
          hasUnread,
          loading: false
        });
        if (typeof this.getTabBar === "function" && this.getTabBar()) {
          const tabBar = this.getTabBar();
          if (tabBar.updateMessageBadge) {
            tabBar.updateMessageBadge(hasUnread);
          }
        }
      });
  },
  onConversationTap(event) {
    const dataset = event.currentTarget.dataset || {};
    const id = dataset.id;
    const type = dataset.type;
    if (!id || !type) {
      return;
    }
    if (type === "system") {
      router.navigateTo({
        url: "/pages/messageDetail/index?type=system"
      });
      return;
    }
    router.navigateTo({
      url:
        "/pages/messageDetail/index?type=chat&conversationId=" +
        encodeURIComponent(id)
    });
  }
});

