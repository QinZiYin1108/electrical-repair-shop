import { createRouter, createWebHistory } from 'vue-router';
import routes from '../router/routes';
import { getToken } from '../utils/auth';

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const token = getToken();
  if (to.path.startsWith('/admin') && !token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && token) {
    return '/admin/dashboard';
  }
  return true;
});

export default router;
