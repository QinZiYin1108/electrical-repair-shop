<template>
  <div class="aftersales-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">售后申请处理</div>
          <div class="page-subtitle">集中查看用户售后申请，并进入详情页处理退款或驳回。</div>
        </div>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            clearable
            class="search-input"
            placeholder="搜索订单号、原因或说明"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="status" clearable class="status-select" placeholder="全部状态" @change="handleSearch">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        class="request-table"
        header-cell-class-name="request-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userName" label="用户" min-width="140" show-overflow-tooltip />
        <el-table-column prop="userPhone" label="用户电话" min-width="140" show-overflow-tooltip />
        <el-table-column prop="technicianName" label="师傅" min-width="120" show-overflow-tooltip />
        <el-table-column prop="serviceTypeName" label="服务项目" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="申请类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.applicationTypeText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">{{ row.statusText || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="170">
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
import { fetchAdminAfterSalesRequests } from '../../api/adminAftersales';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const status = ref(undefined);

const statusOptions = [
  { label: '待审核', value: 1 },
  { label: '审核通过', value: 2 },
  { label: '审核拒绝', value: 3 },
  { label: '处理中', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 }
];

function getStatusTagType(value) {
  if (value === 1) return 'warning';
  if (value === 4) return 'primary';
  if (value === 5) return 'success';
  if (value === 3 || value === 6) return 'info';
  return 'danger';
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
    const res = await fetchAdminAfterSalesRequests(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || page.list || [];
      total.value = page.total || 0;
      pageNum.value = page.current || page.pageNum || pageNum.value;
      pageSize.value = page.size || page.pageSize || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取售后申请列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取售后申请列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadList();
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
  router.push(`/admin/aftersales/requests/${id}`);
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.aftersales-page {
  padding: 16px;
  box-sizing: border-box;
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.status-select {
  width: 140px;
}

.request-table {
  width: 100%;
}

.request-table-header {
  background-color: #f5f7fa;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
