<template>
  <div class="warranty-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">保修卡管理</div>
          <div class="page-subtitle">查看系统自动生成的保修卡，并支持手动补录保修卡信息。</div>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增保修卡</el-button>
      </div>

      <div class="filter-panel">
        <el-input
          v-model="keyword"
          clearable
          class="filter-item keyword-input"
          placeholder="搜索保修卡号"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="warrantyStatus" clearable class="filter-item" placeholder="保修状态">
          <el-option label="有效" :value="1" />
          <el-option label="已过期" :value="2" />
          <el-option label="已使用" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border class="warranty-table" header-cell-class-name="warranty-table-header">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="cardNo" label="保修卡号" min-width="180" show-overflow-tooltip />
        <el-table-column label="商品信息" min-width="220">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.productName || '-' }}</div>
            <div class="stack-text muted">型号：{{ row.productModel || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="170">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.userName || '-' }}</div>
            <div class="stack-text muted">{{ row.userPhone || row.userId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="保修期" min-width="180">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.warrantyStartDate || '-' }}</div>
            <div class="stack-text muted">至 {{ row.warrantyEndDate || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">{{ row.warrantyTypeText || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getWarrantyStatusTagType(row.warrantyStatus)">{{ row.warrantyStatusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="维修次数" width="100" align="center">
          <template #default="{ row }">{{ row.repairCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetailDialog(row.id)">查看详情</el-button>
            <el-button type="success" link @click="openUsageDialog(row)">使用记录</el-button>
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

    <el-dialog v-model="detailDialogVisible" title="保修卡详情" width="620px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="保修卡号">{{ detail.cardNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.userName || '-' }} / {{ detail.userPhone || detail.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ detail.productName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品型号">{{ detail.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="购买日期">{{ detail.purchaseDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保修时间">{{ detail.warrantyStartDate || '-' }} 至 {{ detail.warrantyEndDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保修类型">{{ detail.warrantyTypeText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保修状态">{{ detail.warrantyStatusText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修次数">{{ detail.repairCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近维修">{{ detail.lastRepairDate || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="usageDialogVisible" title="保修使用记录" width="980px" destroy-on-close>
      <div class="usage-dialog-header">
        <div class="usage-card-title">{{ currentUsageCard.productName || '-' }}</div>
        <div class="usage-card-subtitle">保修卡号：{{ currentUsageCard.cardNo || '-' }}</div>
      </div>
      <el-table v-loading="usageLoading" :data="usageRecords" border>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="申请信息" min-width="220">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.issueDescription || '-' }}</div>
            <div class="stack-text muted">联系人：{{ row.contactName || '-' }} / {{ row.contactPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.userName || '-' }}</div>
            <div class="stack-text muted">{{ row.userPhone || row.userId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getUsageStatusTagType(row.status)">{{ row.statusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" min-width="200">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.processRemark || '-' }}</div>
            <div class="stack-text muted">处理时间：{{ formatDateTime(row.processTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="Number(row.status) === 1" type="success" link @click="openProcessDialog(row, 2)">完成处理</el-button>
            <el-button v-if="Number(row.status) === 1" type="danger" link @click="openProcessDialog(row, 3)">驳回申请</el-button>
            <span v-if="Number(row.status) !== 1" class="stack-text muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="createDialogVisible" title="新增保修卡" width="720px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="选择用户" required>
          <el-select
            v-model="createForm.userId"
            filterable
            remote
            reserve-keyword
            clearable
            style="width: 100%"
            placeholder="请输入昵称或手机号搜索用户"
            :remote-method="searchUsers"
            :loading="userOptionsLoading"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="`${item.username || '未命名用户'} / ${item.phone || item.id}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择商品" required>
          <el-select
            v-model="createForm.productId"
            filterable
            remote
            reserve-keyword
            clearable
            style="width: 100%"
            placeholder="请输入商品名称搜索商品"
            :remote-method="searchProducts"
            :loading="productOptionsLoading"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="`${item.name || '未命名商品'} / ${item.model || item.id}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="购买日期">
            <el-date-picker v-model="createForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="保修开始">
            <el-date-picker v-model="createForm.warrantyStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="保修月数">
            <el-input-number v-model="createForm.warrantyPeriod" :min="1" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="保修类型" required>
            <el-select v-model="createForm.warrantyType" style="width: 100%">
              <el-option label="厂家保修" :value="1" />
              <el-option label="店铺保修" :value="2" />
              <el-option label="延保" :value="3" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="processDialogVisible" :title="processForm.status === 2 ? '完成保修处理' : '驳回保修申请'" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="处理备注">
          <el-input
            v-model="processForm.processRemark"
            type="textarea"
            :rows="4"
            :placeholder="processForm.status === 2 ? '可填写维修结果、完成说明等' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="processSubmitting" @click="submitUsageProcess">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import { fetchAdminProducts } from '../../api/adminProducts';
import { fetchAdminUserList } from '../../api/adminUsers';
import {
  createAdminWarrantyCard,
  fetchAdminWarrantyCardDetail,
  fetchAdminWarrantyCards,
  fetchAdminWarrantyUsageRecords,
  processAdminWarrantyUsageRecord
} from '../../api/adminWarrantyCards';

const loading = ref(false);
const creating = ref(false);
const usageLoading = ref(false);
const processSubmitting = ref(false);
const detailDialogVisible = ref(false);
const createDialogVisible = ref(false);
const usageDialogVisible = ref(false);
const processDialogVisible = ref(false);
const list = ref([]);
const detail = ref(null);
const usageRecords = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const warrantyStatus = ref(undefined);
const userOptions = ref([]);
const productOptions = ref([]);
const userOptionsLoading = ref(false);
const productOptionsLoading = ref(false);
const currentUsageCard = ref({});

const createForm = reactive(createEmptyForm());
const processForm = reactive({
  recordId: '',
  status: 2,
  processRemark: ''
});

function createEmptyForm() {
  return {
    userId: '',
    productId: '',
    purchaseDate: '',
    warrantyStartDate: '',
    warrantyPeriod: 12,
    warrantyType: 1
  };
}

function getWarrantyStatusTagType(value) {
  const status = Number(value);
  if (status === 1) {
    return 'success';
  }
  if (status === 3) {
    return 'warning';
  }
  return 'info';
}

function getUsageStatusTagType(value) {
  const status = Number(value);
  if (status === 2) {
    return 'success';
  }
  if (status === 3) {
    return 'danger';
  }
  return 'warning';
}

function formatDateTime(value) {
  const timestamp = Number(value || 0);
  if (!timestamp) {
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

function resetCreateForm() {
  Object.assign(createForm, createEmptyForm());
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
    if (typeof warrantyStatus.value === 'number') {
      params.warrantyStatus = warrantyStatus.value;
    }
    const res = await fetchAdminWarrantyCards(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || [];
      total.value = page.total || 0;
      pageNum.value = page.current || pageNum.value;
      pageSize.value = page.size || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取保修卡列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取保修卡列表失败');
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
  warrantyStatus.value = undefined;
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

async function openDetailDialog(id) {
  if (!id) {
    return;
  }
  try {
    const res = await fetchAdminWarrantyCardDetail(id);
    if (res && res.code === 200 && res.data) {
      detail.value = res.data;
      detailDialogVisible.value = true;
      return;
    }
    throw new Error((res && res.message) || '获取保修卡详情失败');
  } catch (error) {
    ElMessage.error(error.message || '获取保修卡详情失败');
  }
}

async function openUsageDialog(row) {
  if (!row || !row.id) {
    return;
  }
  currentUsageCard.value = row;
  usageDialogVisible.value = true;
  usageLoading.value = true;
  try {
    const res = await fetchAdminWarrantyUsageRecords(row.id);
    if (res && res.code === 200 && res.data) {
      usageRecords.value = res.data.items || [];
      return;
    }
    throw new Error((res && res.message) || '获取保修使用记录失败');
  } catch (error) {
    usageRecords.value = [];
    ElMessage.error(error.message || '获取保修使用记录失败');
  } finally {
    usageLoading.value = false;
  }
}

function openCreateDialog() {
  resetCreateForm();
  userOptions.value = [];
  productOptions.value = [];
  createDialogVisible.value = true;
  searchUsers('');
  searchProducts('');
}

function openProcessDialog(row, status) {
  processForm.recordId = row.id || '';
  processForm.status = Number(status || 2);
  processForm.processRemark = row.processRemark || '';
  processDialogVisible.value = true;
}

async function searchUsers(query) {
  userOptionsLoading.value = true;
  try {
    const res = await fetchAdminUserList({
      pageNum: 1,
      pageSize: 20,
      keyword: query || undefined
    });
    if (res && res.code === 200 && res.data) {
      const items = res.data.list || res.data.records || [];
      userOptions.value = items.map(item => ({
        id: item.id,
        username: item.username,
        phone: item.phone
      }));
      return;
    }
    userOptions.value = [];
  } finally {
    userOptionsLoading.value = false;
  }
}

async function searchProducts(query) {
  productOptionsLoading.value = true;
  try {
    const res = await fetchAdminProducts('main', {
      keyword: query || undefined
    });
    if (res && res.code === 200 && Array.isArray(res.data)) {
      productOptions.value = res.data.map(item => ({
        id: item.id,
        name: item.name,
        model: item.model
      }));
      return;
    }
    productOptions.value = [];
  } finally {
    productOptionsLoading.value = false;
  }
}

async function submitCreate() {
  if (!createForm.userId) {
    ElMessage.warning('请选择用户');
    return;
  }
  if (!createForm.productId) {
    ElMessage.warning('请选择商品');
    return;
  }
  if (!createForm.warrantyType) {
    ElMessage.warning('请选择保修类型');
    return;
  }
  creating.value = true;
  try {
    const res = await createAdminWarrantyCard({
      userId: createForm.userId,
      productId: createForm.productId,
      purchaseDate: createForm.purchaseDate || undefined,
      warrantyStartDate: createForm.warrantyStartDate || undefined,
      warrantyPeriod: Number(createForm.warrantyPeriod || 0),
      warrantyType: Number(createForm.warrantyType)
    });
    if (res && res.code === 200) {
      ElMessage.success('保修卡创建成功');
      createDialogVisible.value = false;
      loadList();
      return;
    }
    throw new Error((res && res.message) || '新增保修卡失败');
  } catch (error) {
    ElMessage.error(error.message || '新增保修卡失败');
  } finally {
    creating.value = false;
  }
}

async function submitUsageProcess() {
  if (!processForm.recordId) {
    return;
  }
  processSubmitting.value = true;
  try {
    const res = await processAdminWarrantyUsageRecord(processForm.recordId, {
      status: Number(processForm.status),
      processRemark: processForm.processRemark || undefined
    });
    if (res && res.code === 200) {
      ElMessage.success(processForm.status === 2 ? '保修申请已完成处理' : '保修申请已驳回');
      processDialogVisible.value = false;
      await openUsageDialog(currentUsageCard.value);
      await loadList();
      return;
    }
    throw new Error((res && res.message) || '处理保修申请失败');
  } catch (error) {
    ElMessage.error(error.message || '处理保修申请失败');
  } finally {
    processSubmitting.value = false;
  }
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.warranty-page {
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

.warranty-table {
  width: 100%;
}

.warranty-table-header {
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

.usage-dialog-header {
  margin-bottom: 16px;
}

.usage-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.usage-card-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
