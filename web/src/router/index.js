import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

export const homeByRole = (role) => {
  if (role === 'USER') return '/mine'
  if (role === 'TECHNICIAN') return '/orders'
  return '/dashboard'
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: () => '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据看板', roles: ['ADMIN', 'DISPATCHER'] }
      },
      {
        path: 'assets',
        name: 'Assets',
        component: () => import('@/views/assets/Assets.vue'),
        meta: { title: '设备管理', roles: ['ADMIN', 'DISPATCHER'] }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/Orders.vue'),
        meta: { title: '工单管理', roles: ['ADMIN', 'DISPATCHER', 'TECHNICIAN'] }
      },
      {
        path: 'prediction',
        name: 'Prediction',
        component: () => import('@/views/prediction/Prediction.vue'),
        meta: { title: '预测预警', roles: ['ADMIN', 'DISPATCHER'] }
      },
      {
        path: 'mine',
        name: 'Mine',
        component: () => import('@/views/mine/Mine.vue'),
        meta: { title: '个人中心', roles: ['ADMIN', 'DISPATCHER', 'TECHNICIAN', 'USER'] }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (to.path === '/login') {
    return store.isLoggedIn ? homeByRole(store.role) : true
  }
  if (!store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 根路径按角色跳转默认首页
  if (to.path === '/') {
    return homeByRole(store.role)
  }
  // 角色权限校验
  if (to.meta.roles && !to.meta.roles.includes(store.role)) {
    return homeByRole(store.role)
  }
  document.title = `${to.meta.title || ''} · SZU Ops`
  return true
})

export default router
