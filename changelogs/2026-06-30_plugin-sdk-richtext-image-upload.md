# 更新中心变更记录

| 类型 | 模块 | 描述 |
|---|---|---|
| feat | 工单插件 SDK | 提交工单弹窗升级为富文本编辑框，支持上传图片并在描述中插入图片 |
| feat | 插件开放接口 | 新增 `POST /api/open/v1/plugin/attachments/image`（API000535），支持 LaunchToken 鉴权上传图片 |
| feat | 插件建单 | SDK 提交工单时携带附件 URL 到 `attachments` 字段，便于后端保存附件上下文 |
| fix | SDK 静态发布文件 | 更新 `miduo-frontend/public/sdk/v1/ticket-sdk.min.js` 到富文本版本，避免业务系统继续加载旧 textarea 代码 |
| fix | 插件建单标题 | 标题生成改为提取纯文字：自动移除 `<img>` / HTML / `data:image` 内容，避免标题显示图片编码信息 |
| docs | 工单系统文档 | 同步更新 API 编号与接口映射（API000535/537）及插件方案说明 |
