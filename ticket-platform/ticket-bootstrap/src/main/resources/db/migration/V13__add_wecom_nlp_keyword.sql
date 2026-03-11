-- V13：新增企微自然语言解析关键词配置表，并扩展消息日志表字段
-- Task023：企微文本消息自动创建工单

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

ALTER TABLE `wecom_bot_message_log`
  ADD COLUMN `parse_type` varchar(20) DEFAULT NULL COMMENT '解析类型：template=格式模板 natural_language=自然语言' AFTER `status`,
  ADD COLUMN `nlp_confidence` tinyint DEFAULT NULL COMMENT 'NLU解析置信度(0-100)，自然语言解析时记录' AFTER `parse_type`;
