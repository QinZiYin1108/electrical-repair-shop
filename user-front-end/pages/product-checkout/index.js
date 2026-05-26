const router = require('../../utils/router');
const { fetchMallProductDetail } = require('../../api/userMall');
const { fetchMallCart, submitMallOrder, fetchAvailableMallCoupons } = require('../../api/userMallOrder');
const userAddressApi = require('../../api/userAddress');

const PAYMENT_METHODS = [
  { id: 5, name: '钱包支付', desc: '优先使用钱包余额完成本次订单支付。', accent: 'wallet' },
  { id: 1, name: '微信支付', desc: '提交订单后使用微信完成支付。', accent: 'wechat' },
  { id: 2, name: '支付宝支付', desc: '提交订单后使用支付宝完成支付。', accent: 'alipay' }
];

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? '0.00' : amount.toFixed(2);
}

function normalizeAddressList(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || '',
    contactName: item.contactName || '未设置联系人',
    contactPhone: item.contactPhone || '',
    addressTypeName: item.addressTypeName || '地址',
    fullAddress: item.fullAddress || item.detail || '',
    isDefault: Number(item.isDefault || 0) === 1
  }));
}

function buildItemSpecText(item) {
  const parts = [];
  if (item.brand) {
    parts.push(item.brand);
  }
  if (item.model) {
    parts.push(item.model);
  }
  return parts.join(' / ') || '默认规格';
}

function getPaymentMethodText(paymentMethod) {
  const target = PAYMENT_METHODS.find((item) => item.id === Number(paymentMethod));
  return target ? target.name : '在线支付';
}

function formatDate(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) {
    return '未设置';
  }
  const date = new Date(value);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function mapCouponItem(item) {
  return {
    userCouponId: item.userCouponId || '',
    couponId: item.couponId || '',
    name: item.name || '优惠券',
    typeText: item.typeText || '优惠',
    discountText: item.discountText || '待优惠',
    minAmountText: formatPrice(item.minAmount),
    discountAmountText: formatPrice(item.discountAmount),
    applicableText: item.applicableText || '当前订单可用',
    available: item.available !== false,
    unavailableReason: item.unavailableReason || '',
    expireTimeText: item.expireTime ? formatDate(item.expireTime) : '长期有效'
  };
}

