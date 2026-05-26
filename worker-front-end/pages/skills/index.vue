<template>
  <view class="page worker-skills-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">{{ i18n.pageTitle }}</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view class="content" scroll-y>
      <view class="hero-card">
        <view>
          <text class="hero-title">{{ i18n.mySkills }}</text>
          <text class="hero-subtitle">{{ filteredSkills.length }} {{ i18n.items }} / {{ skills.length }} {{ i18n.total }}</text>
        </view>
        <view class="hero-icon">
          <u-icon name="grid" size="24" color="#1677ff" />
        </view>
      </view>

      <view class="card filter-card">
        <view class="search-row">
          <input
            v-model="skillKeywordInput"
            class="search-input"
            confirm-type="search"
            :placeholder="i18n.skillSearchPlaceholder"
            @confirm="applySkillSearch"
          />
          <view class="icon-btn icon-btn-primary" @click="applySkillSearch">
            <u-icon name="search" size="18" color="#ffffff" />
          </view>
        </view>

        <view class="current-tree-wrap">
          <view class="tree-section-head" @click="toggleCurrentCategoryTree">
            <text class="tree-section-title">{{ i18n.currentCategoryFilter }}</text>
            <view class="tree-section-toggle">
              <text class="tree-section-toggle-text">{{ showCurrentCategoryTree ? i18n.collapse : i18n.expand }}</text>
              <u-icon :name="showCurrentCategoryTree ? 'arrow-up' : 'arrow-down'" size="14" color="#64748b" />
            </view>
          </view>
          <view v-if="showCurrentCategoryTree && currentCategoryTree.length" class="tree-wrap current-tree-panel">
            <view class="tree-col">
              <text class="tree-title">{{ i18n.level1Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedCurrentLevel1Id === '' ? 'tree-item-active' : ''"
                  @click="onCurrentLevel1Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in currentLevel1List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedCurrentLevel1Id === item.id ? 'tree-item-active' : ''"
                  @click="onCurrentLevel1Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>

            <view class="tree-col">
              <text class="tree-title">{{ i18n.level2Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedCurrentLevel2Id === '' ? 'tree-item-active' : ''"
                  @click="onCurrentLevel2Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in currentLevel2List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedCurrentLevel2Id === item.id ? 'tree-item-active' : ''"
                  @click="onCurrentLevel2Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>

            <view class="tree-col">
              <text class="tree-title">{{ i18n.level3Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedCurrentLevel3Id === '' ? 'tree-item-active' : ''"
                  @click="onCurrentLevel3Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in currentLevel3List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedCurrentLevel3Id === item.id ? 'tree-item-active' : ''"
                  @click="onCurrentLevel3Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>
          </view>
          <view v-else-if="showCurrentCategoryTree" class="placeholder filter-placeholder">{{ i18n.emptyCurrentCategories }}</view>
        </view>
      </view>

      <view class="card">
        <view class="section-head">
          <text class="section-title">{{ i18n.skillList }}</text>
          <text class="section-count">{{ filteredSkills.length }} {{ i18n.items }}</text>
        </view>

        <view v-if="loadingSkills" class="placeholder">{{ i18n.loading }}</view>
        <view v-else-if="!skills.length" class="placeholder">{{ i18n.emptySkills }}</view>
        <view v-else-if="!filteredSkills.length" class="placeholder">{{ i18n.emptyFilteredSkills }}</view>
        <view v-else class="skill-list">
          <view
            v-for="skill in filteredSkills"
            :key="skill.serviceTypeId"
            class="skill-card"
          >
            <view class="skill-card-main">
              <view class="skill-card-top">
                <text class="skill-name">{{ skill.serviceTypeName || i18n.unknownService }}</text>
                <text class="skill-level">{{ skill.skillLevelText || i18n.levelPrimary }}</text>
              </view>
              <text class="skill-category">{{ formatSkillCategory(skill) }}</text>
              <view class="skill-tags">
                <text class="skill-tag">{{ skill.serviceModeText || i18n.modeUnknown }}</text>
                <text class="skill-tag skill-tag-light">{{ formatSkillStatus(skill) }}</text>
              </view>
            </view>
            <view
              class="icon-btn icon-btn-danger icon-btn-small"
              :class="deletingServiceTypeId === skill.serviceTypeId ? 'icon-btn-disabled' : ''"
              @click="onDeleteSkill(skill)"
            >
              <u-icon
                :name="deletingServiceTypeId === skill.serviceTypeId ? 'reload' : 'trash'"
                size="18"
                color="#f56c6c"
              />
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="fab-btn" @click="openAddPopup">
      <u-icon name="plus" size="26" color="#ffffff" />
    </view>

    <u-popup
      v-model:show="showAddPopup"
      mode="bottom"
      round="24"
      :closeable="true"
      :safe-area-inset-bottom="true"
    >
      <view class="popup-panel">
        <view class="popup-header">
          <text class="popup-title">{{ i18n.addSkills }}</text>
          <text class="popup-subtitle">{{ availableServiceTypes.length }} {{ i18n.items }}</text>
        </view>

        <scroll-view class="popup-body" scroll-y>
          <view class="search-row popup-search-row">
            <input
              v-model="addKeywordInput"
              class="search-input"
              confirm-type="search"
              :placeholder="i18n.addSearchPlaceholder"
              @confirm="applyAddSearch"
            />
            <view class="icon-btn icon-btn-primary" @click="applyAddSearch">
              <u-icon name="search" size="18" color="#ffffff" />
            </view>
          </view>

          <view class="mode-row">
            <view
              v-for="mode in modeOptions"
              :key="mode.value"
              class="mode-chip"
              :class="selectedServiceMode === mode.value ? 'mode-chip-active' : ''"
              @click="onServiceModeChange(mode.value)"
            >
              {{ mode.label }}
            </view>
          </view>

          <view class="tree-section-head" @click="toggleAddCategoryTree">
            <text class="tree-section-title">{{ i18n.addCategoryFilter }}</text>
            <view class="tree-section-toggle">
              <text class="tree-section-toggle-text">{{ showAddCategoryTree ? i18n.collapse : i18n.expand }}</text>
              <u-icon :name="showAddCategoryTree ? 'arrow-up' : 'arrow-down'" size="14" color="#64748b" />
            </view>
          </view>

          <view v-if="showAddCategoryTree" class="tree-wrap">
            <view class="tree-col">
              <text class="tree-title">{{ i18n.level1Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedLevel1Id === '' ? 'tree-item-active' : ''"
                  @click="onLevel1Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in level1List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedLevel1Id === item.id ? 'tree-item-active' : ''"
                  @click="onLevel1Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>

            <view class="tree-col">
              <text class="tree-title">{{ i18n.level2Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedLevel2Id === '' ? 'tree-item-active' : ''"
                  @click="onLevel2Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in level2List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedLevel2Id === item.id ? 'tree-item-active' : ''"
                  @click="onLevel2Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>

            <view class="tree-col">
              <text class="tree-title">{{ i18n.level3Category }}</text>
              <scroll-view class="tree-scroll" scroll-y>
                <view
                  class="tree-item"
                  :class="selectedLevel3Id === '' ? 'tree-item-active' : ''"
                  @click="onLevel3Select(null)"
                >
                  {{ i18n.all }}
                </view>
                <view
                  v-for="item in level3List"
                  :key="item.id"
                  class="tree-item"
                  :class="selectedLevel3Id === item.id ? 'tree-item-active' : ''"
                  @click="onLevel3Select(item)"
                >
                  {{ item.name }}
                </view>
              </scroll-view>
            </view>
          </view>

          <view class="service-header">
            <text class="service-title">{{ i18n.availableServiceTypes }}</text>
            <text class="service-count">{{ availableServiceTypes.length }} {{ i18n.items }}</text>
          </view>

          <view v-if="loadingAvailable" class="placeholder">{{ i18n.loading }}</view>
          <view v-else-if="!availableServiceTypes.length" class="placeholder">{{ i18n.emptyAvailable }}</view>
          <checkbox-group v-else @change="onSelectedServiceTypesChange">
            <label
              v-for="item in availableServiceTypes"
              :key="item.id"
              class="service-row"
            >
              <view class="service-check">
                <checkbox
                  :value="item.id"
                  :checked="selectedServiceTypeIds.includes(item.id)"
                  color="#1677ff"
                />
              </view>
              <view class="service-main">
                <text class="service-name">{{ item.name }}</text>
                <text class="service-meta">{{ item.typeText || formatServiceMode(item.type) }}</text>
                <text class="service-meta">{{ item.categoryPath || item.categoryName || i18n.uncategorized }}</text>
              </view>
            </label>
          </checkbox-group>
        </scroll-view>

        <view class="popup-footer">
          <text class="selected-text">{{ i18n.selected }} {{ selectedServiceTypeIds.length }} {{ i18n.items }}</text>
          <u-button
            class="popup-submit"
            :text="i18n.addSelected"
            type="primary"
            shape="circle"
            :disabled="!selectedServiceTypeIds.length"
            :loading="adding"
            @click="onBatchAddSkills"
          />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import {
  batchAddWorkerSkills,
  deleteWorkerSkill,
  getWorkerAvailableSkillCategoryTree,
  getWorkerAvailableSkillServiceTypes,
  getWorkerSkills
} from '@/api/workerSkill';

const I18N = {
  pageTitle: '技能管理',
  mySkills: '当前技能',
  skillList: '技能列表',
  addSkills: '添加技能',
  loading: '加载中...',
  emptySkills: '暂未添加任何技能',
  emptyFilteredSkills: '当前筛选条件下没有技能',
  unknownService: '未知服务',
  uncategorized: '未分类',
  levelPrimary: '初级',
  skillSearchPlaceholder: '搜索当前技能名称或服务分类',
  addSearchPlaceholder: '搜索可添加的服务类型',
  currentCategoryFilter: '当前技能分类',
  addCategoryFilter: '可添加分类',
  emptyCurrentCategories: '暂无可筛选的服务分类',
  level1Category: '一级分类',
  level2Category: '二级分类',
  level3Category: '三级分类',
  all: '全部',
  expand: '展开',
  collapse: '收起',
  allCategories: '全部分类',
  availableServiceTypes: '可添加技能',
  emptyAvailable: '当前条件下没有可添加技能',
  selected: '已选',
  items: '项',
  total: '总计',
  addSelected: '添加所选技能',
  statusEnabled: '已启用',
  statusDisabled: '已停用',
  modeAll: '全部服务方式',
  modeOnsiteRepair: '上门维修',
  modeOnsiteInstall: '上门安装',
  modeOfflineRepair: '到店维修',
  modeUnknown: '未知类型',
  msgLoadSkillsFailed: '获取技能失败',
  msgLoadCategoriesFailed: '获取分类失败',
  msgLoadServiceTypesFailed: '获取服务类型失败',
  msgAddSuccess: '添加成功',
  msgAddFailed: '添加失败',
  msgDeleteSuccess: '删除成功',
  msgDeleteFailed: '删除失败',
  dialogTitle: '提示',
  dialogDeleteConfirm: '确定删除该技能吗？'
};

function normalizeText(value) {
  return String(value || '').trim().toLowerCase();
}

function splitCategoryPath(value) {
  return String(value || '')
    .split('/')
    .map((item) => String(item || '').trim())
    .filter(Boolean);
}

function buildCurrentSkillCategoryTree(skills, uncategorizedLabel) {
  const root = [];
  const nodeMap = new Map();

  (Array.isArray(skills) ? skills : []).forEach((skill) => {
    const segments = splitCategoryPath(skill && (skill.categoryPath || skill.categoryName));
    if (!segments.length) {
      if (!nodeMap.has('uncategorized')) {
        const node = {
          id: 'uncategorized',
          name: uncategorizedLabel,
          pathKey: uncategorizedLabel,
          children: []
        };
        nodeMap.set('uncategorized', node);
        root.push(node);
      }
      return;
    }

    let parentKey = '';
    let siblings = root;
    segments.forEach((segment, index) => {
      const pathKey = parentKey ? `${parentKey} / ${segment}` : segment;
      let node = nodeMap.get(pathKey);
      if (!node) {
        node = {
          id: index === segments.length - 1 && skill && skill.categoryId ? skill.categoryId : `path:${pathKey}`,
          name: segment,
          pathKey,
          children: []
        };
        nodeMap.set(pathKey, node);
        siblings.push(node);
      }
      parentKey = pathKey;
      siblings = node.children;
    });
  });

  return root;
}

function containsCategoryId(nodes, targetId) {
  return (Array.isArray(nodes) ? nodes : []).some((node) => {
    if (!node) return false;
    if (node.id === targetId) return true;
    return containsCategoryId(node.children, targetId);
  });
}

function findCategoryNodeById(nodes, targetId) {
  for (const node of (Array.isArray(nodes) ? nodes : [])) {
    if (!node) continue;
    if (node.id === targetId) return node;
    const child = findCategoryNodeById(node.children, targetId);
    if (child) return child;
  }
  return null;
}

export default {
  name: 'WorkerSkillPage',
  data() {
    return {
      i18n: I18N,
      loadingSkills: false,
      loadingAvailable: false,
      adding: false,
      deletingServiceTypeId: '',
      showAddPopup: false,
      showCurrentCategoryTree: true,
      showAddCategoryTree: true,
      skills: [],
      categoryTree: [],
      availableServiceTypes: [],
      skillKeywordInput: '',
      skillKeyword: '',
      selectedCurrentLevel1Id: '',
      selectedCurrentLevel2Id: '',
      selectedCurrentLevel3Id: '',
      selectedCurrentCategoryPath: '',
      addKeywordInput: '',
      addKeyword: '',
      selectedServiceMode: 0,
      selectedLevel1Id: '',
      selectedLevel2Id: '',
      selectedLevel3Id: '',
      selectedCategoryId: '',
      selectedServiceTypeIds: [],
      modeOptions: [
        { label: I18N.modeAll, value: 0 },
        { label: I18N.modeOnsiteRepair, value: 1 },
        { label: I18N.modeOnsiteInstall, value: 2 },
        { label: I18N.modeOfflineRepair, value: 3 }
      ]
    };
  },
  computed: {
    level1List() {
      return Array.isArray(this.categoryTree) ? this.categoryTree : [];
    },
    level2List() {
      const current = this.level1List.find((item) => item.id === this.selectedLevel1Id);
      return current && Array.isArray(current.children) ? current.children : [];
    },
    level3List() {
      const current = this.level2List.find((item) => item.id === this.selectedLevel2Id);
      return current && Array.isArray(current.children) ? current.children : [];
    },
    currentCategoryTree() {
      return buildCurrentSkillCategoryTree(this.skills, this.i18n.uncategorized);
    },
    currentLevel1List() {
      return Array.isArray(this.currentCategoryTree) ? this.currentCategoryTree : [];
    },
    currentLevel2List() {
      const current = this.currentLevel1List.find((item) => item.id === this.selectedCurrentLevel1Id);
      return current && Array.isArray(current.children) ? current.children : [];
    },
    currentLevel3List() {
      const current = this.currentLevel2List.find((item) => item.id === this.selectedCurrentLevel2Id);
      return current && Array.isArray(current.children) ? current.children : [];
    },
    filteredSkills() {
      const keyword = normalizeText(this.skillKeyword);
      return this.skills.filter((skill) => {
        if (!skill) return false;
        const categoryPath = this.formatSkillCategory(skill);
        const matchesCategory = !this.selectedCurrentCategoryPath
          || categoryPath === this.selectedCurrentCategoryPath
          || categoryPath.startsWith(`${this.selectedCurrentCategoryPath} / `);
        if (!matchesCategory) {
          return false;
        }
        if (!keyword) {
          return true;
        }
        const searchText = normalizeText([
          skill.serviceTypeName,
          skill.categoryName,
          skill.categoryPath,
          skill.serviceModeText
        ].join(' '));
        return searchText.includes(keyword);
      });
    }
  },
  onShow() {
    this.loadSkills();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    formatServiceMode(type) {
      if (type === 1) return this.i18n.modeOnsiteRepair;
      if (type === 2) return this.i18n.modeOnsiteInstall;
      if (type === 3) return this.i18n.modeOfflineRepair;
      return this.i18n.modeUnknown;
    },
    formatSkillCategory(skill) {
      if (!skill) return this.i18n.uncategorized;
      return skill.categoryPath || skill.categoryName || this.i18n.uncategorized;
    },
    formatSkillStatus(skill) {
      return skill && skill.isActive === 0 ? this.i18n.statusDisabled : this.i18n.statusEnabled;
    },
    toggleCurrentCategoryTree() {
      this.showCurrentCategoryTree = !this.showCurrentCategoryTree;
    },
    toggleAddCategoryTree() {
      this.showAddCategoryTree = !this.showAddCategoryTree;
    },
    buildAddQueryParams() {
      const params = {};
      if (this.addKeyword) params.keyword = this.addKeyword;
      if (this.selectedServiceMode) params.serviceMode = this.selectedServiceMode;
      if (this.selectedCategoryId) params.categoryId = this.selectedCategoryId;
      return params;
    },
    resetAddFilters() {
      this.addKeywordInput = '';
      this.addKeyword = '';
      this.selectedServiceMode = 0;
      this.selectedLevel1Id = '';
      this.selectedLevel2Id = '';
      this.selectedLevel3Id = '';
      this.selectedCategoryId = '';
      this.selectedServiceTypeIds = [];
    },
    loadSkills() {
      this.loadingSkills = true;
      return getWorkerSkills()
        .then((res) => {
          if (res && res.code === 200 && Array.isArray(res.data)) {
            this.skills = res.data;
            this.syncCurrentCategorySelection();
            return;
          }
          this.skills = [];
          this.syncCurrentCategorySelection(true);
          uni.showToast({
            title: (res && res.message) || this.i18n.msgLoadSkillsFailed,
            icon: 'none'
          });
        })
        .catch(() => {
          this.skills = [];
          this.syncCurrentCategorySelection(true);
          uni.showToast({
            title: this.i18n.msgLoadSkillsFailed,
            icon: 'none'
          });
        })
        .finally(() => {
          this.loadingSkills = false;
        });
    },
    loadCategoryTree(resetSelection = false) {
      const params = {};
      if (this.addKeyword) params.keyword = this.addKeyword;
      if (this.selectedServiceMode) params.serviceMode = this.selectedServiceMode;
      return getWorkerAvailableSkillCategoryTree(params)
        .then((res) => {
          if (res && res.code === 200 && Array.isArray(res.data)) {
            this.categoryTree = res.data;
            this.syncCategorySelection(resetSelection);
            return;
          }
          this.categoryTree = [];
          this.syncCategorySelection(true);
          uni.showToast({
            title: (res && res.message) || this.i18n.msgLoadCategoriesFailed,
            icon: 'none'
          });
        })
        .catch(() => {
          this.categoryTree = [];
          this.syncCategorySelection(true);
          uni.showToast({
            title: this.i18n.msgLoadCategoriesFailed,
            icon: 'none'
          });
        });
    },
    loadAvailableServiceTypes() {
      this.loadingAvailable = true;
      return getWorkerAvailableSkillServiceTypes(this.buildAddQueryParams())
        .then((res) => {
          if (res && res.code === 200 && Array.isArray(res.data)) {
            this.availableServiceTypes = res.data;
            const availableIds = new Set(res.data.map((item) => item.id));
            this.selectedServiceTypeIds = this.selectedServiceTypeIds.filter((id) => availableIds.has(id));
            return;
          }
          this.availableServiceTypes = [];
          this.selectedServiceTypeIds = [];
          uni.showToast({
            title: (res && res.message) || this.i18n.msgLoadServiceTypesFailed,
            icon: 'none'
          });
        })
        .catch(() => {
          this.availableServiceTypes = [];
          this.selectedServiceTypeIds = [];
          uni.showToast({
            title: this.i18n.msgLoadServiceTypesFailed,
            icon: 'none'
          });
        })
        .finally(() => {
          this.loadingAvailable = false;
        });
    },
    refreshAddPanel(resetSelection = false) {
      this.selectedServiceTypeIds = [];
      return this.loadCategoryTree(resetSelection).then(() => this.loadAvailableServiceTypes());
    },
    syncCategorySelection(forceReset = false) {
      if (forceReset) {
        this.selectedLevel1Id = '';
        this.selectedLevel2Id = '';
        this.selectedLevel3Id = '';
        this.selectedCategoryId = '';
        return;
      }

      const allIds = new Set();
      const walk = (nodes = []) => {
        nodes.forEach((node) => {
          if (!node || !node.id) return;
          allIds.add(node.id);
          if (Array.isArray(node.children) && node.children.length) {
            walk(node.children);
          }
        });
      };
      walk(this.level1List);

      if (this.selectedCategoryId && !allIds.has(this.selectedCategoryId)) {
        this.selectedLevel1Id = '';
        this.selectedLevel2Id = '';
        this.selectedLevel3Id = '';
        this.selectedCategoryId = '';
      }
    },
    applySkillSearch() {
      this.skillKeyword = (this.skillKeywordInput || '').trim();
    },
    syncCurrentCategorySelection(forceReset = false) {
      if (forceReset || !this.currentCategoryTree.length) {
        this.selectedCurrentLevel1Id = '';
        this.selectedCurrentLevel2Id = '';
        this.selectedCurrentLevel3Id = '';
        this.selectedCurrentCategoryPath = '';
        return;
      }
      if (this.selectedCurrentLevel1Id && !containsCategoryId(this.currentCategoryTree, this.selectedCurrentLevel1Id)) {
        this.selectedCurrentLevel1Id = '';
        this.selectedCurrentLevel2Id = '';
        this.selectedCurrentLevel3Id = '';
        this.selectedCurrentCategoryPath = '';
        return;
      }
      if (this.selectedCurrentLevel2Id && !containsCategoryId(this.currentCategoryTree, this.selectedCurrentLevel2Id)) {
        this.selectedCurrentLevel2Id = '';
        this.selectedCurrentLevel3Id = '';
      }
      if (this.selectedCurrentLevel3Id && !containsCategoryId(this.currentCategoryTree, this.selectedCurrentLevel3Id)) {
        this.selectedCurrentLevel3Id = '';
      }
    },
    onCurrentLevel1Select(item) {
      this.selectedCurrentLevel1Id = item && item.id ? item.id : '';
      this.selectedCurrentLevel2Id = '';
      this.selectedCurrentLevel3Id = '';
      this.selectedCurrentCategoryPath = item && item.pathKey ? item.pathKey : '';
    },
    onCurrentLevel2Select(item) {
      this.selectedCurrentLevel2Id = item && item.id ? item.id : '';
      this.selectedCurrentLevel3Id = '';
      if (item && item.pathKey) {
        this.selectedCurrentCategoryPath = item.pathKey;
        return;
      }
      const parent = findCategoryNodeById(this.currentCategoryTree, this.selectedCurrentLevel1Id);
      this.selectedCurrentCategoryPath = parent && parent.pathKey ? parent.pathKey : '';
    },
    onCurrentLevel3Select(item) {
      this.selectedCurrentLevel3Id = item && item.id ? item.id : '';
      if (item && item.pathKey) {
        this.selectedCurrentCategoryPath = item.pathKey;
        return;
      }
      const level2 = findCategoryNodeById(this.currentCategoryTree, this.selectedCurrentLevel2Id);
      if (level2 && level2.pathKey) {
        this.selectedCurrentCategoryPath = level2.pathKey;
        return;
      }
      const level1 = findCategoryNodeById(this.currentCategoryTree, this.selectedCurrentLevel1Id);
      this.selectedCurrentCategoryPath = level1 && level1.pathKey ? level1.pathKey : '';
    },
    openAddPopup() {
      this.showAddPopup = true;
      this.showAddCategoryTree = true;
      this.resetAddFilters();
      this.refreshAddPanel(true).catch(() => {});
    },
    applyAddSearch() {
      this.addKeyword = (this.addKeywordInput || '').trim();
      this.refreshAddPanel(true);
    },
    onServiceModeChange(mode) {
      if (this.selectedServiceMode === mode) return;
      this.selectedServiceMode = mode;
      this.refreshAddPanel(true);
    },
    onLevel1Select(item) {
      this.selectedLevel1Id = item && item.id ? item.id : '';
      this.selectedLevel2Id = '';
      this.selectedLevel3Id = '';
      this.selectedCategoryId = this.selectedLevel1Id;
      this.selectedServiceTypeIds = [];
      this.loadAvailableServiceTypes();
    },
    onLevel2Select(item) {
      this.selectedLevel2Id = item && item.id ? item.id : '';
      this.selectedLevel3Id = '';
      this.selectedCategoryId = this.selectedLevel2Id || this.selectedLevel1Id;
      this.selectedServiceTypeIds = [];
      this.loadAvailableServiceTypes();
    },
    onLevel3Select(item) {
      this.selectedLevel3Id = item && item.id ? item.id : '';
      this.selectedCategoryId = this.selectedLevel3Id || this.selectedLevel2Id || this.selectedLevel1Id;
      this.selectedServiceTypeIds = [];
      this.loadAvailableServiceTypes();
    },
    onSelectedServiceTypesChange(event) {
      this.selectedServiceTypeIds = (event && event.detail && Array.isArray(event.detail.value))
        ? event.detail.value
        : [];
    },
    onBatchAddSkills() {
      if (!this.selectedServiceTypeIds.length || this.adding) return;
      this.adding = true;
      batchAddWorkerSkills(this.selectedServiceTypeIds)
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({
              title: this.i18n.msgAddSuccess,
              icon: 'success'
            });
            this.showAddPopup = false;
            this.resetAddFilters();
            this.availableServiceTypes = [];
            this.categoryTree = [];
            return this.loadSkills();
          }
          uni.showToast({
            title: (res && res.message) || this.i18n.msgAddFailed,
            icon: 'none'
          });
          return null;
        })
        .catch(() => {
          uni.showToast({
            title: this.i18n.msgAddFailed,
            icon: 'none'
          });
        })
        .finally(() => {
          this.adding = false;
        });
    },
    onDeleteSkill(skill) {
      if (!skill || !skill.serviceTypeId || this.deletingServiceTypeId) return;
      uni.showModal({
        title: this.i18n.dialogTitle,
        content: this.i18n.dialogDeleteConfirm,
        success: (modalRes) => {
          if (!modalRes.confirm) return;
          this.deletingServiceTypeId = skill.serviceTypeId;
          deleteWorkerSkill(skill.serviceTypeId)
            .then((res) => {
              if (res && res.code === 200) {
                uni.showToast({
                  title: this.i18n.msgDeleteSuccess,
                  icon: 'success'
                });
                this.loadSkills();
                return;
              }
              uni.showToast({
                title: (res && res.message) || this.i18n.msgDeleteFailed,
                icon: 'none'
              });
            })
            .catch(() => {
              uni.showToast({
                title: this.i18n.msgDeleteFailed,
                icon: 'none'
              });
            })
            .finally(() => {
              this.deletingServiceTypeId = '';
            });
        }
      });
    }
  }
};
</script>

<style scoped>
.worker-skills-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f3f7ff 0%, #f7f7f8 38%, #f5f5f5 100%);
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  background-color: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-left,
.nav-right {
  width: 120rpx;
  display: flex;
  align-items: center;
}

.nav-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
}

.content {
  height: calc(100vh - var(--status-bar-height) - 88rpx);
  padding: 16rpx 24rpx 160rpx;
  box-sizing: border-box;
}

.hero-card,
.card {
  background-color: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.06);
}

.hero-card {
  margin-bottom: 20rpx;
  padding: 28rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #ffffff 0%, #f4f8ff 100%);
}

.hero-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #7c8aa5;
}

.hero-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 24rpx;
  background-color: #edf4ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  padding: 22rpx;
  margin-bottom: 18rpx;
}

.filter-card {
  padding-bottom: 18rpx;
}

.current-tree-wrap {
  margin-top: 18rpx;
}

.tree-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.tree-section-title {
  font-size: 25rpx;
  font-weight: 600;
  color: #334155;
}

.tree-section-toggle {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.tree-section-toggle-text {
  font-size: 22rpx;
  color: #64748b;
}

.current-tree-panel {
  min-height: 240rpx;
  margin-bottom: 0;
}

.filter-placeholder {
  padding: 28rpx 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.section-count {
  font-size: 22rpx;
  color: #909399;
}

.search-row {
  display: flex;
  align-items: center;
}

.popup-search-row {
  margin-bottom: 18rpx;
}

.search-input {
  flex: 1;
  height: 74rpx;
  border-radius: 16rpx;
  border: 1rpx solid #dbe4f0;
  padding: 0 22rpx;
  font-size: 26rpx;
  box-sizing: border-box;
  background-color: #f8fbff;
}

.mode-chip {
  padding: 12rpx 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid #dce6f2;
  background-color: #f8fbff;
  color: #5b6475;
  font-size: 24rpx;
  line-height: 1;
  white-space: nowrap;
}

.mode-chip-active {
  color: #1677ff;
  border-color: #1677ff;
  background-color: #ecf5ff;
}

.placeholder {
  padding: 36rpx 0;
  font-size: 24rpx;
  color: #909399;
  text-align: center;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.skill-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 20rpx;
  border-radius: 20rpx;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1rpx solid #edf2f7;
}

.skill-card-main {
  flex: 1;
  min-width: 0;
}

.skill-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.skill-name {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.skill-level {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background-color: #eef4ff;
  font-size: 22rpx;
  color: #2563eb;
}

.skill-category {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #667085;
  line-height: 1.5;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 12rpx;
}

.skill-tag {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background-color: #eff6ff;
  font-size: 22rpx;
  color: #1677ff;
  line-height: 1;
}

.skill-tag-light {
  background-color: #f5f7fa;
  color: #7b8794;
}

.mode-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.tree-wrap {
  display: flex;
  border-radius: 16rpx;
  border: 1rpx solid #ebeef5;
  overflow: hidden;
  margin-bottom: 18rpx;
  min-height: 260rpx;
}

.tree-col {
  flex: 1;
  border-right: 1rpx solid #ebeef5;
  background-color: #ffffff;
}

.tree-col:last-child {
  border-right: none;
}

.tree-title {
  display: block;
  padding: 14rpx 16rpx;
  font-size: 22rpx;
  color: #909399;
  border-bottom: 1rpx solid #f2f6fc;
}

.tree-scroll {
  height: 240rpx;
}

.tree-item {
  padding: 16rpx;
  font-size: 24rpx;
  color: #606266;
  border-bottom: 1rpx solid #f7f8fa;
}

.tree-item-active {
  color: #1677ff;
  background-color: #ecf5ff;
}

.service-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.service-title {
  font-size: 26rpx;
  color: #303133;
  font-weight: 600;
}

.service-count {
  font-size: 22rpx;
  color: #909399;
}

.service-row {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.service-row:last-child {
  border-bottom: none;
}

.service-check {
  margin-right: 12rpx;
  padding-top: 6rpx;
}

.service-main {
  flex: 1;
  min-width: 0;
}

.service-name {
  font-size: 26rpx;
  color: #303133;
  line-height: 1.4;
}

.service-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #909399;
  line-height: 1.5;
}

.icon-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-btn-small {
  width: 60rpx;
  height: 60rpx;
}

.icon-btn-primary {
  margin-left: 12rpx;
  background-color: #1677ff;
}

.icon-btn-danger {
  background-color: #fff4f4;
  border: 1rpx solid #ffdede;
}

.icon-btn-disabled {
  opacity: 0.45;
}

.fab-btn {
  position: fixed;
  right: 32rpx;
  bottom: calc(54rpx + env(safe-area-inset-bottom));
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff 0%, #4f9bff 100%);
  box-shadow: 0 18rpx 36rpx rgba(22, 119, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.popup-panel {
  display: flex;
  flex-direction: column;
  height: 82vh;
  max-height: 82vh;
  overflow: hidden;
  background-color: #ffffff;
}

.popup-header {
  padding: 28rpx 24rpx 16rpx;
}

.popup-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.popup-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #909399;
}

.popup-body {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: 0 24rpx 16rpx;
  box-sizing: border-box;
}

.popup-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 18rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eef2f6;
  background-color: #ffffff;
}

.selected-text {
  font-size: 24rpx;
  color: #667085;
}

.popup-submit {
  flex: 1;
}
</style>
