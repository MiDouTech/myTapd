-- 发布时暂停评论写入，并同时部署所有后端实例及前端。
-- 仅迁移执行前的存量保持公开，不按 type 或内容筛选历史记录。
ALTER TABLE ticket_comment ADD COLUMN visibility varchar(20) NULL COMMENT 'PUBLIC:公开回复 INTERNAL:内部备注';
UPDATE ticket_comment SET visibility = 'PUBLIC' WHERE visibility IS NULL;
ALTER TABLE ticket_comment MODIFY COLUMN visibility varchar(20) NOT NULL DEFAULT 'INTERNAL' COMMENT 'PUBLIC:公开回复 INTERNAL:内部备注';
CREATE INDEX idx_ticket_comment_visibility ON ticket_comment (ticket_id, visibility, create_time, id);
-- 回滚应用时必须保留过滤逻辑；禁止回滚到全量公开读取或删除本列。
