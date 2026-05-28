<template>
  <div class="store-page">
    <el-card class="store-card" shadow="never">
      <div class="store-header">
        <div class="store-title-group">
          <div class="store-title">门店列表</div>
          <div class="store-subtitle">创建和管理门店及门店管理员</div>
        </div>
        <div class="store-toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索门店名称"
            clearable
            class="store-search-input"
            @keyup.enter="fetchList"
            @clear="fetchList"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterAuditStatus" placeholder="审核状态" clearable style="width: 130px; margin-left: 8px" @change="fetchList">
            <el-option label="待审核" :value="1" />
            <el-option label="审核通过" :value="2" />
            <el-option label="审核拒绝" :value="3" />
          </el-select>
          <el-select v-model="filterBusinessStatus" placeholder="营业状态" clearable style="width: 130px; margin-left: 8px" @change="fetchList">
            <el-option label="营业中" :value="1" />
            <el-option label="休息中" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
          <el-button type="primary" style="margin-left: 8px" @click="fetchList">查询</el-button>
          <el-button v-if="isSuperAdmin" type="primary" style="margin-left: 8px" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon> 创建门店
          </el-button>
        </div>
      </div>

      <el-table :data="storeList" v-loading="loading" border class="store-table" header-cell-class-name="store-table-header">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="Logo" width="80" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.logoImageUrl" :src="row.logoImageUrl" size="small" />
            <el-avatar v-else size="small">{{ row.name?.charAt(0) || '门' }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="门店名称" min-width="160" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="address" label="门店地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="师傅数" width="80" align="center">
          <template #default="{ row }">{{ row.technicianCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="营业状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="businessStatusTag(row.businessStatus)" size="small">{{ businessStatusText(row.businessStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusTag(row.auditStatus)" size="small">{{ auditStatusText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="80" align="center">
          <template #default="{ row }">{{ row.rating != null ? row.rating.toFixed(1) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="isSuperAdmin && row.auditStatus === 1" size="small" type="success" link @click="auditStore(row, 2)">通过</el-button>
            <el-button v-if="isSuperAdmin && row.auditStatus === 1" size="small" type="danger" link @click="auditStore(row, 3)">拒绝</el-button>
            <el-button v-if="canToggleStatus()" size="small" link @click="toggleStatus(row)">
              {{ row.businessStatus === 1 ? '休息' : row.businessStatus === 2 ? '营业' : '开启' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="store-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="pageNum"
          @size-change="(s) => { pageSize = s; fetchList(); }"
          @current-change="(p) => { pageNum = p; fetchList(); }"
        />
      </div>
    </el-card>

    <!-- 创建门店对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建门店" width="500px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-divider content-position="left">门店信息</el-divider>
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="createForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="门店介绍">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="请输入门店介绍（选填）" />
        </el-form-item>

        <el-divider content-position="left">门店管理员</el-divider>
        <el-form-item label="管理员姓名" prop="adminName">
          <el-input v-model="createForm.adminName" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="管理员手机" prop="adminPhone">
          <el-input v-model="createForm.adminPhone" placeholder="请输入管理员手机号" />
        </el-form-item>
        <el-form-item label="管理员邮箱" prop="adminEmail">
          <el-input v-model="createForm.adminEmail" placeholder="请输入管理员登录邮箱" />
        </el-form-item>
        <el-form-item label="登录密码" prop="adminPassword">
          <el-input v-model="createForm.adminPassword" type="password" placeholder="请输入管理员登录密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import { useAdminStore } from '../../stores/admin';
import request from '../../api/request';

const router = useRouter();
const adminStore = useAdminStore();
const isSuperAdmin = computed(() => adminStore.adminRole === 1);
const isStoreAdmin = computed(() => adminStore.adminRole === 2);

const loading = ref(false);
const storeList = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref('');
const filterAuditStatus = ref(null);
const filterBusinessStatus = ref(null);

const showCreateDialog = ref(false);
const creating = ref(false);
const createFormRef = ref();
const createForm = reactive({
  name: '', contactPhone: '', description: '',
  latitude: null, longitude: null, address: '',
  adminName: '', adminPhone: '', adminEmail: '', adminPassword: ''
});
const createRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  adminName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminPhone: [{ required: true, message: '请输入管理员手机号', trigger: 'blur' }],
  adminEmail: [{ required: true, message: '请输入管理员邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员登录密码', trigger: 'blur' }]
};

onMounted(() => { fetchList(); });

async function fetchList() {
  loading.value = true;
  try {
    const res = await request({
      url: '/admin/stores', method: 'get',
      params: {
        page: pageNum.value, size: pageSize.value,
        keyword: searchKeyword.value || undefined,
        auditStatus: filterAuditStatus.value,
        businessStatus: filterBusinessStatus.value
      }
    });
    if (res.code === 200 && res.data) {
      storeList.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } finally { loading.value = false; }
}

function goDetail(id) {
  router.push(`/admin/stores/list/${id}`);
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) return;
  creating.value = true;
  try {
    const res = await request({ url: '/admin/stores/create', method: 'post', data: createForm });
    if (res.code === 200) {
      ElMessage.success('门店创建成功');
      showCreateDialog.value = false;
      fetchList();
    }
  } finally { creating.value = false; }
}

async function auditStore(row, status) {
  if (status === 3) {
    try {
      const { value: remark } = await ElMessageBox.prompt('请输入拒绝理由', '审核拒绝', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputType: 'textarea'
      });
      await request({ url: `/admin/stores/${row.id}/audit`, method: 'post', data: { auditStatus: status, remark } });
      ElMessage.success('已拒绝');
      fetchList();
    } catch (e) { /* 取消 */ }
    return;
  }
  await request({ url: `/admin/stores/${row.id}/audit`, method: 'post', data: { auditStatus: status, remark: '' } });
  ElMessage.success('审核通过');
  fetchList();
}

async function toggleStatus(row) {
  const newStatus = row.businessStatus === 1 ? 2 : 1;
  const text = newStatus === 1 ? '营业中' : '休息中';
  try {
    await ElMessageBox.confirm(`确认切换为"${text}"吗？`, '提示', { type: 'warning' });
    await request({ url: `/admin/stores/${row.id}/status`, method: 'post', data: { businessStatus: newStatus } });
    ElMessage.success('已切换为' + text);
    fetchList();
  } catch (e) { /* ignore */ }
}

function canToggleStatus() { return isSuperAdmin.value || isStoreAdmin.value; }

function businessStatusTag(s) { return s === 1 ? 'success' : s === 2 ? 'warning' : 'info'; }
function businessStatusText(s) { return s === 1 ? '营业中' : s === 2 ? '休息中' : '已关闭'; }
function auditStatusTag(s) { return s === 1 ? 'warning' : s === 2 ? 'success' : 'danger'; }
function auditStatusText(s) { return s === 1 ? '待审核' : s === 2 ? '通过' : '拒绝'; }
</script>

<style scoped>
.store-page { padding: 16px; box-sizing: border-box; }
.store-card { width: 100%; }
.store-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 12px; }
.store-title-group { display: flex; flex-direction: column; }
.store-title { font-size: 18px; font-weight: 600; color: #303133; }
.store-subtitle { margin-top: 4px; font-size: 13px; color: #909399; }
.store-toolbar { display: flex; align-items: center; flex-wrap: wrap; }
.store-search-input { width: 220px; }
.store-table { width: 100%; }
.store-table-header { background-color: #f5f7fa; }
.store-pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
