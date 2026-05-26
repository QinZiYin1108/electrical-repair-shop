<template>
  <div class="reserve-orders-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">预约订单列表</div>
          <div class="page-subtitle">支持多条件筛选，快速查看预约订单并进入详情页。</div>
        </div>
      </div>

      <div class="filter-panel">
        <el-input
          v-model="keyword"
          clearable
          class="filter-item keyword-input"
          placeholder="搜索订单号、用户、师傅、服务项或地址"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="status" clearable class="filter-item" placeholder="订单状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="paymentStatus" clearable class="filter-item" placeholder="支付状态">
          <el-option v-for="item in paymentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="serviceMode" clearable class="filter-item" placeholder="服务方式">
          <el-option v-for="item in serviceModeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="appointmentRange"
          class="filter-item date-range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="预约开始时间"
          end-placeholder="预约结束时间"
          value-format="x"
        />
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
        <el-table-column label="服务信息" min-width="220">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.serviceTypeName || '-' }}</div>
            <div class="stack-text muted">{{ row.serviceCategoryPath || row.serviceCategoryName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="170">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.userName || row.contactName || '-' }}</div>
            <div class="stack-text muted">{{ row.userPhone || row.contactPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="师傅信息" min-width="170">
          <template #default="{ row }">
            <div class="stack-text strong">{{ row.technicianName || '-' }}</div>
            <div class="stack-text muted">{{ row.technicianPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="服务方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.serviceModeText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">{{ row.statusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getPaymentTagType(row.paymentStatus)">{{ row.paymentStatusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.appointmentTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="订单金额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="已支付" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.paidAmount) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看详情</el-button>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { fetchAdminReserveOrders } from '../../api/adminOrders';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const status = ref(undefined);
const paymentStatus = ref(undefined);
const serviceMode = ref(undefined);
const appointmentRange = ref([]);

const statusOptions = [
  { label: '待接单', value: 1 },
  { label: '待上门', value: 2 },
  { label: '待检查', value: 3 },
  { label: '待支付', value: 4 },
  { label: '服务中', value: 5 },
  { label: '已完成', value: 6 },
  { label: '已取消', value: 7 },
  { label: '已退款', value: 8 }
];

const paymentStatusOptions = [
  { label: '待支付', value: 1 },
  { label: '已支付/已预付', value: 2 },
  { label: '已退款', value: 3 }
];

const serviceModeOptions = [
  { label: '上门维修', value: 1 },
  { label: '上门安装', value: 2 },
  { label: '线下维修', value: 3 }
];

function getStatusTagType(value) {
  if (value === 1 || value === 2 || value === 3 || value === 4) return 'warning';
  if (value === 5) return 'primary';
  if (value === 6) return 'success';
  return 'info';
}

function getPaymentTagType(value) {
  if (value === 2) return 'success';
  if (value === 3) return 'info';
  return 'warning';
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
    if (typeof paymentStatus.value === 'number') {
      params.paymentStatus = paymentStatus.value;
    }
    if (typeof serviceMode.value === 'number') {
      params.serviceMode = serviceMode.value;
    }
    if (Array.isArray(appointmentRange.value) && appointmentRange.value.length === 2) {
      params.appointmentStart = Number(appointmentRange.value[0]);
      params.appointmentEnd = Number(appointmentRange.value[1]);
    }

    const res = await fetchAdminReserveOrders(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || page.list || [];
      total.value = page.total || 0;
      pageNum.value = page.current || page.pageNum || pageNum.value;
      pageSize.value = page.size || page.pageSize || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取预约订单列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取预约订单列表失败');
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
  paymentStatus.value = undefined;
  serviceMode.value = undefined;
  appointmentRange.value = [];
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
  router.push(`/admin/orders/reserve/${id}`);
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.reserve-orders-page {
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
  width: 300px;
}

.date-range {
  width: 360px;
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
</style>
