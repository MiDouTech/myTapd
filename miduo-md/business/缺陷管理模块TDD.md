# 缺陷管理模块技术设计文档（TDD）

> 版本：v1.0  
> 状态：已评审定稿  
> 日期：2026-03-07  
> 关联 PRD：缺陷管理模块PRD.md

---

## 一、架构评审发现与修正

在输出最终方案前，对初版方案进行了深度代码级评审，发现以下问题并全部修正：

### 1.1 初版方案问题清单（已修正）

| # | 问题类型 | 问题描述 | 修正方案 |
|---|---------|---------|---------|
| P1 | **API编号冲突** | `TicketController` 使用 API000020-025，`BugReportController` 使用 API000020-028，两段完全重叠 | Bug报告相关接口统一从 API000500 段起，不触碰现有已分配编号 |
| P1 | **状态字符串不一致** | `TicketBugApplicationService` 中权限断言使用大写字符串常量（如 `"PENDING_TEST"`），而 `TicketStatus` 枚举的 code 字段为小写（如 `"pending_test_accept"`），`normalizeStatus()` 方法做了大写转换来兼容，但 `PENDING_TEST` 并不是有效枚举值 | 权限断言集合统一改为使用 `TicketStatus` 枚举的 `code` 值；`normalizeStatus()` 方法的大小写转换仅作兼容历史数据，不作为主要匹配逻辑 |
| P2 | **循环内单条 insert（DB 红线）** | `BugReportApplicationService.syncReportTickets()` 内部循环调用 `bugReportTicketMapper.insert()`，违反数据库红线 | 改为先构建 List，再批量 `saveBatch()` |
| P2 | **缺陷字段来源分散** | 初版方案将"有效报告"、"缺陷划分"、"责任人"定义为需新增数据库字段，而这些字段已存在于 `bug_report` 和 `bug_report_responsible` 表中 | 不新增表字段，通过工单关联简报（`bug_report_ticket` 表）关联查询获取，避免数据冗余和不一致 |
| P2 | **ticket_log 现有字段已够用** | 初版方案提议新增 `field_name`/`field_label`/`change_type` 字段，但 `ticket_log` 已有 `action`/`old_value`/`new_value`/`remark` 四字段，可通过约定 `remark` 存储 JSON 格式的字段变更列表，无需 DDL 变更 | 利用现有字段，以 JSON 格式在 `remark` 字段存储批量字段变更明细，彻底避免破坏性 DDL |
| P3 | **SeverityLevel 二义性** | `ticket_bug_test_info.severity_level` 使用 FATAL/CRITICAL/NORMAL/MINOR，`SeverityLevel` 枚举使用 P0-P4，两套体系并存 | 测试信息表的 severity_level 字段维持 FATAL/CRITICAL/NORMAL/MINOR 体系不变（映射测试质量维度），`SeverityLevel` 枚举（P0-P4）用于简报维度，两者职责明确，在详情页展示时转换为统一的 P0-P4 显示 |
| P3 | **导航接口设计过重** | 初版方案提议后端提供 navigate 接口，但工单列表分页数据前端已持有 | 导航功能完全在前端实现：列表页打开详情时将相邻 ID 通过路由 state 传递，无需后端接口 |
| P3 | **BaseEntity 使用 Date 而非 LocalDateTime** | 项目 `BaseEntity` 实际使用 `java.util.Date`，规范文档模板使用 `LocalDateTime`，存在不一致 | 新增代码**沿用现有项目的 `Date` 类型**，不引入不必要的类型切换风险 |

---

## 二、技术方案总体设计

### 2.1 模块职责不变

本次开发**不新增后端服务模块**，在现有分层结构内增量开发：

```
ticket-platform/
├── ticket-common/       新增枚举：BugChangeTypeEnum
├── ticket-entity/       新增 DTO：BugChangeHistoryOutput、BugFieldChangeItem
├── ticket-infrastructure/ 新增 Mapper XML：ticket_log 变更历史查询
├── ticket-application/  增强：TicketBugApplicationService（字段级变更记录）
│                         新增：TicketChangeHistoryApplicationService
├── ticket-controller/   新增：TicketChangeHistoryController
└── miduo-frontend/      新增：BugChangeHistory 组件体系
```

### 2.2 核心设计决策

#### 决策1：变更历史存储复用 ticket_log.remark 字段

**背景**：ticket_log 表已有 `action`/`old_value`/`new_value`/`remark` 字段，但现有使用方式仅记录单一字段变更或操作类型，无法表达"一次操作变更多个字段"的场景。

