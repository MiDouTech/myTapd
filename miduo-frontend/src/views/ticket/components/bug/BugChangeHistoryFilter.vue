<template>
  <div class="bug-change-history-filter">
    <div class="filter-item">
      <span class="filter-label">变更内容：</span>
      <el-select v-model="localFieldName" placeholder="全部" clearable size="small" @change="emitChange" style="width: 130px">
        <el-option label="全部" value="" />
        <el-option label="状态" value="status" />
        <el-option label="处理人" value="assignee_id" />
        <el-option label="缺陷等级" value="severity_level" />
        <el-option label="有效报告" value="is_valid_report" />
        <el-option label="缺陷划分" value="defect_category" />
        <el-option label="公司名称" value="company_name" />
        <el-option label="商户编号" value="merchant_no" />
        <el-option label="根因分析" value="root_cause" />
        <el-option label="复现步骤" value="reproduce_steps" />
      </el-select>
    </div>
    <div class="filter-item">
      <span class="filter-label">变更方式：</span>
      <el-select v-model="localChangeType" placeholder="全部" clearable size="small" @change="emitChange" style="width: 120px">
        <el-option label="全部" value="" />
        <el-option label="创建缺陷" value="CREATE" />
        <el-option label="手动变更" value="MANUAL_CHANGE" />
        <el-option label="状态流转" value="STATUS_CHANGE" />
        <el-option label="系统自动" value="SYSTEM_AUTO" />
        <el-option label="添加评论" value="COMMENT" />
        <el-option label="附件操作" value="ATTACHMENT" />
      </el-select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  change: [changeType: string, fieldName: string]
}>()

const localChangeType = ref('')
const localFieldName = ref('')

function emitChange(): void {
  emit('change', localChangeType.value, localFieldName.value)
}
</script>

<style scoped>
.bug-change-history-filter {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0 12px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
</style>
