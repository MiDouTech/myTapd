# 设计文档：企微视频附件兼容方案

## 企微视频消息格式（官方文档 path/90239）

```xml
<xml>
  <ToUserName><![CDATA[toUser]]></ToUserName>
  <FromUserName><![CDATA[fromUser]]></FromUserName>
  <CreateTime>1348831860</CreateTime>
  <MsgType><![CDATA[video]]></MsgType>
  <MediaId><![CDATA[media_id]]></MediaId>
  <ThumbMediaId><![CDATA[thumb_media_id]]></ThumbMediaId>
  <MsgId>1234567890123456</MsgId>
</xml>
```

关键约束：
- 无 `PicUrl`，无 `DownloadUrl`，只能通过 `MediaId` 调用 `media/get` 接口下载
- `ThumbMediaId` 对应封面缩略图，体积小，可用字节数组下载
- AI Bot（智能机器人）不产生视频消息，无需处理 JSON 格式

## 处理流程设计

```
MsgType=video 回调
        │
        ▼  (@Async)
handleVideoMessageAsync(message)
        │
        ├─① InputStream videoStream = wecomClient.downloadMediaStream(mediaId)
        │
        ├─② String videoUrl = qiniuUploadService.uploadStream(
        │        videoStream, "wecom_<msgId>.mp4", "video/mp4", videoPathPrefix)
        │
        ├─③ [if thumbMediaId非空]
        │    byte[] thumbBytes = wecomClient.downloadMediaById(thumbMediaId)
        │    String thumbUrl = qiniuUploadService.uploadImageBytes(thumbBytes, "thumb_<msgId>.jpg")
        │
        ├─④ savePendingRecord(... media_type='video', qiniu_url=videoUrl, thumb_url=thumbUrl)
        │
        └─⑤ tryLinkToRecentTicket(pending, config)
                └─ createAttachmentRecord(ticketId, videoUrl, msgId, 'video')
                        file_type='video/mp4', file_name='企微视频'
```

## 数据模型变更

### wecom_pending_image 表扩展（向后兼容）

```sql
ALTER TABLE wecom_pending_image
  ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'image'
      COMMENT '媒体类型：image/video',
  ADD COLUMN thumb_url  VARCHAR(500) DEFAULT NULL
      COMMENT '视频缩略图七牛URL（仅视频消息有值）';
```

现有记录 `media_type` 自动填充为 `'image'`，完全兼容。

## 关键决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 视频上传方式 | InputStream 流式 | 视频体积大（企微限制25MB），字节数组会撑爆内存 |
| 是否新建暂存表 | 扩展现有表（方案A） | 改动小，Rule A/B 逻辑完全复用，向后兼容 |
| 前端视频播放 | `<video controls>` + poster封面 | HTML5原生，Element Plus无专用视频组件 |
| 视频 MIME | 固定 `video/mp4` | 企微上传限制为MP4，无需动态推断 |
| 缩略图策略 | 上传至七牛，存 thumb_url 字段 | 封面预览无需实时从企微下载，避免临时URL失效 |

## 兼容性保证

- `WecomImageHandlerService.handleImageMessageAsync` 代码不改动
- `linkPendingImagesToTicket` 查询不加 `media_type` 过滤，图片和视频都关联
- `createAttachmentRecord` 通过参数区分类型，不影响现有图片逻辑

## 风险与应对

| 风险 | 概率 | 应对 |
|------|------|------|
| 企微 `media/get` 视频下载超时 | 中 | InputStream 超时配置 30s，失败记 FAILED 状态，不影响工单创建 |
| 七牛流式上传视频失败 | 低 | try-catch 后 `qiniuUrl=null`，暂存记录状态置 FAILED，工单不报错 |
| 缩略图 `ThumbMediaId` 为空 | 低 | null 保护，`thumbUrl` 为 null，不影响视频本体上传 |

## 文件变更清单

```
ticket-bootstrap/src/main/resources/db/migration/
└── V20__add_video_support_to_pending_media.sql

ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/wecom/
└── WecomCallbackMessageDTO.java        [+thumbMediaId]

ticket-infrastructure/...
├── persistence/mybatis/wecom/po/WecomPendingImagePO.java  [+mediaType, +thumbUrl]
├── external/qiniu/QiniuUploadService.java                [+uploadStream]
├── external/qiniu/QiniuProperties.java                   [+videoPathPrefix]
└── external/wework/WecomClient.java                      [+downloadMediaStream]

ticket-application/...
├── wecom/WecomCallbackApplicationService.java            [+video分支, +ThumbMediaId映射]
└── wecom/WecomImageHandlerService.java                   [+handleVideoMessageAsync, 改createAttachmentRecord]

miduo-frontend/src/...
└── 工单附件展示组件                                        [image/video条件渲染]
```