**方案**：约定 `remark` 字段存储 JSON 格式的字段变更批次数据，`old_value`/`new_value` 字段降级为兼容字段（保持不变），新业务统一通过 `remark` 读写。

```json
// ticket_log.remark 存储格式（新约定）
{
  "batchId": "op-20260306-001",
  "changeType": "MANUAL_CHANGE",
  "fields": [
    {
      "fieldName": "status",
      "fieldLabel": "状态",
      "oldValue": "processing",
      "oldLabel": "处理中",
      "newValue": "closed",
      "newLabel": "已关闭"
    },
    {
      "fieldName": "closed_at",
      "fieldLabel": "关闭时间",
      "oldValue": null,
      "oldLabel": null,
      "newValue": "2026-03-06 09:46:44",
      "newLabel": "2026-03-06 09:46:44"
    }
  ]
}
```

**优势**：
- 零 DDL 变更，不影响现有数据和索引；
- 与旧日志数据完全兼容（旧数据 `remark` 为普通文本，解析失败时降级展示）；
- 扩展灵活，可随时增减 JSON 字段。

**约束**：`remark` 字段为 varchar(500)，需确保 JSON 内容不超限（字段变更数量控制在合理范围，超长值截断处理）。

#### 决策2：变更历史写入机制 —— Service 层显式调用，不用 AOP

**背景**：初版方案建议用 AOP 切面自动捕获变更，但 AOP 方案在本项目中存在以下隐患：
1. 切面切入点需要精确匹配方法签名，维护成本高；
2. AOP 无法感知业务上下文（如"本次操作的变更类型是什么"）；
3. 调试困难，隐式写入日志难以追踪。

**方案**：在 ApplicationService 层的每个更新方法中**显式调用** `TicketChangeHistoryRecorder.record()` 方法，由调用方决定记录哪些字段的变更，保持清晰的业务意图。

```java
// 调用示例（在 TicketBugApplicationService 中）
@Transactional
public void updateTestInfo(Long ticketId, TicketBugTestInfoInput input, Long userId) {
    TicketBugTestInfoPO old = getOrCreateBugTestInfo(ticketId);
    // 记录变更快照
    List<BugFieldChangeItem> changes = changeDetector.detectTestInfoChanges(old, input);
    // 执行更新
    applyTestInfoChanges(old, input);
    saveBugTestInfo(old);
    // 写入变更历史（同一事务内）
    if (!changes.isEmpty()) {
        changeHistoryRecorder.record(ticketId, userId, BugChangeTypeEnum.MANUAL_CHANGE, changes);
    }
}
```

#### 决策3：缺陷详情接口复用现有 TicketController.getTicketDetail()

现有 `GET /api/ticket/detail/{id}` 已返回完整的 `TicketDetailOutput`（含 bugInfo/testInfo/devInfo）。本次新增字段（从简报获取的 defectCategory/isValidReport/responsibleName）通过在 `TicketDetailOutput` 中增加 `bugSummaryInfo` 内嵌对象来扩展，不新建独立的缺陷详情接口，避免接口碎片化。

---

## 三、数据库变更（V13迁移）

**结论：无 DDL 变更。**

经评审，通过以下方式满足所有新功能需求，无需修改数据库表结构：

1. `ticket_log.remark` 字段复用（varchar(500) 已足够）；
2. `defectCategory`/`isValidReport`/`responsibleName` 从已有 `bug_report` / `bug_report_responsible` 表关联查询；
3. `isOverdue` 为计算字段，由后端逻辑计算后返回，不存储。

**若未来有明确存储需求，可在 V13 追加以下 ALTER（预留）：**

```sql
-- V13__enhance_bug_ticket_fields.sql（预留，当前不执行）
-- 仅在确认需要将 defectCategory 等字段去除简报关联时才执行

-- 示例：如需要在工单层面独立存储有效报告字段
-- ALTER TABLE `ticket_bug_info`
--   ADD COLUMN `is_valid_report` TINYINT(1) DEFAULT NULL COMMENT '是否有效报告(0:否 1:是)' AFTER `problem_screenshot`;
```

---

## 四、后端开发方案

### 4.1 新增枚举类

**文件**：`ticket-common/src/main/java/com/miduo/cloud/ticket/common/enums/BugChangeTypeEnum.java`

