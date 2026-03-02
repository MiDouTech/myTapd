<script setup lang="ts">
import { Tickets } from '@element-plus/icons-vue'
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wecomLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loginForm = reactive({
  code: '',
})

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
        <div class="header-title">企业微信扫码登录</div>
      </template>
      <div class="qr-placeholder">
        <el-icon :size="56" color="#1675d1"><Tickets /></el-icon>
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

.qr-placeholder {
  border: 1px dashed #d9ecff;
  background: #f7fbff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  text-align: center;
}

.tip {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 500;
}

.sub-tip {
  color: #909399;
  margin-top: 6px;
}
</style>
