# 设计文档：插件「我的工单」列表体验升级

## 1. 现状与设计原则

当前查询使用 `integration_app_id`、`creator_id`、`source=plugin` 三个固定条件并按创建时间倒序分页。分类能力必须作为附加条件，不能替代任何归属条件。

设计遵循以下原则：

1. **服务端分页和搜索**：客户端只处理当前页，避免加载全部工单及本地搜索漏数据。
2. **选项不随筛选收缩**：选择某分类后仍可直接切换到其他分类。
3. **批量关联**：分类名称和选项均禁止 N+1 查询。
4. **加法兼容**：请求参数可选，响应保留 `records/total/pageNum/pageSize` 及原记录字段。

## 2. API 契约

### 2.1 请求

```http
GET /api/open/v1/plugin/tickets/mine?pageNum=1&pageSize=10&title=签名&categoryId=123&status=pending_customer_service
Authorization: Bearer <launchToken>
```

`PluginTicketMinePageInput` 新增：

| 字段 | 类型 | 必填 | 规则 |
|------|------|------|------|
| `title` | `String` | 否 | 去除首尾空格后进行标题模糊搜索；最长 100 字符，空串视为未传 |
| `categoryId` | `Long` | 否 | 正整数；不传表示全部分类 |
| `status` | `String` | 否 | 必须是有效 `TicketStatus` code；不传表示全部状态 |

三个条件按 AND 组合。`title` 使用参数绑定后的 `LIKE` 查询并转义 `%`、`_` 等通配符，分类不存在或不属于当前数据范围时返回成功空页；非法状态返回统一参数错误。任何搜索参数都不能改变 LaunchToken 确定的数据边界。

### 2.2 响应

新增专用输出 `PluginTicketMinePageOutput`，保持标准分页字段并增加 `categoryOptions`：

```json
{
  "records": [
    {
      "ticketId": 1001,
      "ticketNo": "WO-20260901-003-7478",
      "title": "签名错误",
      "status": "pending_customer_service",
      "statusLabel": "待客服受理",
      "priority": "medium",
      "categoryId": 123,
      "categoryName": "支付问题",
      "createTime": "2026-09-01T10:00:00+00:00",
      "updateTime": "2026-09-01T10:10:00+00:00"
    }
  ],
  "total": 8,
  "pageNum": 1,
  "pageSize": 10,
  "categoryOptions": [
    { "id": 123, "name": "支付问题" },
    { "id": 456, "name": "功能异常/Bug" }
  ],
  "statusOptions": [
    { "code": "pending_customer_service", "name": "待客服受理" },
    { "code": "processing", "name": "处理中" }
  ]
}
```

`PluginTicketSummaryOutput` 同步增加 `categoryId`、`categoryName`，使现有工单摘要接口也保持同一摘要结构。`categoryName` 的兜底规则统一为「未分类」。

`categoryOptions` 和 `statusOptions` 的语义为：当前 LaunchToken 所属应用中，由当前用户创建的全部插件工单实际涉及的可筛选值。它们不受标题、分类、状态和分页参数影响；无工单时返回空数组而不是 `null`。分类按 `sortOrder/id` 排序，状态按 `TicketStatus` 的产品顺序排序。

> 不采用“只从当前页 records 去重”的方案，因为会导致其他页的分类不可筛选；也不直接返回系统全量分类，避免暴露与该应用用户无关的分类。

## 3. 后端实现

### 3.1 主列表查询

在现有 `LambdaQueryWrapper<TicketPO>` 的三个固定归属条件后追加：

```java
.like(hasTitle, TicketPO::getTitle, escapedTitle)
.eq(input.getCategoryId() != null, TicketPO::getCategoryId, input.getCategoryId())
.eq(hasStatus, TicketPO::getStatus, normalizedStatus)
```

继续由数据库分页和按 `create_time DESC` 排序。建议追加 `id DESC` 作为相同创建时间下的稳定排序条件。

