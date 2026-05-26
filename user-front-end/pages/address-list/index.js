const router = require("../../utils/router");
const userAddressApi = require("../../api/userAddress");

Page({
  data: {
    loading: false,
    addressList: []
  },

  onShow() {
    this.loadAddressList();
  },

  loadAddressList() {
    this.setData({
      loading: true
    });
    userAddressApi
      .listUserAddresses()
      .then((resp) => {
        if (resp.code === 200) {
          this.setData({
            addressList: Array.isArray(resp.data) ? resp.data : []
          });
          return;
        }
        wx.showToast({
          title: resp.message || "地址加载失败",
          icon: "none"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "地址加载失败",
          icon: "none"
        });
      })
      .finally(() => {
        this.setData({
          loading: false
        });
      });
  },

  onAddAddress() {
    router.navigateTo({
      url: "/pages/address-edit/index"
    });
  },

  onEditAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    if (!addressId) {
      return;
    }
    router.navigateTo({
      url: `/pages/address-edit/index?id=${addressId}`
    });
  },

  onSetDefaultAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    if (!addressId) {
      return;
    }
    wx.showLoading({
      title: "设置中..."
    });
    userAddressApi
      .setDefaultUserAddress(addressId)
      .then((resp) => {
        if (resp.code === 200) {
          wx.showToast({
            title: "已设为默认",
            icon: "success"
          });
          this.loadAddressList();
          return;
        }
        wx.showToast({
          title: resp.message || "设置失败",
          icon: "none"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "设置失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onDeleteAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    if (!addressId) {
      return;
    }
    wx.showModal({
      title: "提示",
      content: "确认删除该地址吗？",
      confirmText: "删除",
      cancelText: "取消",
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        this.deleteAddress(addressId);
      }
    });
  },

  deleteAddress(addressId) {
    wx.showLoading({
      title: "删除中..."
    });
    userAddressApi
      .deleteUserAddress(addressId)
      .then((resp) => {
        if (resp.code === 200) {
          wx.showToast({
            title: "删除成功",
            icon: "success"
          });
          this.loadAddressList();
          return;
        }
        wx.showToast({
          title: resp.message || "删除失败",
          icon: "none"
        });
      })
      .catch(() => {
        wx.showToast({
          title: "删除失败",
          icon: "none"
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  }
});
