<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'

import {
  createAlertMapping,
  deleteAlertMapping,
  getAlertEventLogPage,
  getAlertMappingPage,
  getAlertToken,
  resetAlertToken,
  updateAlertMapping,
} from '@/api/alert'
import { getCategoryTree } from '@/api/category'
import { getUserList } from '@/api/user'
import type {
  AlertEventLogOutput,
  AlertEventLogPageInput,
  AlertRuleMappingCreateInput,
  AlertRuleMappingOutput,
  AlertRuleMappingPageInput,
  AlertRuleMappingUpdateInput,
  AlertTokenOutput,
} from '@/types/alert'
import type { CategoryTreeOutput } from '@/types/category'
import type { UserListOutput } from '@/types/user'
import { formatDateTime } from '@/utils/formatter'

const activeTab = ref<'mapping' | 'eventLog' | 'webhook'>('mapping')

const loading = ref(false)
const mappingList = ref<AlertRuleMappingOutput[]>([])
const mappingTotal = ref(0)
const mappingQuery = reactive<AlertRuleMappingPageInput>({
  pageNum: 1,
  pageSize: 20,
  ruleName: '',
  enabled: null,
})

const eventLogLoading = ref(false)
const eventLogList = ref<AlertEventLogOutput[]>([])
const eventLogTotal = ref(0)
const eventLogQuery = reactive<AlertEventLogPageInput>({
  pageNum: 1,
  pageSize: 20,
  ruleName: '',
  targetIdent: '',
  processResult: '',
})

const tokenInfo = ref<AlertTokenOutput>({ token: '', webhookUrl: '' })
const tokenLoading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增告警规则映射')
const formLoading = ref(false)
const isEdit = ref(false)

const formData = reactive<AlertRuleMappingCreateInput & { id?: number }>({
  ruleName: '',
  matchMode: 'EXACT',
  categoryId: 0,
  priorityP1: 'urgent',
  priorityP2: 'high',
  priorityP3: 'medium',
  assigneeId: null,
  dedupWindowMinutes: 30,
  enabled: true,
})

const categories = ref<CategoryTreeOutput[]>([])
const users = ref<UserListOutput[]>([])

const priorityOptions = [
  { value: 'urgent', label: '紧急' },
  { value: 'high', label: '高' },
  { value: 'medium', label: '中' },
  { value: 'low', label: '低' },
]

const matchModeOptions = [
  { value: 'EXACT', label: '精确匹配' },
  { value: 'PREFIX', label: '前缀匹配' },
]

const processResultOptions = [
  { value: '', label: '全部' },
  { value: 'CREATED', label: '已创建工单' },
  { value: 'DEDUP', label: '去重跳过' },
  { value: 'RECOVERED', label: '恢复事件' },
  { value: 'UNMAPPED', label: '无映射配置' },
  { value: 'ERROR', label: '处理异常' },
]

const severityLabelMap: Record<number, string> = {
  1: 'P1',
  2: 'P2',
  3: 'P3',
}

const processResultLabelMap: Record<string, string> = {
  CREATED: '已创建工单',
  DEDUP: '去重跳过',
  RECOVERED: '恢复事件',
  UNMAPPED: '无映射(默认)',
  ERROR: '处理异常',
}

const processResultTagType: Record<string, string> = {
  CREATED: 'success',
  DEDUP: 'info',
  RECOVERED: 'warning',
  UNMAPPED: '',
  ERROR: 'danger',
}

const flatCategories = computed(() => {
  const result: { id: number; name: string }[] = []
  function flatten(list: CategoryTreeOutput[]) {
    for (const item of list) {
      result.push({ id: item.id, name: item.name })
      if (item.children && item.children.length > 0) {
        flatten(item.children)
      }
    }
  }
  flatten(categories.value)
  return result
})