```java
package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷变更类型枚举
 */
@Getter
@AllArgsConstructor
public enum BugChangeTypeEnum {

    CREATE("CREATE", "创建缺陷"),
    MANUAL_CHANGE("MANUAL_CHANGE", "手动变更"),
    STATUS_CHANGE("STATUS_CHANGE", "状态流转"),
    SYSTEM_AUTO("SYSTEM_AUTO", "系统自动"),
    COMMENT("COMMENT", "添加评论"),
    ATTACHMENT("ATTACHMENT", "附件操作");

    private final String code;
    private final String label;

    public static BugChangeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BugChangeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

### 4.2 新增 DTO 类

#### 4.2.1 变更历史列表响应

**文件**：`ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/BugChangeHistoryOutput.java`

```java
/**
 * 缺陷变更历史条目响应
 */
public class BugChangeHistoryOutput {
    private Long id;
    private Integer seq;               // 序号（倒序，1为最新）
    private String changeTime;         // 变更时间 yyyy-MM-dd HH:mm:ss
    private Long changeByUserId;
    private String changeByUserName;   // 变更人姓名
    private String changeByAvatar;     // 变更人头像URL
    private String changeType;         // BugChangeTypeEnum.code
    private String changeTypeLabel;    // BugChangeTypeEnum.label
    private List<BugFieldChangeItem> fields;  // 变更字段列表
}
```

**文件**：`ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/BugFieldChangeItem.java`

```java
/**
 * 单个字段变更明细
 */
public class BugFieldChangeItem {
    private String fieldName;    // 字段标识（英文，用于筛选）
    private String fieldLabel;   // 字段中文名（用于展示）
    private String oldValue;     // 旧值（原始存储值）
    private String oldLabel;     // 旧值展示文本
    private String newValue;     // 新值（原始存储值）
    private String newLabel;     // 新值展示文本
}
```

#### 4.2.2 扩展 TicketDetailOutput（增加简报摘要信息）

在现有 `TicketDetailOutput` 中增加内嵌对象：

```java
// 新增内嵌对象：从简报关联获取的缺陷维度字段
private BugSummaryInfoOutput bugSummaryInfo;
```

**文件**：`ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/BugSummaryInfoOutput.java`

```java
/**
 * 缺陷维度摘要信息（从Bug简报关联获取）
 */
public class BugSummaryInfoOutput {
    private Long bugReportId;             // 关联简报ID（若有）
    private String bugReportNo;           // 简报编号
    private String defectCategory;        // 缺陷划分code
    private String defectCategoryLabel;   // 缺陷划分中文
    private String isValidReport;         // 有效报告：YES/NO
    private String isValidReportLabel;    // 有效报告中文：是/否
    private String responsibleUserName;   // 责任人姓名（可能多人，逗号分隔）
    private Boolean isOverdue;            // 是否逾期（计算字段）
}
```

#### 4.2.3 变更历史查询请求

**文件**：`ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/BugChangeHistoryPageInput.java`

```java
/**
 * 缺陷变更历史查询请求
 * 接口编号：API000501
 */
public class BugChangeHistoryPageInput extends PageInput {
    @NotNull
    private Long ticketId;
    private String changeType;   // 变更类型筛选（BugChangeTypeEnum.code），null表示全部
    private String fieldName;    // 变更字段筛选，null表示全部
}
```

### 4.3 基础设施层扩展

#### 4.3.1 TicketLogMapper 扩展

**文件**：`TicketLogMapper.java` 增加方法：

```java
/**
 * 按工单ID查询变更历史（倒序）
 * 关联 sys_user 获取用户姓名
 */
List<TicketLogPO> selectChangeHistoryByTicketId(@Param("ticketId") Long ticketId,
                                                 @Param("changeType") String changeType,
                                                 @Param("fieldName") String fieldName);
```

**对应 XML**（`TicketLogMapper.xml`）：

```xml
<select id="selectChangeHistoryByTicketId" resultType="...TicketLogPO">
    SELECT tl.*
    FROM ticket_log tl
    WHERE tl.ticket_id = #{ticketId}
      AND tl.deleted = 0
    <if test="changeType != null and changeType != ''">
      AND JSON_UNQUOTE(JSON_EXTRACT(tl.remark, '$.changeType')) = #{changeType}
    </if>
    ORDER BY tl.create_time DESC
