<script setup lang="ts">
import { computed } from 'vue'

import type { WorkflowDetailOutput, WorkflowDetailStateItem, WorkflowDetailTransitionItem } from '@/types/workflow'

interface GraphNode extends WorkflowDetailStateItem {
  x: number
  y: number
  level: number
}

interface GraphEdge extends WorkflowDetailTransitionItem {
  fromNode?: GraphNode
  toNode?: GraphNode
}

const props = withDefaults(
  defineProps<{
    detail?: WorkflowDetailOutput
    selectedNodeCode?: string
    selectedTransitionId?: string
  }>(),
  {
    detail: undefined,
    selectedNodeCode: undefined,
    selectedTransitionId: undefined,
  },
)

const emit = defineEmits<{
  selectNode: [node: WorkflowDetailStateItem]
  selectTransition: [transition: WorkflowDetailTransitionItem]
}>()

const NODE_WIDTH = 168
const NODE_HEIGHT = 72
const H_GAP = 90
const V_GAP = 34
const PADDING_X = 40
const PADDING_Y = 36

/**
 * 用状态上的 order（产品定义的主线顺序）映射到列，避免对含环图做「最长路径」松弛导致卡死或列被拉爆。
 * 回边、挂起环、终态提前关闭等由现有贝塞尔路径按左右关系绘制。
 */
function buildOrderBasedLevels(states: WorkflowDetailStateItem[]): Map<string, number> {
  const levelMap = new Map<string, number>()
  if (states.length === 0) {
    return levelMap
  }
  const maxDefined = states.reduce((m, s) => {
    if (typeof s.order === 'number' && Number.isFinite(s.order)) {
      return Math.max(m, s.order)
    }
    return m
  }, 0)
  const fallbackStart = maxDefined > 0 ? maxDefined + 1 : states.length + 1
  const effectiveOrders = states.map((s, i) => {
    if (typeof s.order === 'number' && Number.isFinite(s.order)) {
      return s.order
    }
    return fallbackStart + i
  })
  const uniqueSorted = [...new Set(effectiveOrders)].sort((a, b) => a - b)
  const orderToColumn = new Map<number, number>()
  uniqueSorted.forEach((ord, col) => orderToColumn.set(ord, col))
  states.forEach((s, i) => {
    const ord = effectiveOrders[i]
    levelMap.set(s.code, orderToColumn.get(ord) ?? 0)
  })
  return levelMap
}

const graph = computed(() => {
  const states = [...(props.detail?.states || [])].sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  const transitions = props.detail?.transitions || []
  const nodeMap = new Map<string, GraphNode>()

  states.forEach((state) => incoming.set(state.code, 0))
  transitions.forEach((transition) => {
    incoming.set(transition.to, (incoming.get(transition.to) || 0) + 1)
  })

  const initialStates = states.filter((item) => item.type === 'INITIAL')
  const fallbackRoots = states.filter((item) => (incoming.get(item.code) || 0) === 0)
  const roots = (initialStates.length > 0 ? initialStates : fallbackRoots).length > 0
    ? (initialStates.length > 0 ? initialStates : fallbackRoots)
    : states.slice(0, 1)

  const levelMap = new Map<string, number>()
  const queue: string[] = roots.map((item) => item.code)
  roots.forEach((item) => levelMap.set(item.code, 0))

  // 最长简单路径层级不会超过状态数；工作流定义里若存在环路（非退回边），原逻辑会无限抬高 level 并入队，导致页面卡死。
  const maxLevel = Math.max(states.length, 1)
  const maxIterations = Math.max(
    10000,
    states.length * states.length * 2 + transitions.length * 4,
  )
  let iterations = 0

  while (queue.length > 0 && iterations < maxIterations) {
    iterations += 1
    const code = queue.shift() as string
    const currentLevel = levelMap.get(code) ?? 0
    transitions
      .filter((transition) => transition.from === code && !transition.isReturn)
      .forEach((transition) => {
        const nextLevel = Math.min(currentLevel + 1, maxLevel)
        const prevLevel = levelMap.get(transition.to)
        if (prevLevel === undefined || nextLevel > prevLevel) {
          levelMap.set(transition.to, nextLevel)
          queue.push(transition.to)
        }
      })
  }

  states.forEach((state, index) => {
    if (!levelMap.has(state.code)) {
      levelMap.set(state.code, Math.min(index, 3))
    }
  })

  const groups = new Map<number, WorkflowDetailStateItem[]>()
  states.forEach((state) => {
    const level = levelMap.get(state.code) ?? 0
    const group = groups.get(level) || []
    group.push(state)
    groups.set(level, group)
  })

  const sortedLevels = [...groups.keys()].sort((a, b) => a - b)
  sortedLevels.forEach((level) => {
    const group = groups.get(level) || []
    group.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
    group.forEach((state, index) => {
      const node: GraphNode = {
        ...state,
        level,
        x: PADDING_X + level * (NODE_WIDTH + H_GAP),
        y: PADDING_Y + index * (NODE_HEIGHT + V_GAP),
      }
      nodeMap.set(state.code, node)
    })
  })

  const nodes = states.map((state) => nodeMap.get(state.code) as GraphNode).filter(Boolean)
  const edges: GraphEdge[] = transitions.map((transition) => ({
    ...transition,
    fromNode: nodeMap.get(transition.from),
    toNode: nodeMap.get(transition.to),
  }))

  const maxX = nodes.reduce((max, node) => Math.max(max, node.x), 0)
  const maxY = nodes.reduce((max, node) => Math.max(max, node.y), 0)
  const width = Math.max(maxX + NODE_WIDTH + PADDING_X, 640)
  const height = Math.max(maxY + NODE_HEIGHT + PADDING_Y, 320)

  return { nodes, edges, width, height }
})

