<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  mode: 'temp' | 'complete' | 'unknown' | 'none'
  disabled: boolean
  ticketStatusLoading: boolean
  tempResolveDate: string
  tempSolution: string
  resolveDate: string
  solution: string
  resolveTime: string
  /** 窄屏折叠内用稍大的最小行高 */
  compact?: boolean
}>()

const emit = defineEmits<{
  'update:tempResolveDate': [value: string]
  'update:tempSolution': [value: string]
  'update:resolveDate': [value: string]
  'update:solution': [value: string]
  'update:resolveTime': [value: string]
}>()

const textareaRows = computed(() =>
  props.compact ? { temp: 4, perm: 5 } : { temp: 3, perm: 4 },
)
</script>

<template>
  <template v-if="mode === 'temp'">
    <el-form-item label="临时解决时间" required>
      <el-date-picker
        :model-value="tempResolveDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="请选择临时解决日期"
        :disabled="disabled"
        @update:model-value="emit('update:tempResolveDate', $event ?? '')"
      />
    </el-form-item>
    <el-form-item label="临时解决方案" required>
      <el-input
        :model-value="tempSolution"
        type="textarea"
        :autosize="{ minRows: textareaRows.temp, maxRows: 18 }"
        maxlength="1000"
        show-word-limit
        placeholder="请输入临时解决方案（权宜之计）"
        class="textarea-autosize"
        :disabled="disabled"
        @update:model-value="emit('update:tempSolution', $event)"
      />
    </el-form-item>
    <el-form-item label="彻底解决时间" required>
      <el-date-picker
        :model-value="resolveDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="请选择计划/实际彻底解决日期"
        :disabled="disabled"
        @update:model-value="emit('update:resolveDate', $event ?? '')"
      />
    </el-form-item>
    <el-form-item label="彻底解决方案" required>
      <el-input
        :model-value="solution"
        type="textarea"
        :autosize="{ minRows: textareaRows.perm, maxRows: 22 }"
        maxlength="1000"
        show-word-limit
        placeholder="请输入彻底解决方案（根本性修复）"
        class="textarea-autosize"
        :disabled="disabled"
        @update:model-value="emit('update:solution', $event)"
      />
    </el-form-item>
  </template>

  <el-form-item v-else-if="mode === 'complete'" label="解决时间" required>
    <el-date-picker
      :model-value="resolveTime"
      type="datetime"
      value-format="YYYY-MM-DDTHH:mm:ss"
      placeholder="请选择处理完成时间"
      :disabled="disabled"
      style="width: 100%"
      @update:model-value="emit('update:resolveTime', $event ?? '')"
    />
    <div class="resolution-hint">关联工单已处理完成：此处填写解决时间即可，无需填写临时/彻底方案。</div>
  </el-form-item>

  <el-alert
    v-else-if="mode === 'none'"
    type="info"
    :closable="false"
    show-icon
    class="resolution-alert"
    title="请先关联至少一个工单，系统将根据工单状态提示应填写的解决信息。"
  />

  <el-alert
    v-else
    type="warning"
    :closable="false"
    show-icon
    class="resolution-alert"
    :title="
      ticketStatusLoading
        ? '正在读取关联工单状态…'
        : '关联工单状态未处于「临时解决」或「已完成」，请完成缺陷流转后再填写对应简报字段。'
    "
  />
</template>

<style scoped lang="scss">
.resolution-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.resolution-alert {
  margin-bottom: 8px;
}
</style>