</select>
```

> **注意**：`fieldName` 的过滤在 Service 层内存完成，不下推到 SQL，避免 JSON 函数索引失效问题。

### 4.4 Application 层

#### 4.4.1 新增 TicketChangeHistoryRecorder（变更记录工具）

**文件**：`ticket-application/.../ticket/TicketChangeHistoryRecorder.java`

职责：
- 接收字段变更列表，序列化为 JSON 写入 `ticket_log.remark`；
- 提供 `detectXxxChanges()` 系列方法，对比 PO 旧值和 Input 新值，生成 `BugFieldChangeItem` 列表。

```java
@Component
public class TicketChangeHistoryRecorder {

    private final TicketLogMapper logMapper;

    /**
     * 记录一批字段变更
     */
    public void record(Long ticketId, Long userId, BugChangeTypeEnum changeType,
                       List<BugFieldChangeItem> changes) {
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        TicketLogPO log = new TicketLogPO();
        log.setTicketId(ticketId);
        log.setUserId(userId);
        log.setAction(TicketAction.UPDATE.getCode());
        log.setRemark(buildRemark(changeType, changes));  // JSON序列化
        logMapper.insert(log);
    }

    /**
     * 检测客服信息变更（对比旧PO和新Input）
     */
    public List<BugFieldChangeItem> detectCustomerInfoChanges(TicketBugInfoPO old,
                                                               TicketBugCustomerInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        detectChange(changes, "company_name", "公司名称",
                     old.getCompanyName(), input.getCompanyName());
        detectChange(changes, "merchant_no", "商户编号",
                     old.getMerchantNo(), input.getMerchantNo());
        detectChange(changes, "merchant_account", "商户账号",
                     old.getMerchantAccount(), input.getMerchantAccount());
        detectChange(changes, "problem_desc", "问题描述",
                     truncate(old.getProblemDesc()), truncate(input.getProblemDesc()));
        detectChange(changes, "expected_result", "预期结果",
                     truncate(old.getExpectedResult()), truncate(input.getExpectedResult()));
        detectChange(changes, "scene_code", "场景码",
                     old.getSceneCode(), input.getSceneCode());
        return changes;
    }

    /**
     * 检测测试信息变更
     */
    public List<BugFieldChangeItem> detectTestInfoChanges(TicketBugTestInfoPO old,
                                                           TicketBugTestInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        detectEnumChange(changes, "severity_level", "缺陷等级",
                         old.getSeverityLevel(), input.getSeverityLevel(),
                         this::getSeverityLevelLabel);
        detectChange(changes, "reproduce_env", "复现环境",
                     old.getReproduceEnv(), input.getReproduceEnv());
        detectChange(changes, "impact_scope", "影响范围",
                     old.getImpactScope(), input.getImpactScope());
        detectChange(changes, "module_name", "所属模块",
                     old.getModuleName(), input.getModuleName());
        detectChange(changes, "reproduce_steps", "复现步骤",
                     truncate(old.getReproduceSteps()), truncate(input.getReproduceSteps()));
        detectChange(changes, "actual_result", "实际结果",
                     truncate(old.getActualResult()), truncate(input.getActualResult()));
        return changes;
    }

    /**
     * 检测开发信息变更
     */
    public List<BugFieldChangeItem> detectDevInfoChanges(TicketBugDevInfoPO old,
                                                          TicketBugDevInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        detectChange(changes, "root_cause", "根因分析",
                     truncate(old.getRootCause()), truncate(input.getRootCause()));
        detectChange(changes, "fix_solution", "修复方案",
                     truncate(old.getFixSolution()), truncate(input.getFixSolution()));
        detectChange(changes, "git_branch", "Git分支",
                     old.getGitBranch(), input.getGitBranch());
        detectChange(changes, "impact_assessment", "影响评估",
                     truncate(old.getImpactAssessment()), truncate(input.getImpactAssessment()));
        return changes;
    }

    // 工具方法：比较两个值，不同则加入变更列表
    private void detectChange(List<BugFieldChangeItem> list, String field, String label,
                               String oldVal, String newVal) {
        if (!Objects.equals(trimNull(oldVal), trimNull(newVal))) {
            BugFieldChangeItem item = new BugFieldChangeItem();
            item.setFieldName(field);
            item.setFieldLabel(label);
            item.setOldValue(oldVal);
            item.setOldLabel(oldVal);
            item.setNewValue(newVal);
            item.setNewLabel(newVal);
            list.add(item);
        }
    }

    // 截断 text 字段，防止 remark 超长
    private String truncate(String val) {
        if (val == null) return null;
        return val.length() > 200 ? val.substring(0, 200) + "..." : val;
    }

