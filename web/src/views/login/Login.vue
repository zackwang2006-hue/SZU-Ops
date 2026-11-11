<template>
  <div class="login-page">
    <div class="theme-entry">
      <ThemeSwitcher />
    </div>
    <div class="login-box">
      <div class="brand">
        <el-icon :size="36" color="var(--c-primary)"><Tools /></el-icon>
        <h1>SZU Ops</h1>
        <p>校园设备智能运维系统</p>
        <p class="signature">by ZackWang</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        测试账号：admin / dispatcher01 / tech01 / user01（密码均为 123456）
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useUserStore } from '@/store/user'
import { homeByRole } from '@/router'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    store.setLogin(data.token, data.user)
    ElMessage.success(`欢迎回来，${data.user.realName || data.user.username}`)
    const redirect = route.query.redirect
    router.push(redirect || homeByRole(data.user.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-bg-page);
  position: relative;
}

.theme-entry {
  position: absolute;
  top: 16px;
  right: 16px;
}

.login-box {
  width: 380px;
  background: var(--c-bg-card);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-lg);
  padding: 40px 36px 28px;
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand h1 {
  margin: 8px 0 4px;
  font-size: 24px;
  font-weight: 500;
  color: var(--c-text-primary);
}

.brand p {
  margin: 0;
  color: var(--c-text-secondary);
  font-size: 14px;
}

.brand .signature {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.tips {
  margin-top: 8px;
  font-size: 12px;
  color: var(--c-text-secondary);
  text-align: center;
  line-height: 1.6;
}
</style>
