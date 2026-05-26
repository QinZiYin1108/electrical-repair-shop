<template>
  <div class="service-config-page">
    <el-card class="switch-card" shadow="never">
      <div class="switch-row">
        <div class="switch-title">服务项目配置</div>
        <el-radio-group v-model="activePanel" size="default">
          <el-radio-button label="categories">服务分类</el-radio-button>
          <el-radio-button label="types">服务类型</el-radio-button>
          <el-radio-button label="faults">故障现象</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card v-if="activePanel === 'categories'" class="panel-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="openCategoryCreate">新增分类</el-button>
        </div>
      </div>

      <el-table
        :data="categoryTreeRows"
        row-key="id"
        border
        class="main-table"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="240" show-overflow-tooltip />
        <el-table-column prop="level" label="层级" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图标" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.iconUrl"
              :src="row.iconUrl"
              fit="cover"
              :preview-src-list="[row.iconUrl]"
              preview-teleported
              class="category-icon"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-dropdown @command="command => handleCategoryAction(command, row)">
              <el-button size="small">操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="row.level === 3" command="uploadIcon">上传图标</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-else-if="activePanel === 'types'" class="panel-card" shadow="never">
      <div class="toolbar toolbar-between">
        <div class="toolbar-left">
          <el-button type="primary" @click="openTypeCreate">新增服务类型</el-button>
          <el-button :disabled="!selectedTypeRows.length" @click="openTypeCopy">批量复制</el-button>
        </div>
        <div class="toolbar-right filter-wrap">
          <el-input
            v-model="typeFilter.keyword"
            clearable
            placeholder="名称/描述/分类"
            style="width: 220px"
          />
          <el-select v-model="typeFilter.type" clearable placeholder="服务类型" style="width: 140px">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="线下维修" />
          </el-select>
          <el-select v-model="typeFilter.isActive" clearable placeholder="状态" style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
          <el-button @click="resetTypeFilters">重置筛选</el-button>
        </div>
      </div>

      <div class="panel-content">
        <aside class="category-tree-panel">
          <div class="category-tree-header">
            <span class="category-tree-title">服务分类</span>
            <el-button text @click="clearTypeCategoryFilter">全部分类</el-button>
          </div>
          <div
            class="category-tree-all"
            :class="{ 'is-active': !typeFilter.categoryId }"
            @click="clearTypeCategoryFilter"
          >
            全部分类
          </div>
          <el-tree
            ref="typeCategoryTreeRef"
            :data="categoryTreeRows"
            node-key="id"
            :props="categoryTreeProps"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            empty-text="暂无分类"
            class="category-tree"
            @node-click="handleTypeCategoryNodeClick"
          />
        </aside>

        <div class="table-content">
          <div class="filter-summary">当前分类：{{ typeSelectedCategoryLabel }}</div>
          <el-table
            ref="typeTableRef"
            :data="filteredTypeRows"
            row-key="id"
            border
            class="main-table"
            @selection-change="handleTypeSelectionChange"
          >
            <el-table-column type="selection" width="52" align="center" reserve-selection />
            <el-table-column prop="name" label="服务类型名称" min-width="170" show-overflow-tooltip />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                {{ serviceTypeLabel(row.type) }}
              </template>
            </el-table-column>
            <el-table-column label="所属分类" min-width="230" show-overflow-tooltip>
              <template #default="{ row }">
                {{ resolveTypeCategoryDisplay(row.categoryId) || row.categoryName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="basePrice" label="基础价格" width="120" />
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
                  {{ row.isActive === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-dropdown @command="command => handleTypeAction(command, row)">
                  <el-button size="small">操作</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="createFault">新增故障现象</el-dropdown-item>
                      <el-dropdown-item command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="delete">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <el-card v-else class="panel-card" shadow="never">
      <div class="toolbar toolbar-between">
        <div class="toolbar-left">
          <el-button :disabled="!selectedFaultRows.length" @click="openFaultCopy">批量复制</el-button>
        </div>
        <div class="toolbar-right filter-wrap">
          <el-input
            v-model="faultFilter.keyword"
            clearable
            placeholder="名称/描述关键词"
            style="width: 220px"
          />
          <el-select v-model="faultFilter.serviceTypeType" clearable placeholder="服务类型类别" style="width: 140px">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="线下维修" />
          </el-select>
          <el-select
            v-model="faultFilter.serviceTypeId"
            clearable
            filterable
            placeholder="按服务类型筛选"
            style="width: 260px"
          >
            <el-option
              v-for="item in filteredFaultTypeRows"
              :key="item.id"
              :label="formatServiceTypeOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="faultFilter.isActive" clearable placeholder="状态" style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
          <el-button @click="resetFaultFilters">重置筛选</el-button>
        </div>
      </div>

      <div class="panel-content">
        <aside class="category-tree-panel">
          <div class="category-tree-header">
            <span class="category-tree-title">服务分类</span>
            <el-button text @click="clearFaultCategoryFilter">全部分类</el-button>
          </div>
          <div
            class="category-tree-all"
            :class="{ 'is-active': !faultFilter.categoryId }"
            @click="clearFaultCategoryFilter"
          >
            全部分类
          </div>
          <el-tree
            ref="faultCategoryTreeRef"
            :data="categoryTreeRows"
            node-key="id"
            :props="categoryTreeProps"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            empty-text="暂无分类"
            class="category-tree"
            @node-click="handleFaultCategoryNodeClick"
          />
        </aside>

        <div class="table-content">
          <div class="filter-summary">当前分类：{{ faultSelectedCategoryLabel }}</div>
          <el-table
            ref="faultTableRef"
            :data="filteredFaultRows"
            border
            class="main-table"
            row-key="id"
            @selection-change="handleFaultSelectionChange"
          >
            <el-table-column type="selection" width="52" align="center" reserve-selection />
            <el-table-column prop="name" label="故障现象名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="serviceCategoryPath" label="所属分类" min-width="220" show-overflow-tooltip />
            <el-table-column label="服务类型类别" width="120">
              <template #default="{ row }">
                {{ serviceTypeLabel(row.serviceTypeType) }}
              </template>
            </el-table-column>
            <el-table-column prop="serviceTypeName" label="服务类型" min-width="160" show-overflow-tooltip />
            <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
            <el-table-column label="预估价格" width="160">
              <template #default="{ row }">
                {{ formatPriceRange(row.estimatedPriceMin, row.estimatedPriceMax) }}
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
                  {{ row.isActive === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-dropdown @command="command => handleFaultAction(command, row)">
                  <el-button size="small">操作</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="delete">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="categoryDialogVisible"
      :title="categoryDialogMode === 'create' ? '新增分类' : '编辑分类'"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="130px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-cascader
            v-model="categoryForm.parentId"
            :options="categoryParentOptions"
            :props="categoryCascaderProps"
            clearable
            placeholder="顶级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="categoryForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogMode === 'create' ? '新增服务类型' : '编辑服务类型'"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="130px">
        <el-form-item label="服务类型名称">
          <el-input v-model="typeForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="typeForm.type" style="width: 100%">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="线下维修" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分类">
          <el-cascader
            v-model="typeForm.categoryId"
            :options="typeCategoryOptions"
            :props="categoryCascaderProps"
            clearable
            placeholder="请选择二级或三级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="基础价格">
          <el-input-number v-model="typeForm.basePrice" :min="0" :precision="2" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="typeForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="typeForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSaving" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="copyDialogVisible"
      title="批量复制服务类型"
      width="620px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item label="复制为类型">
          <el-select v-model="copyForm.targetType" clearable placeholder="请选择复制后的类型" style="width: 100%">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="线下维修" />
          </el-select>
        </el-form-item>
        <el-form-item label="已选类型">
          <div class="copy-selected-list">
            <el-tag
              v-for="item in selectedTypeRows"
              :key="item.id"
              class="copy-selected-tag"
            >
              {{ formatServiceTypeOptionLabel(item) }}
            </el-tag>
            <span v-if="!selectedTypeRows.length" class="copy-empty-text">请先在列表中勾选服务类型</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="copySaving" @click="submitTypeCopy">确认复制</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="faultCopyDialogVisible"
      title="批量复制故障现象"
      width="620px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item label="服务类型类别">
          <el-select v-model="faultCopyForm.targetType" clearable placeholder="请选择服务类型类别" style="width: 100%">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="线下维修" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分类">
          <el-cascader
            v-model="faultCopyForm.categoryId"
            :options="typeCategoryOptions"
            :props="categoryCascaderProps"
            clearable
            placeholder="可按分类缩小范围"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目标服务类型">
          <el-select
            v-model="faultCopyForm.targetServiceTypeId"
            clearable
            filterable
            placeholder="请选择复制后的服务类型"
            style="width: 100%"
          >
            <el-option
              v-for="item in filteredFaultCopyTypeRows"
              :key="item.id"
              :label="formatServiceTypeOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="已选故障现象">
          <div class="copy-selected-list">
            <el-tag
              v-for="item in selectedFaultRows"
              :key="item.id"
              class="copy-selected-tag"
            >
              {{ formatFaultSelectionLabel(item) }}
            </el-tag>
            <span v-if="!selectedFaultRows.length" class="copy-empty-text">请先在列表中勾选故障现象</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultCopyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="faultCopySaving" @click="submitFaultCopy">确认复制</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="faultDialogVisible"
      :title="faultDialogMode === 'create' ? '新增故障现象' : '编辑故障现象'"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="130px">
        <el-form-item label="服务类型">
          <el-select v-model="faultForm.serviceTypeId" :disabled="faultDialogServiceTypeLocked" filterable style="width: 100%">
            <el-option
              v-for="item in typeRows"
              :key="item.id"
              :label="formatServiceTypeOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障现象名称">
          <el-input v-model="faultForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="faultForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="最低预估价">
          <el-input-number v-model="faultForm.estimatedPriceMin" :min="0" :precision="2" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="最高预估价">
          <el-input-number v-model="faultForm.estimatedPriceMax" :min="0" :precision="2" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="faultForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="faultForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="faultSaving" @click="saveFault">保存</el-button>
      </template>
    </el-dialog>

    <input
      ref="iconFileInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onCategoryIconFileChange"
    >
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';
import {
  copyFaultPhenomena,
  copyServiceTypes,
  createFaultPhenomenon,
  createServiceCategory,
  createServiceType,
  deleteFaultPhenomenon,
  deleteServiceCategory,
  deleteServiceType,
  fetchFaultPhenomena,
  fetchServiceCategories,
  fetchServiceTypes,
  updateFaultPhenomenon,
  updateServiceCategory,
  updateServiceType,
  uploadServiceCategoryIcon
} from '../../api/adminServiceConfig';

const activePanel = ref('categories');

const categoryRows = ref([]);
const typeRows = ref([]);
const faultRows = ref([]);
const typeTableRef = ref(null);
const faultTableRef = ref(null);
const typeCategoryTreeRef = ref(null);
const faultCategoryTreeRef = ref(null);
const selectedTypeRows = ref([]);
const selectedFaultRows = ref([]);
const iconFileInputRef = ref(null);
const pendingIconCategoryRow = ref(null);

const categoryDialogVisible = ref(false);
const categoryDialogMode = ref('create');
const categorySaving = ref(false);
const categoryForm = reactive({
  id: '',
  name: '',
  parentId: '',
  description: '',
  isActive: 1,
  sortOrder: 0
});

const typeDialogVisible = ref(false);
const typeDialogMode = ref('create');
const typeSaving = ref(false);
const copyDialogVisible = ref(false);
const copySaving = ref(false);
const typeForm = reactive({
  id: '',
  name: '',
  type: 1,
  categoryId: '',
  description: '',
  basePrice: null,
  isActive: 1,
  sortOrder: 0
});
const copyForm = reactive({
  targetType: undefined
});
const typeFilter = reactive({
  keyword: '',
  type: undefined,
  categoryId: '',
  isActive: undefined
});

const faultDialogVisible = ref(false);
const faultDialogMode = ref('create');
const faultSaving = ref(false);
const faultDialogServiceTypeLocked = ref(false);
const faultCopyDialogVisible = ref(false);
const faultCopySaving = ref(false);
const faultForm = reactive({
  id: '',
  serviceTypeId: '',
  name: '',
  description: '',
  estimatedPriceMin: null,
  estimatedPriceMax: null,
  isActive: 1,
  sortOrder: 0
});
const faultFilter = reactive({
  keyword: '',
  categoryId: '',
  serviceTypeType: undefined,
  serviceTypeId: '',
  isActive: undefined
});
const faultCopyForm = reactive({
  targetType: undefined,
  categoryId: '',
  targetServiceTypeId: ''
});

const categoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
};
const categoryTreeProps = {
  label: 'name',
  children: 'children'
};

const categoryTreeRows = computed(() => buildCategoryTree(categoryRows.value || []));

const categoryParentTreeRows = computed(() => {
  const treeRows = categoryTreeRows.value || [];
  const excludedIdSet = new Set();
  const currentId = categoryForm.id;

  if (categoryDialogMode.value === 'edit' && currentId) {
    excludedIdSet.add(currentId);
    collectDescendantIds(treeRows, currentId, excludedIdSet);
  }

  return markDisabledTreeNodes(treeRows, excludedIdSet);
});

const categoryParentOptions = computed(() => toParentCascaderOptions(categoryParentTreeRows.value || []));
const typeCategoryOptions = computed(() => toTypeCategoryOptions(categoryTreeRows.value || []));

const categoryNodeMap = computed(() => {
  const map = {};
  buildCategoryNodeMap(categoryTreeRows.value || [], map);
  return map;
});

const categoryPathMap = computed(() => {
  const map = {};
  buildCategoryPathMap(categoryTreeRows.value || [], [], map);
  return map;
});

const typeSelectedCategoryLabel = computed(() => resolveCategoryPath(typeFilter.categoryId) || '全部分类');
const faultSelectedCategoryLabel = computed(() => resolveCategoryPath(faultFilter.categoryId) || '全部分类');

const filteredTypeRows = computed(() => {
  const keyword = String(typeFilter.keyword || '').trim().toLowerCase();
  return (typeRows.value || []).filter(item => {
    if (typeFilter.type != null && typeFilter.type !== '' && Number(item.type) !== Number(typeFilter.type)) {
      return false;
    }
    if (typeFilter.isActive != null && typeFilter.isActive !== '' && Number(item.isActive) !== Number(typeFilter.isActive)) {
      return false;
    }
    if (typeFilter.categoryId && !matchesTypeCategoryFilter(item.categoryId, typeFilter.categoryId)) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    const searchText = [
      item.name,
      item.description,
      resolveCategoryPath(item.categoryId),
      serviceTypeLabel(item.type)
    ].filter(Boolean).join(' ').toLowerCase();
    return searchText.includes(keyword);
  });
});

const filteredFaultRows = computed(() => {
  const keyword = String(faultFilter.keyword || '').trim().toLowerCase();
  return (faultRows.value || []).filter(item => {
    if (faultFilter.serviceTypeType != null && faultFilter.serviceTypeType !== ''
      && Number(item.serviceTypeType) !== Number(faultFilter.serviceTypeType)) {
      return false;
    }
    if (faultFilter.serviceTypeId && item.serviceTypeId !== faultFilter.serviceTypeId) {
      return false;
    }
    if (faultFilter.isActive != null && faultFilter.isActive !== '' && Number(item.isActive) !== Number(faultFilter.isActive)) {
      return false;
    }
    if (faultFilter.categoryId && !matchesTypeCategoryFilter(item.serviceCategoryId, faultFilter.categoryId)) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    const searchText = [
      item.name,
      item.description,
      item.serviceTypeName,
      serviceTypeLabel(item.serviceTypeType),
      item.serviceCategoryPath
    ].filter(Boolean).join(' ').toLowerCase();
    return searchText.includes(keyword);
  });
});

const filteredFaultTypeRows = computed(() => {
  return (typeRows.value || []).filter(item => {
    if (faultFilter.serviceTypeType != null && faultFilter.serviceTypeType !== ''
      && Number(item.type) !== Number(faultFilter.serviceTypeType)) {
      return false;
    }
    if (faultFilter.categoryId && !matchesTypeCategoryFilter(item.categoryId, faultFilter.categoryId)) {
      return false;
    }
    return true;
  });
});

const filteredFaultCopyTypeRows = computed(() => {
  return (typeRows.value || []).filter(item => {
    if (faultCopyForm.targetType != null && faultCopyForm.targetType !== ''
      && Number(item.type) !== Number(faultCopyForm.targetType)) {
      return false;
    }
    if (faultCopyForm.categoryId && !matchesTypeCategoryFilter(item.categoryId, faultCopyForm.categoryId)) {
      return false;
    }
    return true;
  });
});

onMounted(async () => {
  await reloadAll();
  window.addEventListener('admin-page-refresh', handleExternalRefresh);
});

onBeforeUnmount(() => {
  window.removeEventListener('admin-page-refresh', handleExternalRefresh);
});

watch(
  () => [faultFilter.categoryId, faultFilter.serviceTypeType],
  () => {
    if (!faultFilter.serviceTypeId) {
      return;
    }
    const matched = filteredFaultTypeRows.value.some(item => item.id === faultFilter.serviceTypeId);
    if (!matched) {
      faultFilter.serviceTypeId = '';
    }
  }
);

watch(
  () => typeFilter.categoryId,
  value => {
    nextTick(() => {
      typeCategoryTreeRef.value?.setCurrentKey(value || null);
    });
  },
  { immediate: true }
);

watch(
  () => faultFilter.categoryId,
  value => {
    nextTick(() => {
      faultCategoryTreeRef.value?.setCurrentKey(value || null);
    });
  },
  { immediate: true }
);

watch(
  () => [faultCopyForm.categoryId, faultCopyForm.targetType],
  () => {
    if (!faultCopyForm.targetServiceTypeId) {
      return;
    }
    const matched = filteredFaultCopyTypeRows.value.some(item => item.id === faultCopyForm.targetServiceTypeId);
    if (!matched) {
      faultCopyForm.targetServiceTypeId = '';
    }
  }
);

async function reloadAll() {
  await Promise.all([reloadCategories(), reloadTypes()]);
  await reloadFaults();
}

async function handleExternalRefresh(event) {
  if (!event || !event.detail || !String(event.detail.path || '').startsWith('/admin/config/services')) {
    return;
  }
  event.detail.handled = true;
  await reloadAll();
  ElMessage.success('刷新成功');
}

async function reloadCategories() {
  try {
    const res = await fetchServiceCategories();
    if (res.code === 200) {
      categoryRows.value = res.data || [];
      if (typeFilter.categoryId && !categoryNodeMap.value[typeFilter.categoryId]) {
        typeFilter.categoryId = '';
      }
      if (faultFilter.categoryId && !categoryNodeMap.value[faultFilter.categoryId]) {
        faultFilter.categoryId = '';
      }
    } else {
      ElMessage.error(res.message || '获取服务分类失败');
    }
  } catch (e) {
    ElMessage.error('获取服务分类失败');
  }
}

async function reloadTypes() {
  try {
    const res = await fetchServiceTypes();
    if (res.code === 200) {
      typeRows.value = res.data || [];
      selectedTypeRows.value = [];
      await nextTick();
      typeTableRef.value?.clearSelection();
    } else {
      ElMessage.error(res.message || '获取服务类型失败');
    }
  } catch (e) {
    ElMessage.error('获取服务类型失败');
  }
}

async function reloadFaults() {
  try {
    const res = await fetchFaultPhenomena();
    if (res.code === 200) {
      faultRows.value = res.data || [];
      selectedFaultRows.value = [];
      await nextTick();
      faultTableRef.value?.clearSelection();
    } else {
      ElMessage.error(res.message || '获取故障现象失败');
    }
  } catch (e) {
    ElMessage.error('获取故障现象失败');
  }
}

function openCategoryCreate() {
  categoryDialogMode.value = 'create';
  categoryForm.id = '';
  categoryForm.name = '';
  categoryForm.parentId = '';
  categoryForm.description = '';
  categoryForm.isActive = 1;
  categoryForm.sortOrder = 0;
  categoryDialogVisible.value = true;
}

function openCategoryEdit(row) {
  categoryDialogMode.value = 'edit';
  categoryForm.id = row.id;
  categoryForm.name = row.name;
  categoryForm.parentId = row.parentId || '';
  categoryForm.description = row.description || '';
  categoryForm.isActive = row.isActive ?? 1;
  categoryForm.sortOrder = row.sortOrder ?? 0;
  categoryDialogVisible.value = true;
}

async function saveCategory() {
  if (!categoryForm.name) {
    ElMessage.warning('请输入分类名称');
    return;
  }
  categorySaving.value = true;
  try {
    const payload = {
      name: categoryForm.name,
      parentId: categoryForm.parentId || null,
      description: categoryForm.description || null,
      isActive: categoryForm.isActive,
      sortOrder: categoryForm.sortOrder
    };
    const res = categoryDialogMode.value === 'create'
      ? await createServiceCategory(payload)
      : await updateServiceCategory(categoryForm.id, payload);
    if (res.code === 200) {
      ElMessage.success('分类保存成功');
      categoryDialogVisible.value = false;
      await reloadAll();
    } else {
      ElMessage.error(res.message || '分类保存失败');
    }
  } catch (e) {
    ElMessage.error('分类保存失败');
  } finally {
    categorySaving.value = false;
  }
}

async function confirmDeleteCategory(row) {
  try {
    await ElMessageBox.confirm(`确认删除分类“${row.name}”吗？`, '删除确认', { type: 'warning' });
  } catch (e) {
    return;
  }
  try {
    const res = await deleteServiceCategory(row.id);
    if (res.code === 200) {
      ElMessage.success('分类删除成功');
      await reloadAll();
    } else {
      ElMessage.error(getDeleteErrorMessage('category', res?.message, res?.code));
    }
  } catch (e) {
    ElMessage.error(getDeleteErrorMessage('category', getErrorMessageFromException(e), e?.response?.data?.code));
  }
}

function handleCategoryAction(action, row) {
  if (action === 'uploadIcon') {
    triggerCategoryIconUpload(row);
    return;
  }
  if (action === 'edit') {
    openCategoryEdit(row);
    return;
  }
  if (action === 'delete') {
    confirmDeleteCategory(row);
  }
}

function openTypeCreate() {
  if (!categoryRows.value.length) {
    ElMessage.warning('请先创建服务分类');
    return;
  }
  typeDialogMode.value = 'create';
  typeForm.id = '';
  typeForm.name = '';
  typeForm.type = 1;
  typeForm.categoryId = findFirstCategoryId(categoryTreeRows.value) || '';
  typeForm.description = '';
  typeForm.basePrice = null;
  typeForm.isActive = 1;
  typeForm.sortOrder = 0;
  typeDialogVisible.value = true;
}

function openTypeEdit(row) {
  typeDialogMode.value = 'edit';
  typeForm.id = row.id;
  typeForm.name = row.name;
  typeForm.type = row.type ?? 1;
  typeForm.categoryId = row.categoryId || '';
  typeForm.description = row.description || '';
  typeForm.basePrice = row.basePrice ?? null;
  typeForm.isActive = row.isActive ?? 1;
  typeForm.sortOrder = row.sortOrder ?? 0;
  typeDialogVisible.value = true;
}

function openTypeCopy() {
  if (!selectedTypeRows.value.length) {
    ElMessage.warning('请先勾选要复制的服务类型');
    return;
  }
  copyForm.targetType = undefined;
  copyDialogVisible.value = true;
}

async function saveType() {
  if (!typeForm.name) {
    ElMessage.warning('请输入服务类型名称');
    return;
  }
  if (!typeForm.categoryId) {
    ElMessage.warning('请选择所属分类');
    return;
  }
  if (!isTypeSelectableCategoryId(typeForm.categoryId)) {
    ElMessage.warning('服务类型只能选择二级或三级分类');
    return;
  }
  typeSaving.value = true;
  try {
    const payload = {
      name: typeForm.name,
      type: typeForm.type,
      categoryId: typeForm.categoryId,
      description: typeForm.description || null,
      basePrice: typeForm.basePrice,
      isActive: typeForm.isActive,
      sortOrder: typeForm.sortOrder
    };
    const res = typeDialogMode.value === 'create'
      ? await createServiceType(payload)
      : await updateServiceType(typeForm.id, payload);
    if (res.code === 200) {
      ElMessage.success('服务类型保存成功');
      typeDialogVisible.value = false;
      await reloadAll();
    } else {
      ElMessage.error(res.message || '服务类型保存失败');
    }
  } catch (e) {
    ElMessage.error('服务类型保存失败');
  } finally {
    typeSaving.value = false;
  }
}

async function submitTypeCopy() {
  if (!selectedTypeRows.value.length) {
    ElMessage.warning('请先勾选要复制的服务类型');
    return;
  }
  if (copyForm.targetType == null || copyForm.targetType === '') {
    ElMessage.warning('请选择复制后的类型');
    return;
  }

  copySaving.value = true;
  try {
    const res = await copyServiceTypes({
      sourceIds: selectedTypeRows.value.map(item => item.id),
      targetType: copyForm.targetType
    });
    if (res.code === 200) {
      ElMessage.success(`已复制 ${Array.isArray(res.data) ? res.data.length : selectedTypeRows.value.length} 条服务类型`);
      copyDialogVisible.value = false;
      await reloadTypes();
      await reloadFaults();
    } else {
      ElMessage.error(res.message || '批量复制服务类型失败');
    }
  } catch (e) {
    ElMessage.error(getErrorMessageFromException(e) || '批量复制服务类型失败');
  } finally {
    copySaving.value = false;
  }
}

async function confirmDeleteType(row) {
  try {
    await ElMessageBox.confirm(`确认删除服务类型“${row.name}”吗？`, '删除确认', { type: 'warning' });
  } catch (e) {
    return;
  }
  try {
    const res = await deleteServiceType(row.id);
    if (res.code === 200) {
      ElMessage.success('服务类型删除成功');
      await reloadAll();
    } else {
      ElMessage.error(getDeleteErrorMessage('type', res?.message, res?.code));
    }
  } catch (e) {
    ElMessage.error(getDeleteErrorMessage('type', getErrorMessageFromException(e), e?.response?.data?.code));
  }
}

function handleTypeAction(action, row) {
  if (action === 'createFault') {
    openFaultCreateByType(row);
    return;
  }
  if (action === 'edit') {
    openTypeEdit(row);
    return;
  }
  if (action === 'delete') {
    confirmDeleteType(row);
  }
}

function handleTypeSelectionChange(rows) {
  selectedTypeRows.value = rows || [];
}

function resetTypeFilters() {
  typeFilter.keyword = '';
  typeFilter.type = undefined;
  typeFilter.categoryId = '';
  typeFilter.isActive = undefined;
}

function handleTypeCategoryNodeClick(node) {
  typeFilter.categoryId = node?.id || '';
}

function clearTypeCategoryFilter() {
  typeFilter.categoryId = '';
}

function resetFaultFilters() {
  faultFilter.keyword = '';
  faultFilter.categoryId = '';
  faultFilter.serviceTypeType = undefined;
  faultFilter.serviceTypeId = '';
  faultFilter.isActive = undefined;
}

function handleFaultCategoryNodeClick(node) {
  faultFilter.categoryId = node?.id || '';
}

function clearFaultCategoryFilter() {
  faultFilter.categoryId = '';
}

function triggerCategoryIconUpload(row) {
  if (row.level !== 3) {
    ElMessage.warning('仅第三级分类支持上传图标');
    return;
  }
  pendingIconCategoryRow.value = row;
  if (!iconFileInputRef.value) {
    return;
  }
  iconFileInputRef.value.value = '';
  iconFileInputRef.value.click();
}

async function onCategoryIconFileChange(event) {
  const file = event?.target?.files?.[0];
  const row = pendingIconCategoryRow.value;
  pendingIconCategoryRow.value = null;
  if (!file || !row) {
    return;
  }
  if (!beforeIconUpload(file)) {
    return;
  }
  await doUploadCategoryIcon(row, { file });
}

function beforeIconUpload(file) {
  const isImage = file.type && file.type.startsWith('image/');
  if (!isImage) {
    showUploadLimitDialog('请上传图片文件');
    return false;
  }
  const maxSizeMb = 5;
  if (file.size > maxSizeMb * 1024 * 1024) {
    showUploadLimitDialog(`图片大小不能超过 ${maxSizeMb}MB`);
    return false;
  }
  return true;
}

async function doUploadCategoryIcon(row, opts) {
  try {
    const res = await uploadServiceCategoryIcon(row.id, opts.file);
    if (res.code === 200) {
      if (opts.onSuccess) {
        opts.onSuccess(res);
      }
      ElMessage.success('图标上传成功');
      await reloadCategories();
    } else {
      if (opts.onError) {
        opts.onError(new Error(res.message || '图标上传失败'));
      }
      showUploadErrorDialog(res.message || '图标上传失败', '图标上传失败', '图标上传失败');
    }
  } catch (e) {
    if (opts.onError) {
      opts.onError(e);
    }
    showUploadErrorDialog(e, '图标上传失败', '图标上传失败');
  }
}

function openFaultCreateByType(row) {
  if (!row?.id) {
    ElMessage.warning('请选择有效的服务类型');
    return;
  }
  faultDialogMode.value = 'create';
  faultDialogServiceTypeLocked.value = true;
  faultForm.id = '';
  faultForm.serviceTypeId = row.id;
  faultForm.name = '';
  faultForm.description = '';
  faultForm.estimatedPriceMin = null;
  faultForm.estimatedPriceMax = null;
  faultForm.isActive = 1;
  faultForm.sortOrder = 0;
  faultDialogVisible.value = true;
}

function openFaultEdit(row) {
  faultDialogMode.value = 'edit';
  faultDialogServiceTypeLocked.value = false;
  faultForm.id = row.id;
  faultForm.serviceTypeId = row.serviceTypeId || '';
  faultForm.name = row.name;
  faultForm.description = row.description || '';
  faultForm.estimatedPriceMin = row.estimatedPriceMin ?? null;
  faultForm.estimatedPriceMax = row.estimatedPriceMax ?? null;
  faultForm.isActive = row.isActive ?? 1;
  faultForm.sortOrder = row.sortOrder ?? 0;
  faultDialogVisible.value = true;
}

function openFaultCopy() {
  if (!selectedFaultRows.value.length) {
    ElMessage.warning('请先勾选要复制的故障现象');
    return;
  }
  faultCopyForm.targetType = undefined;
  faultCopyForm.categoryId = '';
  faultCopyForm.targetServiceTypeId = '';
  faultCopyDialogVisible.value = true;
}

async function saveFault() {
  if (!faultForm.serviceTypeId) {
    ElMessage.warning('请选择服务类型');
    return;
  }
  if (!faultForm.name) {
    ElMessage.warning('请输入故障现象名称');
    return;
  }
  if (faultForm.estimatedPriceMin != null
    && faultForm.estimatedPriceMax != null
    && faultForm.estimatedPriceMin > faultForm.estimatedPriceMax) {
    ElMessage.warning('最低预估价不能大于最高预估价');
    return;
  }

  faultSaving.value = true;
  try {
    const payload = {
      serviceTypeId: faultForm.serviceTypeId,
      name: faultForm.name,
      description: faultForm.description || null,
      estimatedPriceMin: faultForm.estimatedPriceMin,
      estimatedPriceMax: faultForm.estimatedPriceMax,
      isActive: faultForm.isActive,
      sortOrder: faultForm.sortOrder
    };
    const res = faultDialogMode.value === 'create'
      ? await createFaultPhenomenon(payload)
      : await updateFaultPhenomenon(faultForm.id, payload);
    if (res.code === 200) {
      ElMessage.success('故障现象保存成功');
      faultDialogVisible.value = false;
      await reloadFaults();
    } else {
      ElMessage.error(res.message || '故障现象保存失败');
    }
  } catch (e) {
    ElMessage.error('故障现象保存失败');
  } finally {
    faultSaving.value = false;
  }
}

async function submitFaultCopy() {
  if (!selectedFaultRows.value.length) {
    ElMessage.warning('请先勾选要复制的故障现象');
    return;
  }
  if (!faultCopyForm.targetServiceTypeId) {
    ElMessage.warning('请选择目标服务类型');
    return;
  }

  faultCopySaving.value = true;
  try {
    const res = await copyFaultPhenomena({
      sourceIds: selectedFaultRows.value.map(item => item.id),
      targetServiceTypeId: faultCopyForm.targetServiceTypeId
    });
    if (res.code === 200) {
      ElMessage.success(`已复制 ${Array.isArray(res.data) ? res.data.length : selectedFaultRows.value.length} 条故障现象`);
      faultCopyDialogVisible.value = false;
      await reloadFaults();
    } else {
      ElMessage.error(res.message || '批量复制故障现象失败');
    }
  } catch (e) {
    ElMessage.error(getErrorMessageFromException(e) || '批量复制故障现象失败');
  } finally {
    faultCopySaving.value = false;
  }
}

async function confirmDeleteFault(row) {
  try {
    await ElMessageBox.confirm(`确认删除故障现象“${row.name}”吗？`, '删除确认', { type: 'warning' });
  } catch (e) {
    return;
  }
  try {
    const res = await deleteFaultPhenomenon(row.id);
    if (res.code === 200) {
      ElMessage.success('故障现象删除成功');
      await reloadFaults();
    } else {
      ElMessage.error(getDeleteErrorMessage('fault', res?.message, res?.code));
    }
  } catch (e) {
    ElMessage.error(getDeleteErrorMessage('fault', getErrorMessageFromException(e), e?.response?.data?.code));
  }
}

function handleFaultAction(action, row) {
  if (action === 'edit') {
    openFaultEdit(row);
    return;
  }
  if (action === 'delete') {
    confirmDeleteFault(row);
  }
}

function handleFaultSelectionChange(rows) {
  selectedFaultRows.value = rows || [];
}

function resolveCategoryPath(categoryId) {
  if (!categoryId) {
    return '';
  }
  return categoryPathMap.value[categoryId] || '';
}

function resolveTypeCategoryDisplay(categoryId) {
  if (!categoryId) {
    return '';
  }
  const path = resolveCategoryPath(categoryId);
  const category = categoryNodeMap.value[categoryId];
  if (!category || Number(category.level) !== 2) {
    return path;
  }
  const childNames = collectLeafCategoryNames(category.children || []);
  if (!childNames.length) {
    return path;
  }
  return `${path}（包含：${childNames.join('、')}）`;
}

function formatServiceTypeOptionLabel(item) {
  if (!item) {
    return '';
  }
  const typeText = serviceTypeLabel(item.type);
  const categoryText = resolveCategoryPath(item.categoryId);
  if (typeText && categoryText) {
    return `${item.name}（${typeText} / ${categoryText}）`;
  }
  if (typeText) {
    return `${item.name}（${typeText}）`;
  }
  return categoryText ? `${item.name}（${categoryText}）` : item.name;
}

function formatFaultSelectionLabel(item) {
  if (!item) {
    return '';
  }
  const typeText = serviceTypeLabel(item.serviceTypeType);
  const serviceTypeText = item.serviceTypeName || '';
  const suffix = [typeText, serviceTypeText].filter(Boolean).join(' / ');
  return suffix ? `${item.name}（${suffix}）` : item.name;
}

function matchesTypeCategoryFilter(typeCategoryId, filterCategoryId) {
  const currentCategory = categoryNodeMap.value[typeCategoryId];
  const filterCategory = categoryNodeMap.value[filterCategoryId];
  if (!currentCategory || !filterCategory) {
    return false;
  }

  if (currentCategory.id === filterCategory.id) {
    return true;
  }

  let parentId = currentCategory.parentId;
  while (parentId) {
    if (parentId === filterCategory.id) {
      return true;
    }
    parentId = categoryNodeMap.value[parentId]?.parentId;
  }

  return false;
}

function getErrorMessageFromException(error) {
  return error?.response?.data?.message || error?.message || '';
}

function getDeleteErrorMessage(entity, rawMessage, code) {
  const message = String(rawMessage || '');
  const lowerMessage = message.toLowerCase();
  const isFkError = code === 422
    || lowerMessage.includes('foreign key')
    || lowerMessage.includes('constraint')
    || lowerMessage.includes('integrity');

  if (entity === 'category') {
    if (lowerMessage.includes('child categories')) {
      return '该分类下存在子分类，无法删除';
    }
    if (lowerMessage.includes('service types')) {
      return '该分类下存在服务类型，无法删除';
    }
    if (isFkError) {
      return '该分类存在关联数据，无法删除';
    }
    return message || '删除分类失败';
  }

  if (entity === 'type') {
    if (lowerMessage.includes('fault phenomena')) {
      return '该服务类型下存在故障现象，无法删除';
    }
    if (isFkError) {
      return '该服务类型存在关联数据，无法删除';
    }
    return message || '删除服务类型失败';
  }

  if (entity === 'fault') {
    if (lowerMessage.includes('order')) {
      return '排序值冲突，请调整后重试';
    }
    if (isFkError) {
      return '该故障现象存在关联数据，无法删除';
    }
    return message || '删除故障现象失败';
  }

  return message || '删除失败';
}

function serviceTypeLabel(type) {
  const normalizedType = Number(type);
  if (normalizedType === 1) return '上门维修';
  if (normalizedType === 2) return '上门安装';
  if (normalizedType === 3) return '线下维修';
  return String(type ?? '-');
}

function formatPriceRange(min, max) {
  if (min == null && max == null) return '-';
  if (min != null && max == null) return `${min}+`;
  if (min == null && max != null) return `<= ${max}`;
  return `${min} - ${max}`;
}

function buildCategoryTree(list) {
  if (!Array.isArray(list) || !list.length) {
    return [];
  }

  const nodeMap = {};
  const roots = [];

  list.forEach(item => {
    nodeMap[item.id] = {
      ...item,
      children: []
    };
  });

  Object.values(nodeMap).forEach(node => {
    if (node.parentId && nodeMap[node.parentId]) {
      nodeMap[node.parentId].children.push(node);
    } else {
      roots.push(node);
    }
  });

  const sortNodes = nodes => {
    nodes.sort((a, b) => {
      const sortA = a.sortOrder ?? 0;
      const sortB = b.sortOrder ?? 0;
      if (sortA !== sortB) {
        return sortA - sortB;
      }
      const timeA = a.createdTime ?? 0;
      const timeB = b.createdTime ?? 0;
      return timeB - timeA;
    });
    nodes.forEach(node => {
      if (node.children && node.children.length) {
        sortNodes(node.children);
      } else {
        delete node.children;
      }
    });
  };

  sortNodes(roots);
  return roots;
}

function collectDescendantIds(nodes, targetId, resultSet) {
  for (const node of nodes || []) {
    if (node.id === targetId) {
      addChildrenIds(node.children || [], resultSet);
      return true;
    }
    if (node.children && node.children.length) {
      const found = collectDescendantIds(node.children, targetId, resultSet);
      if (found) {
        return true;
      }
    }
  }
  return false;
}

function addChildrenIds(children, resultSet) {
  for (const child of children || []) {
    resultSet.add(child.id);
    if (child.children && child.children.length) {
      addChildrenIds(child.children, resultSet);
    }
  }
}

function markDisabledTreeNodes(nodes, disabledIdSet) {
  return (nodes || []).map(node => {
    const children = node.children && node.children.length
      ? markDisabledTreeNodes(node.children, disabledIdSet)
      : undefined;
    return {
      ...node,
      disabled: disabledIdSet.has(node.id),
      children
    };
  });
}

function toParentCascaderOptions(nodes) {
  return (nodes || []).map(node => ({
    id: node.id,
    name: node.name,
    disabled: !!node.disabled || Number(node.level) >= 3,
    children: node.children && node.children.length ? toParentCascaderOptions(node.children) : undefined
  }));
}

function toTypeCategoryOptions(nodes) {
  return (nodes || []).map(node => ({
    id: node.id,
    name: node.name,
    disabled: !!node.disabled || ![2, 3].includes(Number(node.level)),
    children: node.children && node.children.length ? toTypeCategoryOptions(node.children) : undefined
  }));
}

function findFirstCategoryId(nodes) {
  for (const node of nodes || []) {
    if (node.id && [2, 3].includes(Number(node.level))) {
      return node.id;
    }
    const childId = findFirstCategoryId(node.children || []);
    if (childId) {
      return childId;
    }
  }
  return '';
}

function buildCategoryPathMap(nodes, path, map) {
  for (const node of nodes || []) {
    const currentPath = [...path, node.name || ''].filter(Boolean);
    map[node.id] = currentPath.join(' / ');
    if (node.children && node.children.length) {
      buildCategoryPathMap(node.children, currentPath, map);
    }
  }
}

function buildCategoryNodeMap(nodes, map) {
  for (const node of nodes || []) {
    map[node.id] = node;
    if (node.children && node.children.length) {
      buildCategoryNodeMap(node.children, map);
    }
  }
}

function collectLeafCategoryNames(nodes) {
  const names = [];
  for (const node of nodes || []) {
    if (Number(node.level) === 3 && node.name) {
      names.push(node.name);
      continue;
    }
    if (node.children && node.children.length) {
      names.push(...collectLeafCategoryNames(node.children));
    }
  }
  return names;
}

function isTypeSelectableCategoryId(categoryId) {
  const category = categoryNodeMap.value[categoryId];
  if (!category) {
    return false;
  }
  return [2, 3].includes(Number(category.level));
}
</script>

<style scoped>
.service-config-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.switch-card,
.panel-card {
  border-radius: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.switch-title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-between {
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-wrap {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.copy-selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  min-height: 32px;
}

.copy-selected-tag {
  max-width: 100%;
}

.copy-empty-text {
  color: #909399;
  font-size: 13px;
}

.main-table {
  width: 100%;
}

.panel-content {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.category-tree-panel {
  width: 260px;
  flex-shrink: 0;
  padding: 14px 12px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafbfc;
}

.category-tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.category-tree-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.category-tree-all {
  display: flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tree-all:hover,
.category-tree-all.is-active {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.category-tree {
  background: transparent;
}

.category-tree :deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 8px;
}

.table-content {
  flex: 1;
  min-width: 0;
}

.filter-summary {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

@media (max-width: 768px) {
  .toolbar,
  .toolbar-between {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-right {
    width: 100%;
  }

  .panel-content {
    flex-direction: column;
  }

  .category-tree-panel {
    width: 100%;
  }

  .toolbar-right .el-select {
    width: 100% !important;
  }

  .toolbar-right :deep(.el-cascader),
  .toolbar-right :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
