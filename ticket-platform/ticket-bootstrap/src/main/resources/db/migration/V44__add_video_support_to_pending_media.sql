-- ============================================================
-- wecom-video-attachment-support
-- V44: 扩展 wecom_pending_image 表支持视频媒体类型
-- ============================================================

-- 1. 新增 media_type 字段（默认 'image'，向后兼容）
-- 2. 新增 thumb_url 字段（视频缩略图七牛URL，仅视频消息有值）
ALTER TABLE `wecom_pending_image`
    ADD COLUMN `media_type` VARCHAR(20) NOT NULL DEFAULT 'image'
        COMMENT '媒体类型：image/video' AFTER `qiniu_url`,
    ADD COLUMN `thumb_url`  VARCHAR(500) DEFAULT NULL
        COMMENT '视频缩略图七牛URL（仅视频消息有值）' AFTER `media_type`;
