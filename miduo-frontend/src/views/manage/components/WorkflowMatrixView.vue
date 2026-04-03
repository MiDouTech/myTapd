<script setup lang="ts">
import { computed } from 'vue'

import type { WorkflowDetailOutput, WorkflowDetailTransitionItem } from '@/types/workflow'
import { formatRoleLabel } from '@/utils/formatter'

const props = defineProps<{
  detail?: WorkflowDetailOutput
}>()

const stateColumns = computed(() => {
  return [...(props.detail?.states || [])].sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
})

const rowItems = computed(() => {
  return [...(props.detail?.states || [])].sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
})

function getTransition(from: string, to: string): WorkflowDetailTransitionItem | undefined {
  return (props.detail?.transitions || []).find((item) => item.from === from && item.to === to)
}

function formatRoles(roles?: string[]): string {
  if (!roles || roles.length === 0) {
    return '全部角色'
  }
  return roles.map((item) => formatRoleLabel(item)).join(' / ')
}
</script>

<template>
  <div class="workflow-matrix">
    <div class="matrix-tip">
      纵轴为当前状态，横轴为目标状态；存在流转时展示动作名、角色摘要与退回标记。
    </div>
    <div class="matrix-scroll">
      <table class="matrix-table">
        <thead>
          <tr>
            <th class="matrix-header matrix-corner">当前 \ 目标</th>
            <th v-for="column in stateColumns" :key="column.code" class="matrix-header">
              <div class="matrix-state-name">{{ column.name }}</div>
              <div class="matrix-state-code">{{ column.code }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rowItems" :key="row.code">
            <th class="matrix-row-header">
              <div class="matrix-state-name">{{ row.name }}</div>
              <div class="matrix-state-code">{{ row.code }}</div>
            </th>
            <td v-for="column in stateColumns" :key="`${row.code}-${column.code}`" class="matrix-cell">
              <template v-if="getTransition(row.code, column.code)">
                <div
                  :class="[
                    'matrix-transition-card',
                    { 'matrix-transition-card--return': getTransition(row.code, column.code)?.isReturn },
                  ]"
                >
                  <div class="matrix-transition-name">
                    {{ getTransition(row.code, column.code)?.name || '-' }}
                  </div>
                  <div class="matrix-transition-roles">
                    {{ formatRoles(getTransition(row.code, column.code)?.allowedRoles) }}
                  </div>
                  <div class="matrix-transition-flags">
                    <el-tag
                      v-if="getTransition(row.code, column.code)?.isReturn"
                      size="small"
                      type="warning"
                    >
                      退回
                    </el-tag>
                    <el-tag
                      v-if="getTransition(row.code, column.code)?.requireRemark"
                      size="small"
                      type="info"
                    >
                      备注必填
                    </el-tag>
                    <el-tag
                      v-if="getTransition(row.code, column.code)?.allowTransfer"
                      size="small"
                      type="success"
                    >
                      可转派
                    </el-tag>
                  </div>
                </div>
              </template>
              <span v-else class="matrix-empty">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.workflow-matrix {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.matrix-tip {
  font-size: 13px;
  color: #4e5969;
}

.matrix-scroll {
  overflow: auto;
}

.matrix-table {
  width: 100%;
  min-width: 900px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

.matrix-header,
.matrix-row-header {
  background: #f5f7fa;
  color: #1d2129;
  font-weight: 600;
}

.matrix-header,
.matrix-row-header,
.matrix-cell {
  border: 1px solid #e5e6eb;
  padding: 10px;
  vertical-align: top;
}

.matrix-corner {
  width: 140px;
}

.matrix-row-header {
  width: 140px;
  text-align: left;
}

.matrix-state-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}

.matrix-state-code {
  font-size: 12px;
  color: #86909c;
  margin-top: 4px;
  word-break: break-all;
}

.matrix-cell {
  background: #fff;
}

.matrix-transition-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 72px;
  padding: 8px;
  border-radius: 8px;
  background: #f7fbff;
  border: 1px solid rgba(22, 117, 209, 0.18);
}

.matrix-transition-card--return {
  background: #fff9f0;
  border-color: rgba(255, 153, 0, 0.3);
}

.matrix-transition-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}

.matrix-transition-roles {
  font-size: 12px;
  color: #4e5969;
  line-height: 18px;
}

.matrix-transition-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.matrix-empty {
  color: #c9cdd4;
}
</style>
