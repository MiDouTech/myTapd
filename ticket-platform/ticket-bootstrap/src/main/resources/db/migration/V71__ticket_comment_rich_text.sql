-- 内部图片以内嵌数据随授权后的评论返回，不写入公开附件/CDN。
ALTER TABLE ticket_comment MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '评论正文';
ALTER TABLE ticket_comment ADD COLUMN content_format varchar(20) NOT NULL DEFAULT 'PLAIN_TEXT' COMMENT 'PLAIN_TEXT或HTML';
-- 既有内部备注维持纯文本；历史公开评论仍按原富文本契约展示。
UPDATE ticket_comment SET content_format = 'HTML' WHERE visibility = 'PUBLIC';
