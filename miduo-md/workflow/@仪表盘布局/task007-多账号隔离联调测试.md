# task007 · 多账号数据隔离联调测试

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task001–task006 全部完成  
> 预估工时：1h

---

## 目标

通过真实账号验证仪表盘个人布局功能的完整性，重点验证**多用户数据隔离**、**降级容灾**和**跨账号不干扰**。

---

## 测试前置条件

1. 数据库已执行 V20 迁移脚本，`dashboard_user_layout` 表存在
2. 后端服务正常启动
3. 前端服务正常启动
4. 准备至少 2 个测试账号（账号A、账号B），使用真实系统用户，禁止 mock

---

## 测试场景清单

### 场景 T01：首次访问（无个人布局记录）

**步骤**：
1. 账号A 登录，访问 `/dashboard`
2. 查看 `dashboard_user_layout` 表，确认无该用户记录

**预期结果**：
- 仪表盘按默认顺序展示：overview → trend_category → efficiency_workload
- 页面无报错，数据正常加载

---

### 场景 T02：账号A 自定义布局并保存

**步骤**：
1. 账号A 点击"编辑布局"
2. 将 `efficiency_workload` 行组拖到第一位（`trend_category` 移至第二位）
3. 点击"保存布局"

**预期结果**：
- 提示"布局已保存"
- 仪表盘顺序变为：overview → efficiency_workload → trend_category
- 数据库 `dashboard_user_layout` 表中插入账号A 的 3 条记录，`sort_order` 为 0/1/2
- Redis Key `dashboard:layout:user:{账号A的userId}` 已清除（下次请求会重建）

---

### 场景 T03：刷新后布局持久化

**步骤**：
1. 账号A 保存布局后刷新页面

**预期结果**：
- 仪表盘仍按账号A 保存的顺序显示（efficiency_workload 在前）

---

### 场景 T04：账号B 独立布局（数据隔离核心验证）

**步骤**：
1. 账号A 保存自定义布局（T02 完成后）
2. 账号B 登录，访问 `/dashboard`

**预期结果**：
- 账号B 仪表盘仍按默认顺序展示
- 账号B 的 `GET /api/dashboard/layout` 响应为默认顺序，与账号A 无关
- 数据库中无账号B 的布局记录

---

### 场景 T05：账号B 独立修改布局

**步骤**：
1. 账号B 点击"编辑布局"，拖拽并保存不同于账号A 的顺序

**预期结果**：
- 账号B 仪表盘按账号B 的顺序显示
- 账号A 仪表盘顺序不变（刷新验证）

---

### 场景 T06：恢复默认布局

**步骤**：
1. 账号A 点击"编辑布局" → "恢复默认" → 确认

**预期结果**：
- 提示"已恢复默认布局"
- 账号A 仪表盘恢复为默认顺序（overview → trend_category → efficiency_workload）
- 数据库中账号A 的记录 `deleted=1`
- 账号B 布局不受影响

---

### 场景 T07：取消编辑不保存

**步骤**：
1. 账号A 点击"编辑布局"，拖拽修改顺序
2. 点击"取消"

**预期结果**：
- 布局恢复为进入编辑前的顺序
- 数据库无新增/修改记录
- 无接口调用（Network 面板无 PUT/DELETE 请求）

---

### 场景 T08：布局接口降级容灾

**步骤**：
1. 临时关闭后端服务，账号A 访问 `/dashboard`

**预期结果**：
- 仪表盘按前端内置默认顺序展示，无白屏
- 页面提示加载失败（或静默降级，不阻塞数据展示）
- 数据接口独立于布局接口，数据区域也可能报错（属正常，不影响验收）

---

### 场景 T09：移动端不显示编辑入口

**步骤**：
1. 将浏览器宽度调整至 < 768px（DevTools 模拟），访问仪表盘

**预期结果**：
- 不显示"编辑布局"按钮
- 仪表盘按已保存布局只读展示

---

## 接口级验证（API 维度）

使用 curl 或 Postman，携带有效 JWT Token：

```bash
# T-API-01: 获取布局（无记录时返回默认）
GET /api/dashboard/layout
# 预期：返回 3 条，sortOrder 0/1/2

# T-API-02: 保存布局
PUT /api/dashboard/layout
Body: {"layouts":[{"rowGroupKey":"overview","sortOrder":0},{"rowGroupKey":"efficiency_workload","sortOrder":1},{"rowGroupKey":"trend_category","sortOrder":2}]}
# 预期：200 成功

# T-API-03: 再次获取布局，验证已保存顺序
GET /api/dashboard/layout
# 预期：efficiency_workload sortOrder=1，trend_category sortOrder=2

# T-API-04: 非法 rowGroupKey 返回 400
PUT /api/dashboard/layout
Body: {"layouts":[{"rowGroupKey":"unknown_key","sortOrder":1}]}
# 预期：400 错误响应

# T-API-05: 恢复默认
DELETE /api/dashboard/layout
# 预期：200 成功

# T-API-06: 恢复后再次 GET，返回默认顺序
GET /api/dashboard/layout
# 预期：trend_category sortOrder=1，efficiency_workload sortOrder=2
```

---

## 验收标准

- [ ] T01–T09 所有场景测试通过
- [ ] T-API-01–T-API-06 接口验证通过
- [ ] 无控制台 JavaScript 报错
- [ ] 两个测试账号的布局数据完全隔离，互不干扰
- [ ] 前后端代码已通过编译（`mvn clean install`、`npm run build`）
- [ ] 服务可正常启动运行
