# 修复生产环境动态页无数据

| 类型 | 模块 | 描述 |
|---|---|---|
| fix | 动态 | 生产 Docker 镜像构建时打包 CHANGELOG.md、changelogs 和周报文件，并配置 update-center.repo-root |
| fix | 动态 | JDK 8 访问 GitHub API 时强制 TLSv1.2，修复 Git 提交回退读取失败 |
