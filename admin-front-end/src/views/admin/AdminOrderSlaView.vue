<template>
  <div class="ops-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">订单 SLA 超时事件</div>
          <div class="page-subtitle">
            维修订单各阶段超时事件（责任方 / 通知次数 / 升级级别 / 处理结果）。
          </div>
        </div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="filter-panel">
        <el-input
          v-model="filter.orderId"
          placeholder="订单ID"
          clearable
          class="w220"
          @keyup.enter="loadList"
        />
        <el-select
          v-model="filter.status"
          clearable
          placeholder="事件状态"
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
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="orderId" label="订单ID" width="180" show-overflow-tooltip />
        <el-table-column prop="slaType" label="SLA类型" width="120" align="center" />
        <el-table-column prop="responsibleParty" label="责任方" width="110" align="center" />
        <el-table-column label="事件状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{
              STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="escalationLevel" label="升级级别" width="90" align="center" />
        <el-table-column prop="notifyCount" label="通知次数" width="90" align="center" />
        <el-table-column prop="overdueMinutes" label="超时(分)" width="90" align="center" />
        <el-table-column prop="actionTaken" label="处理动作" width="120" align="center" />
        <el-table-column label="首次超时" width="170">
          <template #default="{ row }">{{ formatTime(row.firstExceededTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { fetchOrderSla } from '../../api/adminOrderSla';

const STATUS_TEXT = { 1: '处理中', 2: '已恢复', 3: '已自动处理', 4: '已忽略' };

const loading = ref(false);
const list = ref([]);
const filter = reactive({ orderId: '', status: null });

function statusTagType(status) {
  return { 1: 'warning', 2: 'success', 3: 'info', 4: 'info' }[status] || '';
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
    const res = await fetchOrderSla({
      orderId: filter.orderId || undefined,
      status: filter.status ?? undefined,
      limit: 100
    });
    if (res.code === 200) list.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
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
.w220 {
  width: 220px;
}
</style>
