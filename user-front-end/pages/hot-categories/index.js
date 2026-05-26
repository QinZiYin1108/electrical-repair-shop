const router = require("../../utils/router");
const { fetchHomePublicData } = require("../../api/userHome");

function decodeText(value) {
  if (!value) {
    return "";
  }
  try {
    return decodeURIComponent(value);
  } catch (error) {
    return value;
  }
}

function normalizeCategories(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    id: item.id || "",
    name: item.name || "维修分类",
    desc: item.desc || "常用维修服务",
    iconUrl: item.iconUrl || ""
  }));
}

Page({
  data: {
    loading: true,
    categories: [],
    focusCategoryId: "",
    focusCategoryName: ""
  },

  onLoad(options) {
    this.setData({
      focusCategoryId: (options && options.focusCategoryId) || "",
      focusCategoryName: decodeText(options && options.focusCategoryName)
    });
    this.loadCategories();
  },

  loadCategories() {
    this.setData({ loading: true });

    fetchHomePublicData()
      .then((res) => {
        const data = res && res.code === 200 && res.data ? res.data : {};
        const list = normalizeCategories(data.hotCategories);
        this.setData({
          categories: list
        });
      })
      .catch(() => {
        this.setData({
          categories: []
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onCategoryTap(e) {
    const categoryId = e.currentTarget.dataset.id || "";
    const categoryName = e.currentTarget.dataset.name || "";
    const query = [];
    if (categoryId) {
      query.push(`focusCategoryId=${encodeURIComponent(categoryId)}`);
    }
    if (categoryName) {
      query.push(`focusCategoryName=${encodeURIComponent(categoryName)}`);
    }
    query.push(`sourceTag=${encodeURIComponent("热门维修分类")}`);
    router.navigateTo({
      url: `/pages/category/index?${query.join("&")}`
    });
  },

  onViewAllCategories() {
    router.navigateTo({
      url: "/pages/category/index"
    });
  }
});
