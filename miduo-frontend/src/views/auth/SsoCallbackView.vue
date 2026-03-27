<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ssoCallback } from '@/api/sso'
import { useAuthStore } from '@/stores/auth'
import { notifyError, notifySuccess } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const errorMsg = ref('')

onMounted(async () => {
  const token = route.query.token as string | undefined
  const state = route.query.state as string | undefined

  if (!token) {
    errorMsg.value = '缺少 SSO token 参数'
    loading.value = false
    return
  }

  try {
    const loginOutput = await ssoCallback({ token, state })
    authStore.setLoginState(loginOutput)
    await authStore.loadCurrentUser(true)
    notifySuccess('SSO 登录成功')
    await router.replace('/dashboard')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : 'SSO 登录失败'
    errorMsg.value = msg
    notifyError(msg)
  } finally {
    loading.value = false
  }
})

function goToLogin(): void {
  router.replace('/login')
}
</script>

<template>
  <div class="sso-callback-page">
    <el-card class="sso-callback-card">
      <div v-if="loading" class="loading-state">
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
        <p class="loading-text">正在通过米多星球验证身份...</p>
      </div>

      <div v-else-if="errorMsg" class="error-state">
        <el-result icon="error" title="SSO 登录失败" :sub-title="errorMsg">
          <template #extra>
            <el-button type="primary" @click="goToLogin">返回登录页</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.sso-callback-page {
  min-height: 100vh;
  background: linear-gradient(120deg, #f0f7ff, #ffffff);
  display: flex;
  justify-content: center;
  align-items: center;
}

.sso-callback-card {
  width: 480px;
  text-align: center;
}

.loading-state {
  padding: 40px 0;
}

.loading-text {
  margin-top: 16px;
  font-size: 16px;
  color: #606266;
}

.error-state {
  padding: 20px 0;
}
</style>
