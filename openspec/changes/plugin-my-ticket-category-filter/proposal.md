# 变更提案：插件「我的工单」列表体验升级

## 背景

业务系统通过工单 SDK 打开的「我的工单」弹窗目前只展示标题、工单号和状态，而且固定读取第一页 20 条。随着工单增多，用户既无法快速判断工单所属分类，也无法通过标题、分类或状态定位目标工单。

现有 `GET /api/open/v1/plugin/tickets/mine` 仅支持分页，`PluginTicketSummaryOutput` 也不包含分类信息；SDK 固定请求第一页 20 条，因此不能在浏览器端对完整结果做可靠筛选。

## 目标

- `tickets/mine` 支持分页以及标题、分类、状态组合搜索，同时继续以 LaunchToken 中的应用和用户作为强制数据边界。
- 每条工单清晰展示编号、标题、分类、状态和「查看」操作。
- SDK 弹窗升级为带搜索区、结果区、分页区的完整列表页面，并兼顾窄屏展示。
- 保持原有调用方式和已有响应字段兼容；不传搜索参数时行为与当前一致。

## 非目标

- 不改造提交工单区域当前的业务分类选择逻辑。
- 不支持多分类、分类树级联、复杂查询语法或跨应用查询。
- 不改造工单详情内容和「补充信息」「催单」等已有详情操作。
- 不允许客户端通过分类参数突破 `integrationAppId + creatorId + source=plugin` 的归属约束。

## 验收标准

1. 不传搜索参数调用 `tickets/mine`，分页、排序和归属范围与现状一致。
2. `title` 按去除首尾空格后的标题关键字模糊搜索；`categoryId` 精确筛选；`status` 精确筛选，三者同时存在时按 AND 组合。
3. `pageNum`、`pageSize` 生效，响应总数与当前搜索条件一致；切换筛选条件会回到第一页。
4. 每条记录包含 `ticketNo`、`title`、`categoryId`、`categoryName`、`status` 和 `statusLabel`；分类异常时显示「未分类」。
5. 分类和状态选项不受当前页及当前筛选影响，用户可随时切换到其他选项。
6. 桌面端列表采用清晰的五列布局，窄屏自动切换为卡片；点击「查看」进入现有工单详情。
7. 搜索、翻页时提供加载、空结果、失败重试和请求竞态保护。
8. 后端单元测试、SDK 构建及发布到前端的产物同步检查通过。

## 影响范围

- **后端 DTO**：`PluginTicketMinePageInput`、`PluginTicketSummaryOutput`，新增专用分页输出和分类选项 DTO。
- **后端应用层**：`PluginTicketApplicationService` 增加筛选条件，并批量补齐分类名称和分类选项。
- **持久化层**：按实现需要增加一次“当前用户工单分类聚合”查询；禁止逐条查询分类。
- **SDK 源码与发布产物**：`ticket-platform/ticket-sdk/src/index.ts` 和发布后的 `miduo-frontend/public/sdk/v1/ticket-sdk.min.js`。
- **数据库**：无表结构变更、无 Flyway 脚本。
