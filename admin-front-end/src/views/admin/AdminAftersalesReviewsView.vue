<template>
  <div class="reviews-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">评价管理</div>
          <div class="page-subtitle">统一查看服务评价与商品评价，支持按类型、状态和评分筛选。</div>
        </div>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            clearable
            class="search-input"
            placeholder="搜索订单号、评价内容、用户、师傅或商品"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="reviewType" clearable class="filter-select" placeholder="评价类型" @change="handleSearch">
            <el-option label="服务评价" :value="1" />
            <el-option label="商品评价" :value="2" />
          </el-select>
          <el-select v-model="status" clearable class="filter-select" placeholder="评价状态" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="已隐藏" :value="2" />
          </el-select>
          <el-select v-model="rating" clearable class="filter-select" placeholder="评分" @change="handleSearch">
            <el-option v-for="item in [5, 4, 3, 2, 1]" :key="item" :label="`${item} 星`" :value="item" />
          </el-select>
          <el-select v-model="hasReply" clearable class="filter-select" placeholder="回复状态" @change="handleSearch">
            <el-option label="已回复" :value="1" />
            <el-option label="未回复" :value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        class="review-table"
        header-cell-class-name="review-table-header"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-panel">
              <div class="expand-block">
                <div class="expand-title">评价内容</div>
                <div class="expand-content">{{ row.content || '用户未填写文字评价' }}</div>
              </div>

              <div v-if="row.images && row.images.length" class="expand-block">
                <div class="expand-title">评价图片</div>
                <div class="image-grid">
                  <el-image
                    v-for="item in row.images"
                    :key="item.id || item.url"
                    class="review-image"
                    :src="item.url"
                    :preview-src-list="row.images.map((image) => image.url).filter(Boolean)"
                    preview-teleported
                    fit="cover"
                  />
                </div>
              </div>

              <div v-if="row.replyContent" class="expand-block">
                <div class="expand-title">{{ row.orderType === 2 ? '商家回复' : '管理员回复' }}</div>
                <div class="expand-content">{{ row.replyContent }}</div>
                <div class="expand-time">{{ formatTime(row.replyTime) || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderType === 2 ? 'primary' : 'success'">
              {{ row.orderTypeText || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <span>{{ row.userName || '-' }}</span>
              <el-tag v-if="row.isAnonymous === 1" size="small" type="info">匿名</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价对象" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.orderType === 2">{{ row.productName || '-' }}</span>
            <span v-else>{{ row.technicianName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联信息" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.orderType === 2">{{ row.productId || '-' }}</span>
            <span v-else>{{ row.serviceTypeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="110" align="center">
          <template #default="{ row }">
            <span class="rating-text">{{ row.rating || 0 }} 星</span>
          </template>
        </el-table-column>
        <el-table-column label="回复" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.hasReply ? 'success' : 'warning'">
              {{ row.hasReply ? '已回复' : '未回复' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 2 ? 'info' : 'success'">
              {{ row.statusText || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="command => handleRowCommand(command, row)">
              <el-button size="small" class="action-trigger">
                操作
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!row.hasReply" command="reply">回复</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 1" command="hide">隐藏</el-dropdown-item>
                  <el-dropdown-item v-else command="show">恢复</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog v-model="replyDialogVisible" title="回复评价" width="520px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="评价对象">
          <div class="reply-readonly">{{ currentReviewTarget }}</div>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span>
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="replySubmitting" @click="submitReply">确认回复</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ArrowDown } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { fetchAdminReviews, replyAdminReview, updateAdminReviewStatus } from '../../api/adminAftersales';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const reviewType = ref(undefined);
const status = ref(undefined);
const rating = ref(undefined);
const hasReply = ref(undefined);
const replyDialogVisible = ref(false);
const replySubmitting = ref(false);
const currentReplyRow = ref(null);
const replyContent = ref('');

const currentReviewTarget = computed(() => {
  const row = currentReplyRow.value || {};
  if (Number(row.orderType) === 2) {
    return row.productName || row.productId || '-';
  }
  return row.technicianName || row.serviceTypeName || '-';
});

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
    if (typeof reviewType.value === 'number') {
      params.reviewType = reviewType.value;
    }
    if (typeof status.value === 'number') {
      params.status = status.value;
    }
    if (typeof rating.value === 'number') {
      params.rating = rating.value;
    }
    if (typeof hasReply.value === 'number') {
      params.hasReply = hasReply.value;
    }
    const res = await fetchAdminReviews(params);
    if (res && res.code === 200 && res.data) {
      const page = res.data;
      list.value = page.records || [];
      total.value = page.total || 0;
      pageNum.value = page.current || pageNum.value;
      pageSize.value = page.size || pageSize.value;
      return;
    }
    throw new Error((res && res.message) || '获取评价列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error.message || '获取评价列表失败');
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

async function changeStatus(row, nextStatus) {
  if (!row || !row.id) return;
  try {
    const res = await updateAdminReviewStatus(row.id, { status: nextStatus });
    if (res && res.code === 200) {
      ElMessage.success(nextStatus === 2 ? '评价已隐藏' : '评价已恢复');
      loadList();
      return;
    }
    throw new Error((res && res.message) || '更新评价状态失败');
  } catch (error) {
    ElMessage.error(error.message || '更新评价状态失败');
  }
}

function handleRowCommand(command, row) {
  if (!row || !row.id) return;
  if (command === 'reply' && !row.hasReply) {
    openReplyDialog(row);
    return;
  }
  if (command === 'hide' && row.status === 1) {
    changeStatus(row, 2);
    return;
  }
  if (command === 'show' && row.status !== 1) {
    changeStatus(row, 1);
  }
}

function openReplyDialog(row) {
  if (!row || !row.id || row.hasReply) return;
  currentReplyRow.value = row;
  replyContent.value = '';
  replyDialogVisible.value = true;
}

async function submitReply() {
  const row = currentReplyRow.value;
  const content = replyContent.value.trim();
  if (!row || !row.id) return;
  if (!content) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  replySubmitting.value = true;
  try {
    const res = await replyAdminReview(row.id, { replyContent: content });
    if (res && res.code === 200) {
      ElMessage.success('回复成功');
      replyDialogVisible.value = false;
      currentReplyRow.value = null;
      replyContent.value = '';
      loadList();
      return;
    }
    throw new Error((res && res.message) || '回复评价失败');
  } catch (error) {
    ElMessage.error(error.message || '回复评价失败');
  } finally {
    replySubmitting.value = false;
  }
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.reviews-page {
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.search-input {
  width: 300px;
}

.filter-select {
  width: 130px;
}

.review-table {
  width: 100%;
}

.review-table-header {
  background-color: #f5f7fa;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-text {
  color: #d97706;
  font-weight: 600;
}

.expand-panel {
  padding: 8px 16px;
  background-color: #fafcff;
}

.expand-block + .expand-block {
  margin-top: 16px;
}

.expand-title {
  margin-bottom: 8px;
  color: #303133;
  font-weight: 600;
}

.expand-content {
  color: #475467;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.expand-time {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.review-image {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.reply-readonly {
  color: #606266;
  line-height: 1.6;
}

.action-trigger {
  min-width: 76px;
}
</style>