    private String trimNull(String val) {
        return val == null ? "" : val.trim();
    }
}
```

#### 4.4.2 新增 TicketChangeHistoryApplicationService

**文件**：`ticket-application/.../ticket/TicketChangeHistoryApplicationService.java`

职责：变更历史查询、解析、组装响应。

```java
@Service
public class TicketChangeHistoryApplicationService {

    private final TicketLogMapper logMapper;
    private final SysUserMapper userMapper;

    /**
     * 查询缺陷变更历史
     * 接口编号：API000501
     */
    public List<BugChangeHistoryOutput> listChangeHistory(Long ticketId,
                                                           String changeType,
                                                           String fieldName) {
        // 1. 批量查询 ticket_log（倒序）
        List<TicketLogPO> logs = logMapper.selectChangeHistoryByTicketId(ticketId, changeType, null);

        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }

        // 2. 批量查询变更人信息（一次性批量查，禁止循环单条查）
        Set<Long> userIds = logs.stream().map(TicketLogPO::getUserId)
                                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUserPO> userMap = batchQueryUsers(userIds);

        // 3. 组装结果，序号从1开始（倒序展示，最新为1）
        List<BugChangeHistoryOutput> result = new ArrayList<>();
        int seq = 1;
        for (TicketLogPO log : logs) {
            BugChangeHistoryOutput output = buildOutput(log, userMap, seq++);
            if (output == null) {
                continue;
            }
            // fieldName 在内存过滤
            if (StringUtils.hasText(fieldName)) {
                output.setFields(output.getFields().stream()
                    .filter(f -> fieldName.equals(f.getFieldName()))
                    .collect(Collectors.toList()));
                if (output.getFields().isEmpty()) {
                    continue;
                }
            }
            result.add(output);
        }
        return result;
    }

    private Map<Long, SysUserPO> batchQueryUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserPO> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
        return users.stream().collect(Collectors.toMap(SysUserPO::getId, u -> u));
    }

    private BugChangeHistoryOutput buildOutput(TicketLogPO log,
                                                Map<Long, SysUserPO> userMap,
                                                int seq) {
        BugChangeHistoryOutput output = new BugChangeHistoryOutput();
        output.setId(log.getId());
        output.setSeq(seq);
        output.setChangeTime(formatDate(log.getCreateTime()));

        SysUserPO user = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
        output.setChangeByUserId(log.getUserId());
        output.setChangeByUserName(user != null ? user.getRealName() : "系统");
        output.setChangeByAvatar(user != null ? user.getAvatar() : null);

        // 解析 remark JSON
        List<BugFieldChangeItem> fields = parseFields(log);
        if (fields == null) {
            return null;  // 旧格式 remark，跳过（降级兼容）
        }
        output.setFields(fields);

        // 推断变更类型
        String changeTypeCode = extractChangeType(log);
        BugChangeTypeEnum type = BugChangeTypeEnum.fromCode(changeTypeCode);
        if (type != null) {
            output.setChangeType(type.getCode());
            output.setChangeTypeLabel(type.getLabel());
        }
        return output;
    }
}
```

#### 4.4.3 修正 TicketBugApplicationService（显式记录变更）

在现有三个 update 方法中注入 `TicketChangeHistoryRecorder`，在保存前后记录变更：

```java
@Transactional(rollbackFor = Exception.class)
public void updateCustomerInfo(Long ticketId, TicketBugCustomerInfoInput input, Long userId) {
    TicketPO ticket = requireTicket(ticketId);
    List<String> roleCodes = getRoleCodes(userId);
    assertCanEditCustomerInfo(ticket, userId, roleCodes);

    TicketBugInfoPO infoPO = getOrCreateBugInfo(ticketId);
    // 变更检测必须在 apply 之前
    List<BugFieldChangeItem> changes = changeHistoryRecorder.detectCustomerInfoChanges(infoPO, input);

    applyCustomerInfoChanges(infoPO, input);  // 抽取赋值逻辑为 private 方法
    saveBugInfo(infoPO);

    if (!changes.isEmpty()) {
        changeHistoryRecorder.record(ticketId, userId, BugChangeTypeEnum.MANUAL_CHANGE, changes);
    }
}
```

> **注意**：`TicketApplicationService.createTicket()` 在创建时同样调用 `changeHistoryRecorder.record()` 记录初始字段值（变更类型为 `CREATE`）。

#### 4.4.4 修正 BugReportApplicationService（循环insert DB红线修复）

定位问题代码（syncReportTickets 方法），改为批量 saveBatch：

```java
// 修复前（违反DB红线）
for (Long ticketId : ticketIds) {
    BugReportTicketPO po = new BugReportTicketPO();
    po.setReportId(reportId);
    po.setTicketId(ticketId);
    bugReportTicketMapper.insert(po);   // 循环内单条insert
}

