<template>
  <div class="report-page">
    <el-card class="report-card" shadow="never">
      <div class="report-header">
        <div class="report-title-group">
          <div class="report-title">举报管理</div>
          <div class="report-subtitle">审核用户、师傅和门店管理员的举报内容</div>
        </div>
        <div class="report-toolbar">
          <el-select
            v-model="filterStatus"
            placeholder="状态"
            clearable
            style="width: 130px"
            @change="fetchList"
          >
            <el-option label="待处理" :value="1" />
            <el-option label="处理中" :value="2" />
            <el-option label="已成立" :value="3" />
            <el-option label="已驳回" :value="4" />
          </el-select>
          <el-select
            v-model="filterTargetType"
            placeholder="对象类型"
            clearable
            style="width: 120px; margin-left: 8px"
            @change="fetchList"
          >
            <el-option label="账号" :value="1" />
            <el-option label="门店" :value="2" />
            <el-option label="订单" :value="3" />
            <el-option label="商品" :value="4" />
          </el-select>
          <el-button type="primary" style="margin-left: 8px" @click="fetchList">查询</el-button>
        </div>
      </div>

      <el-table
        :data="reportList"
        v-loading="loading"
        border
        class="report-table"
        header-cell-class-name="report-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="举报人" width="140">
          <template #default="{ row }"
            >{{ row.reporterId }}<br /><el-tag size="small" type="info">{{
              reporterTypeText(row.reporterType)
            }}</el-tag></template
          >
        </el-table-column>
        <el-table-column label="对象类型" width="80" align="center">
          <template #default="{ row }">{{ targetTypeText(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="对象ID" width="120" show-overflow-tooltip />
        <el-table-column prop="targetField" label="字段/节点" width="120" show-overflow-tooltip />
        <el-table-column prop="reasonCategory" label="原因分类" width="100" />
        <el-table-column
          prop="description"
          label="补充说明"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1 || row.status === 2"
              size="small"
              type="success"
              link
              @click="handleProcess(row, 3)"
              >成立</el-button
            >
            <el-button
              v-if="row.status === 1 || row.status === 2"
              size="small"
              type="danger"
              link
              @click="handleProcess(row, 4)"
              >驳回</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="report-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="pageNum"
          @size-change="
            (s) => {
              pageSize = s;
              fetchList();
            }
          "
          @current-change="
            (p) => {
              pageNum = p;
              fetchList();
            }
          "
        />
      </div>
    </el-card>

    <!-- 举报详情弹窗 -->
    <el-dialog v-model="detailVisible" title="举报详情" width="560px" destroy-on-close>
      <el-descriptions v-if="currentReport" :column="2" border>
        <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(currentReport.status)" size="small">{{
            statusText(currentReport.status)
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报人ID">{{ currentReport.reporterId }}</el-descriptions-item>
        <el-descriptions-item label="举报人类型">{{
          reporterTypeText(currentReport.reporterType)
        }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">{{
          targetTypeText(currentReport.targetType)
        }}</el-descriptions-item>
        <el-descriptions-item label="对象ID">{{ currentReport.targetId }}</el-descriptions-item>
        <el-descriptions-item label="字段/节点" :span="2">{{
          currentReport.targetField || '—'
        }}</el-descriptions-item>
        <el-descriptions-item label="原因分类">{{
          currentReport.reasonCategory
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatTime(currentReport.createdTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="补充说明" :span="2">{{
          currentReport.description || '—'
        }}</el-descriptions-item>
        <el-descriptions-item v-if="currentReport.evidenceImages" label="截图" :span="2">
          <div
            v-for="(url, i) in parseImages(currentReport.evidenceImages)"
            :key="i"
            style="display: inline-block; margin: 4px"
          >
            <el-image
              :src="url"
              style="width: 80px; height: 80px"
              fit="cover"
              :preview-src-list="[url]"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.result" label="处理结果" :span="2">{{
          currentReport.result
        }}</el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handlerId" label="处理人">{{
          currentReport.handlerId
        }}</el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handleTime" label="处理时间">{{
          formatTime(currentReport.handleTime)
        }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理举报弹窗 -->
    <el-dialog
      v-model="processVisible"
      :title="processStatus === 3 ? '举报成立 — 执行处罚' : '驳回举报'"
      width="480px"
      destroy-on-close
    >
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="处理结果" prop="result">
          <el-input
            v-model="processForm.result"
            type="textarea"
            :rows="2"
            :placeholder="processStatus === 3 ? '请填写处理结果说明' : '请填写驳回原因'"
          />
        </el-form-item>
        <template v-if="processStatus === 3">
          <el-divider content-position="left">处罚设置</el-divider>
          <el-form-item label="违规等级" prop="violationLevel">
            <el-select
              v-model="processForm.violationLevel"
              placeholder="选择违规等级"
              style="width: 100%"
            >
              <el-option label="一级（轻微）- 扣5分" :value="1" />
              <el-option label="二级（一般）- 扣10分" :value="2" />
              <el-option label="三级（严重）- 扣20分" :value="3" />
              <el-option label="四级（重大）- 扣40分" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="处罚类型" prop="penaltyType">
            <el-select
              v-model="processForm.penaltyType"
              placeholder="选择处罚类型"
              style="width: 100%"
            >
              <el-option label="警告" :value="1" />
              <el-option label="功能限制" :value="2" />
              <el-option label="封禁" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="processForm.penaltyType === 3" label="封禁时长(小时)">
            <el-input-number
              v-model="processForm.banDurationHours"
              :min="1"
              :max="8760"
              placeholder="默认168小时(7天)"
              style="width: 100%"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="processing" @click="confirmProcess">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchReports, fetchReportDetail, processReport } from '../../api/adminReport';