function buildEdgePath(edge: GraphEdge): string {
  if (!edge.fromNode || !edge.toNode) {
    return ''
  }
  const startX = edge.fromNode.x + NODE_WIDTH
  const startY = edge.fromNode.y + NODE_HEIGHT / 2
  const endX = edge.toNode.x
  const endY = edge.toNode.y + NODE_HEIGHT / 2

  if (edge.isReturn || startX > endX) {
    const midX = Math.max(endX - 40, startX - 60)
    return `M ${startX} ${startY} C ${midX} ${startY}, ${midX} ${endY}, ${endX} ${endY}`
  }

  const midX = (startX + endX) / 2
  return `M ${startX} ${startY} C ${midX} ${startY}, ${midX} ${endY}, ${endX} ${endY}`
}

function getNodeClass(node: GraphNode): string[] {
  const result = ['workflow-node']
  if (node.type === 'INITIAL') result.push('workflow-node--initial')
  if (node.type === 'TERMINAL') result.push('workflow-node--terminal')
  if (props.selectedNodeCode === node.code) result.push('workflow-node--selected')
  return result
}

function getEdgeClass(edge: GraphEdge): string[] {
  const result = ['workflow-edge']
  if (edge.isReturn) result.push('workflow-edge--return')
  if (props.selectedTransitionId && edge.id === props.selectedTransitionId) {
    result.push('workflow-edge--selected')
  }
  return result
}
</script>

