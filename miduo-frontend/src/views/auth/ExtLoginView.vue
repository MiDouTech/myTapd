<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { localLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  phone: '',
  password: '',
})
const loading = ref(false)

async function handleLogin(): Promise<void> {
  if (!form.phone || !form.password) {
    notifyWarning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    const loginOutput = await localLogin({ phone: form.phone, password: form.password })
    authStore.setLoginState(loginOutput)
    await authStore.loadCurrentUser(true)
    notifySuccess('登录成功')
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect
        ? route.query.redirect
        : '/dashboard'
    await router.replace(redirect)
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : ''
    if (!msg || msg === 'Request failed') {
      notifyError('手机号或密码错误，请确认后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ext-login-page">
    <el-card class="login-card login-card-animate">
      <template #header>
        <div class="header-area">
          <div class="header-title">米多工单平台</div>
          <div class="header-subtitle">外部访客登录</div>
        </div>
      </template>

      <div class="notice-bar">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            此入口专供<strong>上下游合作伙伴</strong>查看工单进展使用。如有疑问，请联系对接人员。
          </template>
        </el-alert>
      </div>

      <el-form class="login-form" @submit.prevent="handleLogin" label-width="70px">
        <el-form-item label="手机号">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            maxlength="11"
            clearable
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-tip">
        <span>账号由管理员分配，如有问题请联系对接人员</span>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.ext-login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f2fc 0%, #f5f7fa 50%, #ffffff 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40%;
    right: -20%;
    width: max(50vw, 300px);
    height: max(50vw, 300px);
    max-width: 600px;
    max-height: 600px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(22, 117, 209, 0.06) 0%, transparent 70%);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -15%;
    width: max(40vw, 250px);
    height: max(40vw, 250px);
    max-width: 500px;
    max-height: 500px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(22, 117, 209, 0.04) 0%, transparent 70%);
    pointer-events: none;
  }
}

.login-card {
  width: 440px;
  border-radius: 16px !important;
  box-shadow: 0 8px 32px rgba(22, 117, 209, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  border: 1px solid rgba(22, 117, 209, 0.08) !important;
  position: relative;
  z-index: 1;

  :deep(.el-card__header) {
    padding: 28px 24px 20px !important;
    border-bottom: 1px solid #eef2f7 !important;
  }

  :deep(.el-card__body) {
    padding: 24px !important;
  }
}

.header-area {
  text-align: center;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #1675d1;
  letter-spacing: 1px;
}

.header-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.notice-bar {
  margin-bottom: 20px;
  font-size: 13px;
  line-height: 1.6;
}

.login-form {
  margin-top: 4px;
}

.footer-tip {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card-animate {
  animation: fadeInUp 0.5s ease-out;
}

@media (max-width: 480px) {
  .login-card {
    width: calc(100vw - 32px);
    margin: 0 16px;
  }
}
</style>