// 修复后
List<BugReportTicketPO> batchList = ticketIds.stream().map(ticketId -> {
    BugReportTicketPO po = new BugReportTicketPO();
    po.setReportId(reportId);
    po.setTicketId(ticketId);
    return po;
}).collect(Collectors.toList());
bugReportTicketService.saveBatch(batchList);  // 批量insert
```

#### 4.4.5 扩展 TicketApplicationService.getTicketDetail()

在现有 detail 组装逻辑中，增加 `bugSummaryInfo` 字段的组装：

```java
// 在 getTicketDetail() 末尾增加
BugSummaryInfoOutput bugSummaryInfo = buildBugSummaryInfo(detail.getId());
detail.setBugSummaryInfo(bugSummaryInfo);
```

```java
private BugSummaryInfoOutput buildBugSummaryInfo(Long ticketId) {
    // 通过 bug_report_ticket 表查关联简报（取最新一条）
    BugReportTicketPO reportTicket = bugReportTicketMapper.selectLatestByTicketId(ticketId);
    if (reportTicket == null) {
        return null;
    }
    BugReportPO report = bugReportMapper.selectById(reportTicket.getReportId());
    if (report == null) {
        return null;
    }

    BugSummaryInfoOutput output = new BugSummaryInfoOutput();
    output.setBugReportId(report.getId());
    output.setBugReportNo(report.getReportNo());
    output.setDefectCategory(report.getDefectCategory());

    // 查询责任人列表（批量查）
    List<BugReportResponsiblePO> responsibles = bugReportResponsibleMapper
        .selectByReportId(report.getId());
    if (!CollectionUtils.isEmpty(responsibles)) {
        Set<Long> userIds = responsibles.stream()
            .map(BugReportResponsiblePO::getUserId)
            .collect(Collectors.toSet());
        List<SysUserPO> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
        String names = users.stream().map(SysUserPO::getRealName)
                            .collect(Collectors.joining("、"));
        output.setResponsibleUserName(names);
    }

    // 计算是否逾期
    TicketPO ticket = ticketMapper.selectById(ticketId);
    if (ticket != null && ticket.getExpectedTime() != null
        && !isTerminalStatus(ticket.getStatus())) {
        output.setOverdue(new Date().after(ticket.getExpectedTime()));
    }
    return output;
}
```

### 4.5 Controller 层

#### 4.5.1 新增 TicketChangeHistoryController

**文件**：`ticket-controller/.../ticket/TicketChangeHistoryController.java`

| 接口编号 | 方法 | 路径 | 描述 |
|---------|------|------|------|
| API000501 | GET | `/api/ticket/{ticketId}/change-history` | 查询缺陷变更历史列表 |

```java
/**
 * 缺陷变更历史控制器
 */
@RestController
@RequestMapping("/api/ticket")
@Api(tags = "缺陷变更历史")
public class TicketChangeHistoryController {

    @Resource
    private TicketChangeHistoryApplicationService changeHistoryService;

    /**
     * 查询缺陷变更历史
     * 接口编号：API000501
     * 产品文档功能：缺陷详情-变更历史Tab
     *
     * @param ticketId  工单ID
     * @param changeType 变更类型筛选（可选）
     * @param fieldName  变更字段筛选（可选）
     */
    @GetMapping("/{ticketId}/change-history")
    @ApiOperation(value = "查询缺陷变更历史", notes = "接口编号：API000501")
    public ApiResult<List<BugChangeHistoryOutput>> listChangeHistory(
            @PathVariable Long ticketId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String fieldName) {
        List<BugChangeHistoryOutput> result =
            changeHistoryService.listChangeHistory(ticketId, changeType, fieldName);
        return ApiResult.success(result);
    }
}
```

---

## 五、前端开发方案

### 5.1 新增组件清单

```
miduo-frontend/src/views/ticket/components/bug/
├── BugDetailInfoPanel.vue        右侧基础信息面板（完整字段体系）
├── BugChangeHistory.vue          变更历史Tab主组件
├── BugChangeHistoryFilter.vue    筛选栏（变更内容/变更方式）
├── BugChangeHistoryItem.vue      单条变更记录（含多字段展示）
└── BugStatusBadge.vue            状态徽章组件（颜色映射）
```

### 5.2 新增类型定义

**文件**：`src/types/ticket.ts` 追加：

```typescript
// 单个字段变更明细
export interface BugFieldChangeItem {
  fieldName: string
  fieldLabel: string
  oldValue: string | null
  oldLabel: string | null
  newValue: string | null
  newLabel: string | null
}

