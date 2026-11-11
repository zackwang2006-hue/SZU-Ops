<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="22" color="var(--c-primary)"><Tools /></el-icon>
        <span>SZU Ops</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header" height="56px">
        <div class="crumb">{{ $route.meta.title }}</div>
        <div class="right">
          <ThemeSwitcher />
          <el-tag size="small" effect="plain">{{ roleLabel }}</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user">
              <el-icon><UserFilled /></el-icon>
              <span class="name">{{ store.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="mine">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>

      <el-footer class="footer" height="36px">
        SZU Ops · 校园设备智能运维系统 · by ZackWang
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

const router = useRouter()
const store = useUserStore()

const ROLE_LABELS = {
  ADMIN: '管理员',
  DISPATCHER: '调度员',
  TECHNICIAN: '维修工',
  USER: '师生用户'
}

const ALL_MENUS = [
  { path: '/dashboard', title: '数据看板', icon: 'DataAnalysis', roles: ['ADMIN', 'DISPATCHER'] },
  { path: '/assets', title: '设备管理', icon: 'Box', roles: ['ADMIN', 'DISPATCHER'] },
  { path: '/orders', title: '工单管理', icon: 'Tickets', roles: ['ADMIN', 'DISPATCHER', 'TECHNICIAN'] },
  { path: '/prediction', title: '预测预警', icon: 'Warning', roles: ['ADMIN', 'DISPATCHER'] },
  { path: '/mine', title: '个人中心', icon: 'User', roles: ['ADMIN', 'DISPATCHER', 'TECHNICIAN', 'USER'] }
]

const menus = computed(() => ALL_MENUS.filter((m) => m.roles.includes(store.role)))
const roleLabel = computed(() => ROLE_LABELS[store.role] || store.role)

const handleCommand = async (command) => {
  if (command === 'mine') {
    router.push('/mine')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background-color: var(--c-bg-sidebar);
  border-right: 1px solid var(--c-border);
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--c-text-primary);
  font-size: 20px;
  font-weight: 500;
  border-bottom: 1px solid var(--c-border);
}

.menu {
  border-right: none;
}

.header {
  background: var(--c-bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--c-border);
  padding: 0 16px;
}

.crumb {
  font-size: 16px;
  font-weight: 500;
  color: var(--c-text-primary);
}

.right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--c-text-regular);
  outline: none;
}

.user .name {
  font-size: 14px;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--c-text-secondary);
  background: var(--c-bg-card);
  border-top: 1px solid var(--c-border);
}

.main {
  background: var(--c-bg-page);
  padding: 16px;
  overflow-y: auto;
}
</style>
