<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { devLogin } from '@/api/auth'
import { getSsoBridgeUrl, getSsoStatus } from '@/api/sso'
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
const ssoRedirecting = ref(false)

const isTestMode = computed(() => route.query.test === 'test')
const showPasswordLogin = computed(
  () => !ssoStatus.value?.enabled || isTestMode.value,
)

onMounted(async () => {
  try {
    ssoStatus.value = await getSsoStatus()
  } catch {
    return
  }

  if (ssoStatus.value?.enabled && !isTestMode.value) {
    ssoRedirecting.value = true
    try {
      const pendingRedirect =
        typeof route.query.redirect === 'string' && route.query.redirect
          ? route.query.redirect
          : '/dashboard'
      sessionStorage.setItem('sso_redirect', pendingRedirect)

      const { bridgeUrl } = await getSsoBridgeUrl()
      if (bridgeUrl) {
        window.location.href = bridgeUrl
        return
      }
    } catch {
      // 获取 SSO 跳转地址失败时降级显示登录页
    }
    ssoRedirecting.value = false
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
</script>

<template>
  <div class="login-page">
    <!-- SSO 自动跳转中 -->
    <el-card v-if="ssoRedirecting" class="login-card">
      <div class="sso-redirecting">
        <el-icon class="is-loading" :size="48" color="#1675d1">
          <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32z"
              fill="currentColor"
            />
            <path
              d="M512 736a32 32 0 0 1 32 32v192a32 32 0 1 1-64 0V768a32 32 0 0 1 32-32z"
              fill="currentColor"
              opacity="0.3"
            />
          </svg>
        </el-icon>
        <p class="redirecting-text">正在跳转至米多星球登录...</p>
      </div>
    </el-card>

    <el-card v-else class="login-card">
      <template #header>
        <div class="header-title">米多工单平台</div>
      </template>

      <!-- SSO 登录引导（SSO 启用时始终显示） -->
      <div v-if="ssoStatus?.enabled" class="sso-section">
        <div class="section-label">
          <el-tag type="success" size="small">统一登录</el-tag>
          <span class="section-title">米多星球 SSO 登录</span>
        </div>

        <el-alert class="sso-notice" type="success" :closable="false" show-icon>
          <template #default>
            本系统已接入米多星球 SSO 单点登录。请从
            <strong>米多星球工作台</strong>
            快捷入口进入，系统将自动完成身份验证。
          </template>
        </el-alert>

        <!-- 非 test 模式下的纯 SSO 提示 -->
        <el-alert
          v-if="!isTestMode"
          class="sso-only-notice"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            账号密码登录已停用，请统一使用米多星球 SSO 方式登录。
          </template>
        </el-alert>

        <el-divider v-if="showPasswordLogin" />
      </div>

      <!-- 账号密码登录（仅在 SSO 未启用 或 test 模式下显示） -->
      <div v-if="showPasswordLogin" class="dev-login-section">
        <div v-if="isTestMode && ssoStatus?.enabled" class="section-label">
          <el-tag type="warning" size="small">调试模式</el-tag>
          <span class="section-title">账号密码登录</span>
        </div>
        <div v-else-if="!ssoStatus?.enabled" class="section-label">
          <el-tag type="warning" size="small">过渡阶段</el-tag>
          <span class="section-title">账号密码登录</span>
        </div>

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

.sso-only-notice {
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

.sso-redirecting {
  padding: 40px 0;
  text-align: center;
}

.redirecting-text {
  margin-top: 16px;
  font-size: 16px;
  color: #606266;
}
</style>