Page({
  data: {
    paymentMethods: PAYMENT_METHODS,
    loading: true,
    submitting: false,
    addressLoading: false,
    couponLoading: false,
    showAddressSheet: false,
    showCouponSheet: false,
    scene: '',
    cartIds: [],
    directItems: [],
    items: [],
    addressList: [],
    couponList: [],
    selectedAddressId: '',
    selectedAddress: null,
    selectedPaymentMethod: PAYMENT_METHODS[0].id,
    selectedCouponId: '',
    selectedCoupon: null,
    remark: '',
    totalCount: 0,
    totalAmountText: '0.00',
    discountAmountText: '0.00',
    payAmountText: '0.00'
  },

  onLoad(options) {
    const payload = wx.getStorageSync('mallCheckoutPayload') || {};
    const scene = (options && options.scene) || payload.scene || '';
    const cartIds = Array.isArray(payload.cartIds) ? payload.cartIds.filter(Boolean) : [];
    const directItems = Array.isArray(payload.items)
      ? payload.items
          .filter((item) => item && item.productId)
          .map((item) => ({
            productId: item.productId,
            quantity: Math.max(1, Number(item.quantity || 1))
          }))
      : [];

    if (!scene || (scene === 'cart' && !cartIds.length) || (scene !== 'cart' && !directItems.length)) {
      wx.showToast({ title: '结算信息已失效', icon: 'none' });
      setTimeout(() => wx.navigateBack({ delta: 1 }), 500);
      return;
    }

    this.setData({ scene, cartIds, directItems });
    this.loadPageData();
  },

  onShow() {
    if (this.data.scene) {
      this.loadAddressList();
    }
  },

  onPullDownRefresh() {
    this.loadPageData(true);
  },

  loadPageData(fromPullDown) {
    this.setData({ loading: true });
    Promise.allSettled([this.loadCheckoutItems(), this.loadAddressList()]).finally(() => {
      this.setData({ loading: false });
      if (fromPullDown) {
        wx.stopPullDownRefresh();
      }
    });
  },

  loadCheckoutItems() {
    return (this.data.scene === 'cart' ? this.loadCartCheckoutItems() : this.loadDirectCheckoutItems())
      .then(() => this.loadAvailableCoupons());
  },

  loadCartCheckoutItems() {
    return fetchMallCart()
      .then((res) => {
        if (!res || res.code !== 200) {
          throw new Error('cart');
        }
        const sourceItems = res.data && Array.isArray(res.data.items) ? res.data.items : [];
        const idSet = new Set(this.data.cartIds);
        const items = sourceItems
          .filter((item) => idSet.has(item.cartId))
          .map((item) => ({
            key: item.cartId || item.productId,
            cartId: item.cartId || '',
            productId: item.productId || '',
            name: item.name || '商品信息待补全',
            mainImageUrl: item.mainImageUrl || '',
            brand: item.brand || '',
            model: item.model || '',
            specText: buildItemSpecText(item),
            quantity: Math.max(1, Number(item.quantity || 1)),
            stockQuantity: Number(item.stockQuantity || 0),
            sellingPrice: Number(item.sellingPrice || 0),
            sellingPriceText: formatPrice(item.sellingPrice),
            lineAmountText: formatPrice(item.lineAmount)
          }));
        this.setData({ items });
        this.recalculateSummary();
      })
      .catch(() => {
        wx.showToast({ title: '购物车商品加载失败', icon: 'none' });
        this.setData({ items: [] });
        this.recalculateSummary();
      });
  },

  loadDirectCheckoutItems() {
    const tasks = this.data.directItems.map((item) =>
      fetchMallProductDetail(item.productId).then((res) => ({ res, quantity: item.quantity, productId: item.productId }))
    );
    return Promise.all(tasks)
      .then((resultList) => {
        const items = resultList
          .filter((entry) => entry && entry.res && entry.res.code === 200 && entry.res.data)
          .map((entry) => {
            const data = entry.res.data;
            const quantity = Math.max(1, Number(entry.quantity || 1));
            const sellingPrice = Number(data.sellingPrice || 0);
            return {
              key: data.id || entry.productId,
              cartId: '',
              productId: data.id || entry.productId,
              name: data.name || '商品信息待补全',
              mainImageUrl: data.mainImageUrl || (Array.isArray(data.galleryUrls) ? data.galleryUrls[0] : '') || '',
              brand: data.brand || '',
              model: data.model || '',
              specText: buildItemSpecText(data),
              quantity,
              stockQuantity: Number(data.stockQuantity || 0),
              sellingPrice,
              sellingPriceText: formatPrice(sellingPrice),
              lineAmountText: formatPrice(sellingPrice * quantity)
            };
          });
        this.setData({ items });
        this.recalculateSummary();
      })
      .catch(() => {
        wx.showToast({ title: '商品信息加载失败', icon: 'none' });
        this.setData({ items: [] });
        this.recalculateSummary();
      });
  },

  loadAddressList() {
    this.setData({ addressLoading: true });
    return userAddressApi
      .listUserAddresses()
      .then((res) => {
        if (!res || res.code !== 200) {
          this.setData({ addressList: [], selectedAddressId: '', selectedAddress: null });
          return;
        }
        const addressList = normalizeAddressList(res.data);
        const currentSelected = addressList.find((item) => item.id === this.data.selectedAddressId) || null;
        const fallbackSelected = currentSelected || addressList.find((item) => item.isDefault) || addressList[0] || null;
        this.setData({
          addressList,
          selectedAddressId: fallbackSelected ? fallbackSelected.id : '',
          selectedAddress: fallbackSelected
        });
      })
      .catch(() => {
        this.setData({ addressList: [], selectedAddressId: '', selectedAddress: null });
      })
      .finally(() => {
        this.setData({ addressLoading: false });
      });
  },

  loadAvailableCoupons() {
    if (!this.data.items.length) {
      this.setData({ couponList: [], selectedCouponId: '', selectedCoupon: null });
      this.recalculateSummary();
      return Promise.resolve();
    }
    const payload = this.data.scene === 'cart'
      ? { cartIds: this.data.cartIds }
      : { items: this.data.items.map((item) => ({ productId: item.productId, quantity: item.quantity })) };
    this.setData({ couponLoading: true });
    return fetchAvailableMallCoupons(payload)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data || !Array.isArray(res.data.coupons)) {
          throw new Error('coupon');
        }
        const couponList = res.data.coupons.map(mapCouponItem);
        let selectedCouponId = this.data.selectedCouponId;
        let selectedCoupon = couponList.find((item) => item.userCouponId === selectedCouponId && item.available) || null;
        if (!selectedCoupon && res.data.bestCouponId) {
          selectedCouponId = res.data.bestCouponId;
          selectedCoupon = couponList.find((item) => item.userCouponId === selectedCouponId) || null;
        }
        if (!selectedCoupon) {
          selectedCouponId = '';
        }
        this.setData({ couponList, selectedCouponId, selectedCoupon });
        this.recalculateSummary();
      })
      .catch(() => {
        this.setData({ couponList: [], selectedCouponId: '', selectedCoupon: null });
        this.recalculateSummary();
      })
      .finally(() => {
        this.setData({ couponLoading: false });
      });
  },

  recalculateSummary() {
    const items = Array.isArray(this.data.items) ? this.data.items : [];
    let totalAmount = 0;
    let totalCount = 0;
    items.forEach((item) => {
      const quantity = Math.max(1, Number(item.quantity || 1));
      const price = Number(item.sellingPrice || 0);
      totalAmount += quantity * price;
      totalCount += quantity;
    });
    const selectedCoupon = this.data.selectedCoupon || null;
    const discountAmount = selectedCoupon && selectedCoupon.available ? Number(selectedCoupon.discountAmountText || 0) : 0;
    this.setData({
      totalCount,
      totalAmountText: formatPrice(totalAmount),
      discountAmountText: formatPrice(discountAmount),
      payAmountText: formatPrice(Math.max(totalAmount - discountAmount, 0))
    });
  },

  onOpenAddressSheet() {
    this.setData({ showAddressSheet: true });
  },

  onCloseAddressSheet() {
    this.setData({ showAddressSheet: false });
  },

  onOpenCouponSheet() {
    this.setData({ showCouponSheet: true });
  },

  onCloseCouponSheet() {
    this.setData({ showCouponSheet: false });
  },

  onSelectAddress(e) {
    const id = e.currentTarget.dataset.id || '';
    const selectedAddress = this.data.addressList.find((item) => item.id === id) || null;
    if (!selectedAddress) {
      return;
    }
    this.setData({ selectedAddressId: selectedAddress.id, selectedAddress, showAddressSheet: false });
  },

  onManageAddress() {
    this.onCloseAddressSheet();
    router.navigateTo({ url: '/pages/address-list/index' });
  },

  onAddAddress() {
    this.onCloseAddressSheet();
    router.navigateTo({ url: '/pages/address-edit/index' });
  },

  onSelectCoupon(e) {
    const id = e.currentTarget.dataset.id || '';
    if (!id) {
      this.setData({ selectedCouponId: '', selectedCoupon: null, showCouponSheet: false });
      this.recalculateSummary();
      return;
    }
    const selectedCoupon = this.data.couponList.find((item) => item.userCouponId === id) || null;
    if (!selectedCoupon) {
      return;
    }
    if (!selectedCoupon.available) {
      wx.showToast({ title: selectedCoupon.unavailableReason || '当前优惠券不可用', icon: 'none' });
      return;
    }
    this.setData({ selectedCouponId: id, selectedCoupon, showCouponSheet: false });
    this.recalculateSummary();
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value || '' });
  },

  onSelectPaymentMethod(e) {
    const paymentMethod = Number(e.currentTarget.dataset.id || 0);
    if (!paymentMethod) {
      return;
    }
    this.setData({ selectedPaymentMethod: paymentMethod });
  },

  onDirectQuantityTap(e) {
    if (this.data.scene === 'cart') {
      return;
    }
    const index = Number(e.currentTarget.dataset.index);
    const type = e.currentTarget.dataset.type;
    const items = this.data.items.slice();
    const directItems = this.data.directItems.slice();
    const target = items[index];
    if (!target) {
      return;
    }
    const nextQuantity = type === 'decrease' ? target.quantity - 1 : target.quantity + 1;
    if (nextQuantity < 1) {
      return;
    }
    if (target.stockQuantity > 0 && nextQuantity > target.stockQuantity) {
      wx.showToast({ title: `库存仅剩 ${target.stockQuantity} 件`, icon: 'none' });
      return;
    }
    target.quantity = nextQuantity;
    if (directItems[index]) {
      directItems[index].quantity = nextQuantity;
    }
    target.lineAmountText = formatPrice(nextQuantity * Number(target.sellingPrice || 0));
    this.setData({ items, directItems });
    this.recalculateSummary();
    this.loadAvailableCoupons();
  },

  onSubmitOrder() {
    if (this.data.submitting) {
      return;
    }
    if (!this.data.items.length) {
      wx.showToast({ title: '暂无可结算商品', icon: 'none' });
      return;
    }
    if (!this.data.selectedAddressId) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' });
      return;
    }
    const payload = {
      addressId: this.data.selectedAddressId,
      paymentMethod: this.data.selectedPaymentMethod,
      userCouponId: this.data.selectedCouponId || undefined,
      remark: this.data.remark
    };
    if (this.data.scene === 'cart') {
      payload.cartIds = this.data.cartIds;
    } else {
      payload.items = this.data.items.map((item) => ({ productId: item.productId, quantity: item.quantity }));
    }
    this.setData({ submitting: true });
    submitMallOrder(payload)
      .then((res) => {
        if (!res || res.code !== 200 || !res.data) {
          wx.showToast({ title: (res && res.message) || '提交订单失败', icon: 'none' });
          return;
        }
        wx.removeStorageSync('mallCheckoutPayload');
        const discountLine = Number(res.data.discountAmount || 0) > 0
          ? `\n优惠抵扣：￥${formatPrice(res.data.discountAmount)}${res.data.couponName ? `（${res.data.couponName}）` : ''}`
          : '';
        wx.showModal({
          title: '下单成功',
          content: `已使用${getPaymentMethodText(this.data.selectedPaymentMethod)}提交订单\n订单号：${res.data.orderNo}${discountLine}\n实付金额：￥${formatPrice(res.data.actualAmount)}`,
          showCancel: false,
          success: () => {
            if (this.data.scene === 'cart') {
              wx.switchTab({ url: '/pages/cart/index' });
              return;
            }
            wx.switchTab({ url: '/pages/mall/index' });
          }
        });
      })
      .catch(() => {
        wx.showToast({ title: '提交订单失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  }
});

