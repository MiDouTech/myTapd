# task001 · 数据库迁移脚本

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 优先级：P0（其他 task 的前置依赖）  
> 预估工时：0.5h

---

## 目标

创建 `dashboard_user_layout` 表，用于存储每位用户的个人仪表盘布局配置。

---

## 输出文件

```
ticket-platform/ticket-bootstrap/src/main/resources/db/migration/
└── V20__init_dashboard_user_layout.sql
```

---

## 执行内容

### 1. 建表语句

创建 `dashboard_user_layout` 表，字段要求：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint AUTO_INCREMENT | 主键，从 1 开始自增 |
| `user_id` | bigint NOT NULL | 用户ID，关联 `sys_user.id` |
| `row_group_key` | varchar(64) NOT NULL | 行组Key（overview/trend_category/efficiency_workload） |
| `sort_order` | int NOT NULL DEFAULT 0 | 排列序号，越小越靠前 |
| `is_fixed` | tinyint(1) NOT NULL DEFAULT 0 | 是否固定不可拖拽（1固定/0可拖） |
| `create_time` | datetime | 创建时间，自动填充 |
| `update_time` | datetime | 更新时间，自动更新 |
| `create_by` | varchar(50) | 创建人 |
| `update_by` | varchar(50) | 更新人 |
| `deleted` | tinyint(4) DEFAULT 0 | 逻辑删除标志 |

索引要求：
- 主键：`id`
- 联合唯一索引：`(user_id, row_group_key)` → `uk_user_row_group`（防止同一用户同一行组重复记录）
- 普通索引：`idx_user_id`、`idx_sort_order`、`idx_deleted`

### 2. 无需初始化数据

用户布局为**按需创建**模式：用户首次访问时无记录，后端返回代码常量默认顺序；只有在用户**主动保存布局**时才写入数据库。因此迁移脚本中不需要 INSERT 语句。

---

## 验收标准

- [ ] Flyway 执行 V20 脚本无报错
- [ ] `dashboard_user_layout` 表创建成功，字段和索引与设计一致
- [ ] 联合唯一索引 `uk_user_row_group` 存在且生效
- [ ] `id` 自增起始值为 1

---

## 注意事项

- 文件命名必须为 `V20__init_dashboard_user_layout.sql`（双下划线）
- 不得修改已有 V1–V19 脚本
- `id` 字段须包含 `AUTO_INCREMENT=1`