// 变更历史条目
export interface BugChangeHistoryOutput {
  id: number
  seq: number
  changeTime: string
  changeByUserId: number
  changeByUserName: string
  changeByAvatar: string | null
  changeType: string
  changeTypeLabel: string
  fields: BugFieldChangeItem[]
}

// 缺陷简报摘要信息（从简报关联获取）
export interface BugSummaryInfoOutput {
  bugReportId: number | null
  bugReportNo: string | null
  defectCategory: string | null
  defectCategoryLabel: string | null
  isValidReport: string | null
  isValidReportLabel: string | null
  responsibleUserName: string | null
  isOverdue: boolean | null
}

// 扩展现有 TicketDetailOutput
// 在现有 bugInfo 后增加：
// bugSummaryInfo?: BugSummaryInfoOutput
```

### 5.3 新增 API 封装

**文件**：`src/api/ticket.ts` 追加：

```typescript
/**
 * 查询缺陷变更历史
 * 接口编号：API000501
 * 产品文档功能：缺陷详情-变更历史Tab
 */
export const getTicketChangeHistory = (
  ticketId: number,
  params?: { changeType?: string; fieldName?: string }
): Promise<ApiResult<BugChangeHistoryOutput[]>> => {
  return http.get(`/api/ticket/${ticketId}/change-history`, { params })
}
```

### 5.4 BugStatusBadge 组件规格

```typescript
// 状态颜色映射（对应 PRD 3.2.1）
const STATUS_COLOR_MAP: Record<string, string> = {
  pending_assign:       '#909399',
  pending_accept:       '#909399',
  processing:           '#1675d1',
  pending_test_accept:  '#E6A23C',
  testing:              '#E6A23C',
  pending_dev_accept:   '#6B7280',
  developing:           '#3B82F6',
  pending_cs_confirm:   '#F59E0B',
  pending_verify:       '#8B5CF6',
  completed:            '#67C23A',
  closed:               '#67C23A',
  suspended:            '#F56C6C',
}
```

### 5.5 BugChangeHistoryItem 组件规格

字段变更展示逻辑：

```
// 有旧值有新值 → 显示 "旧值（删除线/灰色）→ 新值"
// 仅有新值（创建场景）→ 直接显示新值
// 仅有旧值（删除场景）→ 显示旧值（删除线）+（已删除）红字
// 旧值和新值都是较长文本 → 展示前100字，超出"..."并提供展开按钮
```

### 5.6 TicketDetailView 改造点

在现有 `TicketDetailView.vue` 的 Tab 体系中增加「变更历史」Tab：

```vue
<el-tabs v-model="activeTab">
  <el-tab-pane label="详细信息" name="detail">
    <!-- 现有内容保持不变 -->
  </el-tab-pane>
  <el-tab-pane :label="`变更历史(${changeHistoryCount})`" name="history">
    <BugChangeHistory :ticket-id="ticketId" @count-update="changeHistoryCount = $event" />
  </el-tab-pane>
  <el-tab-pane :label="`评论(${commentCount})`" name="comment">
    <!-- 现有评论内容 -->
  </el-tab-pane>
  <el-tab-pane label="更多" name="more" disabled>
    <!-- 预留 -->
  </el-tab-pane>
</el-tabs>
```

右侧信息面板独立为 `BugDetailInfoPanel` 组件，从 `TicketDetailView` 中抽取，接收 `detail` 对象，内部处理字段展示和行内编辑逻辑。

### 5.7 缺陷上一条/下一条导航

前端实现方案（无需后端接口）：

```typescript
// 列表页打开详情时，传递前后ID到路由
router.push({
  path: `/ticket/detail/${ticketId}`,
  state: {
    prevId: ticketList[index - 1]?.id ?? null,
    nextId: ticketList[index + 1]?.id ?? null
  }
})

