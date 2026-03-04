# Task009-企微用户管理模块重构技术方案

## 1. 技术目标

围绕“企微用户管理模块”完成一次兼容式重构，目标如下：

1. 同步链路统一为企业微信推荐方式：根部门递归拉取成员；
2. 同步策略从“覆盖写入”升级为“差异收敛（新增/更新/失活）”；
3. 用户查询增强“同步状态”维度，支持运维治理；
4. 消除关键路径循环内查询（N+1），提高同步与查询性能；
5. 前后端保持 API 向后兼容，页面平滑升级。

## 2. 现状与问题定位

## 2.1 现状链路

- 同步入口：`POST /api/v1/sync/manual`
- 后端调用：`WecomSyncApplicationService -> WecomSyncService`
- 企微接口：
  - `GET /cgi-bin/gettoken`
  - `GET /cgi-bin/department/list`
  - `GET /cgi-bin/user/list`

## 2.2 关键问题

1. 成员同步按“部门循环 + 部门成员查询”实现，请求次数随部门数量线性增长；
2. 用户同步存在循环内按 `wecom_userid` 单条查库，性能与可维护性差；
3. 缺少“本地有、企微无”对象的失活收敛，数据会长期陈旧；
4. 员工分页无同步状态筛选，难以排障；
5. `getUserList` 角色查询存在 N+1 查询。

## 3. 重构设计

## 3.1 同步架构设计

### 3.1.1 新同步流程

1. 获取 access_token；
2. 调 `department/list` 全量拉取部门；
3. 调 `user/list(department_id=1, fetch_child=1)` 一次拉全量成员；
4. 本地执行差异收敛：
   - 新增；
   - 更新；
   - 失活（企微已不存在）。

### 3.1.2 差异收敛规则

#### 部门

- 主键匹配：`department.wecom_dept_id`
- 更新字段：`name / parent_id / sort_order / leader_wecom_userid / dept_status / sync_status / sync_time`
- 失活策略：本地存在但企微缺失 → `dept_status=0`，`sync_status=2`

#### 用户

- 主键匹配：`sys_user.wecom_userid`
- 更新字段：`name / phone / email / position / gender / avatar_url / department_id / account_status / sync_status / sync_time`
- 失活策略：本地存在但企微缺失 → `account_status=4`，`sync_status=2`

### 3.1.3 统计模型升级

在手动同步返回值中新增细分统计：

- 部门：`departmentCreatedCount / departmentUpdatedCount / departmentDisabledCount`
- 用户：`userCreatedCount / userUpdatedCount / userDisabledCount`

## 3.2 查询能力升级

员工分页接口新增筛选条件：

- `syncStatus`（0/1/2）

员工列表/详情返回新增字段：

- `syncStatus`
- `syncStatusName`
- `syncTime`

## 3.3 性能优化

1. **同步查询去 N+1**：先全量读取本地用户并构建 `wecom_userid -> User` map，循环内不再查库；
2. **角色查询去 N+1**：新增“按用户ID批量查询角色编码”能力，`getUserList` 一次查询完成角色映射；
3. **企微调用降频**：用户同步改为单次 `user/list`（根部门递归），减少外部 API 调用次数。

## 4. 代码改造清单

## 4.1 后端（ticket-platform）

1. `WecomClient`
   - 新增 `getDepartmentUsers(Long departmentId, boolean fetchChild)`；
   - 解析 `department_leader` 到部门模型；
   - 保留旧方法兼容。

2. `WecomSyncService`
   - 重构部门/用户同步算法为 map 对比；
   - 新增失活收敛逻辑；
   - 输出细分统计。

3. `SyncManualOutput`
   - 增加部门/用户细分统计字段。

4. 用户查询链路
   - `EmployeePageInput` 增加 `syncStatus`；
   - `EmployeePageOutput`/`EmployeeDetailOutput` 增加同步字段；
   - `UserRepository` 增加批量角色映射查询；
   - `UserApplicationService` 使用批量角色映射，移除循环内查询。

## 4.2 前端（miduo-frontend）

1. `types/organization.ts`
   - 增加 `syncStatus` 查询与展示字段；
   - 扩展手动同步细分统计字段。

2. `views/manage/UserManageView.vue`
   - 新增“同步状态”筛选；
   - 增加“同步状态/同步时间”列；
   - 手动同步提示增加分项统计文案。

## 5. 接口兼容性

1. 既有接口路径不变（`API000425~API000430`）；
2. 本次仅增加可选入参与扩展返回字段，前端旧版本可兼容；
3. 不引入 breaking change。

## 6. 风险与对策

1. **企微根部门ID差异风险**
   - 对策：默认使用 `1`，并保留异常日志，便于企业特殊配置时快速定位。

2. **失活策略误伤风险**
   - 对策：仅处理存在 `wecom_userid / wecom_dept_id` 的记录，不影响本地手工账号。

3. **历史脏数据映射风险**
   - 对策：所有映射字段做空值保护；同步状态与时间可观测。

## 7. 自测计划

1. 后端编译：
   - `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn -pl ticket-controller -am -DskipTests compile`
2. 前端构建：
   - `cd /workspace/miduo-frontend && npm run build`
3. 关键回归：
   - 手动同步成功并返回分项统计；
   - 员工分页按 `syncStatus` 筛选正确；
   - 同步后用户/部门失活收敛生效；
   - 旧页面功能（部门树、详情抽屉、同步日志）可正常使用。

