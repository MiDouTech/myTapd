<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="error-code">404</div>
    <div class="error-title">页面不存在</div>
    <div class="error-desc">抱歉，您访问的页面未找到，可能已被移除或地址有误</div>
    <div class="error-actions">
      <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
      <el-button @click="router.back()">返回上页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f2fc 100%);
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #1675d1, #42a5f5);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -4px;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--md-text-primary, #1d2129);
  margin-top: 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--md-text-muted, #909399);
  margin-top: 4px;
}

.error-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}
</style>
