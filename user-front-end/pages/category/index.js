const { fetchCategoryTree } = require("../../api/userOrderFlow");
const { fetchHomePublicData } = require("../../api/userHome");
const router = require("../../utils/router");

const ICON_TEXT_MAP = {
  空调: "空",
  冰箱: "冰",
  洗衣机: "洗",
  热水器: "热",
  油烟机: "烟",
  电视: "视"
};

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

function normalizeTree(list) {
  return (Array.isArray(list) ? list : []).map((level1) => ({
    id: level1.id || "",
    name: level1.name || "未命名分类",
    children: (Array.isArray(level1.children) ? level1.children : []).map((level2) => ({
      id: level2.id || "",
      name: level2.name || "未命名子类",
      children: (Array.isArray(level2.children) ? level2.children : []).map((level3) => ({
        id: level3.id || "",
        name: level3.name || "未命名项目"
      }))
    }))
  }));
}

function findTreeSelection(level1List, focusCategoryId) {
  if (!level1List.length) {
    return {
      activeLevel1Id: "",
      activeLevel2Id: ""
    };
  }

  let targetLevel1 = level1List[0];
  let targetLevel2 = (targetLevel1.children || [])[0] || null;

  if (focusCategoryId) {
    level1List.some((level1) => {
      return (level1.children || []).some((level2) => {
        const matched = (level2.children || []).some((level3) => level3.id === focusCategoryId);
        if (matched) {
          targetLevel1 = level1;
          targetLevel2 = level2;
        }
        return matched;
      });
    });
  }

  if (!targetLevel2 && targetLevel1) {
    targetLevel2 = (targetLevel1.children || [])[0] || null;
  }

  return {
    activeLevel1Id: targetLevel1 ? targetLevel1.id : "",
    activeLevel2Id: targetLevel2 ? targetLevel2.id : ""
  };
}

function buildHotCategoryMap(list) {
  const map = {};
  (Array.isArray(list) ? list : []).forEach((item) => {
    if (item && item.id) {
      map[item.id] = item.iconUrl || "";
    }
  });
  return map;
}

function getCategoryIconText(name) {
  const text = String(name || "");
  const key = Object.keys(ICON_TEXT_MAP).find((item) => text.indexOf(item) !== -1);
  if (key) {
    return ICON_TEXT_MAP[key];
  }
  return text.slice(0, 1) || "修";
}

function buildLevel3List(level1List, activeLevel1Id, activeLevel2Id, hotCategoryMap) {
  const activeLevel1 = (level1List || []).find((item) => item.id === activeLevel1Id) || null;
  const activeLevel2 = activeLevel1
    ? (activeLevel1.children || []).find((item) => item.id === activeLevel2Id) || null
    : null;
  const level3List = activeLevel2 ? activeLevel2.children || [] : [];

  return {
    activeLevel1Name: activeLevel1 ? activeLevel1.name : "",
    activeLevel2Name: activeLevel2 ? activeLevel2.name : "",
    level3List: level3List.map((item) => ({
      id: item.id,
      name: item.name,
      iconUrl: hotCategoryMap[item.id] || "",
      iconText: getCategoryIconText(item.name)
    }))
  };
}

Page({
  data: {
    loading: true,
    level1List: [],
    activeLevel1Id: "",
    activeLevel2Id: "",
    activeLevel1Name: "",
    activeLevel2Name: "",
    level3List: [],
    focusCategoryId: "",
    focusCategoryName: "",
    sourceTag: ""
  },

  onLoad(options) {
    this.setData({
      focusCategoryId: (options && options.focusCategoryId) || "",
      focusCategoryName: decodeText(options && options.focusCategoryName),
      sourceTag: decodeText(options && options.sourceTag)
    });
    this.loadCategoryTree();
  },

  loadCategoryTree() {
    this.setData({ loading: true });

    Promise.all([
      fetchCategoryTree("").catch(() => null),
      fetchHomePublicData().catch(() => null)
    ])
      .then((result) => {
        const categoryRes = result[0];
        const homeRes = result[1];

        const level1List = normalizeTree(
          categoryRes && categoryRes.code === 200 && Array.isArray(categoryRes.data)
            ? categoryRes.data
            : []
        );
        const hotCategoryMap = buildHotCategoryMap(
          homeRes && homeRes.code === 200 && homeRes.data ? homeRes.data.hotCategories : []
        );
        const selection = findTreeSelection(level1List, this.data.focusCategoryId);
        const level3State = buildLevel3List(
          level1List,
          selection.activeLevel1Id,
          selection.activeLevel2Id,
          hotCategoryMap
        );

        this._hotCategoryMap = hotCategoryMap;
        this.setData(
          Object.assign(
            {
              level1List,
              activeLevel1Id: selection.activeLevel1Id,
              activeLevel2Id: selection.activeLevel2Id
            },
            level3State
          )
        );
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onLevel1Tap(e) {
    const level1Id = e.currentTarget.dataset.id || "";
    const level1 = (this.data.level1List || []).find((item) => item.id === level1Id) || null;
    const firstLevel2 = level1 && level1.children && level1.children.length ? level1.children[0] : null;
    const activeLevel2Id = firstLevel2 ? firstLevel2.id : "";
    const level3State = buildLevel3List(
      this.data.level1List,
      level1Id,
      activeLevel2Id,
      this._hotCategoryMap || {}
    );
    this.setData(
      Object.assign(
        {
          activeLevel1Id: level1Id,
          activeLevel2Id
        },
        level3State
      )
    );
  },

  onLevel2Tap(e) {
    const level1Id = e.currentTarget.dataset.level1Id || "";
    const level2Id = e.currentTarget.dataset.level2Id || "";
    const level3State = buildLevel3List(
      this.data.level1List,
      level1Id,
      level2Id,
      this._hotCategoryMap || {}
    );
    this.setData(
      Object.assign(
        {
          activeLevel1Id: level1Id,
          activeLevel2Id: level2Id
        },
        level3State
      )
    );
  },

  onCategoryTap(e) {
    const categoryId = e.currentTarget.dataset.id || "";
    const categoryName = e.currentTarget.dataset.name || "";
    if (!categoryId) {
      return;
    }

    router.navigateTo({
      url: `/pages/category-detail/index?focusCategoryId=${encodeURIComponent(categoryId)}&focusCategoryName=${encodeURIComponent(categoryName)}`
    });
  }
});
