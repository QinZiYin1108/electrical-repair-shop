<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="header-left">
        <img class="header-logo" :src="brandLogoIcon" alt="安修到家" />
        <div class="header-text">
          <div class="header-title">安修到家管理后台</div>
          <div class="header-subtitle">欢迎回来，{{ displayName }}</div>
        </div>
      </div>
      <div class="header-right">
        <el-button class="header-refresh-btn" circle @click="handlePageRefresh">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
        <el-dropdown @command="handleUserCommand">
          <span class="header-user">
            <el-avatar v-if="avatarUrl" :src="avatarUrl" size="small" class="header-avatar" />
            <el-avatar v-else size="small" class="header-avatar">{{ displayInitial }}</el-avatar>
            <span class="header-username">{{ displayName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="admin-body">
      <aside class="admin-sider">
        <el-menu
          :key="activeRootMenu"
          :default-active="activeMenu"
          :default-openeds="[activeRootMenu]"
          class="admin-menu"
          router
          :collapse="false"
          unique-opened
        >
          <el-menu-item index="/admin/dashboard">
            <span>仪表盘</span>
          </el-menu-item>

          <el-sub-menu index="/admin/orders">
            <template #title><span>订单管理</span></template>
            <el-menu-item index="/admin/orders/reserve">预约订单列表</el-menu-item>
            <el-menu-item index="/admin/orders/product">商品订单列表</el-menu-item>
            <el-menu-item index="/admin/orders/offline">线下订单代录</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/products">
            <template #title><span>商品管理</span></template>
            <el-menu-item index="/admin/products/categories">商品分类管理</el-menu-item>
            <el-menu-item index="/admin/products/main">商品信息管理</el-menu-item>
            <el-menu-item index="/admin/products/second-hand">二手商品管理</el-menu-item>
            <el-menu-item index="/admin/products/warranty">保修卡管理</el-menu-item>
            <el-menu-item index="/admin/products/coupons">优惠券管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/workers">
            <template #title><span>师傅管理</span></template>
            <el-menu-item index="/admin/workers/info">师傅信息管理</el-menu-item>
            <el-menu-item index="/admin/workers/performance">绩效统计</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/aftersales">
            <template #title><span>售后管理</span></template>
            <el-menu-item index="/admin/aftersales/requests">售后申请处理</el-menu-item>
            <el-menu-item index="/admin/aftersales/product-requests">商品售后管理</el-menu-item>
            <el-menu-item index="/admin/aftersales/reviews">评价管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/config">
            <template #title><span>系统配置</span></template>
            <el-menu-item index="/admin/config/services">服务项目配置</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/users">
            <template #title><span>用户管理</span></template>
            <el-menu-item index="/admin/users/list">用户列表</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/admin/system">
            <template #title><span>系统管理</span></template>
            <el-menu-item index="/admin/system/operation-logs">操作日志</el-menu-item>
            <el-menu-item index="/admin/system/announcements">公告管理</el-menu-item>
            <el-menu-item index="/admin/system/settings">基础设置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </aside>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { RefreshRight } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { clearToken } from '../utils/auth';
import { useAdminStore } from '../stores/admin';
import { fetchAdminProfile } from '../api/adminAccount';
import brandLogoIcon from '../assets/logo-icon.png';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/workers/info/')) {
    return '/admin/workers/info';
  }
  if (route.path.startsWith('/admin/users/list/')) {
    return '/admin/users/list';
  }
  if (route.path.startsWith('/admin/orders/reserve/')) {
    return '/admin/orders/reserve';
  }
  if (route.path.startsWith('/admin/orders/product/')) {
    return '/admin/orders/product';
  }
  if (route.path.startsWith('/admin/aftersales/product-requests/')) {
    return '/admin/aftersales/product-requests';
  }
  return route.path;
});

const activeRootMenu = computed(() => {
  if (!route.path.startsWith('/admin/')) {
    return '';
  }
  const segments = route.path.split('/').filter(Boolean);
  if (segments.length < 2) {
    return '';
  }
  return `/admin/${segments[1]}`;
});

const displayName = computed(() => adminStore.name || adminStore.email || '管理员');
const avatarUrl = computed(() => adminStore.avatar);
const displayInitial = computed(() => (displayName.value ? displayName.value.charAt(0).toUpperCase() : 'A'));

onMounted(async () => {
  if (!adminStore.loaded) {
    try {
      const res = await fetchAdminProfile();
      if (res.code === 200 && res.data) {
        adminStore.setInfo(res.data);
      }
    } catch (e) {
      // ignore
    }
  }
});

function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/admin/profile');
    return;
  }
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        clearToken();
        adminStore.clear();
        ElMessage.success('已退出登录');
        router.push('/login');
      })
      .catch(() => {});
  }
}

function handlePageRefresh() {
  const detail = {
    path: route.path,
    fullPath: route.fullPath,
    handled: false
  };
  window.dispatchEvent(new CustomEvent('admin-page-refresh', { detail }));
  if (!detail.handled) {
    window.location.reload();
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.admin-header {
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  margin-right: 10px;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.18);
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 16px;
}

.header-subtitle {
  font-size: 12px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-refresh-btn {
  width: 32px;
  height: 32px;
}

.header-user {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.header-avatar {
  margin-right: 6px;
}

.header-username {
  font-size: 14px;
  color: #303133;
}

.admin-body {
  flex: 1;
  display: flex;
  height: calc(100vh - 56px);
}

.admin-sider {
  width: 220px;
  background-color: #001529;
  color: #fff;
  overflow-y: auto;
}

.admin-menu {
  border-right: none;
  background-color: transparent;
}

.admin-menu :deep(.el-sub-menu__title),
.admin-menu :deep(.el-menu-item) {
  color: #bfcbd9;
  background-color: transparent;
}

.admin-menu :deep(.el-sub-menu__title:hover),
.admin-menu :deep(.el-menu-item:hover) {
  background-color: #fff !important;
  color: #000 !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #fff !important;
}

.admin-menu :deep(.el-menu-item.is-active:hover) {
  background-color: #409eff !important;
  color: #fff !important;
}

.admin-menu :deep(.el-menu--inline) {
  background-color: #f5f7fa;
}

.admin-menu :deep(.el-menu--inline .el-menu-item) {
  background-color: #f5f7fa;
  color: #333 !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background-color: #e4e7ed !important;
  color: #000 !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #fff !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item.is-active:hover) {
  background-color: #409eff !important;
  color: #fff !important;
}

.admin-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  background-color: transparent;
  color: #bfcbd9;
}

.admin-menu :deep(.el-sub-menu.is-active .el-sub-menu__title:hover) {
  background-color: #fff !important;
  color: #000 !important;
}

.admin-content {
  flex: 1;
  padding: 16px;
  background-color: #f5f7fa;
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
