<template>
  <div class="product-orders-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">商品订单列表</div>
          <div class="page-subtitle">支持按订单状态、支付状态和物流状态筛选商品订单。</div>
        </div>
      </div>

      <div class="filter-panel">
        <el-input
          v-model="keyword"
          clearable
          class="filter-item keyword-input"
          placeholder="搜索订单号、商品、用户或收货信息"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="orderStatus" clearable class="filter-item" placeholder="订单状态">
          <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="paymentStatus" clearable class="filter-item" placeholder="支付状态">
          <el-option v-for="item in paymentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="deliveryStatus" clearable class="filter-item" placeholder="物流状态">
          <el-option v-for="item in deliveryStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        class="orders-table"
        header-cell-class-name="orders-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
        <el-table-column label="商品信息" min-width="230">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.productSummary || '-' }}</div>
            <div class="stack-text muted">共 {{ row.itemCount || 0 }} 件商品</div>
          </template>
        </el-table-column>
        <el-table-column label="收货信息" min-width="190">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.deliveryName || '-' }} / {{ row.deliveryPhone || '-' }}</div>
            <div class="stack-text muted">{{ row.deliveryAddress || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="160">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.userName || '-' }}</div>
            <div class="stack-text muted">{{ row.userPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getOrderTagType(row.orderStatus)">{{ row.orderStatusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getPaymentTagType(row.paymentStatus)">{{ row.paymentStatusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物流状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getDeliveryTagType(row.deliveryStatus)">{{ row.deliveryStatusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="110" align="center">
          <template #default="{ row }">{{ row.paymentMethodText || '-' }}</template>
        </el-table-column>
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) || '-' }}</template>
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
                  <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                  <el-dropdown-item v-if="canShip(row)" command="ship">立即发货</el-dropdown-item>
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

    <el-dialog v-model="shipDialogVisible" title="商品订单发货" width="460px" destroy-on-close>
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="88px">
        <el-form-item label="订单号">
          <div class="readonly-text">{{ currentOrder.orderNo || '-' }}</div>
        </el-form-item>
        <el-form-item label="快递公司" prop="deliveryCompany">
          <el-input v-model="shipForm.deliveryCompany" maxlength="30" placeholder="请输入快递公司" />
        </el-form-item>
        <el-form-item label="快递单号" prop="deliveryNo">
          <el-input v-model="shipForm.deliveryNo" maxlength="50" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span>
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import {
  fetchAdminProductOrders,
  shipAdminProductOrder
} from '../../api/adminProductOrders';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const orderStatus = ref(undefined);
const paymentStatus = ref(undefined);
const deliveryStatus = ref(undefined);
const shipDialogVisible = ref(false);
const shipSubmitting = ref(false);
const shipFormRef = ref();
const currentOrder = ref({ id: '', orderNo: '' });
const shipForm = reactive({
  deliveryCompany: '',
  deliveryNo: ''
});

const orderStatusOptions = [
  { label: '待发货', value: 2 },
  { label: '待收货', value: 3 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 },
  { label: '已退款', value: 7 }
];

const paymentStatusOptions = [
  { label: '待支付', value: 1 },
  { label: '已支付', value: 2 },
  { label: '已退款', value: 3 }
];

const deliveryStatusOptions = [
  { label: '待发货', value: 1 },
  { label: '已发货', value: 2 },
  { label: '配送中', value: 3 },
  { label: '已送达', value: 4 }
];

const shipRules = {
  deliveryCompany: [{ required: true, message: '请输入快递公司', trigger: 'blur' }],
  deliveryNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }]
};

function getOrderTagType(value) {
  if (value === 2 || value === 3 || value === 4) return 'warning';
  if (value === 5) return 'success';
  return 'info';
}

function getPaymentTagType(value) {
  if (value === 2) return 'success';
  if (value === 3) return 'info';
  return 'warning';
}

function getDeliveryTagType(value) {
  if (value === 1) return 'warning';
  if (value === 2 || value === 3) return 'primary';
  if (value === 4) return 'success';
  return 'info';
}

function formatTime(value) {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '';
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '￥0.00';
  return `￥${amount.toFixed(2)}`;
}

function canShip(row) {
  return Number(row.orderStatus) === 2 && Number(row.deliveryStatus) === 1 && Number(row.paymentStatus) === 2;
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
    if (typeof orderStatus.value === 'number') {
      params.orderStatus = orderStatus.value;
    }
    if (typeof paymentStatus.value === 'number') {
      params.paymentStatus = paymentStatus.value;
    }
    if (typeof deliveryStatus.value === 'number') {
      params.deliveryStatus = deliveryStatus.value;
    }

    const res = await fetchAdminProductOrders(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || [];
      total.value = page.total || 0;
      pageNum.value = page.current || pageNum.value;
      pageSize.value = page.size || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取商品订单列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取商品订单列表失败');
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
  orderStatus.value = undefined;
  paymentStatus.value = undefined;
  deliveryStatus.value = undefined;
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

function goDetail(id) {
  if (!id) return;
  router.push(`/admin/orders/product/${id}`);
}

function handleRowCommand(command, row) {
  if (command === 'detail') {
    goDetail(row.id);
    return;
  }
  if (command === 'ship' && canShip(row)) {
    openShipDialog(row);
  }
}

function openShipDialog(row) {
  currentOrder.value = row || { id: '', orderNo: '' };
  shipForm.deliveryCompany = '';
  shipForm.deliveryNo = '';
  shipDialogVisible.value = true;
}

async function submitShip() {
  if (!shipFormRef.value || !currentOrder.value.id) return;
  const valid = await shipFormRef.value.validate().catch(() => false);
  if (!valid) return;
  shipSubmitting.value = true;
  try {
    const res = await shipAdminProductOrder(currentOrder.value.id, {
      deliveryCompany: shipForm.deliveryCompany,
      deliveryNo: shipForm.deliveryNo
    });
    if (res && res.code === 200) {
      ElMessage.success('发货成功');
      shipDialogVisible.value = false;
      loadList();
      return;
    }
    throw new Error((res && res.message) || '发货失败');
  } catch (error) {
    ElMessage.error(error.message || '发货失败');
  } finally {
    shipSubmitting.value = false;
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
.product-orders-page {
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
  width: 320px;
}

.orders-table {
  width: 100%;
}

.orders-table-header {
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

.readonly-text {
  color: #606266;
}

.action-trigger {
  min-width: 76px;
}
</style>
