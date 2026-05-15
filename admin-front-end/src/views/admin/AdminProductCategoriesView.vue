<template>
  <div class="product-category-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-row">
        <div>
          <div class="hero-title">商品分类管理</div>
          <div class="hero-desc">维护电器商城与二手商品共用的商品分类，支持三级分类结构。</div>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openCreateDialog">新增分类</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="categoryTreeRows"
        row-key="id"
        border
        class="main-table"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="层级" width="90">
          <template #default="{ row }">
            第{{ row.level || 1 }}级
          </template>
        </el-table-column>
        <el-table-column prop="parentName" label="父级分类" min-width="160" show-overflow-tooltip />
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
        <el-table-column prop="description" label="分类描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedTime || row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-dropdown @command="command => handleCategoryAction(command, row)">
              <el-button size="small">操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.level < 3" command="createChild">新增子分类</el-dropdown-item>
                  <el-dropdown-item v-if="row.level === 3" command="uploadIcon">上传图标</el-dropdown-item>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增商品分类' : '编辑商品分类'"
      width="620px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-cascader
            v-model="form.parentId"
            :options="parentOptions"
            :props="parentCascaderProps"
            clearable
            filterable
            placeholder="顶级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <input
      ref="iconFileInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onIconFileChange"
    >
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';
import {
  createAdminProductCategory,
  deleteAdminProductCategory,
  fetchAdminProductCategories,
  uploadAdminProductCategoryIcon,
  updateAdminProductCategory
} from '../../api/adminProducts';

const route = useRoute();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const categoryTreeRows = ref([]);
const currentEditId = ref('');
const iconFileInputRef = ref(null);
const pendingUploadRow = ref(null);

const form = reactive({
  name: '',
  parentId: '',
  description: '',
  isActive: 1,
  sortOrder: 0
});

const parentCascaderProps = {
  value: 'id',
  label: 'name',
  emitPath: false,
  checkStrictly: true
};

const parentOptions = computed(() => {
  const disabledIds = currentEditId.value ? collectDescendantIds(categoryTreeRows.value, currentEditId.value) : new Set();
  if (currentEditId.value) {
    disabledIds.add(currentEditId.value);
  }
  return mapParentOptions(categoryTreeRows.value, disabledIds);
});

onMounted(() => {
  loadCategories();
  window.addEventListener('admin-page-refresh', handleExternalRefresh);
});

onBeforeUnmount(() => {
  window.removeEventListener('admin-page-refresh', handleExternalRefresh);
});

