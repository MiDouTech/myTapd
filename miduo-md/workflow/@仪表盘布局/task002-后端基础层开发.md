# task002 · 后端常量/枚举/Entity/DTO/Mapper 开发

> 所属模块：仪表盘个性化布局  
> PRD 版本：v2.0  
> 前置依赖：task001（数据库表已创建）  
> 预估工时：1.5h

---

## 目标

完成后端基础层代码，为 task003（Service/Controller）提供数据层支撑。

---

## 输出文件清单

```
ticket-platform/
├── ticket-common/src/main/java/com/miduo/cloud/ticket/common/
│   ├── constants/DashboardLayoutConstants.java          # 系统默认布局常量
│   └── enums/DashboardRowGroupEnum.java                 # 行组Key枚举
│
├── ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/
│   └── persistence/mybatis/dashboard/
│       ├── po/DashboardUserLayoutPO.java                # PO（数据库映射对象）
│       ├── mapper/DashboardUserLayoutMapper.java         # Mapper接口
│       └── mapper/xml/DashboardUserLayoutMapper.xml      # Mapper XML
│
└── ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/
    └── dto/dashboard/
        ├── DashboardLayoutItemOutput.java               # 布局配置响应DTO（单条）
        └── DashboardLayoutSaveInput.java                # 保存布局请求DTO
```

---

## 各文件详细说明

### 1. `DashboardRowGroupEnum.java`（枚举）

位置：`ticket-common/.../enums/DashboardRowGroupEnum.java`

定义所有合法的行组 Key 枚举值：
- `OVERVIEW`（key="overview"，固定，sortOrder=0）
- `TREND_CATEGORY`（key="trend_category"，可拖拽，sortOrder=1）
- `EFFICIENCY_WORKLOAD`（key="efficiency_workload"，可拖拽，sortOrder=2）

每个枚举值包含属性：`key`（String）、`defaultSortOrder`（int）、`isFixed`（boolean）

提供静态方法：`fromKey(String key)` → 根据字符串查找枚举，不存在抛出 `IllegalArgumentException`

### 2. `DashboardLayoutConstants.java`（常量类）

位置：`ticket-common/.../constants/DashboardLayoutConstants.java`

维护系统默认布局列表（`List<DashboardLayoutItemOutput>`），用于：
- 用户无个人布局记录时的 fallback 返回
- 恢复默认后的展示顺序

使用 `DashboardRowGroupEnum` 的 `defaultSortOrder` 和 `isFixed` 填充，确保常量与枚举定义一致。

### 3. `DashboardUserLayoutPO.java`（PO）

位置：`ticket-infrastructure/.../po/DashboardUserLayoutPO.java`

- 继承 `BaseEntity`（包含 id、createTime、updateTime、createBy、updateBy、deleted）
- `@TableName("dashboard_user_layout")`
- 字段：`userId`（Long）、`rowGroupKey`（String）、`sortOrder`（Integer）、`isFixed`（Integer）

### 4. `DashboardUserLayoutMapper.java`（Mapper 接口）

位置：`ticket-infrastructure/.../mapper/DashboardUserLayoutMapper.java`

- 继承 `BaseMapper<DashboardUserLayoutPO>`
- 自定义方法：`List<DashboardUserLayoutPO> selectByUserId(Long userId)` → 查询指定用户的布局列表，按 `sort_order` 升序，过滤 `deleted=0`

### 5. `DashboardUserLayoutMapper.xml`（Mapper XML）

位置：`ticket-infrastructure/.../mapper/xml/DashboardUserLayoutMapper.xml`

实现 `selectByUserId` SQL：

```sql
SELECT id, user_id, row_group_key, sort_order, is_fixed, create_time, update_time, create_by, update_by, deleted
FROM dashboard_user_layout
WHERE user_id = #{userId}
  AND deleted = 0
ORDER BY sort_order ASC
```

**禁止在 Mapper 接口上用 `@Select` 等注解写 SQL，所有 SQL 必须在 XML 中定义。**

### 6. `DashboardLayoutItemOutput.java`（响应 DTO）

位置：`ticket-entity/.../dto/dashboard/DashboardLayoutItemOutput.java`

字段：
- `rowGroupKey`（String）：行组Key
- `sortOrder`（Integer）：排列序号
- `isFixed`（Boolean）：是否固定

### 7. `DashboardLayoutSaveInput.java`（请求 DTO）

位置：`ticket-entity/.../dto/dashboard/DashboardLayoutSaveInput.java`

字段：
- `layouts`（`List<LayoutItem>`）：布局列表，`@NotNull @NotEmpty`

内嵌静态类 `LayoutItem`：
- `rowGroupKey`（String）：`@NotBlank`
- `sortOrder`（Integer）：`@NotNull @Min(0)`

---

## 编码规范要求

- 所有枚举放到 `com.miduo.cloud.ticket.common.enums` 包下
- 所有常量放到 `com.miduo.cloud.ticket.common.constants` 包下
- PO 必须继承 `BaseEntity`，使用 `@EqualsAndHashCode(callSuper = true)`
- DTO 字段使用 `@NotBlank`/`@NotNull` 等 JSR-303 注解
- 禁止在代码中使用字符串字面量作为行组 Key，统一使用枚举

---

## 验收标准

- [ ] `DashboardRowGroupEnum` 包含 3 个枚举值，`fromKey()` 方法正常工作
- [ ] `DashboardLayoutConstants.DEFAULT_LAYOUT` 返回正确的 3 条默认记录
- [ ] `DashboardUserLayoutPO` 继承 `BaseEntity`，字段与数据库表一致
- [ ] `selectByUserId` SQL 执行正确，返回 `sort_order` 升序结果
- [ ] DTO 字段校验注解完整
- [ ] 模块 `mvn clean compile` 无报错
