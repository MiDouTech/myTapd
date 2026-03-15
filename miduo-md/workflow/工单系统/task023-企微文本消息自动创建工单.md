# Task023：企微文本消息自动创建工单

> **业务模块**：企业微信集成 — 自然语言建单  
> **依赖**：Task006（企微深度集成）、Task020（系统设置增强）、Task021（接口编号治理）  
> **预估工时**：9.5 人天  
> **对应产品文档**：《企微文本消息自动创建工单产品方案》第三章 ~ 第六章

---

## 一、任务目标

在现有企微群机器人工单体系（格式化模板建单）的基础上，新增**自然语言文本建单**能力：

- 用户在企微群 @工单助手 后，可发送**任意自由文本**，系统自动解析意图、提取字段，返回工单预览卡片
- 用户回复数字指令完成确认/修改/取消，最终正式创建工单
- 提供管理后台对关键词规则进行动态配置

---

## 二、功能清单

| 功能点 | 优先级 | 说明 |
|--------|--------|------|
| 自然语言消息意图识别 | P0 | 区分建单/查询/催办/帮助/无效 |
| 分类关键词自动推断 | P0 | 基于规则引擎匹配工单分类 |
| 优先级信号词识别 | P0 | 紧急/高/中/低自动判断 |
| 缺陷关键实体提取 | P0 | 商户编号、公司名称、账号等 |
| 工单草稿预览卡片 | P0 | 文本卡片展示预填内容 |
| 数字指令确认/修改/取消 | P0 | 交互式确认流程 |
| Redis 会话状态管理 | P0 | 60/300 秒 TTL，支持多用户并发 |
| 群聊 vs 私聊差异处理 | P1 | 触发方式、超时时长差异 |
| NLP 关键词配置 CRUD | P1 | 管理端动态配置关键词规则 |
| NLP 解析日志查询 | P1 | 运营监控建单效果 |
| 系统设置配置项 | P2 | 启用开关、置信度阈值等 |

---

## 三、后端实现

### 3.1 数据库变更

**新增表**：`wecom_nlp_keyword`

