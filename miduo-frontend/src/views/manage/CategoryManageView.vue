<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { createCategory, getCategoryDetail, getCategoryTree, updateCategory } from '@/api/category'
import type { CategoryCreateInput, CategoryTreeOutput, CategoryUpdateInput } from '@/types/category'
import { notifySuccess, notifyWarning } from '@/utils/feedback'

const treeLoading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const categoryTree = ref<CategoryTreeOutput[]>([])
const selectedCategoryId = ref<number>()
const createDialogVisible = ref(false)

const editForm = reactive<CategoryUpdateInput>({
  name: '',
  sortOrder: 0,
  isActive: 1,
})

const createForm = reactive<CategoryCreateInput>({
  name: '',
  parentId: undefined,
  level: 1,
  sortOrder: 0,
})

const flatCategoryOptions = computed(() => {
  const result: Array<{ label: string; value: number }> = []
  const walk = (nodes: CategoryTreeOutput[], prefix = ''): void => {
    nodes.forEach((node) => {
      const label = prefix ? `${prefix} / ${node.name}` : node.name
      result.push({ label, value: node.id })
      if (node.children?.length) {
        walk(node.children, label)
      }
    })
  }
  walk(categoryTree.value)
  return result
})

function findNodeById(
  nodes: CategoryTreeOutput[],
  categoryId: number,
): CategoryTreeOutput | undefined {
  for (const node of nodes) {
    if (node.id === categoryId) {
      return node
    }
    if (node.children?.length) {
      const found = findNodeById(node.children, categoryId)
      if (found) {
        return found
      }
    }
  }
  return undefined
}

async function loadCategoryTree(): Promise<void> {
  treeLoading.value = true
  try {
    categoryTree.value = await getCategoryTree()
  } finally {
    treeLoading.value = false
  }
}

async function loadCategoryDetail(categoryId: number): Promise<void> {
  detailLoading.value = true
  try {
    const detail = await getCategoryDetail(categoryId)
    editForm.name = detail.name
    editForm.templateId = detail.templateId
    editForm.workflowId = detail.workflowId
    editForm.slaPolicyId = detail.slaPolicyId
    editForm.defaultGroupId = detail.defaultGroupId
    editForm.sortOrder = detail.sortOrder
    editForm.isActive = detail.isActive
  } finally {
    detailLoading.value = false
  }
}

function handleTreeNodeClick(node: CategoryTreeOutput): void {
  selectedCategoryId.value = node.id
  loadCategoryDetail(node.id)
}

function openCreateDialog(parent?: CategoryTreeOutput): void {
  createForm.name = ''
  createForm.parentId = undefined
  createForm.level = 1
  createForm.templateId = undefined
  createForm.workflowId = undefined
  createForm.slaPolicyId = undefined
  createForm.defaultGroupId = undefined
  createForm.sortOrder = 0
  if (parent) {
    createForm.parentId = parent.id
    createForm.level = Math.min(parent.level + 1, 3)
  }
  createDialogVisible.value = true
}

async function handleCreateCategory(): Promise<void> {
  if (!createForm.name) {
    notifyWarning('请输入分类名称')
    return
  }
  submitLoading.value = true
  try {
    await createCategory(createForm)
    notifySuccess('分类创建成功')
    createDialogVisible.value = false
    await loadCategoryTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleUpdateCategory(): Promise<void> {
  if (!selectedCategoryId.value) {
    notifyWarning('请先选择分类节点')
    return
  }
  submitLoading.value = true
  try {
    await updateCategory(selectedCategoryId.value, editForm)
    notifySuccess('分类更新成功')
    await loadCategoryTree()
    await loadCategoryDetail(selectedCategoryId.value)
  } finally {
    submitLoading.value = false
  }
}

function handleDeletePlaceholder(): void {
  notifyWarning('后端暂未提供分类删除接口，已预留前端入口。')
}

onMounted(async () => {
  await loadCategoryTree()
})
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="8">
      <el-card shadow="never" v-loading="treeLoading">
        <template #header>
          <div class="card-header">
            <span>分类树</span>
            <el-button type="primary" link @click="openCreateDialog()">新增一级</el-button>
          </div>
        </template>
        <el-tree
          :data="categoryTree"
          node-key="id"
          default-expand-all
          highlight-current
          :props="{ label: 'name', children: 'children' }"
          @node-click="handleTreeNodeClick"
        />
      </el-card>
    </el-col>

    <el-col :xs="24" :md="16">
      <el-card shadow="never" v-loading="detailLoading">
        <template #header>
          <div class="card-header">
            <span>分类详情维护</span>
            <el-space>
              <el-button
                type="primary"
                link
                :disabled="!selectedCategoryId"
                @click="
                  openCreateDialog(
                    selectedCategoryId ? findNodeById(categoryTree, selectedCategoryId) : undefined,
                  )
                "
              >
                新增子级
              </el-button>
              <el-button
                type="danger"
                link
                :disabled="!selectedCategoryId"
                @click="handleDeletePlaceholder"
              >
                删除（预留）
              </el-button>
            </el-space>
          </div>
        </template>
        <el-form label-width="110px" class="edit-form">
          <el-form-item label="分类名称">
            <el-input v-model="editForm.name" placeholder="请输入分类名称" />
          </el-form-item>
          <el-form-item label="模板ID">
            <el-input-number v-model="editForm.templateId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="工作流ID">
            <el-input-number v-model="editForm.workflowId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="SLA策略ID">
            <el-input-number v-model="editForm.slaPolicyId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="默认处理组ID">
            <el-input-number v-model="editForm.defaultGroupId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="editForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="editForm.isActive">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitLoading" @click="handleUpdateCategory">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="createDialogVisible" title="新增分类" width="520px">
    <el-form label-width="100px">
      <el-form-item label="分类名称" required>
        <el-input v-model="createForm.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="父级分类">
        <el-select v-model="createForm.parentId" clearable placeholder="请选择父级分类">
          <el-option
            v-for="option in flatCategoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="层级" required>
        <el-input-number v-model="createForm.level" :min="1" :max="3" controls-position="right" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="createForm.sortOrder" :min="0" controls-position="right" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleCreateCategory"
        >确认创建</el-button
      >
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edit-form {
  max-width: 560px;
}
</style>
