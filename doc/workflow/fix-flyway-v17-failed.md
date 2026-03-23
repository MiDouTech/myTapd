# Flyway V17 失败迁移修复指南

## 问题说明
测试环境出现：`Detected failed migration to version 17 (add bug report temp solution)`  
原因：V17 曾在测试库执行时失败，Flyway 在 `flyway_schema_history` 中记录了失败状态，导致后续启动无法继续。

## 修复步骤

### 1. 连接测试库
- 主机：10.0.4.4
- 端口：3306
- 数据库：ticket_platform_test
- 用户名：root（参考 application-test.yml）

### 2. 移除失败的 V17 记录
执行以下 SQL：

```sql
-- 查看当前 V17 记录状态
SELECT installed_rank, version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version = '17';

-- 删除失败的 V17 记录（仅删除 success=0 的）
DELETE FROM flyway_schema_history 
WHERE version = '17' AND success = 0;

-- 如果上面没有匹配到记录，可能是所有 V17 都要删掉再重跑，则执行：
-- DELETE FROM flyway_schema_history WHERE version = '17';
```

### 3. 可选：检查半完成变更
V17 会向 `bug_report` 添加 `temp_resolve_date`、`temp_solution` 两列。若执行了一部分，脚本是幂等的，重跑时会补齐缺失列，一般无需额外处理。

```sql
-- 查看 bug_report 表当前列（可选）
SHOW COLUMNS FROM bug_report;
```

### 4. 重启应用
修复后重启应用，Flyway 会重新执行 V17 并标记为成功。

## 方案二：使用 Flyway Repair（需 Flyway CLI）

若有 Flyway 命令行，可使用：

```bash
flyway -url=jdbc:mysql://10.0.4.4:3306/ticket_platform_test \
  -user=root -password='Testchjskn@2025SQL' \
  -table=flyway_schema_history \
  repair
```

效果与手动删除 `success=0` 的 V17 记录相同。
