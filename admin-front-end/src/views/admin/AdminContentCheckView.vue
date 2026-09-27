<template>
  <div class="content-check-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">内容审核管理</div>
          <div class="page-subtitle">审核日志查看、存疑内容复审、图片/视频审核队列管理与改判。</div>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- ==================== 审核日志 Tab ==================== -->
        <el-tab-pane label="审核日志" name="logs">
          <div class="filter-panel">
            <el-input
              v-model="logFilter.accountId"
              clearable
              placeholder="账号ID"
              style="width: 200px"
            />
            <el-select
              v-model="logFilter.checkResult"
              clearable
              placeholder="审核结果"
              style="width: 140px"
            >
              <el-option :value="1" label="通过" />
              <el-option :value="2" label="拦截" />
              <el-option :value="4" label="存疑" />
            </el-select>
            <el-date-picker
              v-model="logFilter.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="x"
              style="width: 360px"
            />
            <el-button type="primary" @click="loadLogs(1)">查询</el-button>
            <el-button @click="resetLogFilter">重置</el-button>
          </div>

          <el-table v-loading="logLoading" :data="logList" border>
            <el-table-column prop="id" label="日志ID" width="180" show-overflow-tooltip />
            <el-table-column prop="accountId" label="账号ID" width="160" show-overflow-tooltip />
            <el-table-column label="账号类型" width="100" align="center">
              <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
            </el-table-column>
            <el-table-column label="内容类型" width="90" align="center">
              <template #default="{ row }">{{ row.contentType === 1 ? '文字' : '图片' }}</template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="checkResultTag(row.checkResult)" size="small">{{
                  checkResultText(row.checkResult)
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="hitKeyword" label="命中标签" width="120" show-overflow-tooltip />
            <el-table-column label="来源" width="80" align="center">
              <template #default="{ row }">{{
                row.source === 1 ? '自建' : row.source === 2 ? '阿里云' : '-'
              }}</template>
            </el-table-column>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="logTotal"
              :page-sizes="[10, 20, 50]"
              :page-size="logPageSize"
              :current-page="logPageNum"
              @size-change="
                (s) => {
                  logPageSize = s;
                  loadLogs(1);
                }
              "
              @current-change="(p) => loadLogs(p)"
            />
          </div>
        </el-tab-pane>

        <!-- ==================== 图片审核队列 Tab ==================== -->
        <el-tab-pane label="图片审核队列" name="images">
          <div class="filter-panel">
            <el-select
              v-model="imageFilter.status"
              clearable
              placeholder="审核状态"
              style="width: 140px"
            >
              <el-option :value="1" label="待审核" />
              <el-option :value="2" label="已通过" />
              <el-option :value="3" label="已拒绝" />
              <el-option :value="4" label="存疑" />
            </el-select>
            <el-select
              v-model="imageFilter.businessType"
              clearable
              placeholder="业务类型"
              style="width: 160px"
            >
              <el-option value="AVATAR" label="头像" />
              <el-option value="STORE" label="门店" />
              <el-option value="GOODS" label="商品" />
              <el-option value="CASE" label="案例" />
              <el-option value="VIDEO" label="视频" />
              <el-option value="FAULT" label="故障" />
            </el-select>
            <el-button type="primary" @click="loadImages(1)">查询</el-button>
            <el-button @click="resetImageFilter">重置</el-button>
          </div>

          <el-table v-loading="imageLoading" :data="imageList" border>
            <el-table-column prop="id" label="队列ID" width="180" show-overflow-tooltip />
            <el-table-column label="预览" width="100" align="center">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  fit="cover"
                  :preview-src-list="[row.imageUrl]"
                  preview-teleported
                  style="width: 56px; height: 56px; border-radius: 8px"
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="businessType" label="业务类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.businessType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="imageStatusTag(row.status)" size="small">{{
                  imageStatusText(row.status)
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="rejectReason"
              label="原因/备注"
              min-width="160"
              show-overflow-tooltip
            />
            <el-table-column label="审核时间" width="170">
              <template #default="{ row }">{{ formatTime(row.reviewTime) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button
                    v-if="row.status === 1 || row.status === 4"
                    link
                    type="success"
                    @click="handleImageOverride(row, 2)"
                    >通过</el-button
                  >
                  <el-button
                    v-if="row.status === 1 || row.status === 4"
                    link
                    type="danger"
                    @click="handleImageOverride(row, 3)"
                    >拒绝</el-button
                  >
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="imageTotal"
              :page-sizes="[10, 20, 50]"
              :page-size="imagePageSize"
              :current-page="imagePageNum"
              @size-change="
                (s) => {
                  imagePageSize = s;
                  loadImages(1);
                }
              "
              @current-change="(p) => loadImages(p)"
            />
          </div>
        </el-tab-pane>

        <!-- ==================== 存疑复审 Tab ==================== -->
        <el-tab-pane label="存疑复审" name="reviews">
          <div class="filter-panel">
            <el-select
              v-model="reviewFilter.contentType"
              clearable
              placeholder="内容类型"
              style="width: 140px"
            >
              <el-option :value="1" label="文字" />
              <el-option :value="2" label="图片" />
            </el-select>
            <el-button type="primary" @click="loadReviews(1)">查询</el-button>
            <el-button @click="resetReviewFilter">重置</el-button>
          </div>

          <el-table v-loading="reviewLoading" :data="reviewList" border>
            <el-table-column prop="id" label="记录ID" width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.reviewType === 'text' ? '' : 'warning'">{{
                  row.reviewType === 'text'
                    ? '文字'
                    : row.reviewType === 'image'
                      ? '图片'
                      : row.reviewType
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="accountId" label="账号ID" width="160" show-overflow-tooltip />
            <el-table-column label="账号类型" width="100" align="center">
              <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="label" label="命中标签" width="120" show-overflow-tooltip />
            <el-table-column prop="labelDesc" label="标签描述" width="140" show-overflow-tooltip />
            <el-table-column label="预览" width="100" align="center">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  fit="cover"
                  :preview-src-list="[row.imageUrl]"
                  preview-teleported
                  style="width: 56px; height: 56px; border-radius: 8px"
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button link type="success" @click="handleTextOverride(row, 1)"
                    >放行</el-button
                  >
                  <el-button link type="danger" @click="handleTextOverride(row, 2)">拦截</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="reviewTotal"
              :page-sizes="[10, 20, 50]"
              :page-size="reviewPageSize"
              :current-page="reviewPageNum"
              @size-change="
                (s) => {
                  reviewPageSize = s;
                  loadReviews(1);
                }
              "
              @current-change="(p) => loadReviews(p)"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  fetchContentCheckLogs,
  fetchContentCheckReviews,
  overrideTextReview,
  fetchImageReviewQueue,
  overrideImageReview
} from '../../api/adminContentCheck';