async function loadCategories() {
  loading.value = true;
  try {
    const res = await fetchAdminProductCategories();
    if (res.code !== 200) {
      ElMessage.error(res.message || '加载商品分类失败');
      return;
    }
    categoryTreeRows.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载商品分类失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog(parentRow) {
  if (parentRow && Number(parentRow.level) >= 3) {
    ElMessage.warning('第三级分类下不能继续新增子分类');
    return;
  }
  dialogMode.value = 'create';
  currentEditId.value = '';
  form.name = '';
  form.parentId = parentRow?.id || '';
  form.description = '';
  form.isActive = parentRow?.isActive === 0 ? 0 : 1;
  form.sortOrder = 0;
  dialogVisible.value = true;
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  currentEditId.value = row.id || '';
  form.name = row.name || '';
  form.parentId = row.parentId || '';
  form.description = row.description || '';
  form.isActive = row.isActive ?? 1;
  form.sortOrder = row.sortOrder ?? 0;
  dialogVisible.value = true;
}

function handleCategoryAction(command, row) {
  if (command === 'createChild') {
    openCreateDialog(row);
    return;
  }
  if (command === 'uploadIcon') {
    triggerIconUpload(row);
    return;
  }
  if (command === 'edit') {
    openEditDialog(row);
    return;
  }
  if (command === 'delete') {
    confirmDelete(row);
  }
}

async function submitForm() {
  const payload = {
    name: (form.name || '').trim(),
    parentId: form.parentId || null,
    description: (form.description || '').trim(),
    isActive: form.isActive,
    sortOrder: form.sortOrder ?? 0
  };

  if (!payload.name) {
    ElMessage.warning('请输入分类名称');
    return;
  }

  saving.value = true;
  try {
    const res = dialogMode.value === 'create'
      ? await createAdminProductCategory(payload)
      : await updateAdminProductCategory(currentEditId.value, payload);
    if (res.code !== 200) {
      ElMessage.error(res.message || '保存商品分类失败');
      return;
    }
    ElMessage.success(dialogMode.value === 'create' ? '商品分类新增成功' : '商品分类更新成功');
    dialogVisible.value = false;
    await loadCategories();
    if (dialogMode.value === 'create') {
      ElMessage.info('如需设置分类图标，请在第三级分类创建完成后点击“上传图标”');
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '保存商品分类失败');
  } finally {
    saving.value = false;
  }
}

function triggerIconUpload(row) {
  if (!row || Number(row.level) !== 3) {
    ElMessage.warning('只有第三级分类才能上传图标');
    return;
  }
  pendingUploadRow.value = row;
  if (!iconFileInputRef.value) {
    return;
  }
  iconFileInputRef.value.value = '';
  iconFileInputRef.value.click();
}

async function onIconFileChange(event) {
  const file = event?.target?.files?.[0];
  const row = pendingUploadRow.value;
  pendingUploadRow.value = null;
  if (!file || !row) {
    return;
  }
  if (!beforeIconUpload(file)) {
    return;
  }
  try {
    const res = await uploadAdminProductCategoryIcon(row.id, file);
    if (res.code !== 200) {
      showUploadErrorDialog(res.message || '上传商品分类图标失败', '上传商品分类图标失败', '图标上传失败');
      return;
    }
    ElMessage.success('商品分类图标上传成功');
    await loadCategories();
  } catch (error) {
    showUploadErrorDialog(error, '上传商品分类图标失败', '图标上传失败');
  }
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

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除商品分类“${row.name || row.id}”吗？`, '删除确认', { type: 'warning' });
  } catch (error) {
    return;
  }
  try {
    const res = await deleteAdminProductCategory(row.id);
    if (res.code !== 200) {
      ElMessage.error(res.message || '删除商品分类失败');
      return;
    }
    ElMessage.success('商品分类删除成功');
    await loadCategories();
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '删除商品分类失败');
  }
}

async function handleExternalRefresh(event) {
  if (event?.detail?.path !== route.path) {
    return;
  }
  event.detail.handled = true;
  await loadCategories();
  ElMessage.success('刷新成功');
}

function mapParentOptions(nodes, disabledIds) {
  return (nodes || []).map(item => ({
    id: item.id,
    name: item.name,
    disabled: disabledIds.has(item.id) || Number(item.level) >= 3,
    children: mapParentOptions(item.children || [], disabledIds)
  }));
}

function collectDescendantIds(nodes, targetId) {
  const result = new Set();
  walkDescendants(nodes, targetId, result);
  return result;
}

function walkDescendants(nodes, targetId, result) {
  for (const node of nodes || []) {
    if (node.id === targetId) {
      addChildren(node.children || [], result);
      return true;
    }
    if (walkDescendants(node.children || [], targetId, result)) {
      return true;
    }
  }
  return false;
}

function addChildren(children, result) {
  for (const child of children || []) {
    result.add(child.id);
    addChildren(child.children || [], result);
  }
}

function formatTime(timestamp) {
  if (!timestamp) {
    return '-';
  }
  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function getErrorMessage(error) {
  if (typeof error?.response?.data?.message === 'string' && error.response.data.message) {
    return error.response.data.message;
  }
  if (typeof error?.message === 'string' && error.message) {
    return error.message;
  }
  return '';
}
</script>

<style scoped>
.product-category-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-card,
.panel-card {
  border-radius: 14px;
}

.hero-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.hero-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.main-table {
  width: 100%;
}

.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

@media (max-width: 768px) {
  .hero-actions {
    width: 100%;
  }
}
</style>
