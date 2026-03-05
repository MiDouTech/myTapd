<script setup lang="ts">
import { Tickets } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { devLogin, wecomLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loginForm = reactive({
  code: '',
})

const devForm = reactive({
  username: '',
  password: '',
})
const devLoginLoading = ref(false)

async function handleLoginByCode(): Promise<void> {
  if (!loginForm.code) {
    notifyWarning('请输入企微授权 code')
    return
  }
  authStore.loginLoading = true
  try {
    const loginOutput = await wecomLogin({ code: loginForm.code })
    authStore.setLoginState(loginOutput)
    await authStore.loadCurrentUser(true)
    notifySuccess('登录成功')
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect
        ? route.query.redirect
        : '/dashboard'
    await router.replace(redirect)
  } catch {
    notifyError('登录失败，请检查授权码后重试')
  } finally {
    authStore.loginLoading = false
  }
}

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

function openWecomOauth(): void {
  const oauthUrl = import.meta.env.VITE_WECOM_OAUTH_URL
  if (!oauthUrl) {
    notifyWarning('未配置 VITE_WECOM_OAUTH_URL，请先在 .env 中配置企微授权地址')
    return
  }
  globalThis.location.href = oauthUrl
}

const queryCode = route.query.code
if (typeof queryCode === 'string' && queryCode) {
  loginForm.code = queryCode
  void handleLoginByCode()
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="header-title">米多工单平台</div>
      </template>

      <!-- 测试账号登录区域 -->
      <div class="dev-login-section">
        <div class="section-label">
          <el-tag type="warning" size="small">测试环境</el-tag>
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

      <el-divider>企业微信登录</el-divider>

      <div class="qr-placeholder">
        <el-icon :size="48" color="#1675d1"><Tickets /></el-icon>
        <div class="tip">请使用企业微信扫码登录</div>
        <div class="sub-tip">扫码完成后会回调并携带 code 参数</div>
      </div>

      <el-form @submit.prevent>
        <el-form-item label="授权码">
          <el-input
            v-model="loginForm.code"
            placeholder="请输入企微授权回调 code（开发联调用）"
            clearable
          />
        </el-form-item>
        <el-space wrap>
          <el-button type="primary" :loading="authStore.loginLoading" @click="handleLoginByCode">
            使用授权码登录
          </el-button>
          <el-button @click="openWecomOauth">前往企业微信授权页</el-button>
        </el-space>
      </el-form>
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
  width: 520px;
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

.qr-placeholder {
  border: 1px dashed #d9ecff;
  background: #f7fbff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  text-align: center;
}

.tip {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 500;
}

.sub-tip {
  color: #909399;
  margin-top: 6px;
}
</style>
