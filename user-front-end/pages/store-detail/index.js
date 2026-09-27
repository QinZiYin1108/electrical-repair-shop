const router = require('../../utils/router');
const { fetchStoreDetail, fetchMallProducts } = require('../../api/userMall');

function formatPrice(value) {
  const amount = Number(value || 0);
  return Number.isNaN(amount) ? '0.00' : amount.toFixed(2);
}

function normalizeDetail(data) {
  if (!data) return null;
  const ratingCount = Number(data.ratingCount || 0);
  const rating = data.rating != null ? Number(data.rating) : null;
  return {
    id: data.id || '',
    name: data.name || '未知门店',
    logoUrl: data.logoUrl || '',
    rating,
    ratingCount,
    ratingText: rating != null ? rating.toFixed(1) : '暂无评分',
    isOnline: Number(data.isOnline || 0),
    statusText: Number(data.isOnline || 0) === 1 ? '营业中' : '已打烊',
    description: data.description || '',
    contactPhone: data.contactPhone || '',
    address: data.address || '',
    businessHours: Array.isArray(data.businessHours) ? data.businessHours : []
  };
}

function normalizeProduct(item) {
  const sellingPrice = formatPrice(item.sellingPrice);
  const originalPrice = formatPrice(item.originalPrice);
  return {
    id: item.id || '',
    name: item.name || '未命名商品',
    mainImageUrl: item.mainImageUrl || '',
    sellingPriceText: sellingPrice,
    originalPriceText: originalPrice,
    showOriginalPrice: Number(originalPrice) > 0 && originalPrice !== sellingPrice,
    isHot: Number(item.isHot || 0) === 1,
    isNew: Number(item.isNew || 0) === 1,
    salesCount: Number(item.salesCount || 0)
  };
}

Page({
  data: {
    loading: true,
    storeId: '',
    detail: null,
    products: [],
    productsLoading: false
  },

  onLoad(options) {
    const id = (options && options.id) ? options.id : '';
    this.setData({ storeId: id });
    if (!id) {
      wx.showToast({ title: '门店不存在', icon: 'none' });
      return;
    }
    this.loadAll();
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  loadAll() {
    const id = this.data.storeId;
    this.setData({ loading: true, productsLoading: true });
    return Promise.allSettled([
      this.loadDetail(id),
      this.loadProducts(id)
    ]).finally(() => {
      this.setData({ loading: false });
    });
  },

  loadDetail(id) {
    return fetchStoreDetail(id)
      .then((res) => {
        if (res && res.code === 200 && res.data) {
          const detail = normalizeDetail(res.data);
          wx.setNavigationBarTitle({ title: detail.name || '门店详情' });
          this.setData({ detail });
        } else {
          wx.showToast({ title: (res && res.message) || '门店加载失败', icon: 'none' });
        }
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '门店加载失败', icon: 'none' });
      });
  },

  loadProducts(id) {
    return fetchMallProducts({ storeId: id })
      .then((res) => {
        const list = res && res.code === 200 && Array.isArray(res.data) ? res.data : [];
        this.setData({ products: list.map(normalizeProduct), productsLoading: false });
      })
      .catch(() => {
        this.setData({ productsLoading: false });
      });
  },

  onProductTap(e) {
    const productId = e.currentTarget.dataset.id;
    if (!productId) return;
    wx.navigateTo({ url: `/pages/product-detail/index?id=${productId}` });
  }
});
