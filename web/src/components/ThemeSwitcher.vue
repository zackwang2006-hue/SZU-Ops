<template>
  <el-dropdown trigger="click" @command="themeStore.setTheme">
    <span class="theme-trigger" title="切换主题">
      <el-icon :size="18"><Brush /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in themes"
          :key="item.value"
          :command="item.value"
          :class="{ 'is-current': themeStore.theme === item.value }"
        >
          <span class="theme-item">
            <span>{{ item.label }}</span>
            <el-icon v-if="themeStore.theme === item.value"><Check /></el-icon>
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { Brush, Check } from '@element-plus/icons-vue'
import { useThemeStore } from '@/store/theme'

const themeStore = useThemeStore()

const themes = [
  { value: 'arco', label: '清透企业风' },
  { value: 'material', label: 'Material 3 明亮风' },
  { value: 'dark', label: '深色科技风' }
]
</script>

<style scoped>
.theme-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--c-radius-md);
  color: var(--c-text-regular);
  cursor: pointer;
  outline: none;
}

.theme-trigger:hover {
  background: var(--c-primary-light);
  color: var(--c-primary);
}

.theme-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

:deep(.is-current) {
  color: var(--c-primary);
  font-weight: 500;
}
</style>
