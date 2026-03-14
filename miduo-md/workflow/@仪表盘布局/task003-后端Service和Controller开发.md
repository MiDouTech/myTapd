# task003 · 后端 Service + Controller 开发（API000411–API000413）

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task001（表已创建）、task002（基础层已完成）  
> 预估工时：2h

---

## 目标

实现仪表盘个人布局的三个后端接口，包含 Redis 缓存、JWT 用户身份解析、事务保障。

---

## 输出文件清单

```
ticket-platform/
├── ticket-application/src/main/java/com/miduo/cloud/ticket/application/dashboard/
│   └── DashboardLayoutApplicationService.java          # 布局业务服务
│
└── ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/dashboard/
    └── DashboardLayoutController.java                   # 布局控制器（新建，与现有 DashboardController 分离）
```

> 现有 `DashboardController.java` 保持不变，新接口单独放到 `DashboardLayoutController.java`。

---

## 接口实现要求

### API000411 · GET /api/dashboard/layout（获取个人布局）

**Service 逻辑（`getLayout(Long userId)`）**：

1. 先查 Redis Key `dashboard:layout:user:{userId}`；
2. 若命中，直接反序列化返回；
3. 若未命中，查 `DashboardUserLayoutMapper.selectByUserId(userId)`；
4. 若查询结果为空，返回 `DashboardLayoutConstants.DEFAULT_LAYOUT`；
5. 若有记录，将 PO 列表转为 `List<DashboardLayoutItemOutput>`；
6. 将结果 JSON 序列化写入 Redis（TTL 30 分钟）；
7. 返回结果按 `sortOrder` 升序。

**Controller**：
- `@GetMapping("/layout")`
- 从 `SecurityContextHolder`（或现有 JWT 工具类）获取当前 `userId`
- 调用 Service，返回 `ApiResult<List<DashboardLayoutItemOutput>>`
- 注释中填写：接口编号 API000411

---

### API000412 · PUT /api/dashboard/layout（保存个人布局）

**Service 逻辑（`saveLayout(Long userId, DashboardLayoutSaveInput input)`）**：

1. 校验 `input.layouts` 不为空；
2. 遍历请求的 `rowGroupKey`，通过 `DashboardRowGroupEnum.fromKey()` 校验合法性，非法则抛异常返回 400；
3. 强制将 `rowGroupKey = "overview"` 的项 `sortOrder` 置为 0，`isFixed` 置为 true（即使前端传了其他值）；
4. **事务内**执行：
   a. 软删除该用户所有旧布局记录：`UPDATE dashboard_user_layout SET deleted=1, update_time=now(), update_by=? WHERE user_id=? AND deleted=0`
   b. 批量插入新布局记录（`userId`、`rowGroupKey`、`sortOrder`、`isFixed`、`createBy`、`updateBy` 填充）
5. 删除 Redis Key `dashboard:layout:user:{userId}`；
6. 返回成功。

**Controller**：
- `@PutMapping("/layout")`
- `@RequestBody @Valid DashboardLayoutSaveInput input`
- 注释中填写：接口编号 API000412

---

### API000413 · DELETE /api/dashboard/layout（恢复默认布局）

**Service 逻辑（`resetLayout(Long userId)`）**：

1. 软删除 `dashboard_user_layout` 中该用户所有记录（`deleted=1`）；
2. 删除 Redis Key `dashboard:layout:user:{userId}`；
3. 返回成功（前端再次调用 GET 接口即可得到系统默认顺序）。

**Controller**：
- `@DeleteMapping("/layout")`
- 注释中填写：接口编号 API000413

---

## 编码规范要求

- Controller 层只做：参数绑定 → 获取 userId → 调用 Service → 返回响应；禁止包含任何业务逻辑
- Service 层的批量插入使用 `saveBatch` 或 `batchInsert`，禁止循环内单条 INSERT
- 所有数据库操作在同一事务中（使用 `@Transactional`）
- Redis 操作使用现有的 `RedisTemplate`，Key 格式：`dashboard:layout:user:{userId}`（常量化到 `DashboardLayoutConstants`）
- 获取当前登录用户 ID 复用项目现有的 SecurityContext 工具（不得重新实现）
- 方法参数和返回值做好空指针防御

---

## 新增 Controller 类头部注释模板

```java
/**
 * 仪表盘个人布局控制器
 * 提供用户个人仪表盘行组排序配置的读写接口
 */
@Tag(name = "仪表盘个人布局", description = "获取/保存/恢复用户个人仪表盘布局")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardLayoutController {
    // ...
}
```

---

## 验收标准

- [ ] `GET /api/dashboard/layout`：
  - [ ] 新用户（无记录）返回 3 条默认布局，`sortOrder` 0/1/2
  - [ ] 已保存布局的用户返回个人顺序
  - [ ] 第二次调用命中 Redis（通过日志或调试确认）
- [ ] `PUT /api/dashboard/layout`：
  - [ ] 保存后 `dashboard_user_layout` 表中该用户有新记录
  - [ ] 旧记录 `deleted=1`
  - [ ] Redis Key 已清除
  - [ ] `overview` 强制 `sort_order=0`
  - [ ] 非法 `rowGroupKey` 返回 400
- [ ] `DELETE /api/dashboard/layout`：
  - [ ] 执行后该用户记录 `deleted=1`
  - [ ] 再次 GET 返回默认布局
- [ ] 用户 A 的操作不影响用户 B 的布局数据
- [ ] 后端模块 `mvn clean compile` 无报错，服务可正常启动
