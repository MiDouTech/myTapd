# 通用组件使用说明（Task014）

## BaseTable

统一表格样式封装，符合 `frontend-ui-standards`：

- 无边框 + 斑马纹
- 表头背景 `#f5f7fa`
- 行 hover 背景 `#f0f9ff`
- 表头/内容居中

### Props

| 名称          | 类型                        | 默认值  | 说明           |
| ------------- | --------------------------- | ------- | -------------- |
| data          | `Record<string, unknown>[]` | `[]`    | 表格数据       |
| loading       | `boolean`                   | `false` | 加载状态       |
| showSelection | `boolean`                   | `false` | 是否显示复选框 |
| rowKey        | `string`                    | `id`    | 行 key         |

### Events

- `selectionChange(rows)`
- `sortChange({ prop, order })`

---

## BasePagination

统一分页封装，符合 `10/20/50/100`、默认 `20` 的规范。

### Props

| 名称        | 类型     | 说明     |
| ----------- | -------- | -------- |
| currentPage | `number` | 当前页码 |
| pageSize    | `number` | 每页条数 |
| total       | `number` | 总条数   |

### Events

- `update({ pageNum, pageSize })`

---

## EmptyState

统一空状态组件，基于 `el-empty` 封装。

### Props

| 名称        | 类型     | 默认值     | 说明       |
| ----------- | -------- | ---------- | ---------- |
| description | `string` | `暂无数据` | 空状态文案 |
