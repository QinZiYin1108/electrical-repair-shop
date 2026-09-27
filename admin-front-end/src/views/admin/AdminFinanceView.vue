<template>
  <div class="ops-page">
    <el-card shadow="never" class="mb16">
      <div class="page-title">财务报表</div>
      <div class="page-subtitle">
        按时间口径汇总订单财务快照（佣金率/税率默认 0，仅记录与展示）。
      </div>
      <div class="filter-panel" style="margin-top: 12px">
        <el-select v-model="report.timeBasis" class="w180">
          <el-option value="PAY_TIME" label="按支付时间" />
          <el-option value="PERFORM_TIME" label="按履约时间" />
          <el-option value="REFUND_TIME" label="按退款时间" />
        </el-select>
        <el-date-picker
          v-model="report.range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="x"
          class="date-range"
        />
        <el-button type="primary" @click="loadReport">生成报表</el-button>
      </div>

      <div v-if="reportRow" class="stats-row">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">订单数</div>
          <div class="stat-count">{{ reportRow.orderCount }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">实收(元)</div>
          <div class="stat-count">{{ reportRow.totalPaid }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">退款(元)</div>
          <div class="stat-count">{{ reportRow.totalRefunded }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">净额(元)</div>
          <div class="stat-count">{{ reportRow.netAmount }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">平台佣金</div>
          <div class="stat-count">{{ reportRow.platformCommission }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">师傅应结</div>
          <div class="stat-count">{{ reportRow.technicianIncome }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">税额</div>
          <div class="stat-count">{{ reportRow.taxAmount }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="page-header">
        <div class="page-title">订单财务快照</div>
        <div>
          <el-button type="primary" plain @click="openRebuild">重算快照</el-button>
          <el-button :icon="Refresh" @click="loadSnapshots">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="snapshots" border>
        <el-table-column prop="orderId" label="订单ID" width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ row.orderType === 1 ? '维修' : '商品' }}</template>
        </el-table-column>
        <el-table-column prop="incomeService" label="服务费" width="90" align="right" />
        <el-table-column prop="incomeMaterial" label="配件" width="90" align="right" />
        <el-table-column prop="incomeProduct" label="商品" width="90" align="right" />
        <el-table-column prop="incomeShipping" label="运费" width="90" align="right" />
        <el-table-column prop="totalPaid" label="实收" width="100" align="right" />
        <el-table-column prop="totalRefunded" label="退款" width="100" align="right" />
        <el-table-column prop="netAmount" label="净额" width="100" align="right" />
        <el-table-column prop="platformCommission" label="佣金" width="90" align="right" />
        <el-table-column prop="technicianIncome" label="师傅应结" width="100" align="right" />
        <el-table-column prop="taxAmount" label="税额" width="90" align="right" />
        <el-table-column prop="discountBearer" label="优惠承担" width="100" align="center" />
      </el-table>
    </el-card>

    <el-dialog v-model="rebuildVisible" title="重算订单财务快照" width="420px">
      <el-form label-width="80px">
        <el-form-item label="订单类型">
          <el-select v-model="rebuild.orderType" class="w-full">
            <el-option :value="1" label="维修订单" />
            <el-option :value="2" label="商品订单" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID">
          <el-input v-model="rebuild.orderId" placeholder="请输入订单ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rebuildVisible = false">取消</el-button>
        <el-button type="primary" :loading="rebuilding" @click="doRebuild">确认重算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchFinanceSnapshots,
  fetchFinanceReport,
  rebuildFinanceSnapshot
} from '../../api/adminFinance';

const loading = ref(false);
const snapshots = ref([]);
const report = reactive({ timeBasis: 'PAY_TIME', range: null });
const reportRow = ref(null);
const rebuildVisible = ref(false);
const rebuilding = ref(false);
const rebuild = reactive({ orderType: 1, orderId: '' });

async function loadSnapshots() {
  loading.value = true;
  try {
    const res = await fetchFinanceSnapshots({ limit: 100 });
    if (res.code === 200) snapshots.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadReport() {
  try {
    const params = { timeBasis: report.timeBasis };
    if (report.range && report.range.length === 2) {
      params.from = report.range[0];
      params.to = report.range[1];
    }
    const res = await fetchFinanceReport(params);
    if (res.code === 200 && res.data) reportRow.value = res.data.row;
  } catch (e) {
    ElMessage.error(e.message || '生成失败');
  }
}

function openRebuild() {
  rebuild.orderType = 1;
  rebuild.orderId = '';
  rebuildVisible.value = true;
}

async function doRebuild() {
  if (!rebuild.orderId) {
    ElMessage.warning('请输入订单ID');
    return;
  }
  rebuilding.value = true;
  try {
    await rebuildFinanceSnapshot({ orderType: rebuild.orderType, orderId: rebuild.orderId });
    ElMessage.success('已重算');
    rebuildVisible.value = false;
    loadSnapshots();
  } catch (e) {
    ElMessage.error(e.message || '重算失败');
  } finally {
    rebuilding.value = false;
  }
}

onMounted(() => {
  loadSnapshots();
  loadReport();
});
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  flex-wrap: wrap;
  align-items: center;
}
.stats-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
}
.stat-card {
  flex: 0 0 auto;
  min-width: 130px;
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}
.stat-count {
  font-size: 22px;
  font-weight: 600;
  color: #1677ff;
}
.w180 {
  width: 180px;
}
.date-range {
  width: 360px;
}
.w-full {
  width: 100%;
}
</style>
