import LoginView from '../views/login/LoginView.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import DashboardView from '../views/dashboard/DashboardView.vue';
import AdminPlaceholderView from '../views/admin/AdminPlaceholderView.vue';
import AdminProfileView from '../views/admin/AdminProfileView.vue';
import AdminUsersListView from '../views/admin/AdminUsersListView.vue';
import AdminUserDetailView from '../views/admin/AdminUserDetailView.vue';
import AdminWorkersInfoView from '../views/admin/AdminWorkersInfoView.vue';
import AdminWorkerDetailView from '../views/admin/AdminWorkerDetailView.vue';
import AdminWorkerPerformanceView from '../views/admin/AdminWorkerPerformanceView.vue';
import AdminOperationLogsView from '../views/admin/AdminOperationLogsView.vue';
import AdminServiceConfigView from '../views/admin/AdminServiceConfigView.vue';
import AdminAnnouncementsView from '../views/admin/AdminAnnouncementsView.vue';
import AdminAftersalesRequestsView from '../views/admin/AdminAftersalesRequestsView.vue';
import AdminAftersalesRequestDetailView from '../views/admin/AdminAftersalesRequestDetailView.vue';
import AdminProductAftersalesRequestsView from '../views/admin/AdminProductAftersalesRequestsView.vue';
import AdminProductAftersalesRequestDetailView from '../views/admin/AdminProductAftersalesRequestDetailView.vue';
import AdminAftersalesReviewsView from '../views/admin/AdminAftersalesReviewsView.vue';
import AdminProductCategoriesView from '../views/admin/AdminProductCategoriesView.vue';
import AdminProductCouponsView from '../views/admin/AdminProductCouponsView.vue';
import AdminProductsMainView from '../views/admin/AdminProductsMainView.vue';
import AdminProductsSecondHandView from '../views/admin/AdminProductsSecondHandView.vue';
import AdminProductWarrantyCardsView from '../views/admin/AdminProductWarrantyCardsView.vue';
import AdminReserveOrdersView from '../views/admin/AdminReserveOrdersView.vue';
import AdminReserveOrderDetailView from '../views/admin/AdminReserveOrderDetailView.vue';
import AdminOfflineOrderCreateView from '../views/admin/AdminOfflineOrderCreateView.vue';
import AdminProductOrdersView from '../views/admin/AdminProductOrdersView.vue';
import AdminProductOrderDetailView from '../views/admin/AdminProductOrderDetailView.vue';
import AdminSystemSettingsView from '../views/admin/AdminSystemSettingsView.vue';
import ProtocolViewerView from '../views/common/ProtocolViewerView.vue';

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/protocol/:type',
    name: 'ProtocolViewer',
    component: ProtocolViewerView,
    meta: { title: '协议内容' }
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: DashboardView
      },
      {
        path: 'orders/reserve',
        name: 'AdminOrdersReserve',
        component: AdminReserveOrdersView,
        meta: { title: '预约订单列表', description: '用于查看和管理用户提交的报修预约订单。' }
      },
      {
        path: 'orders/reserve/:id',
        name: 'AdminReserveOrderDetail',
        component: AdminReserveOrderDetailView,
        meta: { title: '预约订单详情', description: '用于查看报修预约订单的完整处理信息。' }
      },
      {
        path: 'orders/offline',
        name: 'AdminOrdersOffline',
        component: AdminOfflineOrderCreateView,
        meta: { title: '线下订单录入', description: '用于录入线下订单数据。' }
      },
      {
        path: 'orders/product',
        name: 'AdminOrdersProduct',
        component: AdminProductOrdersView,
        meta: { title: '商品订单列表', description: '用于管理商城商品订单和发货操作。' }
      },
      {
        path: 'orders/product/:id',
        name: 'AdminProductOrderDetail',
        component: AdminProductOrderDetailView,
        meta: { title: '商品订单详情', description: '用于查看商品订单明细和物流信息。' }
      },
      {
        path: 'products/categories',
        name: 'AdminProductCategories',
        component: AdminProductCategoriesView,
        meta: { title: '商品分类管理', description: '用于维护商品分类树结构。' }
      },
      {
        path: 'products/main',
        name: 'AdminProductsMain',
        component: AdminProductsMainView,
        meta: { title: '商品信息管理', description: '用于管理维修服务商品信息。' }
      },
      {
        path: 'products/second-hand',
        name: 'AdminProductsSecondHand',
        component: AdminProductsSecondHandView,
        meta: { title: '二手商品管理', description: '用于管理二手商品。' }
      },
      {
        path: 'products/warranty',
        name: 'AdminProductsWarranty',
        component: AdminProductWarrantyCardsView,
        meta: { title: '保修卡管理', description: '用于管理用户保修卡信息。' }
      },
      {
        path: 'workers/info',
        name: 'AdminWorkersInfo',
        component: AdminWorkersInfoView,
        meta: { title: '师傅信息管理', description: '用于维护维修师傅基础信息。' }
      },
      {
        path: 'workers/info/:id',
        name: 'AdminWorkerDetail',
        component: AdminWorkerDetailView,
        meta: { title: '师傅详情', description: '用于查看并编辑师傅完整资料。' }
      },
      {
        path: 'workers/schedule',
        name: 'AdminWorkersSchedule',
        component: AdminPlaceholderView,
        meta: { title: '工作时间管理', description: '用于配置师傅排班与工作时间。' }
      },
      {
        path: 'workers/performance',
        name: 'AdminWorkersPerformance',
        component: AdminWorkerPerformanceView,
        meta: { title: '绩效统计', description: '用于统计师傅绩效数据。' }
      },
      {
        path: 'aftersales/requests',
        name: 'AdminAftersalesRequests',
        component: AdminAftersalesRequestsView,
        meta: { title: '售后申请处理', description: '用于处理用户售后申请。' }
      },
      {
        path: 'aftersales/requests/:id',
        name: 'AdminAftersalesRequestDetail',
        component: AdminAftersalesRequestDetailView,
        meta: { title: '售后申请详情', description: '用于查看并处理售后申请详情。' }
      },
      {
        path: 'aftersales/product-requests',
        name: 'AdminProductAftersalesRequests',
        component: AdminProductAftersalesRequestsView,
        meta: { title: '商品售后管理', description: '用于处理商品订单售后申请。' }
      },
      {
        path: 'aftersales/product-requests/:id',
        name: 'AdminProductAftersalesRequestDetail',
        component: AdminProductAftersalesRequestDetailView,
        meta: { title: '商品售后详情', description: '用于查看并处理商品售后详情。' }
      },
      {
        path: 'aftersales/reviews',
        name: 'AdminAftersalesReviews',
        component: AdminAftersalesReviewsView,
        meta: { title: '评价管理', description: '用于查看和管理用户评价。' }
      },
      {
        path: 'products/coupons',
        name: 'AdminProductsCoupons',
        component: AdminProductCouponsView,
        meta: { title: '优惠券管理', description: '用于配置和发放优惠券。' }
      },
      {
        path: 'config/services',
        name: 'AdminConfigServices',
        component: AdminServiceConfigView,
        meta: { title: '服务项目配置', description: '用于配置维修服务项目。' }
      },
      {
        path: 'config/fees',
        name: 'AdminConfigFees',
        component: AdminPlaceholderView,
        meta: { title: '费用配置', description: '用于设置各类费用标准。' }
      },
      {
        path: 'config/worktime',
        name: 'AdminConfigWorktime',
        component: AdminPlaceholderView,
        meta: { title: '工作时间配置', description: '用于配置系统全局工作时间。' }
      },
      {
        path: 'stats/orders',
        name: 'AdminStatsOrders',
        component: AdminPlaceholderView,
        meta: { title: '订单统计', description: '用于统计订单数据。' }
      },
      {
        path: 'stats/income',
        name: 'AdminStatsIncome',
        component: AdminPlaceholderView,
        meta: { title: '收入统计', description: '用于统计平台收入。' }
      },
      {
        path: 'stats/hot',
        name: 'AdminStatsHot',
        component: AdminPlaceholderView,
        meta: { title: '热门分析', description: '用于分析热门服务和商品。' }
      },
      {
        path: 'users/list',
        name: 'AdminUsersList',
        component: AdminUsersListView,
        meta: { title: '用户列表', description: '用于查看平台用户信息。' }
      },
      {
        path: 'users/list/:id',
        name: 'AdminUserDetail',
        component: AdminUserDetailView,
        meta: { title: '用户详情', description: '用于查看并维护用户信息。' }
      },
      {
        path: 'system/admin-accounts',
        name: 'AdminSystemAccounts',
        component: AdminPlaceholderView,
        meta: { title: '管理员账号', description: '用于管理后台管理员账号。' }
      },
      {
        path: 'system/operation-logs',
        name: 'AdminSystemOperationLogs',
        component: AdminOperationLogsView,
        meta: { title: '操作日志', description: '用于查看后台操作日志。' }
      },
      {
        path: 'system/announcements',
        name: 'AdminSystemAnnouncements',
        component: AdminAnnouncementsView,
        meta: { title: '公告管理', description: '用于管理轮播图和公告栏内容。' }
      },
      {
        path: 'system/settings',
        name: 'AdminSystemSettings',
        component: AdminSystemSettingsView,
        meta: { title: '基础设置', description: '用于配置系统基础参数。' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: AdminProfileView,
        meta: { title: '个人中心', description: '用于查看和编辑当前管理员个人信息。' }
      }
    ]
  }
];

export default routes;
