# task005 · 前端仪表盘重构：行组配置驱动的动态渲染

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task004（store 和 API 已就绪）  
> 预估工时：2h

---

## 目标

将 `DashboardView.vue` 从硬编码布局重构为**由行组配置驱动**的动态渲染，为 task006（拖拽交互）打下基础。

---

## 输出文件清单

```
miduo-frontend/src/
├── views/dashboard/
│   ├── DashboardView.vue                     # 主视图（重构）
│   └── components/
│       ├── DashboardOverviewRow.vue           # 行组：工单概览（6张统计卡）
│       ├── DashboardTrendCategoryRow.vue      # 行组：工单趋势 + 分类分布
│       └── DashboardEfficiencyWorkloadRow.vue # 行组：处理效率与SLA + 人员工作量TOP10
```

---

## 重构思路

### 现有结构（硬编码）

```vue
<!-- DashboardView.vue 现在的结构 -->
<el-row> <!-- 统计卡行 --> </el-row>
<el-row> <!-- 趋势 + 分类行 --> </el-row>
<el-row> <!-- 效率 + 工作量行 --> </el-row>
```

### 目标结构（配置驱动）

```vue
<!-- DashboardView.vue 重构后 -->
<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 固定置顶：overview 行组 -->
    <DashboardOverviewRow :data="overview" />

    <!-- 可排序行组列表（顺序由 store 驱动） -->
    <template v-for="rowGroup in layoutStore.sortedLayout" :key="rowGroup.rowGroupKey">
      <DashboardTrendCategoryRow
        v-if="rowGroup.rowGroupKey === 'trend_category'"
        :trend="trend"
        :categories="categories"
      />
      <DashboardEfficiencyWorkloadRow
        v-else-if="rowGroup.rowGroupKey === 'efficiency_workload'"
        :efficiency="efficiency"
        :slaAchievement="slaAchievement"
        :workload="workload"
      />
    </template>
  </div>
</template>
```

---

## 各文件详细说明

### 1. `DashboardOverviewRow.vue`

抽取现有 `stat-row` 部分的模板代码和样式到独立组件。

**Props**：
- `data: DashboardOverviewOutput`

**功能**：渲染 6 张统计卡（待受理、处理中、已挂起、已完成、SLA超时、工单总量）

### 2. `DashboardTrendCategoryRow.vue`

抽取现有趋势 + 分类的 `el-row` 模板代码。

**Props**：
- `trend: DashboardTrendPointOutput[]`
- `categories: DashboardCategoryDistributionOutput[]`

**功能**：左侧14列渲染趋势进度条，右侧10列渲染分类分布

### 3. `DashboardEfficiencyWorkloadRow.vue`

抽取现有效率 + 工作量的 `el-row` 模板代码。

**Props**：
- `efficiency: DashboardEfficiencyOutput`
- `slaAchievement: DashboardSlaAchievementOutput`
- `workload: DashboardWorkloadOutput[]`

**功能**：左侧10列渲染效率环形图和指标，右侧14列渲染工作量表格

### 4. `DashboardView.vue`（重构主视图）

**职责**：
- 使用 `dashboardLayoutStore`（task004 的 store）
- `onMounted` 时并行调用 `fetchLayout()` 和 `loadDashboard()`（两个可并行，互不依赖）
- 按 `layoutStore.sortedLayout` 中的 `rowGroupKey` 决定渲染哪个行组组件
- 通过 `v-for` + `v-if/v-else-if` 映射行组 Key 到对应组件

**数据请求**：现有 6 个并行接口（API000405–410）保持不变，只是数据通过 props 传入子组件。

---

## 编码规范要求

- 组件嵌套深度 ≤ 2 层（`DashboardView` → 行组组件，行组组件内不再嵌套自定义组件）
- 行组组件只接收 props 数据，不自行调用 API
- `DashboardView.vue` 中 `v-for` 列表不超过 10 个行组（当前 2 个可拖拽行组）
- 所有现有样式（scoped SCSS）迁移到对应子组件，`DashboardView.vue` 只保留全局布局样式

---

## 验收标准

- [ ] 重构后仪表盘页面展示效果与重构前完全一致（视觉回归）
- [ ] 三个行组组件均可独立渲染，props 数据正确传入
- [ ] `sortedLayout` 顺序变化时，`v-for` 重新渲染行组顺序正确
- [ ] 页面不出现新的控制台报错
- [ ] TypeScript 类型检查无报错（`npm run build`）
- [ ] 前端服务可正常启动（`npm run dev`）
