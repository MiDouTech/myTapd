# 业务原生工单插件实现

| 类型 | 模块 | 描述 |
|---|---|---|
| feat | 工单插件 | 新增接入应用管理（API000527-530）、LaunchToken 签发（API531）、插件建单/查单/配置（API532-534/536） |
| feat | 工单插件 | 新增 ticket-sdk（modal/float），构建产物发布至 `/sdk/v1/ticket-sdk.min.js` |
| feat | 工单插件 | 管理端「接入应用」配置页、工单详情插件上下文面板 |
| feat | 工单插件 | 按应用 Webhook 回调推送（integration_app.callback_url） |
| chore | 数据库 | Flyway V62：integration_app 表 + ticket 插件上下文字段 |
