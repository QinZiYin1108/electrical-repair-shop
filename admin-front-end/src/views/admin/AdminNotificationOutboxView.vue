<template>
  <div class="ops-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">通知 outbox</div>
          <div class="page-subtitle">事务 outbox 待发送/失败/死信记录，支持人工重试。</div>
        </div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="filter-panel">
        <el-select
          v-model="filter.status"
          clearable
          placeholder="状态"
          class="w180"
          @change="loadList"
        >
          <el-option
            v-for="(label, code) in STATUS_TEXT"
            :key="code"
            :value="Number(code)"
            :label="label"
          />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="eventType" label="事件类型" width="200" show-overflow-tooltip />
        <el-table-column prop="channel" label="渠道" width="100" align="center" />
        <el-table-column prop="receiverId" label="接收人" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{
              STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="70" align="center" />
        <el-table-column prop="lastError" label="最近错误" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 4 || row.status === 5"
              size="small"
              type="primary"
              @click="retry(row)"
              >重试</el-button
            >
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchNotificationOutbox,
  retryNotificationOutbox
} from '../../api/adminNotificationOutbox';

const STATUS_TEXT = { 1: '待发送', 2: '发送中', 3: '已发送', 4: '待重试', 5: '死信' };

const loading = ref(false);
const list = ref([]);
const filter = reactive({ status: null });

function statusTagType(status) {
  return { 1: 'info', 2: 'warning', 3: 'success', 4: 'warning', 5: 'danger' }[status] || '';
}

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await fetchNotificationOutbox({ status: filter.status ?? undefined, limit: 100 });
    if (res.code === 200) list.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function retry(row) {
  try {
    await retryNotificationOutbox(row.id);
    ElMessage.success('已重置为待发送');
    loadList();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
}

onMounted(loadList);
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.filter-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.w180 {
  width: 180px;
}
</style>
