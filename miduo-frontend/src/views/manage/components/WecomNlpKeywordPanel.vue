<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import {
  createNlpKeyword,
  deleteNlpKeyword,
  listNlpKeywords,
  updateNlpKeyword,
} from '@/api/wecom'
import type { NlpKeywordCreateInput, NlpKeywordListOutput, NlpKeywordUpdateInput } from '@/types/wecom'
import { notifySuccess } from '@/utils/feedback'
import { ElMessageBox } from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const filterMatchType = ref<number | undefined>(undefined)
const tableData = ref<NlpKeywordListOutput[]>([])
const editingId = ref<number | null>(null)

const matchTypeOptions = [
  { label: '分类', value: 1 },
  { label: '优先级', value: 2 },
  { label: '实体', value: 3 },
]

const defaultForm = (): NlpKeywordCreateInput => ({
  keyword: '',
  matchType: 1,
  targetValue: '',
  confidence: 80,
  sortOrder: 0,
  isActive: 1,
})

const form = reactive<NlpKeywordCreateInput>(defaultForm())

async function loadList(): Promise<void> {
  loading.value = true
  try {
    tableData.value = await listNlpKeywords(filterMatchType.value)
  } catch {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleAdd(): void {
  editingId.value = null
  dialogTitle.value = '新增关键词'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row: NlpKeywordListOutput): void {
  editingId.value = row.id
  dialogTitle.value = '编辑关键词'
  form.keyword = row.keyword
  form.matchType = row.matchType
  form.targetValue = row.targetValue
  form.confidence = row.confidence
  form.sortOrder = row.sortOrder
  form.isActive = row.isActive
  dialogVisible.value = true
}

async function handleDelete(row: NlpKeywordListOutput): Promise<void> {
  await ElMessageBox.confirm(`确认删除关键词「${row.keyword}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  })
  try {
    await deleteNlpKeyword(row.id)
    notifySuccess('删除成功')
    await loadList()
  } catch {
    // 错误由拦截器统一处理
  }
}

async function handleToggleActive(row: NlpKeywordListOutput): Promise<void> {
  const newActive = row.isActive === 1 ? 0 : 1
  try {
    const updateInput: NlpKeywordUpdateInput = {
      keyword: row.keyword,
      matchType: row.matchType,
      targetValue: row.targetValue,
      confidence: row.confidence,
      sortOrder: row.sortOrder,
      isActive: newActive,
    }
    await updateNlpKeyword(row.id, updateInput)
    notifySuccess(newActive === 1 ? '已启用' : '已禁用')
    await loadList()
  } catch {
    // 错误由拦截器统一处理
  }
}

async function handleSubmit(): Promise<void> {
  if (!form.keyword.trim()) {
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value !== null) {
      const updateInput: NlpKeywordUpdateInput = { ...form }
      await updateNlpKeyword(editingId.value, updateInput)
      notifySuccess('更新成功')
    } else {
      await createNlpKeyword({ ...form })
      notifySuccess('创建成功')
    }
    dialogVisible.value = false
    await loadList()
  } catch {
    // 错误由拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}

function getMatchTypeLabel(matchType: number): string {
  const option = matchTypeOptions.find((o) => o.value === matchType)
  return option ? option.label : String(matchType)
}

onMounted(async () => {
  await loadList()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="配置自然语言建单的关键词规则，系统根据关键词自动推断工单分类和优先级。修改后5分钟内生效（缓存刷新）。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">NLP关键词配置</span>
          <el-space>
            <el-select
              v-model="filterMatchType"
              clearable
              placeholder="请选择内容"
              style="width: 120px"
              @change="loadList"
            >
              <el-option
                v-for="opt in matchTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-button type="primary" @click="handleAdd">新增关键词</el-button>
            <el-button @click="loadList">刷新</el-button>
          </el-space>
        </div>
      </template>

      <el-table
        :data="tableData"
        :border="false"
        :stripe="true"
        v-loading="loading"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="keyword" label="关键词" min-width="120" align="center" />
        <el-table-column label="匹配类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.matchType === 1 ? 'primary' : row.matchType === 2 ? 'warning' : 'info'"
              size="small"
            >
              {{ getMatchTypeLabel(row.matchType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetValue" label="映射目标值" min-width="160" align="center" />
        <el-table-column prop="confidence" label="置信度" width="90" align="center">
          <template #default="{ row }">
            {{ row.confidence }}%
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="160" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button
                :type="row.isActive === 1 ? 'warning' : 'success'"
                link
                @click="handleToggleActive(row)"
              >
                {{ row.isActive === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="关键词" required>
          <el-input v-model="form.keyword" placeholder="如：缺陷、bug、VPN" maxlength="50" />
        </el-form-item>
        <el-form-item label="匹配类型" required>
          <el-select v-model="form.matchType" placeholder="请选择匹配类型" style="width: 100%">
            <el-option
              v-for="opt in matchTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="映射目标值" required>
          <el-input
            v-model="form.targetValue"
            placeholder="如：研发需求/缺陷修复 或 urgent"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="置信度">
          <el-input-number
            v-model="form.confidence"
            :min="0"
            :max="100"
            controls-position="right"
          />
          <span class="field-hint">%，低于70时分类留空</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            controls-position="right"
          />
          <span class="field-hint">数值越大优先级越高</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.isActive"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </el-space>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
}

.field-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
