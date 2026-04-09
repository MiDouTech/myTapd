<script setup lang="ts">
import { DocumentCopy, Key, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'

import {
  createUserApiKey,
  deleteUserApiKey,
  disableUserApiKey,
  listUserApiKeys,
  type UserApiKeyListOutput,
} from '@/api/userApiKey'

const loading = ref(false)
const tableData = ref<UserApiKeyListOutput[]>([])
const createVisible = ref(false)
const createName = ref('')
const createSubmitting = ref(false)
const revealVisible = ref(false)
const revealedKey = ref('')

async function loadList(): Promise<void> {
  loading.value = true
  try {
    tableData.value = await listUserApiKeys()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadList()
})

function openCreate(): void {
  createName.value = ''
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  const name = createName.value.trim()
  if (!name) {
    ElMessage.warning('请输入密钥名称')
    return
  }
  createSubmitting.value = true
  try {
    const res = await createUserApiKey({ name })
    createVisible.value = false
    revealedKey.value = res.apiKey
    revealVisible.value = true
    await loadList()
    ElMessage.success('密钥已创建，请立即复制保存（仅显示一次）')
  } finally {
    createSubmitting.value = false
  }
}

async function copyKey(): Promise<void> {
  try {
    await navigator.clipboard.writeText(revealedKey.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

async function handleDisable(row: UserApiKeyListOutput): Promise<void> {
  await ElMessageBox.confirm(`确定禁用密钥「${row.name}」？禁用后 IDE/Agent 将无法再使用该密钥。`, '禁用密钥', {
    type: 'warning',
  })
  await disableUserApiKey(row.id)
  ElMessage.success('已禁用')
  await loadList()
}

async function handleDelete(row: UserApiKeyListOutput): Promise<void> {
  await ElMessageBox.confirm(`确定删除密钥「${row.name}」？此操作不可恢复。`, '删除密钥', {
    type: 'warning',
  })
  await deleteUserApiKey(row.id)
  ElMessage.success('已删除')
  await loadList()
}

function statusLabel(status: number): string {
  return status === 1 ? '启用' : '已禁用'
}

function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}
</script>

<template>
  <div class="page-user-api-keys">
    <div class="page-header">
      <div class="title-wrap">
        <el-icon class="title-icon" :size="22"><Key /></el-icon>
        <div>
          <h1 class="page-title">个人 API 密钥</h1>
          <p class="page-desc">
            用于 Cursor、龙虾等 IDE/Agent 调用工单接口。密钥等同于登录凭证，请勿提交到 Git 或分享给他人。
          </p>
        </div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建密钥</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip-alert">
      请求时在 HTTP 头携带 <code>X-Api-Key</code>（值为完整密钥）。与 JWT 二选一即可访问已授权接口。
    </el-alert>

    <el-table
      v-loading="loading"
      :data="tableData"
      :border="false"
      :stripe="true"
      :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      class="keys-table"
    >
      <el-table-column prop="name" label="名称" min-width="140" align="center" />
      <el-table-column prop="keyPrefixDisplay" label="密钥前缀" min-width="200" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastUsedAt" label="最后使用" min-width="160" align="center" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            type="primary"
            link
            @click="handleDisable(row)"
          >
            禁用
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && tableData.length === 0" description="暂无密钥，点击「新建密钥」用于 IDE 集成" />

    <el-dialog v-model="createVisible" title="新建 API 密钥" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input
            v-model="createName"
            maxlength="100"
            show-word-limit
            placeholder="例如：公司 Mac、家里笔记本"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revealVisible" title="请保存您的密钥" width="520px" @closed="revealedKey = ''">
      <el-alert type="warning" :closable="false" show-icon class="reveal-alert">
        此密钥仅显示一次，关闭窗口后无法再次查看完整内容。
      </el-alert>
      <el-input v-model="revealedKey" type="textarea" :rows="3" readonly class="reveal-input" />
      <template #footer>
        <el-button :icon="DocumentCopy" type="primary" @click="copyKey">复制密钥</el-button>
        <el-button @click="revealVisible = false">已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-user-api-keys {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.title-wrap {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.title-icon {
  color: #1675d1;
  margin-top: 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.tip-alert {
  margin-bottom: 16px;

  code {
    padding: 0 6px;
    background: #f0f2f5;
    border-radius: 4px;
    font-size: 13px;
  }
}

.keys-table {
  width: 100%;
}

.reveal-alert {
  margin-bottom: 12px;
}

.reveal-input :deep(textarea) {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
</style>