const activeTab = ref('logs');

// ==================== Logs ====================
const logLoading = ref(false);
const logList = ref([]);
const logTotal = ref(0);
const logPageNum = ref(1);
const logPageSize = ref(20);
const logFilter = reactive({ accountId: '', checkResult: null, dateRange: null });

function buildLogParams(page) {
  const p = { page, size: logPageSize.value };
  if (logFilter.accountId) p.accountId = logFilter.accountId;
  if (logFilter.checkResult != null) p.checkResult = logFilter.checkResult;
  if (logFilter.dateRange && logFilter.dateRange.length === 2) {
    p.startTime = logFilter.dateRange[0];
    p.endTime = logFilter.dateRange[1];
  }
  return p;
}

async function loadLogs(page) {
  logLoading.value = true;
  logPageNum.value = page || 1;
  try {
    const res = await fetchContentCheckLogs(buildLogParams(logPageNum.value));
    if (res.code === 200 && res.data) {
      logList.value = res.data.records || [];
      logTotal.value = res.data.total || 0;
    }
  } catch (e) {
    ElMessage.error('加载审核日志失败');
  } finally {
    logLoading.value = false;
  }
}

function resetLogFilter() {
  logFilter.accountId = '';
  logFilter.checkResult = null;
  logFilter.dateRange = null;
  loadLogs(1);
}

// ==================== Images ====================
const imageLoading = ref(false);
const imageList = ref([]);
const imageTotal = ref(0);
const imagePageNum = ref(1);
const imagePageSize = ref(20);
const imageFilter = reactive({ status: null, businessType: '' });

function buildImageParams(page) {
  const p = { page, size: imagePageSize.value };
  if (imageFilter.status != null) p.status = imageFilter.status;
  if (imageFilter.businessType) p.businessType = imageFilter.businessType;
  return p;
}

