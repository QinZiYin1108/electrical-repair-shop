<template>
  <div class="cancel-reasons-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">订单取消原因</div>
          <div class="page-subtitle">查看取消原因记录与分类统计，支持按订单类型和时间筛选。</div>
        </div>
      </div>

      <div class="filter-panel">
        <el-select v-model="filter.orderType" clearable class="filter-item" placeholder="订单类型">
          <el-option :value="1" label="维修订单" />
          <el-option :value="2" label="商品订单" />
        </el-select>
        <el-date-picker
          v-model="filter.dateRange"
          class="filter-item date-range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="x"
        />
        <el-button type="primary" @click="doSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="success" plain @click="exportCSV" style="margin-left: auto"
          >导出CSV</el-button
        >
      </div>

      <!-- Stats cards -->
      <div v-if="stats.length" class="stats-row">
        <el-card v-for="s in stats" :key="s.reasonCode" class="stat-card" shadow="hover">
          <div class="stat-label">{{ s.reasonLabel || s.reasonCode }}</div>
          <div class="stat-count">{{ s.count }}</div>
        </el-card>
      </div>
      <el-empty v-if="statsSearched && !stats.length" description="暂无统计数据" />

      <el-divider />

      <!-- Record list -->
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="记录ID" width="180" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单ID" width="180" show-overflow-tooltip />
        <el-table-column label="订单类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderType === 1 ? '' : 'warning'">{{
              row.orderType === 1 ? '维修' : '商品'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reasonLabel" label="取消原因" width="120" />
        <el-table-column label="原因编码" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.reasonCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userRemark" label="用户备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
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
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../../api/request';

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const stats = ref([]);
const statsSearched = ref(false);

const filter = reactive({
  orderType: null,
  dateRange: null
});

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function buildFilterParams() {
  const p = {};
  if (filter.orderType != null) p.orderType = filter.orderType;
  if (filter.dateRange && filter.dateRange.length === 2) {
    p.startTime = filter.dateRange[0];
    p.endTime = filter.dateRange[1];
  }
  return p;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await request({
      url: '/admin/cancel-reasons/list',
      method: 'get',
      params: { page: pageNum.value, size: pageSize.value, ...buildFilterParams() }
    });
    if (res.code === 200 && res.data) {
      list.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } catch (e) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  try {
    const res = await request({
      url: '/admin/cancel-reasons/stats',
      method: 'get',
      params: buildFilterParams()
    });
    if (res.code === 200 && res.data) {
      stats.value = res.data || [];
    }
    statsSearched.value = true;
  } catch (e) {
    /* ignore */
  }
}

function doSearch() {
  pageNum.value = 1;
  loadList();
  loadStats();
}

function resetFilters() {
  filter.orderType = null;
  filter.dateRange = null;
  pageNum.value = 1;
  loadList();
  loadStats();
}

function handlePageChange(p) {
  pageNum.value = p;
  loadList();
}
function handleSizeChange(s) {
  pageSize.value = s;
  pageNum.value = 1;
  loadList();
}

async function exportCSV() {
  try {
    // 获取全量数据用于导出（不分页）
    const res = await request({
      url: '/admin/cancel-reasons/list',
      method: 'get',
      params: { page: 1, size: 99999, ...buildFilterParams() }
    });
    if (res.code !== 200 || !res.data?.records?.length) {
      ElMessage.warning('没有可导出的数据');
      return;
    }
    const records = res.data.records;
    const headers = ['记录ID', '订单ID', '订单类型', '原因编码', '原因标签', '用户备注', '时间'];
    const rows = records.map((r) => [
      r.id || '',
      r.orderId || '',
      r.orderType === 1 ? '维修订单' : r.orderType === 2 ? '商品订单' : '未知',
      r.reasonCode || '',
      r.reasonLabel || '',
      r.userRemark || '',
      r.createdTime ? formatTime(r.createdTime) : ''
    ]);
    const bom = '﻿';
    const csv =
      bom +
      [headers, ...rows]
        .map((row) => row.map((c) => `"${(c || '').replace(/"/g, '""')}"`).join(','))
        .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `取消原因统计_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error('导出失败');
  }
}

onMounted(() => {
  loadList();
  loadStats();
});
</script>

<style scoped>
.filter-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  align-items: center;
}
.filter-item {
  width: 180px;
}
.date-range {
  width: 360px;
}
.stats-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.stat-card {
  flex: 0 0 auto;
  min-width: 140px;
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}
.stat-count {
  font-size: 28px;
  font-weight: 600;
  color: #1677ff;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