// 详情页读取
const navState = history.state as { prevId: number | null; nextId: number | null }
const prevId = navState?.prevId
const nextId = navState?.nextId
```

---

## 六、接口编号完整规划（API000500 段）

| 接口编号 | HTTP方法 | 接口路径 | 功能描述 | 开发状态 |
|---------|---------|---------|---------|---------|
| API000501 | GET | `/api/ticket/{ticketId}/change-history` | 查询缺陷变更历史列表 | ⏳待开发 |

> **已有接口复用**（不新增编号）：
> - `GET /api/ticket/detail/{id}`（API000008）：扩展返回 `bugSummaryInfo` 字段，向后兼容

---

## 七、Task 执行计划

严格按序执行，每个 Task 完成后可独立编译验证：

| Task | 内容 | 影响范围 | 估时 |
|------|------|---------|------|
| task001 | 产品方案设计 & 技术分析（本文档） | 文档 | 已完成 |
| task002 | 新增 `BugChangeTypeEnum` 枚举 | ticket-common | 0.5h |
| task003 | 新增 DTO：`BugChangeHistoryOutput`、`BugFieldChangeItem`、`BugSummaryInfoOutput` | ticket-entity | 1h |
| task004 | 扩展 `TicketDetailOutput`，增加 `bugSummaryInfo` 字段 | ticket-entity | 0.5h |
| task005 | 新增 `BugChangeHistoryPageInput` | ticket-entity | 0.5h |
| task006 | TicketLogMapper 扩展 + Mapper XML | ticket-infrastructure | 1h |
| task007 | 新增 `TicketChangeHistoryRecorder`（变更检测 + 写入） | ticket-application | 2h |
| task008 | 增强 `TicketBugApplicationService`（注入 recorder，显式记录变更） | ticket-application | 1.5h |
| task009 | 新增 `TicketChangeHistoryApplicationService`（历史查询组装） | ticket-application | 1.5h |
| task010 | 扩展 `TicketApplicationService.getTicketDetail()`（组装 bugSummaryInfo） | ticket-application | 1h |
| task011 | **修复 `BugReportApplicationService` DB 红线**（批量 insert） | ticket-application | 0.5h |
| task012 | 新增 `TicketChangeHistoryController` | ticket-controller | 0.5h |
| task013 | 前端：新增 TypeScript 类型定义 | miduo-frontend | 0.5h |
| task014 | 前端：新增 API 封装 | miduo-frontend | 0.5h |
| task015 | 前端：开发 `BugStatusBadge`、`BugDetailInfoPanel` 组件 | miduo-frontend | 2h |
| task016 | 前端：开发 `BugChangeHistory`、`BugChangeHistoryItem` 组件 | miduo-frontend | 3h |
| task017 | 前端：改造 `TicketDetailView`（增加变更历史Tab、右侧面板独立） | miduo-frontend | 2h |
| task018 | 联调测试：创建测试工单 → 修改字段 → 验证变更历史记录准确性 | 全栈 | 2h |

---

## 八、质量保障要求

### 8.1 必须通过编译

- 后端：`mvn clean install -DskipTests` 无编译错误；
- 前端：`npm run build` 无 TypeScript 报错。

### 8.2 关键测试场景

| 测试场景 | 预期结果 |
|---------|---------|
| 新建缺陷工单 | ticket_log 中有 CREATE 类型记录，fields 包含初始填写的所有字段 |
| 修改客服信息（公司名称） | ticket_log 新增一条记录，remark JSON 中包含 company_name 字段变更 |
| 一次修改多个测试字段 | 所有字段变更合并在同一条 ticket_log.remark 中 |
| 状态流转 | ticket_log 新增状态流转记录，changeType 为 STATUS_CHANGE |
| 查询变更历史（筛选状态变更） | 仅返回 changeType=STATUS_CHANGE 的记录 |
| 查询变更历史（筛选公司名称字段） | 仅返回 fields 中包含 company_name 的记录 |
| 工单无关联简报时查看详情 | bugSummaryInfo 为 null，右侧面板相关字段展示「-」 |
| 工单有关联简报时查看详情 | bugSummaryInfo 正确返回缺陷划分、有效报告、责任人 |
| 逾期工单 | expectedTime < now 且状态非终态时，isOverdue=true |

### 8.3 代码质量红线（不得违反）

- 禁止在循环内进行数据库单条查询（包括新增的历史记录组装逻辑）；
- 禁止在 Controller 层直接操作 Mapper；
- remark 字段长度不超过 450 字符（留 50 字符冗余，防止超出 varchar(500) 限制）；
- 所有新增枚举使用常量引用，禁止 magic string。
