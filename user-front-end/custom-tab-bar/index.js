const router = require("../utils/router");

Component({
  data: {
    selected: 0,
    hasMessageUnread: false,
    list: [
      {
        key: "home",
        pagePath: "/pages/home/index",
        text: "首页",
        icon: "wap-home-o",
        iconActive: "wap-home"
      },
      {
        key: "mall",
        pagePath: "/pages/mall/index",
        text: "商城",
        icon: "bag-o",
        iconActive: "bag"
      },
      {
        key: "message",
        pagePath: "/pages/message/index",
        text: "消息",
        icon: "more-o",
        iconActive: "more"
      },
      {
        key: "cart",
        pagePath: "/pages/cart/index",
        text: "购物车",
        icon: "cart-o",
        iconActive: "cart"
      },
      {
        key: "mine",
        pagePath: "/pages/mine/index",
        text: "我的",
        icon: "user-o",
        iconActive: "user"
      }
    ]
  },
  methods: {
    onChange(event) {
      const index = event.currentTarget.dataset.index;
      const item = this.data.list[index];
      const ok = router.switchTab({
        url: item.pagePath
      });
      if (ok) {
        this.setData({ selected: index });
      }
    },
    updateMessageBadge(hasUnread) {
      this.setData({
        hasMessageUnread: !!hasUnread
      });
    }
  }
});
