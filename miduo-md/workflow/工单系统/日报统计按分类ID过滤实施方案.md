# 日报统计按分类ID过滤实施方案

## 1. 方案概要

本次改造在不新增接口的前提下，扩展日报配置能力：  
通过新增配置键 `daily_report_stat_category_ids`，将日报统计口径从“全量工单”收敛到“指定分类ID工单”。

核心原则：

1. 只按分类ID过滤，不按名称过滤。
2. 预览、推送、分区统计统一口径。
3. 多环境独立配置，代码保持一致。

## 2. 影响范围

## 2.1 后端

- `ticket-entity`：日报配置DTO新增 `statCategoryIds`
- `ticket-application`：日报服务读取并解析分类ID配置
- `ticket-infrastructure`：日报Mapper接口和SQL增加 `categoryIds` 参数与过滤条件
- `ticket-bootstrap`：新增Flyway配置初始化脚本

## 2.2 前端

- 日报配置页面新增“统计范围分类（多选）”
- 复用分类树接口提供可选项
- 配置提交/回显新增 `statCategoryIds`

## 2.3 数据库

- `system_config` 新增配置键：`daily_report_stat_category_ids`

## 3. 关键决策

1. **按ID而非名称**
   - 解决“测试=功能缺陷、生产=技术缺陷”命名差异问题。

2. **统一过滤口径**
   - 不仅改总数，所有分区统计同步过滤，避免“总数与明细不一致”。

3. **默认兼容策略**
   - 当配置为空时，暂按旧口径全量统计，避免发布后立刻出现统计归零风险。

## 4. 实施步骤

1. 新增配置项初始化脚本（Flyway）
2. 后端DTO扩展字段
3. 日报服务扩展配置读写与ID解析
4. Mapper接口和SQL增加分类过滤
5. 前端配置页新增分类多选并联调
6. 测试环境配置功能缺陷ID并验收
7. 生产环境配置技术缺陷ID并发布验证

## 5. 验证计划

## 5.1 接口验证

- `GET /api/daily-report/config`：返回 `statCategoryIds`
- `PUT /api/daily-report/config`：可保存 `statCategoryIds`
- `GET /api/daily-report/preview`：总数和分区统计均按分类ID过滤

## 5.2 页面验证

- 日报配置页可选择分类并保存
- 刷新后可正确回显
- 预览页统计值变化符合配置预期

## 5.3 推送验证

- 手动推送消息中的统计数据与预览一致

## 6. 发布与回滚

## 6.1 发布前准备

1. 测试环境确认功能缺陷分类ID
2. 生产环境确认技术缺陷分类ID
3. 准备环境配置变更单

## 6.2 回滚方案

1. 快速回滚：清空 `daily_report_stat_category_ids`
2. 代码回滚：回退发布版本

## 7. 风险清单

1. **ID配置错误**
   - 影响：统计异常
   - 应对：发布前通过分类树接口核对ID

2. **只改总数未改分区**
   - 影响：口径不一致
   - 应对：所有日报查询统一增加 `categoryIds` 过滤