const loading = ref(false);
const processing = ref(false);
const reportList = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterStatus = ref(null);
const filterTargetType = ref(null);

const detailVisible = ref(false);
const currentReport = ref(null);

const processVisible = ref(false);
const processStatus = ref(3);
const processForm = ref({
  result: '',
  violationLevel: null,
  penaltyType: null,
  banDurationHours: 168
});

onMounted(() => {
  fetchList();
});

async function fetchList() {
  loading.value = true;
  try {
    const res = await fetchReports({
      page: pageNum.value,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      targetType: filterTargetType.value || undefined
    });
    if (res.code === 200 && res.data) {
      reportList.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } finally {
    loading.value = false;
  }
}

async function showDetail(row) {
  const res = await fetchReportDetail(row.id);
  if (res.code === 200) {
    currentReport.value = res.data;
    detailVisible.value = true;
  }
}

function handleProcess(row, status) {
  processForm.value = {
    result: '',
    violationLevel: null,
    penaltyType: null,
    banDurationHours: 168
  };
  processStatus.value = status;
  currentReport.value = row;
  processVisible.value = true;
}

async function confirmProcess() {
  processing.value = true;
  try {
    const data = {
      status: processStatus.value,
      result: processForm.value.result
    };
    if (processStatus.value === 3) {
      data.violationLevel = processForm.value.violationLevel;
      data.penaltyType = processForm.value.penaltyType;
      data.banDurationHours = processForm.value.banDurationHours;
    }
    const res = await processReport(currentReport.value.id, data);
    if (res.code === 200) {
      ElMessage.success(processStatus.value === 3 ? '举报已成立，处罚已执行' : '举报已驳回');
      processVisible.value = false;
      fetchList();
    }
  } finally {
    processing.value = false;
  }
}

function parseImages(json) {
  try {
    return JSON.parse(json);
  } catch {
    return [];
  }
}

function formatTime(ts) {
  if (!ts) return '—';
  return new Date(ts).toLocaleString('zh-CN');
}

function statusTag(s) {
  return s === 1 ? 'warning' : s === 2 ? '' : s === 3 ? 'success' : 'danger';
}
function statusText(s) {
  return s === 1 ? '待处理' : s === 2 ? '处理中' : s === 3 ? '已成立' : s === 4 ? '已驳回' : '未知';
}
function reporterTypeText(t) {
  return t === 1 ? '用户' : t === 2 ? '师傅' : t === 3 ? '门店管理员' : '未知';
}
function targetTypeText(t) {
  return t === 1 ? '账号' : t === 2 ? '门店' : t === 3 ? '订单' : t === 4 ? '商品' : '未知';
}
</script>

<style scoped>
.report-page {
  padding: 16px;
  box-sizing: border-box;
}
.report-card {
  width: 100%;
}
.report-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 12px;
}
.report-title-group {
  display: flex;
  flex-direction: column;
}
.report-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.report-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
.report-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.report-table {
  width: 100%;
}
.report-table-header {
  background-color: #f5f7fa;
}
.report-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
