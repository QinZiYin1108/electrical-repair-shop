<template>
  <div class="coupon-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">优惠券管理</div>
          <div class="page-subtitle">支持创建、编辑、上下线和定向发放商城优惠券。</div>
        </div>
        <el-button type="primary" @click="openCreateDialog">新建优惠券</el-button>
      </div>

      <div class="filter-panel">
        <el-input
          v-model="keyword"
          clearable
          class="filter-item keyword-input"
          placeholder="搜索优惠券名称"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="status" clearable class="filter-item" placeholder="状态">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="2" />
        </el-select>
        <el-select v-model="applicableType" clearable class="filter-item" placeholder="适用范围">
          <el-option label="全部商品" :value="1" />
          <el-option label="指定分类" :value="2" />
          <el-option label="指定商品" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border class="coupon-table" header-cell-class-name="coupon-table-header">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="优惠券信息" min-width="220">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.name || '-' }}</div>
            <div class="stack-text muted">{{ row.typeText || '-' }} / {{ row.discountTypeText || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="180">
          <template #default="{ row }">
            <div class="stack-text strong">{{ buildDiscountText(row) }}</div>
            <div class="stack-text muted">满 {{ formatMoney(row.minAmount) }} 可用</div>
            <div v-if="row.maxDiscount" class="stack-text muted">最高减 {{ formatMoney(row.maxDiscount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" min-width="160">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.applicableTypeText || '-' }}</div>
            <div class="stack-text muted">{{ buildApplicableDisplay(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发放情况" min-width="150">
          <template #default="{ row }">
            <div class="stack-text strong">已领 {{ row.receiveCount || 0 }} / {{ row.totalCount || 0 }}</div>
            <div class="stack-text muted">已用 {{ row.usedCount || 0 }}，剩余 {{ row.remainingCount || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="180">
          <template #default="{ row }">
            <div class="stack-text strong">{{ formatTime(row.startTime) }}</div>
            <div class="stack-text muted">至 {{ formatTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="Number(row.status) === 1 ? 'success' : 'info'">{{ row.statusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="command => handleRowCommand(command, row)">
              <el-button size="small" class="action-trigger">
                操作
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="grant">发放</el-dropdown-item>
                  <el-dropdown-item :command="row.status === 1 ? 'disable' : 'enable'">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="pageNum"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建优惠券' : '编辑优惠券'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <div class="form-grid">
          <el-form-item label="优惠券名称" required>
            <el-input v-model="form.name" maxlength="60" placeholder="请输入优惠券名称" />
          </el-form-item>
          <el-form-item label="优惠类型" required>
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="满减券" :value="1" />
              <el-option label="折扣券" :value="2" />
              <el-option label="运费券" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="折扣方式" required>
            <el-select v-model="form.discountType" style="width: 100%">
              <el-option label="固定减免" :value="1" />
              <el-option label="折扣" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item :label="form.discountType === 2 ? '折扣值' : '优惠金额'" required>
            <el-input-number v-model="form.discountValue" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="使用门槛" required>
            <el-input-number v-model="form.minAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高减免">
            <el-input-number v-model="form.maxDiscount" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="发放总量" required>
            <el-input-number v-model="form.totalCount" :min="1" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" required>
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="适用范围" required>
            <el-select v-model="form.applicableType" style="width: 100%">
              <el-option label="全部商品" :value="1" />
              <el-option label="指定分类" :value="2" />
              <el-option label="指定商品" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="有效时间" required>
            <el-date-picker
              v-model="form.timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="x"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <el-form-item v-if="form.applicableType === 2" label="指定分类">
          <div v-loading="categoryOptionsLoading" class="category-tree-picker">
            <div class="category-tree-toolbar">
              <span class="category-tree-tip">勾选适用分类，支持父子分类同时选择</span>
              <el-button text @click="clearCategorySelection">清空选择</el-button>
            </div>
            <el-tree
              ref="categoryTreeRef"
              :data="categoryOptions"
              node-key="id"
              show-checkbox
              check-strictly
              check-on-click-node
              default-expand-all
              :expand-on-click-node="false"
              :props="categoryTreeProps"
              class="category-tree-picker-panel"
              empty-text="暂无分类"
              @check="handleCategoryTreeCheck"
            />
          </div>
        </el-form-item>
        <el-form-item v-else-if="form.applicableType === 3" label="指定商品">
          <el-select
            v-model="form.applicableIds"
            multiple
            filterable
            remote
            reserve-keyword
            clearable
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
            placeholder="请输入商品名称搜索并选择"
            :remote-method="handleProductSearch"
            :loading="productOptionsLoading"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantDialogVisible" title="发放优惠券" width="680px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="优惠券">
          <div class="readonly-text">{{ currentCoupon.name || '-' }}</div>
        </el-form-item>
        <el-form-item label="搜索用户">
          <el-input
            v-model="grantKeyword"
            clearable
            placeholder="输入昵称或手机号后回车"
            @keyup.enter="handleGrantSearch"
            @clear="clearGrantSearch"
          >
            <template #append>
              <el-button @click="handleGrantSearch">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="userSearchExecuted" label="搜索结果">
          <div class="grant-search-panel" v-loading="userOptionsLoading">
            <template v-if="userOptions.length">
              <div
                v-for="item in userOptions"
                :key="item.id"
                class="grant-search-item"
              >
                <div class="grant-search-main">
                  <div class="grant-search-name">{{ item.username || '未命名用户' }}</div>
                  <div class="grant-search-meta">
                    <span>{{ item.phone || item.id }}</span>
                    <span v-if="item.realName">{{ item.realName }}</span>
                  </div>
                </div>
                <div class="grant-search-actions">
                  <el-tag size="small" :type="isGrantableUser(item) ? 'success' : 'danger'">
                    {{ item.statusText }}
                  </el-tag>
                  <el-button
                    size="small"
                    :type="isSelectedGrantUser(item.id) ? 'info' : 'primary'"
                    :plain="!isSelectedGrantUser(item.id)"
                    @click="handlePickGrantUser(item)"
                  >
                    {{ isSelectedGrantUser(item.id) ? '已选择' : '加入发放' }}
                  </el-button>
                </div>
              </div>
            </template>
            <div v-else class="grant-search-empty">未找到匹配用户</div>
          </div>
        </el-form-item>
        <el-form-item label="选择用户">
          <el-select
            v-model="grantUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            clearable
            style="width: 100%"
            placeholder="请选择要发放的用户"
            :remote-method="handleUserSearch"
            :loading="userOptionsLoading"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="`${item.username || '未命名用户'} / ${item.phone || item.id} / ${item.statusText}`"
              :value="item.id"
              :disabled="!isGrantableUser(item)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantSubmitting" @click="submitGrant">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import { fetchAdminUserList } from '../../api/adminUsers';
import { fetchAdminProductCategories, fetchAdminProducts } from '../../api/adminProducts';
import {
  createAdminCoupon,
  fetchAdminCoupons,
  grantAdminCoupon,
  updateAdminCoupon,
  updateAdminCouponStatus
} from '../../api/adminCoupons';

const loading = ref(false);
const submitting = ref(false);
const grantSubmitting = ref(false);
const dialogVisible = ref(false);
const grantDialogVisible = ref(false);
const dialogMode = ref('create');
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const status = ref(undefined);
const applicableType = ref(undefined);
const currentCoupon = ref({});
const grantKeyword = ref('');
const grantUserIds = ref([]);
const userOptions = ref([]);
const userOptionsLoading = ref(false);
const userSearchExecuted = ref(false);
const categoryOptions = ref([]);
const categoryOptionsLoading = ref(false);
const productOptions = ref([]);
const productOptionsLoading = ref(false);
const categoryTreeRef = ref(null);

const form = reactive(createEmptyForm());

const categorySelectOptions = computed(() => flattenCategoryOptions(categoryOptions.value));
const categoryTreeProps = {
  label: 'name',
  children: 'children'
};

function createEmptyForm() {
  return {
    id: '',
    name: '',
    type: 1,
    discountType: 1,
    discountValue: 0,
    minAmount: 0,
    maxDiscount: null,
    totalCount: 1,
    applicableType: 1,
    applicableIds: [],
    status: 1,
    timeRange: []
  };
}

function resetForm() {
  Object.assign(form, createEmptyForm());
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '￥0.00';
  }
  return `￥${amount.toFixed(2)}`;
}

function formatTime(value) {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) {
    return '-';
  }
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function buildDiscountText(row) {
  if (Number(row.discountType) === 2) {
    return `${Number(row.discountValue || 0)} 折`;
  }
  if (Number(row.type) === 3) {
    return '运费减免';
  }
  return `${formatMoney(row.discountValue)} 优惠`;
}

function buildApplicableIdsText(values) {
  if (!Array.isArray(values) || !values.length) {
    return '适用于全部';
  }
  return `ID：${values.join('、')}`;
}

function buildApplicableDisplay(row) {
  if (!Array.isArray(row?.applicableIds) || !row.applicableIds.length) {
    return '适用于全部';
  }
  if (Number(row.applicableType) === 2) {
    return row.applicableIds
      .map(id => findCategoryLabel(id) || id)
      .join('、');
  }
  if (Number(row.applicableType) === 3) {
    return row.applicableIds
      .map(id => findProductLabel(id) || id)
      .join('、');
  }
  return buildApplicableIdsText(row.applicableIds);
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    };
    if (keyword.value && keyword.value.trim()) {
      params.keyword = keyword.value.trim();
    }
    if (typeof status.value === 'number') {
      params.status = status.value;
    }
    if (typeof applicableType.value === 'number') {
      params.applicableType = applicableType.value;
    }
    const res = await fetchAdminCoupons(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || [];
      total.value = page.total || 0;
      pageNum.value = page.current || pageNum.value;
      pageSize.value = page.size || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取优惠券列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取优惠券列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadList();
}

function handleReset() {
  keyword.value = '';
  status.value = undefined;
  applicableType.value = undefined;
  handleSearch();
}

function handlePageChange(value) {
  pageNum.value = value;
  loadList();
}

function handleSizeChange(value) {
  pageSize.value = value;
  pageNum.value = 1;
  loadList();
}

function openCreateDialog() {
  dialogMode.value = 'create';
  resetForm();
  ensureCouponOptionsLoaded();
  dialogVisible.value = true;
  nextTick(() => {
    syncCategoryTreeCheckedKeys();
  });
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  Object.assign(form, {
    id: row.id || '',
    name: row.name || '',
    type: Number(row.type || 1),
    discountType: Number(row.discountType || 1),
    discountValue: Number(row.discountValue || 0),
    minAmount: Number(row.minAmount || 0),
    maxDiscount: row.maxDiscount == null ? null : Number(row.maxDiscount),
    totalCount: Number(row.totalCount || 1),
    applicableType: Number(row.applicableType || 1),
    applicableIds: Array.isArray(row.applicableIds) ? [...row.applicableIds] : [],
    status: Number(row.status || 1),
    timeRange: row.startTime && row.endTime ? [String(row.startTime), String(row.endTime)] : []
  });
  ensureCouponOptionsLoaded();
  dialogVisible.value = true;
  nextTick(() => {
    syncCategoryTreeCheckedKeys();
  });
}

function openGrantDialog(row) {
  currentCoupon.value = row || {};
  grantKeyword.value = '';
  grantUserIds.value = [];
  userOptions.value = [];
  userSearchExecuted.value = false;
  grantDialogVisible.value = true;
}

function parseApplicableIds() {
  return Array.isArray(form.applicableIds)
    ? Array.from(new Set(form.applicableIds.map(item => String(item || '').trim()).filter(Boolean)))
    : [];
}

async function submitForm() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入优惠券名称');
    return;
  }
  if (!Array.isArray(form.timeRange) || form.timeRange.length !== 2) {
    ElMessage.warning('请选择有效时间');
    return;
  }
  if (Number(form.discountType) === 2 && Number(form.discountValue) > 10) {
    ElMessage.warning('折扣券的折扣值不能大于 10');
    return;
  }
  const applicableIds = form.applicableType === 1 ? [] : parseApplicableIds();
  if (form.applicableType !== 1 && !applicableIds.length) {
    ElMessage.warning(`请选择${form.applicableType === 2 ? '分类' : '商品'}`);
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      name: form.name.trim(),
      type: Number(form.type),
      discountType: Number(form.discountType),
      discountValue: Number(form.discountValue || 0),
      minAmount: Number(form.minAmount || 0),
      maxDiscount: form.maxDiscount == null ? null : Number(form.maxDiscount),
      totalCount: Number(form.totalCount || 1),
      applicableType: Number(form.applicableType),
      applicableIds,
      status: Number(form.status),
      startTime: Number(form.timeRange[0]),
      endTime: Number(form.timeRange[1])
    };
    const res = dialogMode.value === 'create'
      ? await createAdminCoupon(payload)
      : await updateAdminCoupon(form.id, payload);
    if (res && res.code === 200) {
      ElMessage.success(dialogMode.value === 'create' ? '优惠券创建成功' : '优惠券更新成功');
      dialogVisible.value = false;
      loadList();
      return;
    }
    throw new Error((res && res.message) || '保存优惠券失败');
  } catch (error) {
    ElMessage.error(error.message || '保存优惠券失败');
  } finally {
    submitting.value = false;
  }
}

async function updateStatus(row, nextStatus) {
  try {
    const res = await updateAdminCouponStatus(row.id, { status: nextStatus });
    if (res && res.code === 200) {
      ElMessage.success(nextStatus === 1 ? '优惠券已启用' : '优惠券已停用');
      loadList();
      return;
    }
    throw new Error((res && res.message) || '更新状态失败');
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败');
  }
}

function handleRowCommand(command, row) {
  if (command === 'edit') {
    openEditDialog(row);
    return;
  }
  if (command === 'grant') {
    openGrantDialog(row);
    return;
  }
  if (command === 'enable') {
    updateStatus(row, 1);
    return;
  }
  if (command === 'disable') {
    updateStatus(row, 2);
  }
}

function getUserStatusText(status) {
  if (Number(status) === 1) {
    return '正常';
  }
  if (Number(status) === 2) {
    return '已冻结';
  }
  if (Number(status) === 3) {
    return '注销申请中';
  }
  if (Number(status) === 4) {
    return '已注销';
  }
  return '未知状态';
}

function isGrantableUser(user) {
  return Number(user?.status) === 1;
}

function isSelectedGrantUser(userId) {
  return grantUserIds.value.includes(userId);
}

function clearGrantSearch() {
  grantKeyword.value = '';
  userSearchExecuted.value = false;
  userOptions.value = [];
}

function handlePickGrantUser(user) {
  if (!user?.id) {
    return;
  }
  if (!isGrantableUser(user)) {
    ElMessage.warning('该用户已冻结，无法发放优惠券');
    return;
  }
  if (isSelectedGrantUser(user.id)) {
    return;
  }
  grantUserIds.value = [...grantUserIds.value, user.id];
}

async function loadUserOptions(search, options = {}) {
  const { notifyWhenEmpty = false } = options;
  userOptionsLoading.value = true;
  try {
    const res = await fetchAdminUserList({
      pageNum: 1,
      pageSize: 20,
      keyword: search || undefined
    });
    if (res && res.code === 200 && res.data) {
      const items = res.data.list || res.data.records || [];
      userOptions.value = items.map(item => ({
        id: item.id,
        username: item.username,
        realName: item.realName || '',
        phone: item.phone,
        status: item.status,
        statusText: getUserStatusText(item.status)
      }));
      if (notifyWhenEmpty && !userOptions.value.length) {
        ElMessage.warning('未找到匹配用户');
      }
      return;
    }
    userOptions.value = [];
    if (notifyWhenEmpty) {
      ElMessage.warning('未找到匹配用户');
    }
  } catch (error) {
    userOptions.value = [];
    ElMessage.error(error.message || '搜索用户失败');
  } finally {
    userOptionsLoading.value = false;
  }
}

function handleUserSearch(value) {
  grantKeyword.value = value;
  loadUserOptions(value, { notifyWhenEmpty: false });
}

async function handleGrantSearch() {
  userSearchExecuted.value = true;
  await loadUserOptions(grantKeyword.value, { notifyWhenEmpty: true });
}

async function ensureCouponOptionsLoaded() {
  await Promise.all([loadCategoryOptions(), loadProductOptions('')]);
}

async function loadCategoryOptions() {
  if (categoryOptions.value.length) {
    return;
  }
  categoryOptionsLoading.value = true;
  try {
    const res = await fetchAdminProductCategories();
    if (res && res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data;
      return;
    }
    categoryOptions.value = [];
  } finally {
    categoryOptionsLoading.value = false;
  }
}

async function loadProductOptions(keywordValue) {
  productOptionsLoading.value = true;
  try {
    const res = await fetchAdminProducts('main', {
      keyword: keywordValue || undefined
    });
    if (res && res.code === 200 && Array.isArray(res.data)) {
      const incoming = res.data.map(item => ({
        id: item.id,
        label: buildProductOptionLabel(item),
        name: item.name || '',
        model: item.model || ''
      }));
      productOptions.value = mergeOptionsById(productOptions.value, incoming);
      return;
    }
    if (!keywordValue) {
      productOptions.value = [];
    }
  } finally {
    productOptionsLoading.value = false;
  }
}

function handleProductSearch(value) {
  loadProductOptions(value);
}

function handleCategoryTreeCheck() {
  const checkedKeys = categoryTreeRef.value?.getCheckedKeys?.(false) || [];
  form.applicableIds = checkedKeys.map(item => String(item));
}

function syncCategoryTreeCheckedKeys() {
  if (form.applicableType !== 2 || !categoryTreeRef.value?.setCheckedKeys) {
    return;
  }
  categoryTreeRef.value.setCheckedKeys(form.applicableIds || []);
}

function clearCategorySelection() {
  form.applicableIds = [];
  categoryTreeRef.value?.setCheckedKeys?.([]);
}

function flattenCategoryOptions(nodes, prefix = '') {
  const result = [];
  (Array.isArray(nodes) ? nodes : []).forEach(node => {
    const label = prefix ? `${prefix} / ${node.name || node.id}` : (node.name || node.id);
    result.push({
      id: node.id,
      label
    });
    if (Array.isArray(node.children) && node.children.length) {
      result.push(...flattenCategoryOptions(node.children, label));
    }
  });
  return result;
}

function buildProductOptionLabel(item) {
  const name = item?.name || '未命名商品';
  const model = item?.model ? ` / ${item.model}` : '';
  const id = item?.id ? ` / ${item.id}` : '';
  return `${name}${model}${id}`;
}

function mergeOptionsById(existing, incoming) {
  const map = new Map();
  [...(Array.isArray(existing) ? existing : []), ...(Array.isArray(incoming) ? incoming : [])].forEach(item => {
    if (item && item.id) {
      map.set(item.id, item);
    }
  });
  return Array.from(map.values());
}

function findCategoryLabel(id) {
  const target = categorySelectOptions.value.find(item => item.id === id);
  return target ? target.label : '';
}

function findProductLabel(id) {
  const target = productOptions.value.find(item => item.id === id);
  return target ? target.label : '';
}

watch(
  () => form.applicableType,
  (value, oldValue) => {
    if (value !== oldValue) {
      form.applicableIds = [];
    }
    nextTick(() => {
      if (value === 2) {
        syncCategoryTreeCheckedKeys();
      }
    });
  }
);

watch(
  () => categoryOptions.value,
  () => {
    nextTick(() => {
      syncCategoryTreeCheckedKeys();
    });
  },
  { deep: true }
);

async function submitGrant() {
  if (!currentCoupon.value.id) {
    return;
  }
  if (!grantUserIds.value.length) {
    ElMessage.warning('请选择要发放的用户');
    return;
  }
  grantSubmitting.value = true;
  try {
    const res = await grantAdminCoupon(currentCoupon.value.id, {
      userIds: grantUserIds.value
    });
    if (res && res.code === 200) {
      const data = res.data || {};
      ElMessage.success(`发放完成：成功 ${data.grantCount || 0} 人，跳过 ${data.skipCount || 0} 人`);
      grantDialogVisible.value = false;
      loadList();
      return;
    }
    throw new Error((res && res.message) || '发放优惠券失败');
  } catch (error) {
    ElMessage.error(error.message || '发放优惠券失败');
  } finally {
    grantSubmitting.value = false;
  }
}

onMounted(() => {
  loadList();
  loadCategoryOptions();
  loadProductOptions('');
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.coupon-page {
  padding: 16px;
  box-sizing: border-box;
}

.page-card {
  width: 100%;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.page-subtitle {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-item {
  width: 160px;
}

.keyword-input {
  width: 280px;
}

.coupon-table {
  width: 100%;
}

.coupon-table-header {
  background-color: #f5f7fa;
}

.stack-text {
  line-height: 1.6;
}

.stack-text.strong {
  color: #303133;
  font-weight: 600;
}

.stack-text.muted {
  color: #909399;
  font-size: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.readonly-text {
  color: #606266;
}

.action-trigger {
  min-width: 76px;
}

.grant-search-panel {
  width: 100%;
  min-height: 56px;
  max-height: 260px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
}

.grant-search-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
}

.grant-search-item:last-child {
  border-bottom: none;
}

.grant-search-main {
  min-width: 0;
}

.grant-search-name {
  color: #303133;
  font-weight: 600;
}

.grant-search-meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #909399;
  font-size: 12px;
}

.grant-search-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.grant-search-empty {
  padding: 18px 14px;
  color: #909399;
  font-size: 13px;
}

.category-tree-picker {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
}

.category-tree-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
}

.category-tree-tip {
  font-size: 12px;
  color: #909399;
}

.category-tree-picker-panel {
  max-height: 260px;
  overflow: auto;
  padding: 8px 12px 12px;
}

.category-tree-picker-panel :deep(.el-tree-node__content) {
  min-height: 34px;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .grant-search-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .grant-search-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
