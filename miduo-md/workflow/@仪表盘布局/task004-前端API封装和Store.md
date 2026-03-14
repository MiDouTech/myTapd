# task004 · 前端 API 封装 + dashboardLayoutStore

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task003（后端接口已可联调）  
> 预估工时：1h

---

## 目标

封装三个布局接口的前端调用函数，并建立 Pinia store 统一管理布局状态，供仪表盘组件使用。

---

## 输出文件清单

```
miduo-frontend/src/
├── types/dashboardLayout.ts          # 布局相关 TypeScript 类型定义
├── api/dashboardLayout.ts            # 三个布局接口的封装函数
└── stores/dashboardLayout.ts         # Pinia store：dashboardLayoutStore
```

---

## 各文件详细说明

### 1. `types/dashboardLayout.ts`

定义以下类型：

```typescript
// 行组 Key 枚举（与后端 DashboardRowGroupEnum 对应）
export type DashboardRowGroupKey = 'overview' | 'trend_category' | 'efficiency_workload'

// 单条布局配置项（GET 接口响应）
export interface DashboardLayoutItem {
  rowGroupKey: DashboardRowGroupKey
  sortOrder: number
  isFixed: boolean
}

// 保存布局请求体
export interface DashboardLayoutSaveInput {
  layouts: Array<{
    rowGroupKey: DashboardRowGroupKey
    sortOrder: number
  }>
}
```

### 2. `api/dashboardLayout.ts`

封装三个接口，注释中填写接口编号：

```typescript
/**
 * 获取当前用户个人仪表盘布局配置
 * 接口编号：API000411
 * 产品文档功能：仪表盘个性化布局 §3.1
 */
export function getDashboardLayout(): Promise<DashboardLayoutItem[]>

/**
 * 保存当前用户个人仪表盘布局配置
 * 接口编号：API000412
 * 产品文档功能：仪表盘个性化布局 §3.4
 */
export function saveDashboardLayout(data: DashboardLayoutSaveInput): Promise<void>

/**
 * 恢复当前用户仪表盘为系统默认布局
 * 接口编号：API000413
 * 产品文档功能：仪表盘个性化布局 §3.6
 */
export function resetDashboardLayout(): Promise<void>
```

### 3. `stores/dashboardLayout.ts`（Pinia Store）

Store 名称：`dashboardLayout`

**State 字段**：
- `layout: DashboardLayoutItem[]`：当前用户布局列表（从后端获取）
- `isEditMode: boolean`：是否处于编辑模式，默认 `false`
- `editingLayout: DashboardLayoutItem[]`：编辑中的临时布局（拖拽操作修改此数组，不影响 `layout`）
- `loading: boolean`：布局接口 loading 状态
- `saving: boolean`：保存接口 loading 状态

**Actions**：

| Action | 说明 |
|--------|------|
| `fetchLayout()` | 调用 API000411，成功后赋值 `layout`；失败时 `layout` 保持默认 fallback |
| `enterEditMode()` | `isEditMode = true`；将 `layout` 深拷贝到 `editingLayout`（保存进入时快照） |
| `cancelEditMode()` | `isEditMode = false`；将 `editingLayout` 恢复为快照（丢弃拖拽结果） |
| `saveLayout()` | 调用 API000412（提交 `editingLayout` 数据）；成功后用 `editingLayout` 更新 `layout`，`isEditMode = false` |
| `resetLayout()` | 调用 API000413；成功后重新调用 `fetchLayout()` 获取默认布局，`isEditMode = false` |
| `updateEditingOrder(newList)` | 拖拽完成后更新 `editingLayout`（同时重新计算 `sortOrder`） |

**Getters**：
- `sortedLayout`：返回当前展示用布局（编辑模式下为 `editingLayout`，否则为 `layout`），按 `sortOrder` 升序
- `draggableLayout`：从 `sortedLayout` 中过滤出 `isFixed=false` 的行组（供拖拽列表使用）

**Fallback 常量**（与后端 `DashboardLayoutConstants` 保持一致）：

```typescript
const DEFAULT_LAYOUT: DashboardLayoutItem[] = [
  { rowGroupKey: 'overview',              sortOrder: 0, isFixed: true  },
  { rowGroupKey: 'trend_category',        sortOrder: 1, isFixed: false },
  { rowGroupKey: 'efficiency_workload',   sortOrder: 2, isFixed: false },
]
```

`fetchLayout()` 失败时用此常量赋值 `layout`，保证仪表盘不白屏。

---

## 编码规范要求

- 使用 `defineStore` + Composition API 写法（与现有 `auth.ts`、`notification.ts` 一致）
- 不在 store 中直接操作 DOM 或组件逻辑
- `editingLayout` 的深拷贝使用 `JSON.parse(JSON.stringify(...))` 或 `structuredClone`
- 接口注释中的接口编号必须填写

---

## 验收标准

- [ ] `getDashboardLayout` / `saveDashboardLayout` / `resetDashboardLayout` 三个函数可正常调用后端接口
- [ ] `fetchLayout()` 失败时 `layout` 回退到 `DEFAULT_LAYOUT`
- [ ] `enterEditMode()` 后修改 `editingLayout` 不影响 `layout`
- [ ] `cancelEditMode()` 后 `editingLayout` 恢复为进入前快照
- [ ] `saveLayout()` 成功后 `isEditMode` 变为 `false`，`layout` 更新为新顺序
- [ ] TypeScript 编译无报错（`npm run build` 无 TS 错误）