<template>
  <div class="workflow-graph-card">
    <div class="workflow-graph-toolbar">
      <div class="workflow-graph-legend">
        <span><i class="legend-dot legend-dot--initial" /> 初始</span>
        <span><i class="legend-dot legend-dot--intermediate" /> 中间</span>
        <span><i class="legend-dot legend-dot--terminal" /> 终态</span>
        <span><i class="legend-line legend-line--normal" /> 正常流转</span>
        <span><i class="legend-line legend-line--return" /> 退回流转</span>
      </div>
    </div>

    <div class="workflow-graph-scroll">
      <svg
        v-if="graph.nodes.length > 0"
        class="workflow-graph-svg"
        :viewBox="`0 0 ${graph.width} ${graph.height}`"
        :style="{ minWidth: `${graph.width}px`, minHeight: `${graph.height}px` }"
      >
        <defs>
          <marker
            id="workflow-arrow"
            markerWidth="8"
            markerHeight="8"
            refX="7"
            refY="4"
            orient="auto"
            markerUnits="strokeWidth"
          >
            <path d="M 0 0 L 8 4 L 0 8 z" fill="#7a8599" />
          </marker>
          <marker
            id="workflow-arrow-active"
            markerWidth="8"
            markerHeight="8"
            refX="7"
            refY="4"
            orient="auto"
            markerUnits="strokeWidth"
          >
            <path d="M 0 0 L 8 4 L 0 8 z" fill="#1675d1" />
          </marker>
          <marker
            id="workflow-arrow-return"
            markerWidth="8"
            markerHeight="8"
            refX="7"
            refY="4"
            orient="auto"
            markerUnits="strokeWidth"
          >
            <path d="M 0 0 L 8 4 L 0 8 z" fill="#d97706" />
          </marker>
        </defs>

        <g>
          <template v-for="edge in graph.edges" :key="edge.id || `${edge.from}-${edge.to}-${edge.name}`">
            <path
              :d="buildEdgePath(edge)"
              :class="getEdgeClass(edge)"
              :marker-end="
                edge.isReturn
                  ? 'url(#workflow-arrow-return)'
                  : selectedTransitionId && edge.id === selectedTransitionId
                    ? 'url(#workflow-arrow-active)'
                    : 'url(#workflow-arrow)'
              "
              @click="emit('selectTransition', edge)"
            />
            <text
              v-if="edge.fromNode && edge.toNode"
              class="workflow-edge-label"
              :x="(edge.fromNode.x + NODE_WIDTH + edge.toNode.x) / 2"
              :y="(edge.fromNode.y + NODE_HEIGHT / 2 + edge.toNode.y + NODE_HEIGHT / 2) / 2 - 8"
            >
              {{ edge.name || `${edge.fromName || edge.from}→${edge.toName || edge.to}` }}
            </text>
          </template>
        </g>

        <g>
          <template v-for="node in graph.nodes" :key="node.code">
            <foreignObject :x="node.x" :y="node.y" :width="NODE_WIDTH" :height="NODE_HEIGHT">
              <button type="button" :class="getNodeClass(node)" @click="emit('selectNode', node)">
                <div class="workflow-node__title">{{ node.name }}</div>
                <div class="workflow-node__meta">{{ node.code }}</div>
                <div class="workflow-node__footer">
                  <span>{{ node.type || '-' }}</span>
                  <span>{{ node.slaAction || '无 SLA' }}</span>
                </div>
              </button>
            </foreignObject>
          </template>
        </g>
      </svg>

      <el-empty v-else description="暂无可视化流程数据" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.workflow-graph-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workflow-graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.workflow-graph-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 16px;
  color: #4e5969;
  font-size: 13px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
  background: #c9cdd4;
}

.legend-dot--initial {
  background: #1675d1;
}

.legend-dot--terminal {
  background: #16a34a;
}

.legend-line {
  display: inline-block;
  width: 18px;
  border-top: 2px solid #7a8599;
  margin-right: 6px;
  vertical-align: middle;
}

.legend-line--return {
  border-top-style: dashed;
  border-top-color: #d97706;
}

.workflow-graph-scroll {
  overflow: auto;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  background: linear-gradient(180deg, #fcfdff 0%, #f8fafc 100%);
}

.workflow-graph-svg {
  display: block;
}

.workflow-edge {
  fill: none;
  stroke: #7a8599;
  stroke-width: 2;
  cursor: pointer;
}

.workflow-edge--selected {
  stroke: #1675d1;
  stroke-width: 3;
}

.workflow-edge--return {
  stroke: #d97706;
  stroke-dasharray: 6 5;
}

.workflow-edge-label {
  fill: #4e5969;
  font-size: 12px;
  text-anchor: middle;
  pointer-events: none;
}

.workflow-node {
  width: 100%;
  height: 100%;
  border: 1px solid #d0d5dd;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.workflow-node--initial {
  border-color: #1675d1;
  background: #eff6ff;
}

.workflow-node--terminal {
  border-color: #16a34a;
  background: #f0fdf4;
}

.workflow-node--selected {
  box-shadow: 0 0 0 2px rgba(22, 117, 209, 0.22);
}

.workflow-node__title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  line-height: 20px;
}

.workflow-node__meta {
  font-size: 12px;
  color: #86909c;
  line-height: 16px;
}

.workflow-node__footer {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #4e5969;
}
</style>