```sql
-- Flyway 迁移脚本：V13__add_wecom_nlp_keyword.sql
CREATE TABLE `wecom_nlp_keyword` (
  `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword`      varchar(50) NOT NULL COMMENT '关键词',
  `match_type`   tinyint NOT NULL COMMENT '匹配类型：1=分类 2=优先级 3=实体',
  `target_value` varchar(100) NOT NULL COMMENT '映射目标值（分类路径/优先级枚举/实体类型）',
  `confidence`   tinyint NOT NULL DEFAULT 80 COMMENT '置信度(0-100)',
  `sort_order`   int NOT NULL DEFAULT 0 COMMENT '排序，数值越大优先级越高',
  `is_active`    tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
  `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`    varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by`    varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted`      tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_match_type` (`match_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微自然语言解析关键词配置表';

-- 初始化常用关键词数据
INSERT INTO `wecom_nlp_keyword` (`keyword`,`match_type`,`target_value`,`confidence`,`sort_order`) VALUES
('缺陷',1,'研发需求/缺陷修复',90,100),
('bug',1,'研发需求/缺陷修复',90,100),
('Bug',1,'研发需求/缺陷修复',90,100),
('BUG',1,'研发需求/缺陷修复',90,100),
('报错',1,'研发需求/缺陷修复',85,90),
('空白',1,'研发需求/缺陷修复',80,80),
('无法',1,'研发需求/缺陷修复',75,70),
('不能',1,'研发需求/缺陷修复',75,70),
('商户',1,'研发需求/缺陷修复',80,80),
('申请',1,'IT支持',75,60),
('账号',1,'IT支持/软件问题',80,70),
('权限',1,'IT支持/软件问题',80,70),
('VPN',1,'IT支持/网络问题',90,90),
('网络',1,'IT支持/网络问题',80,70),
('打印机',1,'IT支持/硬件问题',90,90),
('电脑',1,'IT支持/硬件问题',75,60),
('请假',1,'人事服务',90,90),
('打卡',1,'人事服务',90,90),
('薪资',1,'人事服务',90,90),
('需求',1,'研发需求/功能需求',80,70),
('功能',1,'研发需求/功能需求',75,60),
('紧急',2,'urgent',95,100),
('急',2,'urgent',85,90),
('马上',2,'urgent',85,90),
('立刻',2,'urgent',85,90),
('高优',2,'high',90,80),
('重要',2,'high',80,70),
('不急',2,'low',90,80),
('优化建议',2,'low',85,70);
```

**扩展表**：`wecom_bot_message_log` 新增字段

```sql
ALTER TABLE `wecom_bot_message_log`
  ADD COLUMN `parse_type` varchar(20) DEFAULT NULL COMMENT '解析类型：template=格式模板 natural_language=自然语言' AFTER `status`,
  ADD COLUMN `nlp_confidence` tinyint DEFAULT NULL COMMENT 'NLU解析置信度(0-100)，自然语言解析时记录' AFTER `parse_type`;
```

### 3.2 新增实体类（miduo-mbg 模块）

**`WecomNlpKeywordPO`**（路径：`persistence/mybatis/wecom/po/`）

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_nlp_keyword")
public class WecomNlpKeywordPO extends BaseEntity {
    @TableField("keyword")
    private String keyword;
    @TableField("match_type")
    private Integer matchType;
    @TableField("target_value")
    private String targetValue;
    @TableField("confidence")
    private Integer confidence;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("is_active")
    private Integer isActive;
}
```

### 3.3 新增 DTO（miduo-entity 模块）

路径：`dto/wecom/`

- `NlpKeywordListOutput`：关键词列表响应
- `NlpKeywordCreateInput`：创建请求
- `NlpKeywordUpdateInput`：更新请求
- `NlpAnalyzeResult`：NLU 解析结果（内部传递，非接口 DTO）
- `NlpLogPageInput`：解析日志分页查询
- `NlpLogPageOutput`：解析日志分页响应

### 3.4 核心服务实现

#### 3.4.1 `WecomNaturalLangParser`（NLU 规则引擎）

```java
package com.miduo.cloud.ticket.application.wecom;

/**
 * 企微自然语言消息解析器
 * 接口编号：内部组件，非对外 API
 */
@Component
public class WecomNaturalLangParser {
    // 分析意图、提取分类/优先级/实体
    public NlpAnalyzeResult analyze(String text, String defaultCategoryPath);
}
```

**内部逻辑**：
1. 从 Redis 缓存（TTL 5分钟）或数据库读取关键词配置列表
2. 按 `sort_order` 降序排列，逐条匹配
3. 分类匹配：取最高置信度的匹配结果；置信度 < 70 时分类留空
4. 优先级匹配：取首个匹配结果
5. 实体提取：正则匹配商户编号（8-12位数字）、手机号（1[3-9]\d{9}）
6. 标题生成：截取前 50 字符

#### 3.4.2 `WecomDraftSessionService`（会话状态管理）

```java
package com.miduo.cloud.ticket.application.wecom;

/**
 * 企微工单草稿会话管理服务
 * 基于 Redis 存储，支持群聊(60s TTL)和私聊(300s TTL)
 */
@Service
public class WecomDraftSessionService {
    // 创建/获取/更新/删除草稿会话
    public void saveDraft(String chatOrUserId, String wecomUserId, WecomDraftSession session, boolean isGroup);
    public WecomDraftSession getDraft(String chatOrUserId, String wecomUserId);
    public void removeDraft(String chatOrUserId, String wecomUserId);
}
```

#### 3.4.3 `WecomInteractiveConfirmService`（确认流程处理器）

处理用户回复的数字指令：

| 指令 | 处理逻辑 |
|------|----------|
| `1` | 确认创建：调用 `TicketApplicationService.createTicket()` |
| `2` | 修改分类：更新 Redis 会话步骤为 `MODIFY_CATEGORY`，返回分类列表 |
| `3` | 修改优先级：更新步骤为 `MODIFY_PRIORITY`，返回优先级选项 |
| `4` | 补充描述：更新步骤为 `SUPPLEMENT_DESC` |
| `0` | 取消：删除 Redis 会话，回复取消确认 |
| 其他 | 保持会话，提示输入有效指令 |

#### 3.4.4 `WecomBotMessageParser` 改造

在 `parse()` 方法中新增路由分支：

```java
// 新增：处理自然语言消息
// 条件：不以 # 开头，且不匹配任何已知指令前缀
return WecomBotParseResult.naturalLanguage(rawContent);
```

新增枚举值 `NATURAL_LANGUAGE` 到 `WecomBotCommandType`。

#### 3.4.5 `WecomMessageProcessor` 改造

在处理入口增加会话检测逻辑：

```java
// 优先检查用户是否有待确认草稿
WecomDraftSession draft = draftSessionService.getDraft(chatId, fromWecomUserId);
if (draft != null) {
    // 进入确认流程
    return interactiveConfirmService.handleReply(content, draft, chatId, fromWecomUserId);
}
// 继续原有消息解析流程...
```

### 3.5 Controller 层（新增接口）

**`WecomNlpKeywordController`**（路径：`controller/wecom/`）

```java
/**
 * 接口编号：API000432 - 查询NLP关键词列表
 */
@GetMapping("/api/wecom/nlp-keyword/list")
public CommonResult<List<NlpKeywordListOutput>> listKeywords(Integer matchType);

/**
 * 接口编号：API000433 - 创建NLP关键词
 */
@PostMapping("/api/wecom/nlp-keyword/create")
public CommonResult<Long> createKeyword(@RequestBody @Valid NlpKeywordCreateInput input);

/**
 * 接口编号：API000434 - 更新NLP关键词
 */
@PutMapping("/api/wecom/nlp-keyword/update/{id}")
public CommonResult<Void> updateKeyword(@PathVariable Long id, @RequestBody @Valid NlpKeywordUpdateInput input);

/**
 * 接口编号：API000435 - 删除NLP关键词
 */
@DeleteMapping("/api/wecom/nlp-keyword/delete/{id}")
public CommonResult<Void> deleteKeyword(@PathVariable Long id);

/**
 * 接口编号：API000436 - NLP解析日志分页查询
 */
@GetMapping("/api/wecom/nlp-log/page")
public CommonResult<PageOutput<NlpLogPageOutput>> pageNlpLogs(@Valid NlpLogPageInput input);
```

---

## 四、前端实现

### 4.1 新增 API 封装（`src/api/wecom.ts` 扩展）

```typescript
// NLP关键词管理
export const listNlpKeywords = (matchType?: number) => ...
export const createNlpKeyword = (data: NlpKeywordCreateInput) => ...
export const updateNlpKeyword = (id: number, data: NlpKeywordUpdateInput) => ...
export const deleteNlpKeyword = (id: number) => ...
export const pageNlpLogs = (params: NlpLogPageInput) => ...
```

### 4.2 新增页面

**关键词管理页**：`src/views/manage/components/WecomNlpKeywordPanel.vue`

- 列表展示（支持按类型筛选）
- 新增/编辑对话框
- 删除确认
- 启用/禁用切换

**NLP 解析日志页**：`src/views/manage/components/WecomNlpLogPanel.vue`

- 分页列表
- 筛选条件：时间范围、解析类型、置信度范围
- 展示：原始消息、解析结果、置信度、是否建单

### 4.3 系统设置扩展

在 `WecomConfigPanel.vue` 中新增「自然语言建单」配置区：
- 功能开关（调用 `/api/system/config` 保存）
- 关键词管理入口按钮
- NLP 日志入口按钮

---

## 五、接口编号更新

| 接口编号 | 接口名称 | HTTP方法 | 接口路径 | 产品文档功能点 | 开发状态 |
|----------|----------|----------|----------|--------------|----------|
| API000432 | 查询NLP关键词配置列表 | GET | /api/wecom/nlp-keyword/list | 企微自然语言建单 - 关键词管理 | ⏳待开发 |
| API000433 | 创建NLP关键词配置 | POST | /api/wecom/nlp-keyword/create | 企微自然语言建单 - 新增关键词 | ⏳待开发 |
| API000434 | 更新NLP关键词配置 | PUT | /api/wecom/nlp-keyword/update/{id} | 企微自然语言建单 - 修改关键词 | ⏳待开发 |
| API000435 | 删除NLP关键词配置 | DELETE | /api/wecom/nlp-keyword/delete/{id} | 企微自然语言建单 - 删除关键词 | ⏳待开发 |
| API000436 | NLP解析日志分页查询 | GET | /api/wecom/nlp-log/page | 企微自然语言建单 - 解析日志 | ⏳待开发 |

> 当前最大编号：API000436  
> 下一个可用编号：API000437

---

## 六、验收标准

### 后端验收

- [ ] `V13__add_wecom_nlp_keyword.sql` Flyway 脚本执行成功
- [ ] `WecomNaturalLangParser` 对主流场景（缺陷/IT申请/人事/研发需求）识别准确
- [ ] 草稿会话 TTL 符合设计（群聊 60s，私聊 300s）
- [ ] 格式化模板消息走原有流程，自然语言消息走新流程（向后兼容）
- [ ] API000432~436 接口可正常调用
- [ ] `mvn clean install -DskipTests` 编译通过

### 前端验收

- [ ] 关键词管理页面增删改查正常
- [ ] NLP 解析日志页面分页查询正常
- [ ] 系统设置页面新增配置区展示正常
- [ ] `npm run build` 编译通过
- [ ] `npm run lint` 无报错

### 企微端验收（需真实企微环境）

- [ ] 群内 @工单助手 发送自由文本，60 秒内收到预览卡片回复
- [ ] 回复 `1` 创建成功，收到工单编号
- [ ] 回复 `2` 进入分类修改引导
- [ ] 回复 `0` 取消后不创建工单
- [ ] 超时 60 秒后再次发送，触发新会话
- [ ] 发送格式化模板（`#分类 标题`）时，走原有模板流程

---

## 七、注意事项

1. **向后兼容**：现有格式化模板建单流程不受影响，优先级：格式化模板 > 指令 > 自然语言
2. **关键词缓存**：NLP 关键词从 DB 加载后缓存到 Redis（5分钟），管理端修改后需清除缓存
3. **并发安全**：同一用户同一时间只能有一个待确认草稿，新消息到来时覆盖旧草稿
4. **企微消息长度**：回复内容不超过 2048 字符，避免被企微截断
5. **Redis 键格式**：`wecom:draft:{sessionKey}` 严格按规范，避免与其他业务 Key 冲突
