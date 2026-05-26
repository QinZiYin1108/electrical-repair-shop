<template>
  <div class="users-page">
    <el-card class="users-card" shadow="never">
      <div class="users-header">
        <div class="users-title-group">
          <div class="users-title">用户列表</div>
          <div class="users-subtitle">查看平台用户账号信息，并进入详情页继续维护</div>
        </div>
        <div class="users-toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入昵称、手机号或邮箱搜索"
            clearable
            class="users-search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        class="users-table"
        header-cell-class-name="users-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl">
              <span v-if="!row.avatarUrl">{{ getAvatarInitial(row) }}</span>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="昵称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="实名认证" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isVerified === 1 ? 'success' : 'info'" size="small">
              {{ row.isVerified === 1 ? '已认证' : '未认证' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="账户余额" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.balance != null">{{ formatBalance(row.balance) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="注册时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTime(row.createdTime) || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="users-pagination">
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { fetchAdminUserList } from '../../api/adminUsers';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const router = useRouter();
const searchKeyword = ref('');
const loading = ref(false);
const list = ref([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

function getAvatarInitial(row) {
  if (row && row.username) return String(row.username).charAt(0).toUpperCase();
  if (row && row.email) return String(row.email).charAt(0).toUpperCase();
  return 'U';
}

function getStatusText(status) {
  if (status === 1) return '正常';
  if (status === 2) return '冻结';
  if (status === 3) return '注销';
  return '未知';
}

function getStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'danger';
  if (status === 3) return 'info';
  return 'warning';
}

function formatBalance(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '-';
  return amount.toFixed(2);
}

function formatTime(value) {
  if (!value) return '';
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp)) return '';
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize.value
    };
    if (searchKeyword.value && searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim();
    }
    const res = await fetchAdminUserList(params);
    if (res && res.code === 200 && res.data) {
      const data = res.data;
      list.value = data.list || data.records || [];
      total.value = data.total || data.count || 0;
      page.value = data.pageNum || data.current || page.value;
      pageSize.value = data.pageSize || data.size || pageSize.value;
      return;
    }
    list.value = [];
    total.value = 0;
    ElMessage.error((res && res.message) || '获取用户列表失败');
  } catch {
    list.value = [];
    total.value = 0;
    ElMessage.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadList();
}

function handlePageChange(nextPage) {
  page.value = nextPage;
  loadList();
}

function handleSizeChange(size) {
  pageSize.value = size;
  page.value = 1;
  loadList();
}

function goDetail(id) {
  if (!id) return;
  router.push(`/admin/users/list/${id}`);
}

onMounted(() => {
  loadList();
});

useAdminPageRefresh(async () => {
  await loadList();
});
</script>

<style scoped>
.users-page {
  padding: 16px;
  box-sizing: border-box;
}

.users-card {
  width: 100%;
}

.users-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.users-title-group {
  display: flex;
  flex-direction: column;
}

.users-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.users-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.users-toolbar {
  display: flex;
  align-items: center;
}

.users-search-input {
  width: 300px;
  margin-right: 8px;
}

.users-table {
  width: 100%;
}

.users-table-header {
  background-color: #f5f7fa;
}

.users-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>