async function loadMappings() {
  loading.value = true
  try {
    const res = await getAlertMappingPage(mappingQuery)
    mappingList.value = res.records || []
    mappingTotal.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadEventLogs() {
  eventLogLoading.value = true
  try {
    const res = await getAlertEventLogPage(eventLogQuery)
    eventLogList.value = res.records || []
    eventLogTotal.value = res.total
  } finally {
    eventLogLoading.value = false
  }
}

async function loadToken() {
  tokenLoading.value = true
  try {
    tokenInfo.value = await getAlertToken()
  } finally {
    tokenLoading.value = false
  }
}

async function loadBaseData() {
  try {
    const [cats, userList] = await Promise.all([getCategoryTree(), getUserList()])
    categories.value = cats
    users.value = userList
  } catch {
    /* ignore */
  }
}

function handleSearch() {
  mappingQuery.pageNum = 1
  loadMappings()
}

function handleEventLogSearch() {
  eventLogQuery.pageNum = 1
  loadEventLogs()
}

function handleMappingPageChange(page: number) {
  mappingQuery.pageNum = page
  loadMappings()
}

function handleMappingSizeChange(size: number) {
  mappingQuery.pageSize = size
  mappingQuery.pageNum = 1
  loadMappings()
}

function handleEventLogPageChange(page: number) {
  eventLogQuery.pageNum = page
  loadEventLogs()
}

function handleEventLogSizeChange(size: number) {
  eventLogQuery.pageSize = size
  eventLogQuery.pageNum = 1
  loadEventLogs()
}

function openCreateDialog() {
  isEdit.value = false
  dialogTitle.value = '新增告警规则映射'
  formData.id = undefined
  formData.ruleName = ''
  formData.matchMode = 'EXACT'
  formData.categoryId = 0
  formData.priorityP1 = 'urgent'
  formData.priorityP2 = 'high'
  formData.priorityP3 = 'medium'
  formData.assigneeId = null
  formData.dedupWindowMinutes = 30
  formData.enabled = true
  dialogVisible.value = true
}

function openEditDialog(row: AlertRuleMappingOutput) {
  isEdit.value = true
  dialogTitle.value = '编辑告警规则映射'
  formData.id = row.id
  formData.ruleName = row.ruleName
  formData.matchMode = row.matchMode
  formData.categoryId = row.categoryId
  formData.priorityP1 = row.priorityP1
  formData.priorityP2 = row.priorityP2
  formData.priorityP3 = row.priorityP3
  formData.assigneeId = row.assigneeId
  formData.dedupWindowMinutes = row.dedupWindowMinutes
  formData.enabled = row.enabled
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formData.ruleName) {
    ElMessage.warning('请输入规则名称')
    return
  }
  if (!formData.categoryId) {
    ElMessage.warning('请选择工单分类')
    return
  }

  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      const updateData: AlertRuleMappingUpdateInput = {
        id: formData.id,
        ruleName: formData.ruleName,
        matchMode: formData.matchMode,
        categoryId: formData.categoryId,
        priorityP1: formData.priorityP1,
        priorityP2: formData.priorityP2,
        priorityP3: formData.priorityP3,
        assigneeId: formData.assigneeId,
        dedupWindowMinutes: formData.dedupWindowMinutes,
        enabled: formData.enabled,
      }
      await updateAlertMapping(updateData)
      ElMessage.success('更新成功')
    } else {
      const createData: AlertRuleMappingCreateInput = {
        ruleName: formData.ruleName,
        matchMode: formData.matchMode,
        categoryId: formData.categoryId,
        priorityP1: formData.priorityP1,
        priorityP2: formData.priorityP2,
        priorityP3: formData.priorityP3,
        assigneeId: formData.assigneeId,
        dedupWindowMinutes: formData.dedupWindowMinutes,
        enabled: formData.enabled,
      }
      await createAlertMapping(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadMappings()
  } finally {
    formLoading.value = false
  }
}

async function handleDelete(row: AlertRuleMappingOutput) {
  try {
    await ElMessageBox.confirm(`确认删除规则映射「${row.ruleName}」？`, '确认删除', {
      type: 'warning',
    })
    await deleteAlertMapping(row.id)
    ElMessage.success('删除成功')
    loadMappings()
  } catch {
    /* cancelled */
  }
}

async function handleResetToken() {
  try {
    await ElMessageBox.confirm('重置Token后，需要在夜莺监控平台中更新Webhook地址。确认重置？', '确认重置', {
      type: 'warning',
    })
    tokenLoading.value = true
    tokenInfo.value = await resetAlertToken()
    ElMessage.success('Token已重置')
  } catch {
    /* cancelled */
  } finally {
    tokenLoading.value = false
  }
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function handleTabChange(tab: string) {
  if (tab === 'eventLog' && eventLogList.value.length === 0) {
    loadEventLogs()
  }
  if (tab === 'webhook' && !tokenInfo.value.token) {
    loadToken()
  }
}

onMounted(() => {
  loadBaseData()
  loadMappings()
})
</script>

<template>
  <div class="alert-mapping-view">
    <div class="page-header">
      <h2>告警接入管理</h2>
      <p class="page-desc">将夜莺监控平台的告警通过Webhook回调自动创建工单，实现告警工单化闭环管理</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="规则映射" name="mapping">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="mappingQuery.ruleName"
              placeholder="搜索规则名称"
              clearable
              style="width: 240px"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="mappingQuery.enabled"
              placeholder="状态"
              clearable
              style="width: 120px"
              @change="handleSearch"
            >
              <el-option :value="true" label="已启用" />
              <el-option :value="false" label="已停用" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增映射</el-button>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="mappingList"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column prop="ruleName" label="规则名称" min-width="180" :show-overflow-tooltip="true" />
          <el-table-column prop="matchMode" label="匹配模式" width="100" align="center">
            <template #default="{ row }">
              {{ row.matchMode === 'PREFIX' ? '前缀' : '精确' }}
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="工单分类" width="140" :show-overflow-tooltip="true" />
          <el-table-column label="优先级映射" width="200" align="center">
            <template #default="{ row }">
              <span class="priority-mapping">
                P1→{{ row.priorityP1 }} / P2→{{ row.priorityP2 }} / P3→{{ row.priorityP3 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="assigneeName" label="默认处理人" width="120" align="center">
            <template #default="{ row }">
              {{ row.assigneeName || '自动分派' }}
            </template>
          </el-table-column>
          <el-table-column prop="dedupWindowMinutes" label="去重窗口" width="100" align="center">
            <template #default="{ row }">
              {{ row.dedupWindowMinutes }}分钟
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            :current-page="mappingQuery.pageNum"
            :page-size="mappingQuery.pageSize"
            :total="mappingTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleMappingSizeChange"
            @current-change="handleMappingPageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="事件日志" name="eventLog">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="eventLogQuery.ruleName"
              placeholder="规则名称"
              clearable
              style="width: 180px"
              @keyup.enter="handleEventLogSearch"
            />
            <el-input
              v-model="eventLogQuery.targetIdent"
              placeholder="监控对象"
              clearable
              style="width: 180px"
              @keyup.enter="handleEventLogSearch"
            />
            <el-select
              v-model="eventLogQuery.processResult"
              placeholder="处理结果"
              clearable
              style="width: 140px"
              @change="handleEventLogSearch"
            >
              <el-option
                v-for="opt in processResultOptions"
                :key="opt.value"
                :value="opt.value"
                :label="opt.label"
              />
            </el-select>
            <el-button type="primary" @click="handleEventLogSearch">查询</el-button>
          </div>
        </div>

        <el-table
          v-loading="eventLogLoading"
          :data="eventLogList"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column prop="ruleName" label="规则名称" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column prop="severity" label="级别" width="70" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.severity === 1 ? 'danger' : row.severity === 2 ? 'warning' : 'info'"
                size="small"
              >
                {{ severityLabelMap[row.severity] || 'P3' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetIdent" label="监控对象" min-width="180" :show-overflow-tooltip="true" />
          <el-table-column prop="triggerValue" label="触发值" width="100" align="center" />
          <el-table-column prop="processResult" label="处理结果" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="(processResultTagType[row.processResult] as any) || ''" size="small">
                {{ processResultLabelMap[row.processResult] || row.processResult }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ticketNo" label="关联工单" width="140" align="center">
            <template #default="{ row }">
              <router-link
                v-if="row.ticketId"
                :to="`/ticket/detail/${row.ticketId}`"
                class="ticket-link"
              >
                {{ row.ticketNo || `#${row.ticketId}` }}
              </router-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="告警时间" width="170" align="center">
            <template #default="{ row }">
              {{ row.triggerTime ? formatDateTime(row.triggerTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="接收时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            :current-page="eventLogQuery.pageNum"
            :page-size="eventLogQuery.pageSize"
            :total="eventLogTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleEventLogSizeChange"
            @current-change="handleEventLogPageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Webhook配置" name="webhook">
        <div class="webhook-config-panel">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>Webhook地址</span>
                <el-button type="primary" :icon="Refresh" :loading="tokenLoading" @click="handleResetToken">
                  重新生成
                </el-button>
              </div>
            </template>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="Webhook URL">
                <div class="webhook-url-row">
                  <code class="webhook-url">{{ tokenInfo.webhookUrl || '未生成，请点击「重新生成」' }}</code>
                  <el-button
                    v-if="tokenInfo.webhookUrl"
                    :icon="CopyDocument"
                    size="small"
                    @click="copyToClipboard(tokenInfo.webhookUrl)"
                  >
                    复制
                  </el-button>
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="Token">
                <div class="webhook-url-row">
                  <code>{{ tokenInfo.token || '-' }}</code>
                  <el-button
                    v-if="tokenInfo.token"
                    :icon="CopyDocument"
                    size="small"
                    @click="copyToClipboard(tokenInfo.token)"
                  >
                    复制
                  </el-button>
                </div>
              </el-descriptions-item>
            </el-descriptions>

            <el-alert
              type="info"
              :closable="false"
              style="margin-top: 16px"
            >
              <template #title>
                <strong>配置步骤</strong>
              </template>
              <ol class="setup-steps">
                <li>复制上方Webhook URL</li>
                <li>在夜莺监控平台 → 告警规则 → 回调地址中粘贴该URL</li>
                <li>在本页「规则映射」Tab中配置告警规则与工单分类的对应关系</li>
                <li>告警触发后将自动创建工单，可在「事件日志」Tab中查看处理记录</li>
              </ol>
            </el-alert>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form label-width="120px" label-position="right">
        <el-form-item label="规则名称" required>
          <el-input v-model="formData.ruleName" placeholder="请输入夜莺告警规则名称" />
        </el-form-item>
        <el-form-item label="匹配模式">
          <el-radio-group v-model="formData.matchMode">
            <el-radio
              v-for="opt in matchModeOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="工单分类" required>
          <el-select v-model="formData.categoryId" placeholder="请选择工单分类" filterable style="width: 100%">
            <el-option
              v-for="cat in flatCategories"
              :key="cat.id"
              :value="cat.id"
              :label="cat.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="P1优先级">
          <el-select v-model="formData.priorityP1" style="width: 100%">
            <el-option v-for="opt in priorityOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="P2优先级">
          <el-select v-model="formData.priorityP2" style="width: 100%">
            <el-option v-for="opt in priorityOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="P3优先级">
          <el-select v-model="formData.priorityP3" style="width: 100%">
            <el-option v-for="opt in priorityOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认处理人">
          <el-select v-model="formData.assigneeId" placeholder="不选则自动分派" filterable clearable style="width: 100%">
            <el-option
              v-for="user in users"
              :key="user.id"
              :value="user.id"
              :label="user.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="去重窗口(分钟)">
          <el-input-number v-model="formData.dedupWindowMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.alert-mapping-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 8px;
    font-size: 20px;
    font-weight: 600;
  }

  .page-desc {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .toolbar-left {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.priority-mapping {
  font-size: 12px;
  color: #606266;
}

.ticket-link {
  color: #1675d1;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.webhook-config-panel {
  max-width: 800px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .webhook-url-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .webhook-url {
    word-break: break-all;
    font-size: 13px;
  }

  .setup-steps {
    margin: 8px 0 0;
    padding-left: 20px;
    line-height: 2;
  }
}
</style>
