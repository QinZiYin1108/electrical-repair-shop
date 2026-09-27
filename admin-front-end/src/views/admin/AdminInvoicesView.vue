<template>
  <div class="ops-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">发票管理</div>
          <div class="page-subtitle">用户发票申请的开票、驳回与红冲。</div>
        </div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="filter-panel">
        <el-select
          v-model="filter.status"
          clearable
          placeholder="状态"
          class="w160"
          @change="loadList"
        >
          <el-option
            v-for="(label, code) in STATUS_TEXT"
            :key="code"
            :value="Number(code)"
            :label="label"
          />
        </el-select>
        <el-select
          v-model="filter.orderType"
          clearable
          placeholder="订单类型"
          class="w160"
          @change="loadList"
        >
          <el-option :value="1" label="维修订单" />
          <el-option :value="2" label="商品订单" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="invoiceNo" label="申请号" width="200" show-overflow-tooltip />
        <el-table-column prop="userAccountId" label="用户" width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ row.orderType === 1 ? '维修' : '商品' }}</template>
        </el-table-column>
        <el-table-column prop="orderId" label="订单ID" width="180" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额(元)" width="110" align="right" />
        <el-table-column prop="taxAmount" label="税额" width="90" align="right" />
        <el-table-column prop="title" label="抬头" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{
              STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button size="small" type="success" @click="issue(row)">开票</el-button>
              <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button v-else-if="row.status === 2" size="small" type="warning" @click="red(row)"
              >红冲</el-button
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { fetchInvoices, issueInvoice, rejectInvoice, redInvoice } from '../../api/adminInvoices';

const STATUS_TEXT = { 1: '待开票', 2: '已开票', 3: '已红冲', 4: '已驳回' };

const loading = ref(false);
const list = ref([]);
const filter = reactive({ status: null, orderType: null });

function statusTagType(status) {
  return { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }[status] || '';
}

async function loadList() {
  loading.value = true;
  try {
    const res = await fetchInvoices({
      status: filter.status ?? undefined,
      orderType: filter.orderType ?? undefined,
      limit: 100
    });
    if (res.code === 200) list.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function issue(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写开票结果地址/PDF 链接（可空）', '开票', {
      inputValue: ''
    });
    await issueInvoice(row.id, { invoiceUrl: value || '' });
    ElMessage.success('已开票');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入原因')
    });
    await rejectInvoice(row.id, { reason: value });
    ElMessage.success('已驳回');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function red(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写红冲原因', '红冲', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入原因')
    });
    await redInvoice(row.id, { reason: value });
    ElMessage.success('已红冲');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
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
.w160 {
  width: 160px;
}
</style>
