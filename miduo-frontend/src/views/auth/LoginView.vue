<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { devLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const devForm = reactive({
  username: '',
  password: '',
})
const devLoginLoading = ref(false)

async function handleDevLogin(): Promise<void> {
  if (!devForm.username || !devForm.password) {
    notifyWarning('请输入用户名和密码')
    return
  }
  devLoginLoading.value = true
  try {
    const loginOutput = await devLogin({ username: devForm.username, password: devForm.password })
    authStore.setLoginState(loginOutput)
    await authStore.loadCurrentUser(true)
    notifySuccess('登录成功')
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect
        ? route.query.redirect
        : '/dashboard'
    await router.replace(redirect)
  } catch {
    notifyError('登录失败，请检查账号或密码')
  } finally {
    devLoginLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="header-title">米多工单平台</div>
      </template>

      <div class="dev-login-section">
        <div class="section-label">
          <el-tag type="warning" size="small">过渡阶段</el-tag>
          <span class="section-title">账号密码登录</span>
        </div>

        <el-alert
          class="transition-notice"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            当前为过渡阶段，可使用账号密码登录。待 SSO 授权接入后，将不再支持账号密码登录。
          </template>
        </el-alert>

        <el-form @submit.prevent="handleDevLogin" label-width="70px">
          <el-form-item label="用户名">
            <el-input
              v-model="devForm.username"
              placeholder="请输入用户名"
              clearable
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="devForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleDevLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="devLoginLoading"
              style="width: 100%"
              @click="handleDevLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(120deg, #f0f7ff, #ffffff);
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-card {
  width: 480px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1675d1;
  text-align: center;
}

.dev-login-section {
  margin-bottom: 8px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.transition-notice {
  margin-bottom: 20px;
  font-size: 13px;
  line-height: 1.6;
}
</style>
