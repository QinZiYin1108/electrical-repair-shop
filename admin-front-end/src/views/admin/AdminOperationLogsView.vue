<template>
  <div class="logs-page">
    <el-card class="logs-card" shadow="never">
      <div class="logs-header">
        <div class="logs-title-group">
          <div class="logs-title">操作日志</div>
          <div class="logs-subtitle">查看后台所有操作记录，支持多条件查询</div>
        </div>
      </div>

      <el-form
        :inline="true"
        :model="query"
        class="logs-query-form"
        label-width="83px"
      >
        <el-form-item label="模块名称">
          <el-select
            v-model="query.moduleName"
            clearable
            placeholder="请选择模块"
            class="logs-query-item"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="用户管理" value="ADMIN_USER" />
            <el-option label="师傅管理" value="ADMIN_WORKER" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="query.operationType"
            clearable
            placeholder="请选择操作类型"
            class="logs-query-item"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="新增" value="CREATE" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="READ" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人类型">
          <el-select
            v-model="query.operatorType"
            clearable
            placeholder="请选择操作人类型"
            class="logs-query-item"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="管理员" :value="3" />
            <el-option label="用户" :value="1" />
            <el-option label="师傅" :value="2" />
            <el-option label="系统" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input
            v-model="query.operatorName"
            placeholder="请输入操作人姓名"
            clearable
            class="logs-query-item"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            clearable
            placeholder="请选择状态"
            class="logs-query-item"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            value-format="x"
            class="logs-query-range"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="list"
        border
        header-cell-class-name="logs-table-header"
        class="logs-table"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="operatorName" label="操作人" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作人类型" width="110" align="center">
          <template #default="{ row }">
            {{ getOperatorTypeText(row.operatorType) }}
          </template>
        </el-table-column>
        <el-table-column prop="operationType" label="操作类型" width="100" align="center" />
        <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="moduleName" label="模块名称" width="130" align="center" />
        <el-table-column prop="requestMethod" label="方法" width="80" align="center" />
        <el-table-column prop="requestUrl" label="请求URL" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="时间" min-width="160">
          <template #default="{ row }">
            <span v-if="row.createdTime">{{ formatTime(row.createdTime) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="logs-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="page"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" width="800px">
      <template #title>
        操作日志详情
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="日志ID">
          {{ detail.id || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人ID">
          {{ detail.operatorId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人姓名">
          {{ detail.operatorName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人类型">
          {{ getOperatorTypeText(detail.operatorType) }}
        </el-descriptions-item>
        <el-descriptions-item label="模块名称">
          {{ detail.moduleName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          {{ detail.operationType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作描述">
          {{ detail.operationDesc || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getStatusText(detail.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">
          {{ detail.requestMethod || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="请求URL">
          {{ detail.requestUrl || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ detail.ipAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="设备ID">
          {{ detail.deviceId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="执行时间">
          <span v-if="detail.executionTime != null">{{ detail.executionTime }} ms</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="时间">
          <span v-if="detail.createdTime">{{ formatTime(detail.createdTime) }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border class="logs-detail-block">
        <el-descriptions-item label="请求参数">
          <pre class="logs-json" v-if="detail.requestParams">{{ detail.requestParams }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border class="logs-detail-block">
        <el-descriptions-item label="响应数据">
          <pre class="logs-json" v-if="detail.responseData">{{ detail.responseData }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border class="logs-detail-block">
        <el-descriptions-item label="错误信息">
          <pre class="logs-json" v-if="detail.errorMessage">{{ detail.errorMessage }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border class="logs-detail-block">
        <el-descriptions-item label="User-Agent">
          <pre class="logs-json" v-if="detail.userAgent">{{ detail.userAgent }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchOperationLogList, fetchOperationLogDetail } from '../../api/adminOperationLogs';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const query = reactive({
  moduleName: undefined,
  operationType: undefined,
  operatorType: undefined,
  operatorName: '',
  status: undefined,
  timeRange: []
});

const loading = ref(false);
const list = ref([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const detailVisible = ref(false);
const detail = reactive({
  id: '',
  operatorId: '',
  operatorType: null,
  operatorName: '',
  operationType: '',
  operationDesc: '',
  moduleName: '',
  requestMethod: '',
  requestUrl: '',
  requestParams: '',
  responseData: '',
  ipAddress: '',
  userAgent: '',
  deviceId: '',
  executionTime: null,
  status: null,
  errorMessage: '',
  createdTime: null
});

function getOperatorTypeText(type) {
  if (type === 1) {
    return '用户';
  }
  if (type === 2) {
    return '师傅';
  }
  if (type === 3) {
    return '管理员';
  }
  if (type === 4) {
    return '系统';
  }
  return '未知';
}

function getStatusText(status) {
  if (status === 1) {
    return '成功';
  }
  if (status === 2) {
    return '失败';
  }
  return '未知';
}

function getStatusTagType(status) {
  if (status === 1) {
    return 'success';
  }
  if (status === 2) {
    return 'danger';
  }
  return 'info';
}

function formatTime(value) {
  if (!value) {
    return '';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '';
  }
  const date = new Date(num);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize.value
    };
    if (query.moduleName) {
      params.moduleName = query.moduleName;
    }
    if (query.operationType) {
      params.operationType = query.operationType;
    }
    if (query.operatorType != null) {
      params.operatorType = query.operatorType;
    }
    if (query.operatorName) {
      params.operatorName = query.operatorName;
    }
    if (query.status != null) {
      params.status = query.status;
    }
    if (query.timeRange && query.timeRange.length === 2) {
      params.startTime = query.timeRange[0];
      params.endTime = query.timeRange[1];
    }
    const res = await fetchOperationLogList(params);
    if (res && res.code === 200 && res.data) {
      const data = res.data;
      const records = data.list || data.records || [];
      list.value = records;
      total.value = data.total || data.count || 0;
      if (data.pageNum) {
        page.value = data.pageNum;
      } else if (data.current) {
        page.value = data.current;
      }
      if (data.pageSize) {
        pageSize.value = data.pageSize;
      } else if (data.size) {
        pageSize.value = data.size;
      }
    } else {
      list.value = [];
      total.value = 0;
      if (res && res.message) {
        ElMessage.error(res.message);
      } else {
        ElMessage.error('获取操作日志列表失败');
      }
    }
  } catch (e) {
    list.value = [];
    total.value = 0;
    ElMessage.error('获取操作日志列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadList();
}

function handleReset() {
  query.moduleName = undefined;
  query.operationType = undefined;
  query.operatorType = undefined;
  query.operatorName = '';
  query.status = undefined;
  query.timeRange = [];
  page.value = 1;
  loadList();
}

function handlePageChange(p) {
  page.value = p;
  loadList();
}

function handleSizeChange(size) {
  pageSize.value = size;
  page.value = 1;
  loadList();
}

async function handleView(row) {
  if (!row || !row.id) {
    return;
  }
  try {
    const res = await fetchOperationLogDetail(row.id);
    if (res && res.code === 200 && res.data) {
      const data = res.data;
      detail.id = data.id || '';
      detail.operatorId = data.operatorId || '';
      detail.operatorType = data.operatorType ?? null;
      detail.operatorName = data.operatorName || '';
      detail.operationType = data.operationType || '';
      detail.operationDesc = data.operationDesc || '';
      detail.moduleName = data.moduleName || '';
      detail.requestMethod = data.requestMethod || '';
      detail.requestUrl = data.requestUrl || '';
      detail.requestParams = data.requestParams || '';
      detail.responseData = data.responseData || '';
      detail.ipAddress = data.ipAddress || '';
      detail.userAgent = data.userAgent || '';
      detail.deviceId = data.deviceId || '';
      detail.executionTime = data.executionTime ?? null;
      detail.status = data.status ?? null;
      detail.errorMessage = data.errorMessage || '';
      detail.createdTime = data.createdTime ?? null;
      detailVisible.value = true;
    } else if (res && res.message) {
      ElMessage.error(res.message);
    } else {
      ElMessage.error('获取操作日志详情失败');
    }
  } catch (e) {
    ElMessage.error('获取操作日志详情失败');
  }
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
  if (detailVisible.value && detail.id) {
    await handleView({ id: detail.id });
  }
});
</script>

<style scoped>
.logs-page {
  padding: 16px;
  box-sizing: border-box;
}

.logs-card {
  width: 100%;
}

.logs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.logs-title-group {
  display: flex;
  flex-direction: column;
}

.logs-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.logs-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.logs-query-form {
  margin-bottom: 8px;
}

.logs-query-item {
  width: 180px;
}

.logs-query-range {
  width: 320px;
}

.logs-table {
  width: 100%;
}

.logs-table-header {
  background-color: #f5f7fa;
}

.logs-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.logs-detail-block {
  margin-top: 12px;
}

.logs-json {
  margin: 0;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

