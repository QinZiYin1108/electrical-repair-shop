const router = require("../../utils/router");
const { fetchAllTechnicians } = require("../../api/userOrderFlow");

Page({
  data: {
    loading: true,
    referenceAddressText: "",
    technicians: []
  },

  onLoad() {
    this.loadTechnicians();
  },

  onPullDownRefresh() {
    this.loadTechnicians(true);
  },

  loadTechnicians(fromPullDown) {
    this.setData({ loading: true });

    fetchAllTechnicians()
      .then((res) => {
        const data = res && res.code === 200 && res.data ? res.data : {};
        this.setData({
          referenceAddressText: data.referenceAddressDetail || "",
          technicians: Array.isArray(data.technicians) ? data.technicians : []
        });
      })
      .catch(() => {
        wx.showToast({
          title: "师傅列表加载失败",
          icon: "none"
        });
        this.setData({
          referenceAddressText: "",
          technicians: []
        });
      })
      .finally(() => {
        this.setData({ loading: false });
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  onViewProfile(e) {
    const technicianId = e.currentTarget.dataset.id;
    if (!technicianId) {
      return;
    }
    router.navigateTo({
      url: `/pages/technician-detail/index?technicianId=${technicianId}`
    });
  }
});
