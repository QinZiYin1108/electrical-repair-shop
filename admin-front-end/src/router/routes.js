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
import AdminCancelReasonsView from '../views/admin/AdminCancelReasonsView.vue';
import AdminSystemSettingsView from '../views/admin/AdminSystemSettingsView.vue';
import AdminStoreListView from '../views/admin/AdminStoreListView.vue';
import AdminStoreDetailView from '../views/admin/AdminStoreDetailView.vue';
import AdminContentCheckView from '../views/admin/AdminContentCheckView.vue';
import AdminReportManageView from '../views/admin/AdminReportManageView.vue';
import AdminPenaltyManageView from '../views/admin/AdminPenaltyManageView.vue';
import AdminCreditManageView from '../views/admin/AdminCreditManageView.vue';
import AdminWorkerWithdrawalsView from '../views/admin/AdminWorkerWithdrawalsView.vue';
import AdminNotificationOutboxView from '../views/admin/AdminNotificationOutboxView.vue';
import AdminOrderSlaView from '../views/admin/AdminOrderSlaView.vue';
import AdminSupportTicketsView from '../views/admin/AdminSupportTicketsView.vue';
import AdminAppointmentCapacityView from '../views/admin/AdminAppointmentCapacityView.vue';
import AdminInvoicesView from '../views/admin/AdminInvoicesView.vue';
import AdminFinanceView from '../views/admin/AdminFinanceView.vue';
import AdminFundReconciliationView from '../views/admin/AdminFundReconciliationView.vue';
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
        path: 'orders/cancel-reasons',
        name: 'AdminCancelReasons',
        component: AdminCancelReasonsView,
        meta: { title: '取消原因', description: '用于查看订单取消原因记录与分类统计。' }
      },
      {
        path: 'stores/list',
        name: 'AdminStoresList',
        component: AdminStoreListView,
        meta: { title: '门店列表', description: '用于创建和管理门店及门店管理员。' }
      },
      {
        path: 'stores/list/:id',
        name: 'AdminStoreDetail',
        component: AdminStoreDetailView,
        meta: { title: '门店详情', description: '查看门店详情、管理员信息和营业时间。' }
      },
      {
        path: 'content-check',
        name: 'AdminContentCheck',
        component: AdminContentCheckView,
        meta: {
          title: '内容审核管理',
          description: '审核日志查看、存疑复审、图片/视频审核队列与改判。'
        }
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: AdminReportManageView,
        meta: { title: '举报管理', description: '审核用户、师傅和门店管理员的举报内容。' }
      },
      {
        path: 'penalties',
        name: 'AdminPenalties',
        component: AdminPenaltyManageView,
        meta: { title: '处罚管理', description: '查看、执行和管理处罚记录及申诉处理。' }
      },
      {
        path: 'credits',
        name: 'AdminCredits',
        component: AdminCreditManageView,
        meta: { title: '信用积分', description: '查看和管理用户、师傅的信用积分及变动记录。' }
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
      },
      {
        path: 'operations/withdrawals',
        name: 'AdminOperationsWithdrawals',
        component: AdminWorkerWithdrawalsView,
        meta: {
          title: '师傅提现审核',
          description: '审核、打款与提现风控（高金额复核、人工拦截）。'
        }
      },
      {
        path: 'operations/notifications',
        name: 'AdminOperationsNotifications',
        component: AdminNotificationOutboxView,
        meta: { title: '通知 outbox', description: '事务 outbox 待发送/失败/死信记录与人工重试。' }
      },
      {
        path: 'operations/order-sla',
        name: 'AdminOperationsOrderSla',
        component: AdminOrderSlaView,
        meta: {
          title: '订单 SLA',
          description: '维修订单各阶段超时事件（责任方/通知/升级/处理结果）。'
        }
      },
      {
        path: 'operations/support-tickets',
        name: 'AdminOperationsSupportTickets',
        component: AdminSupportTicketsView,
        meta: { title: '客服工单', description: '客服工单分派、优先级、备注、双人审批与操作记录。' }
      },
      {
        path: 'operations/appointment-capacity',
        name: 'AdminOperationsAppointmentCapacity',
        component: AdminAppointmentCapacityView,
        meta: { title: '预约容量', description: '停业/请假时段与师傅时段占用。' }
      },
      {
        path: 'operations/invoices',
        name: 'AdminOperationsInvoices',
        component: AdminInvoicesView,
        meta: { title: '发票管理', description: '用户发票申请的开票、驳回与红冲。' }
      },
      {
        path: 'operations/finance',
        name: 'AdminOperationsFinance',
        component: AdminFinanceView,
        meta: { title: '财务核算', description: '订单财务快照与按时间口径的财务报表。' }
      },
      {
        path: 'operations/reconciliation',
        name: 'AdminOperationsReconciliation',
        component: AdminFundReconciliationView,
        meta: { title: '资金对账', description: '对账批次、异常明细与手动触发。' }
      }
    ]
  }
];

export default routes;