async function loadImages(page) {
  imageLoading.value = true;
  imagePageNum.value = page || 1;
  try {
    const res = await fetchImageReviewQueue(buildImageParams(imagePageNum.value));
    if (res.code === 200 && res.data) {
      imageList.value = res.data.records || [];
      imageTotal.value = res.data.total || 0;
    }
  } catch (e) {
    ElMessage.error('加载图片队列失败');
  } finally {
    imageLoading.value = false;
  }
}

function resetImageFilter() {
  imageFilter.status = null;
  imageFilter.businessType = '';
  loadImages(1);
}

async function handleImageOverride(row, newStatus) {
  const statusText = newStatus === 2 ? '通过' : '拒绝';
  try {
    const { value: remark } = await ElMessageBox.prompt(
      `确认将图片审核改为「${statusText}」？${newStatus === 3 ? '请填写原因：' : '可选填备注：'}`,
      `改判 - ${statusText}`,
      { type: newStatus === 2 ? 'success' : 'warning', inputType: 'textarea' }
    );
    const res = await overrideImageReview(row.id, newStatus, remark || '');
    if (res.code === 200) {
      ElMessage.success(`已${statusText}`);
      loadImages(imagePageNum.value);
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch (e) {
    if (e !== 'cancel' && String(e) !== 'cancel') {
      ElMessage.error('操作失败');
    }
  }
}

// ==================== Reviews ====================
const reviewLoading = ref(false);
const reviewList = ref([]);
const reviewTotal = ref(0);
const reviewPageNum = ref(1);
const reviewPageSize = ref(20);
const reviewFilter = reactive({ contentType: null });

function buildReviewParams(page) {
  const p = { page, size: reviewPageSize.value };
  if (reviewFilter.contentType != null) p.contentType = reviewFilter.contentType;
  return p;
}

async function loadReviews(page) {
  reviewLoading.value = true;
  reviewPageNum.value = page || 1;
  try {
    const res = await fetchContentCheckReviews(buildReviewParams(reviewPageNum.value));
    if (res.code === 200 && res.data) {
      reviewList.value = res.data.records || [];
      reviewTotal.value = res.data.total || 0;
    }
  } catch (e) {
    ElMessage.error('加载复审列表失败');
  } finally {
    reviewLoading.value = false;
  }
}

function resetReviewFilter() {
  reviewFilter.contentType = null;
  loadReviews(1);
}

async function handleTextOverride(row, newStatus) {
  const statusText = newStatus === 1 ? '放行' : '拦截';
  try {
    const { value: remark } = await ElMessageBox.prompt(
      `确认将此文字审核改判为「${statusText}」？`,
      `改判 - ${statusText}`,
      { type: newStatus === 1 ? 'success' : 'warning', inputType: 'textarea' }
    );
    const res = await overrideTextReview(row.id, newStatus, remark || '');
    if (res.code === 200) {
      ElMessage.success(`已${statusText}`);
      loadReviews(reviewPageNum.value);
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch (e) {
    if (e !== 'cancel' && String(e) !== 'cancel') {
      ElMessage.error('操作失败');
    }
  }
}

// ==================== Tab switch ====================
function onTabChange(tab) {
  if (tab === 'logs' && logList.value.length === 0) loadLogs(1);
  else if (tab === 'images' && imageList.value.length === 0) loadImages(1);
  else if (tab === 'reviews' && reviewList.value.length === 0) loadReviews(1);
}

// ==================== Helpers ====================
function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  if (Number.isNaN(d.getTime())) return '-';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function accountTypeText(v) {
  if (v === 1) return '用户';
  if (v === 2) return '师傅';
  if (v === 3) return '管理员';
  return '-';
}

function checkResultText(v) {
  if (v === 1) return '通过';
  if (v === 2) return '拦截';
  if (v === 4) return '存疑';
  return '-';
}

function checkResultTag(v) {
  if (v === 1) return 'success';
  if (v === 2) return 'danger';
  if (v === 4) return 'warning';
  return 'info';
}

function imageStatusText(v) {
  if (v === 1) return '待审核';
  if (v === 2) return '已通过';
  if (v === 3) return '已拒绝';
  if (v === 4) return '存疑';
  return '-';
}

function imageStatusTag(v) {
  if (v === 1) return 'warning';
  if (v === 2) return 'success';
  if (v === 3) return 'danger';
  if (v === 4) return '';
  return 'info';
}

onMounted(() => {
  loadLogs(1);
});
</script>

<style scoped>
.content-check-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-card {
  border-radius: 14px;
}

.page-header {
  margin-bottom: 8px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.page-subtitle {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.filter-panel {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 4px;
}
</style>
