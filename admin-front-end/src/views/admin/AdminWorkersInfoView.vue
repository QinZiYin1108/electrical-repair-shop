<template>
  <div class="workers-page">
    <el-card class="workers-card" shadow="never">
      <div class="workers-header">
        <div class="workers-title-group">
          <div class="workers-title">师傅信息管理</div>
          <div class="workers-subtitle">查看师傅列表并进入详情页进行维护</div>
        </div>
        <div class="workers-toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入师傅姓名、手机号或邮箱"
            clearable
            class="workers-search-input"
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
        class="workers-table"
        header-cell-class-name="workers-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl">
              <span v-if="!row.avatarUrl">{{ getAvatarInitial(row) }}</span>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getAccountStatusTagType(row.accountStatus)" size="small">
              {{ getAccountStatusText(row.accountStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工作状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getWorkStatusTagType(row.workStatus)" size="small">
              {{ getWorkStatusText(row.workStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.rating != null">{{ Number(row.rating).toFixed(1) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="注册时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTime(row.createdTime) || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看详情</el-button>
            <el-button
              v-if="isSuperAdmin && !row.storeId"
              type="success"
              link
              @click="openAssignStore(row)"
              >分配门店</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="workers-pagination">
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

    <el-dialog v-model="showAssignDialog" title="分配门店" width="480px">
      <div style="display: flex; gap: 8px; margin-bottom: 8px">
        <el-input
          v-model="storeKeyword"
          placeholder="搜索门店名称"
          clearable
          @keyup.enter="searchStores"
        />
        <el-button type="primary" @click="searchStores" :loading="storeSearching">搜索</el-button>
      </div>
      <div style="max-height: 300px; overflow-y: auto">
        <div
          v-for="s in storeCandidates"
          :key="s.id"
          style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
          "
        >
          <div>
            <div>{{ s.name }}</div>
            <div style="font-size: 12px; color: #909399">{{ s.address || '-' }}</div>
          </div>
          <el-button size="small" type="primary" @click="handleAssignStore(s)">分配</el-button>
        </div>
        <el-empty
          v-if="storeSearched && !storeCandidates.length"
          description="未找到门店"
          :image-size="30"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { fetchAdminWorkerList } from '../../api/adminWorkers';
import { useAdminStore } from '../../stores/admin';
import request from '../../api/request';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const router = useRouter();
const adminStore = useAdminStore();
const isSuperAdmin = computed(() => adminStore.adminRole === 1);

// 门店分配
const showAssignDialog = ref(false);
const assigningWorker = ref(null);
const storeKeyword = ref('');
const storeCandidates = ref([]);
const storeSearched = ref(false);
const storeSearching = ref(false);

const searchKeyword = ref('');
const loading = ref(false);
const list = ref([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

function getAvatarInitial(row) {
  if (row && row.username) {
    return String(row.username).charAt(0).toUpperCase();
  }
  if (row && row.email) {
    return String(row.email).charAt(0).toUpperCase();
  }
  return 'W';
}

function getAccountStatusText(status) {
  if (status === 1) return '正常';
  if (status === 2) return '未认证';
  if (status === 3) return '冻结';
  if (status === 4) return '离职';
  return '未知';
}

function getAccountStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'warning';
  if (status === 3) return 'danger';
  return 'info';
}

function getWorkStatusText(status) {
  if (status === 1) return '在线';
  if (status === 2) return '忙碌';
  if (status === 3) return '休息';
  return '离线';
}

function getWorkStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'warning';
  if (status === 3) return 'info';
  return '';
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
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value.trim();
    }
    const res = await fetchAdminWorkerList(params);
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
    ElMessage.error((res && res.message) || '获取师傅列表失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    ElMessage.error('获取师傅列表失败');
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
  router.push(`/admin/workers/info/${id}`);
}

function openAssignStore(row) {
  assigningWorker.value = row;
  storeKeyword.value = '';
  storeCandidates.value = [];
  storeSearched.value = false;
  showAssignDialog.value = true;
}

async function searchStores() {
  storeSearched.value = true;
  storeSearching.value = true;
  try {
    const res = await request({
      url: '/admin/stores',
      method: 'get',
      params: { keyword: storeKeyword.value, pageSize: 30, pageNum: 1 }
    });
    if (res.code === 200 && res.data) {
      storeCandidates.value = res.data.list || res.data.records || [];
    } else {
      storeCandidates.value = [];
    }
  } catch (e) {
    storeCandidates.value = [];
  } finally {
    storeSearching.value = false;
  }
}

async function handleAssignStore(store) {
  if (!assigningWorker.value || !store) return;
  try {
    const res = await request({
      url: `/admin/workers/${assigningWorker.value.id}/bind-store`,
      method: 'post',
      data: { storeId: store.id }
    });
    if (res.code === 200) {
      ElMessage.success(`已将 ${assigningWorker.value.username} 分配到 ${store.name}`);
      showAssignDialog.value = false;
      assigningWorker.value.storeId = store.id;
      await loadList();
    } else {
      ElMessage.error(res.message || '分配失败');
    }
  } catch (e) {
    ElMessage.error('分配失败');
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
.workers-page {
  padding: 16px;
  box-sizing: border-box;
}

.workers-card {
  width: 100%;
}

.workers-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.workers-title-group {
  display: flex;
  flex-direction: column;
}

.workers-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.workers-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.workers-toolbar {
  display: flex;
  align-items: center;
}

.workers-search-input {
  width: 280px;
  margin-right: 8px;
}

.workers-table {
  width: 100%;
}

.workers-table-header {
  background-color: #f5f7fa;
}

.workers-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
