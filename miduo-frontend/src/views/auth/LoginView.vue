<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { devLogin } from '@/api/auth'
import { getSsoStatus } from '@/api/sso'
import { useAuthStore } from '@/stores/auth'
import type { SsoStatusOutput } from '@/types/sso'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const devForm = reactive({
  username: '',
  password: '',
})
const devLoginLoading = ref(false)

const ssoStatus = ref<SsoStatusOutput | null>(null)
const ssoLoading = ref(false)

onMounted(async () => {
  try {
    ssoStatus.value = await getSsoStatus()
  } catch {
    // SSO 状态查询失败时静默处理，仅显示密码登录
  }
})

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

function handleSsoLogin(): void {
  if (!ssoStatus.value?.enabled) {
    notifyWarning('SSO 登录未启用')
    return
  }
  ssoLoading.value = true
  notifyWarning('请从米多星球工作台进入本系统，SSO 登录由米多发起跳转')
  ssoLoading.value = false
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="header-title">米多工单平台</div>
      </template>

      <!-- SSO 入口提示 -->
      <div v-if="ssoStatus?.enabled" class="sso-section">
        <div class="section-label">
          <el-tag type="success" size="small">推荐</el-tag>
          <span class="section-title">米多星球 SSO 登录</span>
        </div>

        <el-alert class="sso-notice" type="success" :closable="false" show-icon>
          <template #default>
            已对接米多星球 SSO 单点登录。请从
            <strong>米多星球工作台</strong>
            快捷入口进入本系统，系统将自动完成身份验证。
          </template>
        </el-alert>

        <el-button
          type="primary"
          style="width: 100%; margin-bottom: 20px"
          :loading="ssoLoading"
          @click="handleSsoLogin"
        >
          了解 SSO 登录方式
        </el-button>

        <el-divider>或使用账号密码登录</el-divider>
      </div>

      <!-- 账号密码登录 -->
      <div class="dev-login-section">
        <div v-if="!ssoStatus?.enabled" class="section-label">
          <el-tag type="warning" size="small">过渡阶段</el-tag>
          <span class="section-title">账号密码登录</span>
        </div>

        <el-alert
          v-if="!ssoStatus?.enabled"
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

.sso-section {
  margin-bottom: 8px;
}

.sso-notice {
  margin-bottom: 16px;
  font-size: 13px;
  line-height: 1.6;
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
