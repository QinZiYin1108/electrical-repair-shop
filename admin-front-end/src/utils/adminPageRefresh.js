import { onBeforeUnmount, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';

export function useAdminPageRefresh(refreshHandler, options = {}) {
  const route = useRoute();
  const {
    matcher,
    successMessage = '刷新成功',
    showSuccess = true
  } = options;

  const shouldHandle = typeof matcher === 'function'
    ? matcher
    : detail => detail?.path === route.path;

  const handleExternalRefresh = async event => {
    const detail = event?.detail;
    if (!detail || !shouldHandle(detail, route)) {
      return;
    }
    detail.handled = true;
    await refreshHandler(detail);
    if (showSuccess) {
      ElMessage.success(successMessage);
    }
  };

  onMounted(() => {
    window.addEventListener('admin-page-refresh', handleExternalRefresh);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('admin-page-refresh', handleExternalRefresh);
  });

  return {
    handleExternalRefresh
  };
}
