const router = require("../../utils/router");
const {
  fetchMallCart,
  updateMallCartQuantity,
  toggleMallCartSelected,
  toggleMallCartAll,
  removeMallCartItems
} = require("../../api/userMallOrder");

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? "0.00" : amount.toFixed(2);
}

function normalizeCartResponse(data) {
  const items = (data && Array.isArray(data.items) ? data.items : []).map((item) => {
    const quantity = Number(item.quantity || 0);
    const stockQuantity = Number(item.stockQuantity || 0);
    return {
      cartId: item.cartId || "",
      productId: item.productId || "",
      name: item.name || "未命名商品",
      mainImageUrl: item.mainImageUrl || "",
      brand: item.brand || "",
      model: item.model || "",
      categoryPath: item.categoryPath || "",
      sellingPriceText: formatPrice(item.sellingPrice),
      originalPriceText: formatPrice(item.originalPrice),
      quantity,
      selected: Number(item.selected || 0) === 1,
      stockQuantity,
      lineAmountText: formatPrice(item.lineAmount),
      stockTip: stockQuantity <= 0 ? "暂时缺货" : quantity > stockQuantity ? `库存仅剩 ${stockQuantity} 件` : "",
      showOriginalPrice: Number(item.originalPrice || 0) > Number(item.sellingPrice || 0)
    };
  });

  return {
    items,
    totalCount: Number(data && data.totalCount || 0),
    selectedCount: Number(data && data.selectedCount || 0),
    selectedAmountText: formatPrice(data && data.selectedAmount),
    allSelected: !!items.length && items.every((item) => item.selected)
  };
}

Page({
  data: {
    loading: true,
    actionLoading: false,
    items: [],
    totalCount: 0,
    selectedCount: 0,
    selectedAmountText: "0.00",
    allSelected: false
  },

  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 });
    }
    this.loadCart();
  },

  noop() {},

  onPullDownRefresh() {
    this.loadCart(true);
  },

  loadCart(fromPullDown) {
    this.setData({ loading: true });
    return fetchMallCart()
      .then((res) => {
        if (!res || res.code !== 200) {
          wx.showToast({
            title: (res && res.message) || "购物车加载失败",
            icon: "none"
          });
          this.setData(normalizeCartResponse(null));
          return;
        }
        this.setData(normalizeCartResponse(res.data));
      })
      .catch(() => {
        wx.showToast({
          title: "购物车加载失败",
          icon: "none"
        });
        this.setData(normalizeCartResponse(null));
      })
      .finally(() => {
        this.setData({ loading: false });
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  runCartAction(task, options) {
    if (this.data.actionLoading) {
      return;
    }
    this.setData({ actionLoading: true });
    task()
      .then((res) => {
        if (!res || res.code !== 200) {
          wx.showToast({
            title: (res && res.message) || ((options && options.failText) || "操作失败"),
            icon: "none"
          });
          return;
        }
        this.setData(normalizeCartResponse(res.data));
        if (options && options.successText) {
          wx.showToast({
            title: options.successText,
            icon: "success"
          });
        }
      })
      .catch(() => {
        wx.showToast({
          title: (options && options.failText) || "操作失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({ actionLoading: false });
      });
  },

  onProductTap(e) {
    const productId = e.currentTarget.dataset.id;
    if (!productId) {
      return;
    }
    router.navigateTo({
      url: `/pages/product-detail/index?id=${productId}`
    });
  },

  onToggleItem(e) {
    const cartId = e.currentTarget.dataset.id;
    const selected = Number(e.currentTarget.dataset.selected || 0) !== 1;
    if (!cartId) {
      return;
    }
    this.runCartAction(() => toggleMallCartSelected({ cartId, selected }), {
      failText: "勾选状态更新失败"
    });
  },

  onToggleAll() {
    this.runCartAction(() => toggleMallCartAll({ selected: !this.data.allSelected }), {
      failText: "全选状态更新失败"
    });
  },

  onQuantityStepTap(e) {
    const cartId = e.currentTarget.dataset.id;
    const type = e.currentTarget.dataset.type;
    const current = Number(e.currentTarget.dataset.quantity || 1);
    const stockQuantity = Number(e.currentTarget.dataset.stock || 0);
    if (!cartId) {
      return;
    }
    const nextQuantity = type === "decrease" ? current - 1 : current + 1;
    if (nextQuantity < 1) {
      return;
    }
    if (stockQuantity > 0 && nextQuantity > stockQuantity) {
      wx.showToast({
        title: `库存仅剩 ${stockQuantity} 件`,
        icon: "none"
      });
      return;
    }
    this.runCartAction(() => updateMallCartQuantity({ cartId, quantity: nextQuantity }), {
      failText: "数量更新失败"
    });
  },

  onDeleteItem(e) {
    const cartId = e.currentTarget.dataset.id;
    if (!cartId) {
      return;
    }
    wx.showModal({
      title: "移除商品",
      content: "确认将该商品移出购物车吗？",
      confirmText: "移除",
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.runCartAction(() => removeMallCartItems({ cartIds: [cartId] }), {
          successText: "已移出购物车",
          failText: "移除失败"
        });
      }
    });
  },

  onDeleteSelected() {
    const cartIds = this.data.items.filter((item) => item.selected).map((item) => item.cartId);
    if (!cartIds.length) {
      wx.showToast({
        title: "请先勾选商品",
        icon: "none"
      });
      return;
    }
    wx.showModal({
      title: "删除已选",
      content: `确认删除已选的 ${cartIds.length} 件商品吗？`,
      confirmText: "删除",
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.runCartAction(() => removeMallCartItems({ cartIds }), {
          successText: "已删除所选商品",
          failText: "删除失败"
        });
      }
    });
  },

  onCheckoutTap() {
    const cartIds = this.data.items.filter((item) => item.selected).map((item) => item.cartId);
    if (!cartIds.length) {
      wx.showToast({
        title: "请先勾选要结算的商品",
        icon: "none"
      });
      return;
    }
    wx.setStorageSync("mallCheckoutPayload", {
      scene: "cart",
      cartIds
    });
    router.navigateTo({
      url: "/pages/product-checkout/index?scene=cart"
    });
  },

  onGoMallTap() {
    router.switchTab({
      url: "/pages/mall/index"
    });
  }
});
