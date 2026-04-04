# 实施任务列表：企微视频附件兼容

## task001 — 数据库迁移 V20

**目标**：扩展 `wecom_pending_image` 表，增加 `media_type` 和 `thumb_url` 字段。

**文件**：`ticket-bootstrap/src/main/resources/db/migration/V20__add_video_support_to_pending_media.sql`

**内容**：
```sql
ALTER TABLE wecom_pending_image
  ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'image'
      COMMENT '媒体类型：image/video',
  ADD COLUMN thumb_url  VARCHAR(500) DEFAULT NULL
      COMMENT '视频缩略图七牛URL（仅视频消息有值）';
```

---

## task002 — DTO / PO 层扩展

**目标**：在 DTO 和 PO 中添加视频相关字段。

**文件1**：`WecomCallbackMessageDTO.java`
- 新增字段：`thumbMediaId`（对应 XML `ThumbMediaId`）

**文件2**：`WecomPendingImagePO.java`
- 新增字段：`mediaType`（对应 `media_type` 列）
- 新增字段：`thumbUrl`（对应 `thumb_url` 列）

---

## task003 — WecomClient 流式下载

**目标**：新增 `downloadMediaStream(String mediaId)` 方法，返回 `InputStream` 而不是 `byte[]`，用于大文件流式处理。

**文件**：`WecomClient.java`

**要点**：
- 基于现有 `downloadMediaById` 的 HTTP 请求逻辑，改为返回响应流
- 调用方负责关闭流

---

## task004 — QiniuUploadService 流式上传扩展

**目标**：新增 `uploadStream` 方法支持视频流式上传，新增配置项 `videoPathPrefix`。

**文件1**：`QiniuUploadService.java`
- 新增 `uploadStream(InputStream stream, String originalName, String contentType, String pathPrefix)` 方法

**文件2**：`QiniuProperties.java`
- 新增 `videoPathPrefix` 字段（默认值 `video/`）

---

## task005 — WecomCallbackApplicationService 视频分支

**目标**：在回调分发逻辑中新增 `MsgType=video` 处理分支；在 `buildMessage` 中映射 `ThumbMediaId`。

**文件**：`WecomCallbackApplicationService.java`

**变更**：
1. `buildMessage` 中添加 `message.setThumbMediaId(safeValue(messageMap.get("ThumbMediaId")))`
2. 在 `image` 分支之后增加 `video` 分支，调用 `imageHandlerService.handleVideoMessageAsync(message)`

---

## task006 — WecomImageHandlerService 视频处理

**目标**：新增 `handleVideoMessageAsync` 方法，并改造 `createAttachmentRecord` 支持动态媒体类型。

**文件**：`WecomImageHandlerService.java`

**变更**：
1. 新增 `@Async handleVideoMessageAsync(WecomCallbackMessageDTO message)` 方法
   - 流式下载视频 → 上传七牛 → 下载缩略图 → 写暂存记录（media_type=video）→ tryLinkToRecentTicket
2. `createAttachmentRecord` 增加 `mediaType` 参数
   - `image` → `fileType="image/jpeg"`, `fileName="企微截图"`
   - `video` → `fileType="video/mp4"`, `fileName="企微视频"`
3. `linkPendingImagesToTicket` 中调用 `createAttachmentRecord` 时传入 `pending.getMediaType()`

---

## task007 — WecomXmlParser 视频 XML 解析（仅 aibot JSON 路径补充）

**目标**：确保 `parseAibotJson` 对视频消息 `MsgType=video` 有兜底处理（记录日志，提取 MediaId）。

**文件**：`WecomXmlParser.java`

**说明**：普通应用回调走 `parseFirstLevel`，已能自动解析 `ThumbMediaId`，无需额外修改。只需在 `parseAibotJson` 的 else 分支中为 `video` 类型补充 MediaId 提取逻辑（实际 AI Bot 不产生视频，属防御性代码）。

---

## task008 — 前端附件展示组件条件渲染

**目标**：工单详情中的附件展示组件，根据 `fileType` 动态渲染图片（`<el-image>`）或视频（`<video>`）。

**文件**：前端工单附件展示组件（需定位具体文件）

**要点**：
- `fileType` 以 `image/` 开头 → `<el-image>` 带预览
- `fileType` 以 `video/` 开头 → `<video controls :poster="thumbUrl">` 带封面
- 其他 → 下载链接兜底

---

## 执行顺序

task001 → task002 → task003 → task004 → task005 → task006 → task007 → task008