### 3.2 分类名称补齐

从当前页记录提取非空 `categoryId`，一次 `selectBatchIds` 查询 `TicketCategoryPO` 并构造 `Map<Long, String>`，映射摘要时写入分类字段。不得在 `stream.map` 内逐条访问数据库。

对于 `categoryId == null`、分类已删除或名称为空的历史记录：

- `categoryId` 保留原始值（为空则返回 `null`）；
- `categoryName` 返回「未分类」；
- 不把无法解析的分类加入 `categoryOptions`。

### 3.3 筛选选项查询

在 `TicketMapper` 增加两个轻量聚合查询，二者固定带入与主列表相同的安全边界：

```sql
SELECT c.id, c.name, c.sort_order
FROM ticket t
JOIN ticket_category c ON c.id = t.category_id
WHERE t.integration_app_id = #{integrationAppId}
  AND t.creator_id = #{creatorId}
  AND t.source = 'plugin'
GROUP BY c.id, c.name, c.sort_order
ORDER BY c.sort_order ASC, c.id ASC

SELECT DISTINCT t.status
FROM ticket t
WHERE t.integration_app_id = #{integrationAppId}
  AND t.creator_id = #{creatorId}
  AND t.source = 'plugin'
```

两个选项查询都只应用三项归属条件，**不拼入请求的 `title/categoryId/status`**。应用层过滤未知状态并按 `TicketStatus` 产品顺序组装状态名称。实际 SQL 需遵循项目表名、逻辑删除字段和数据库模式。

### 3.4 鉴权与权限

- 继续由控制器校验 LaunchToken，由应用服务仅使用 claims 中的 `integrationAppId`、`userId`。
- 不接受客户端传 `integrationAppId` 或 `creatorId`。
- 保持现有 `plugin:ticket:read-mine` 权限策略，不新增权限码。

## 4. SDK 页面与交互设计

### 4.1 信息架构与视觉规格

桌面端弹窗建议由当前 420px 扩至 `min(760px, 94vw)`，最大高度 `88vh`，分为三个固定层次：

1. **标题栏**：左侧「我的工单」，可附当前结果总数；右侧关闭按钮。
2. **搜索区**：第一行标题搜索框；第二行分类、状态下拉框和「查询」「重置」按钮。白底、8px 间距，搜索区底部使用浅色分隔线。
3. **结果区**：表头和分页固定，中间列表独立滚动，避免整页滚动时丢失操作入口。

桌面列表列宽建议为：编号 25%、标题自适应、分类 16%、状态 14%、操作 72px。编号用次级文字且允许复制，标题单行省略并用 `title` 展示完整内容，分类用中性色标签，状态使用语义色圆角标签，操作使用文字按钮「查看」，不再只依赖含义不明确的箭头。

窄于 560px 时切换卡片布局：第一行标题和状态，第二行编号，第三行分类和「查看」；不得出现横向滚动。所有输入框、下拉框和按钮具有可见 focus 状态、关联 label/`aria-label`，点击区域不小于 32px。

建议状态色仅作为辅助信息：待受理为橙色、处理中为蓝色、已完成为绿色、已关闭为灰色，文字标签必须始终存在以满足可访问性。

### 4.2 数据流

1. 首次以 `pageNum=1&pageSize=10` 请求，渲染列表、总数和筛选选项。
2. 输入标题后按 Enter 或点击「查询」发起请求；分类、状态变化后仍由「查询」提交，避免连续误触请求。「重置」清空全部条件并立即查询第一页。
3. 查询条件变化、点击查询或重置时页码归 1；仅翻页时保留全部搜索条件。总页数按 `ceil(total/pageSize)` 计算。
4. 分页区显示「共 N 条」、上一页、页码、下一页；页数较多时使用省略号。默认每页 10 条，可选 10/20/50，修改每页条数后回到第一页。
5. 搜索期间保留搜索控件和上一批结果，在结果区覆盖轻量 loading，防止布局跳动；无结果时区分「暂无工单」与「没有符合条件的工单」。
6. 使用递增请求序号或 `AbortController` 取消前一次请求，确保快速查询、翻页时只渲染最后一次请求。
7. 请求失败保留条件和当前页，在结果区显示错误与「重新加载」按钮；查看按钮继续调用现有 `renderTicketDetail`。

