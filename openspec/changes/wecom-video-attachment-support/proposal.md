# 变更提案：企微推送兼容图片与视频附件

## 背景

当前系统通过企微群机器人回调接收消息，支持将图片附件关联到工单（`MsgType=image` 和 `MsgType=mixed`）。但企微用户在发送工单描述时，**视频消息（`MsgType=video`）被静默丢弃**，导致视频附件无法进入工单系统，信息丢失。

## 现状问题

1. `WecomCallbackApplicationService` 对 `MsgType` 非 `text/image/mixed` 的消息统一走 `saveIgnoredLog`，视频被忽略。
2. `WecomImageHandlerService` 写死 `fileType = "image/jpeg"`、`fileName = "企微截图"`，无法区分图片与视频。
3. `QiniuUploadService` 只支持图片字节数组上传，视频体积大，需要流式上传。
4. `wecom_pending_image` 表没有媒体类型字段，无法区分暂存的是图片还是视频。
5. 前端附件展示组件仅渲染 `<img>`，遇到视频 `fileType` 无法播放。

## 目标

- 兼容企微 `MsgType=video` 消息，将视频文件流式上传七牛并关联到工单。
- 同时保存视频缩略图（`ThumbMediaId`）作为封面预览。
- 前端根据 `fileType` 动态渲染图片或视频播放器。
- 不破坏现有图片附件流水线的任何功能。

## 非目标

- 不支持企微 AI Bot（智能机器人）场景的视频——官方文档未定义该场景。
- 不支持 `MsgType=mixed` 中包含视频的组合（企微实际不产生此类型）。
- 不做视频转码或压缩处理。

## 验收标准

1. 在企微群中 `@机器人` 发送视频，系统回调能正确处理 `MsgType=video`。
2. 视频文件通过流式上传存入七牛，`ticket_attachment.file_type = 'video/mp4'`。
3. 视频缩略图同时上传，`wecom_pending_image.thumb_url` 有值。
4. 创建工单后视频通过 Rule A/B 自动关联。
5. 前端工单详情中视频可播放，封面正确显示。
6. 现有图片附件流程测试全部通过，无回归。

## 影响范围

- **后端**：ticket-entity、ticket-infrastructure、ticket-application 模块
- **数据库**：`wecom_pending_image` 表结构变更（2个新列，向后兼容）
- **前端**：工单附件展示组件（条件渲染）
