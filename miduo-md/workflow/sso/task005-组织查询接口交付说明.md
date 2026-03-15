# Task005-组织查询接口交付说明

## 1. 本阶段完成项

1. 部门树接口
   - `GET /api/v1/departments/tree`（`API000428`）
2. 员工分页接口
   - `GET /api/v1/employees/page`（`API000429`）
3. 员工详情接口
   - `GET /api/v1/employees/detail/{id}`（`API000430`）
4. 状态映射统一
   - `accountStatus` 映射：`1=在职`、`2=停用`、`4=离职`
5. 敏感字段脱敏
   - 手机号：中间4位掩码
   - 邮箱：用户名部分掩码
   - 企微UserID：中间掩码

## 2. 接口行为说明

1. 部门树按现有部门层级返回，保持父子关系不变。
2. 员工分页支持按部门、关键字（姓名/工号）、状态筛选。
3. 员工详情返回角色编码列表，并统一输出状态文案和脱敏字段。

## 3. 编译验证

- 执行：`mvn -pl ticket-controller -am -DskipTests compile`
- 结果：`BUILD SUCCESS`