请求 URL 使用 `URLSearchParams` 组装，避免手工字符串拼接：

```ts
const query = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
if (title.trim()) query.set('title', title.trim())
if (categoryId) query.set('categoryId', String(categoryId))
if (status) query.set('status', status)
```

SDK 内部类型新增 `PluginTicketCategoryOption`、`PluginTicketMinePage` 和摘要分类字段，避免继续使用内联匿名返回类型。

## 5. 兼容性与发布

- 未升级的 SDK 不传参数，能忽略新增字段，行为不变。
- 新 SDK 应容忍灰度期间旧后端没有筛选选项，按空数组处理；记录没有 `categoryName` 时展示「未分类」。分页字段缺失时回退到当前 records 长度并禁用下一页。
- `ticket-sdk` 构建完成后执行现有发布脚本，将生成物同步至 `miduo-frontend/public/sdk/v1/ticket-sdk.min.js`，避免源码和线上静态资源不一致。
- 本变更无数据库迁移；接口仅增加可选参数和响应字段。

## 6. 测试策略

### 后端

- 无搜索参数：验证原有三个归属条件、分页和排序。
- 分别和组合验证标题模糊、分类精确、状态精确搜索，并验证 LIKE 通配符被正确转义。
- 验证任一搜索条件都不能看到其他应用或其他用户的工单。
- 摘要映射：正常分类、空分类、已删除/缺失分类。
- 选项聚合：跨页分类和状态均返回；去重；不受当前筛选影响；不混入其他应用/用户；顺序稳定。
- 查询计数检查：分类补齐为批量查询，不随 records 数量线性增长。

### SDK

- 首次加载、组合查询、Enter 查询、重置、上一页/下一页、页码和 pageSize 切换。
- 分别验证无数据和无匹配结果、接口错误、快速连续查询与翻页。
- 桌面表格、窄屏卡片、超长编号/标题/分类及状态语义色。
- 对旧版响应缺少新增字段的兼容。
- 键盘操作与 `aria-label` 基础检查。

## 7. 风险与权衡

| 风险 | 缓解 |
|------|------|
| 每次列表请求额外查询分类选项 | 聚合查询只返回少量去重数据；若后续成为热点，可按 `appId + userId` 短期缓存或拆分独立端点 |
| 分类被删除导致历史记录无法展示名称 | 统一展示「未分类」，保留原始 `categoryId` 便于排查 |
| 快速切换产生响应乱序 | `AbortController` 或请求序号只接纳最后一次响应 |
| 专用分页 DTO 与通用 `PageOutput` 重复 | 通过组合/继承复用分页字段，以明确契约换取 `categoryOptions` 的完整性 |

## 8. 预期文件变更

```text
ticket-platform/ticket-entity/.../plugin/PluginTicketMinePageInput.java
ticket-platform/ticket-entity/.../plugin/PluginTicketSummaryOutput.java
ticket-platform/ticket-entity/.../plugin/PluginTicketMinePageOutput.java
ticket-platform/ticket-entity/.../plugin/PluginTicketCategoryOptionOutput.java
ticket-platform/ticket-controller/.../plugin/PluginOpenController.java
ticket-platform/ticket-application/.../plugin/PluginTicketApplicationService.java
ticket-platform/ticket-infrastructure/.../ticket/mapper/TicketMapper.java
ticket-platform/ticket-infrastructure/src/main/resources/mapper/ticket/TicketMapper.xml
ticket-platform/ticket-sdk/src/index.ts
miduo-frontend/public/sdk/v1/ticket-sdk.min.js
```
