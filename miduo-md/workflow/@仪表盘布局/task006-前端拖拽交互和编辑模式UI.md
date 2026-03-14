# task006 · 前端拖拽交互 + 编辑模式 UI

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task005（动态渲染重构完成）  
> 预估工时：2h

---

## 目标

基于 `vuedraggable@next` 实现仪表盘行组的拖拽排序功能，完成编辑模式的完整 UI 交互：进入/退出、拖拽、保存、取消、恢复默认。

---

## 依赖安装

```bash
cd miduo-frontend
npm install vuedraggable@next
```

---

## 输出文件

```
miduo-frontend/src/views/dashboard/
└── DashboardView.vue    # 在 task005 基础上增加拖拽 + 编辑模式逻辑
```

---

## 功能实现要求

### 1. 编辑模式切换

**"编辑布局"按钮**（查看模式显示）：
- 位置：仪表盘页面右上角（与页面标题同行，靠右对齐）
- 样式：`el-button type="primary" plain size="small"`，图标使用 `Edit` icon
- 点击：调用 `layoutStore.enterEditMode()`

**编辑模式下的操作区**（编辑模式显示）：
- 替换"编辑布局"按钮，显示三个控件：
  - "恢复默认"：文字链样式（`el-button type="default" link`），左侧
  - "取消"：`el-button size="small"`，中间
  - "保存布局"：`el-button type="primary" size="small" :loading="layoutStore.saving"`，右侧

**提示条**（编辑模式显示，查看模式隐藏）：
- `el-alert type="info" :closable="false" show-icon`
- 文字："拖拽模块可调整顺序，仅对您自己生效"
- 位置：操作按钮区下方，统计卡行上方

### 2. 拖拽排序实现

使用 `vuedraggable` 包裹**可拖拽行组列表**（`overview` 行组固定置顶，不参与拖拽）：

```vue
<template>
  <!-- 固定置顶，不参与拖拽 -->
  <DashboardOverviewRow :data="overview" />

  <!-- 编辑模式：vuedraggable 包裹 -->
  <VueDraggable
    v-if="layoutStore.isEditMode"
    v-model="draggableRows"
    item-key="rowGroupKey"
    handle=".drag-handle"
    animation="200"
    ghost-class="drag-ghost"
    @end="onDragEnd"
  >
    <template #item="{ element }">
      <div class="draggable-row-wrapper" :class="{ 'edit-mode': layoutStore.isEditMode }">
        <div class="drag-handle"><el-icon><Grid /></el-icon></div>
        <DashboardTrendCategoryRow v-if="element.rowGroupKey === 'trend_category'" ... />
        <DashboardEfficiencyWorkloadRow v-else-if="element.rowGroupKey === 'efficiency_workload'" ... />
      </div>
    </template>
  </VueDraggable>

  <!-- 查看模式：普通 v-for -->
  <template v-else v-for="rowGroup in layoutStore.draggableLayout" :key="rowGroup.rowGroupKey">
    <DashboardTrendCategoryRow v-if="rowGroup.rowGroupKey === 'trend_category'" ... />
    <DashboardEfficiencyWorkloadRow v-else-if="rowGroup.rowGroupKey === 'efficiency_workload'" ... />
  </template>
</template>
```

**`draggableRows`**：计算属性，等同于 `layoutStore.draggableLayout`（`editingLayout` 中 `isFixed=false` 的部分），双向绑定给 `v-model`。

**`onDragEnd()`**：拖拽结束后，根据新的 `draggableRows` 顺序重新计算 `sortOrder`（从 1 开始递增），然后调用 `layoutStore.updateEditingOrder(newList)`。

### 3. 按钮行为实现

| 按钮 | 行为 |
|------|------|
| 编辑布局 | `layoutStore.enterEditMode()` |
| 取消 | `layoutStore.cancelEditMode()`，无需确认 |
| 保存布局 | 调用 `layoutStore.saveLayout()`；成功后 `ElMessage.success('布局已保存')`；失败后 `ElMessage.error(errorMsg)` |
| 恢复默认 | `ElMessageBox.confirm` 弹出确认框，文案："确认恢复为系统默认布局？您当前的自定义布局将被清除。"；确认后调用 `layoutStore.resetLayout()`；成功后 `ElMessage.success('已恢复默认布局')` |

### 4. 编辑态样式

可拖拽行组包装层（`.draggable-row-wrapper.edit-mode`）：
- 边框：`1px dashed #1675d1`
- 圆角：`8px`
- 背景：`rgba(22, 117, 209, 0.02)`
- `margin-bottom: 16px`

拖拽把手（`.drag-handle`）：
- 位置：行组卡片左上角，绝对定位或 flex 布局
- 图标：`<el-icon><Grid /></el-icon>`（☰ 效果）
- 颜色：`#909399`，悬停变 `#1675d1`
- 鼠标样式：`cursor: grab`，按住时 `cursor: grabbing`

拖拽幽灵（`.drag-ghost`）：
- `opacity: 0.4`
- `background: #e8f4ff`
- `border: 2px solid #1675d1`

---

## 移动端处理

```scss
@media (max-width: 767px) {
  .edit-layout-btn,
  .edit-mode-actions,
  .edit-tip {
    display: none !important;
  }
}
```

---

## 验收标准

- [ ] 任意登录用户可见"编辑布局"按钮
- [ ] 点击"编辑布局"后：提示条出现，行组出现虚线边框和拖拽把手，操作按钮变为"恢复默认/取消/保存布局"
- [ ] 可正常拖拽行组（`trend_category` 和 `efficiency_workload` 可互换位置）
- [ ] `overview` 行组不可拖拽，始终置顶
- [ ] 拖拽过程中有视觉反馈（ghost 半透明）
- [ ] "取消"后布局还原为进入编辑前顺序，无接口调用
- [ ] "保存布局"成功后退出编辑模式，刷新页面布局保持
- [ ] "恢复默认"弹出确认框，确认后布局恢复系统默认顺序
- [ ] 移动端（宽度 < 768px）不显示编辑入口
- [ ] `npm run build` 无错误
