<script setup lang="ts">
import { computed } from 'vue'
import type { DefectCategoryOutput } from '@/types/bugreport'
import { BUGREPORT_LOGIC_CAUSE_GUIDE } from '@/constants/bugreport-logic-cause'

/**
 * Bug 简报编辑页「填写说明」正文：桌面侧栏与移动端抽屉共用，避免重复维护。
 */
const props = defineProps<{
  /** sidebar：与桌面侧栏一致（悬浮提示 + 标签预览）；drawer：移动端抽屉内展示（默认折叠区块、展示全部标签） */
  layout: 'sidebar' | 'drawer'
  /** 缺陷分类字典：与编辑页下拉同源，避免“右侧说明”和“下拉选项”口径不一致 */
  defectCategories?: DefectCategoryOutput[]
}>()

const defectCategoryItems = computed(() => {
  const source = props.defectCategories || []
  return source.map((item, index) => ({
    no: String(index + 1),
    name: item.name,
    desc: item.description || '暂无说明',
  }))
})
</script>

<template>
  <div
    class="instruction-body"
    :class="{ 'instruction-body--drawer': layout === 'drawer' }"
  >
    <el-collapse
      v-if="layout === 'drawer'"
      class="instruction-mobile-collapse"
    >
      <el-collapse-item title="逻辑归因" name="logic">
        <div class="inst-section inst-section--flat">
          <div class="inst-section-desc">
            优先从备选列表挑选，若备选项不符合则与上级沟通后再确定。然后补充具体原因描述
          </div>
          <div class="logic-cause-list">
            <div
              v-for="item in BUGREPORT_LOGIC_CAUSE_GUIDE"
              :key="item.name"
              class="logic-cause-item logic-cause-item--full"
            >
              <span class="logic-cause-name">{{ item.name }}</span>
              <p class="logic-cause-desc">{{ item.desc }}</p>
              <div class="logic-cause-tags">
                <span
                  v-for="tag in item.tags"
                  :key="tag"
                  class="logic-cause-tag"
                >{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item title="缺陷分类" name="defect">
        <div class="inst-section inst-section--flat">
          <div class="defect-category-list defect-category-list--drawer">
            <div
              v-for="item in defectCategoryItems"
              :key="item.no"
              class="defect-category-item defect-category-item--block"
            >
              <div class="defect-category-head">
                <span class="defect-no">{{ item.no }}.</span>
                <span class="defect-name">{{ item.name }}</span>
              </div>
              <p class="defect-desc">{{ item.desc }}</p>
            </div>
            <div v-if="defectCategoryItems.length === 0" class="defect-category-empty">暂无缺陷分类说明</div>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item title="其他字段说明" name="other">
        <div class="inst-section inst-section--flat">
          <div class="field-instructions">
            <div class="field-inst-item">
              <div class="field-inst-label">引入项目</div>
              <div class="field-inst-content">问题引起的关联需求、方案名称，若无则写应用，如"金牌导购员小程序"</div>
            </div>
            <div class="field-inst-item">
              <div class="field-inst-label">开始时间</div>
              <div class="field-inst-content">
                <div>1. 优先根据项目发布、上线公告发布的时间，因为在项目开发期间提交的代码并没有发布，不能作为准确的开始时间。</div>
                <div class="mt4">2. 其次根据代码提交记录查找出现问题的代码提交时间，实在找不到记录，但又发生在2021年1月以前，默认时间写2021-01-01</div>
              </div>
            </div>
            <div class="field-inst-item">
              <div class="field-inst-label">解决方案</div>
              <div class="field-inst-content">需升级小程序特别标明，如：拍照打卡上传图片张数限制1张（需升级云店小程序版本1.5.10.1）</div>
            </div>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item title="缺陷等级说明" name="severity">
        <div class="inst-section inst-section--flat">
          <div class="severity-inst-list">
            <div class="severity-inst-item severity-inst-item--p0">
              <div class="severity-inst-label">P0（致命）</div>
              <div class="severity-inst-content">
                <div>1. 核心功能或重点业务完全不能使用，影响超过20家商户，阻碍1000+用户正常操作</div>
                <div>2. 财务等涉及金钱的数据丢失无法追回，损失超过500元</div>
                <div>3. 造成数据损坏丢失（无法追回）、异常泄露</div>
              </div>
            </div>
            <div class="severity-inst-item severity-inst-item--p1">
              <div class="severity-inst-label">P1（重大）</div>
              <div class="severity-inst-content">
                <div>1. 重要功能用户操作失败率 &gt;20%，无法完成关键操作</div>
                <div>2. 影响10-20家商户，阻碍10-1000用户正常操作</div>
                <div>3. 导致用户资产损失价值小于500元</div>
              </div>
            </div>
            <div class="severity-inst-item severity-inst-item--p2">
              <div class="severity-inst-label">P2（严重）</div>
              <div class="severity-inst-content">
                <div>1. 重要功能用户操作失败率 &lt;20%且每分钟失败量≤10</div>
                <div>2. 非核心业务或功能不能使用，用户体验变差</div>
                <div>3. 出现设计未预料的异常，不阻断正常主流程</div>
              </div>
            </div>
            <div class="severity-inst-item severity-inst-item--p3">
              <div class="severity-inst-label">P3（一般）</div>
              <div class="severity-inst-content">
                <div>1. 非核心业务部分不能使用</div>
                <div>2. 一定条件下才分支链路异常，且异常没有透出终端</div>
                <div>3. 无直接经济损失，产生的偶然错误</div>
              </div>
            </div>
            <div class="severity-inst-item severity-inst-item--p4">
              <div class="severity-inst-label">P4（轻微）</div>
              <div class="severity-inst-content">
                <div>1. 轻微影响非核心业务或功能</div>
                <div>2. 概率性影响用户体验，如文案排版、错别字等</div>
              </div>
            </div>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item title="关键业务" name="keybiz">
        <div class="inst-section inst-section--flat">
          <div class="key-business-list">
            <div class="key-biz-group">
              <div class="key-biz-group-title">1. 活动</div>
              <div class="key-biz-item">扫码活动：无法扫码，或无法领奖，包含所有场景码</div>
              <div class="key-biz-item">互动活动：无法参与互动活动，包含互动营销、云店和微商城中所有未下架的营销应用</div>
            </div>
            <div class="key-biz-group">
              <div class="key-biz-group-title">2. 核销</div>
              <div class="key-biz-item">扫码核销：无法核销，包含扫码活动中的奖品核销和云店中的订单/购物券/赠品核销</div>
            </div>
            <div class="key-biz-group">
              <div class="key-biz-group-title">3. 资产</div>
              <div class="key-biz-item">零钱提现：无法提现零钱，包含H5个人中心和门店助手小程序中的提现</div>
              <div class="key-biz-item">积分兑换：无法进行积分兑换，包含会员小程序和积分商城中的兑换</div>
            </div>
            <div class="key-biz-group">
              <div class="key-biz-group-title">4. 出货</div>
              <div class="key-biz-item">扫码出货：无法扫码出货，包含Web、H5、PDA端的防窜扫码出货</div>
            </div>
            <div class="key-biz-group">
              <div class="key-biz-group-title">5. 支付</div>
              <div class="key-biz-item">商城下单：无法在线下单，包含云店、会员小程序和微商城等在线下单业务</div>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <template v-else>
      <div class="inst-section">
        <div class="inst-section-title">
          <el-icon><Aim /></el-icon>
          逻辑归因
        </div>
        <div class="inst-section-desc">
          优先从备选列表挑选，若备选项不符合则与上级沟通后再确定。然后补充具体原因描述
        </div>

        <div class="logic-cause-list">
          <el-tooltip
            v-for="item in BUGREPORT_LOGIC_CAUSE_GUIDE"
            :key="item.name"
            placement="left"
            effect="light"
            :show-after="200"
          >
            <template #content>
              <div class="tooltip-content">
                <div class="tooltip-title">{{ item.name }}</div>
                <div class="tooltip-desc">{{ item.desc }}</div>
                <div class="tooltip-tags">
                  <el-tag
                    v-for="tag in item.tags"
                    :key="tag"
                    size="small"
                    type="info"
                    class="tooltip-tag"
                  >{{ tag }}</el-tag>
                </div>
              </div>
            </template>
            <div class="logic-cause-item">
              <span class="logic-cause-name">{{ item.name }}</span>
              <div class="logic-cause-tags">
                <span
                  v-for="tag in item.tags.slice(0, 3)"
                  :key="tag"
                  class="logic-cause-tag"
                >{{ tag }}</span>
                <span v-if="item.tags.length > 3" class="logic-cause-more">+{{ item.tags.length - 3 }}</span>
              </div>
            </div>
          </el-tooltip>
        </div>
      </div>

      <el-divider />

      <div class="inst-section">
        <div class="inst-section-title">
          <el-icon><CollectionTag /></el-icon>
          缺陷分类
        </div>

        <div class="defect-category-list">
          <el-tooltip
            v-for="item in defectCategoryItems"
            :key="item.no"
            placement="left"
            effect="light"
            :show-after="200"
          >
            <template #content>
              <div class="tooltip-content">
                <div class="tooltip-title">{{ item.no }}. {{ item.name }}</div>
                <div class="tooltip-desc">{{ item.desc }}</div>
              </div>
            </template>
            <div class="defect-category-item">
              <span class="defect-no">{{ item.no }}.</span>
              <span class="defect-name">{{ item.name }}</span>
            </div>
          </el-tooltip>
          <div v-if="defectCategoryItems.length === 0" class="defect-category-empty">暂无缺陷分类说明</div>
        </div>
      </div>

      <el-divider />

      <div class="inst-section">
        <div class="inst-section-title">
          <el-icon><Document /></el-icon>
          其他字段说明
        </div>
        <div class="field-instructions">
          <div class="field-inst-item">
            <div class="field-inst-label">引入项目</div>
            <div class="field-inst-content">问题引起的关联需求、方案名称，若无则写应用，如"金牌导购员小程序"</div>
          </div>
          <div class="field-inst-item">
            <div class="field-inst-label">开始时间</div>
            <div class="field-inst-content">
              <div>1. 优先根据项目发布、上线公告发布的时间，因为在项目开发期间提交的代码并没有发布，不能作为准确的开始时间。</div>
              <div class="mt4">2. 其次根据代码提交记录查找出现问题的代码提交时间，实在找不到记录，但又发生在2021年1月以前，默认时间写2021-01-01</div>
            </div>
          </div>
          <div class="field-inst-item">
            <div class="field-inst-label">解决方案</div>
            <div class="field-inst-content">需升级小程序特别标明，如：拍照打卡上传图片张数限制1张（需升级云店小程序版本1.5.10.1）</div>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="inst-section">
        <div class="inst-section-title">
          <el-icon><Warning /></el-icon>
          缺陷等级说明
        </div>
        <div class="severity-inst-list">
          <div class="severity-inst-item severity-inst-item--p0">
            <div class="severity-inst-label">P0（致命）</div>
            <div class="severity-inst-content">
              <div>1. 核心功能或重点业务完全不能使用，影响超过20家商户，阻碍1000+用户正常操作</div>
              <div>2. 财务等涉及金钱的数据丢失无法追回，损失超过500元</div>
              <div>3. 造成数据损坏丢失（无法追回）、异常泄露</div>
            </div>
          </div>
          <div class="severity-inst-item severity-inst-item--p1">
            <div class="severity-inst-label">P1（重大）</div>
            <div class="severity-inst-content">
              <div>1. 重要功能用户操作失败率 &gt;20%，无法完成关键操作</div>
              <div>2. 影响10-20家商户，阻碍10-1000用户正常操作</div>
              <div>3. 导致用户资产损失价值小于500元</div>
            </div>
          </div>
          <div class="severity-inst-item severity-inst-item--p2">
            <div class="severity-inst-label">P2（严重）</div>
            <div class="severity-inst-content">
              <div>1. 重要功能用户操作失败率 &lt;20%且每分钟失败量≤10</div>
              <div>2. 非核心业务或功能不能使用，用户体验变差</div>
              <div>3. 出现设计未预料的异常，不阻断正常主流程</div>
            </div>
          </div>
          <div class="severity-inst-item severity-inst-item--p3">
            <div class="severity-inst-label">P3（一般）</div>
            <div class="severity-inst-content">
              <div>1. 非核心业务部分不能使用</div>
              <div>2. 一定条件下才分支链路异常，且异常没有透出终端</div>
              <div>3. 无直接经济损失，产生的偶然错误</div>
            </div>
          </div>
          <div class="severity-inst-item severity-inst-item--p4">
            <div class="severity-inst-label">P4（轻微）</div>
            <div class="severity-inst-content">
              <div>1. 轻微影响非核心业务或功能</div>
              <div>2. 概率性影响用户体验，如文案排版、错别字等</div>
            </div>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="inst-section">
        <div class="inst-section-title">
          <el-icon><StarFilled /></el-icon>
          关键业务
        </div>
        <div class="key-business-list">
          <div class="key-biz-group">
            <div class="key-biz-group-title">1. 活动</div>
            <div class="key-biz-item">扫码活动：无法扫码，或无法领奖，包含所有场景码</div>
            <div class="key-biz-item">互动活动：无法参与互动活动，包含互动营销、云店和微商城中所有未下架的营销应用</div>
          </div>
          <div class="key-biz-group">
            <div class="key-biz-group-title">2. 核销</div>
            <div class="key-biz-item">扫码核销：无法核销，包含扫码活动中的奖品核销和云店中的订单/购物券/赠品核销</div>
          </div>
          <div class="key-biz-group">
            <div class="key-biz-group-title">3. 资产</div>
            <div class="key-biz-item">零钱提现：无法提现零钱，包含H5个人中心和门店助手小程序中的提现</div>
            <div class="key-biz-item">积分兑换：无法进行积分兑换，包含会员小程序和积分商城中的兑换</div>
          </div>
          <div class="key-biz-group">
            <div class="key-biz-group-title">4. 出货</div>
            <div class="key-biz-item">扫码出货：无法扫码出货，包含Web、H5、PDA端的防窜扫码出货</div>
          </div>
          <div class="key-biz-group">
            <div class="key-biz-group-title">5. 支付</div>
            <div class="key-biz-item">商城下单：无法在线下单，包含云店、会员小程序和微商城等在线下单业务</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.instruction-body {
  padding: 12px 16px;

  &--drawer {
    padding: 0 4px 12px;
  }
}

.instruction-mobile-collapse {
  border: none;

  :deep(.el-collapse-item__header) {
    min-height: 48px;
    padding: 0 4px;
    font-weight: 600;
    font-size: 14px;
    color: #303133;
    border-bottom: 1px solid #eef2f7;
    background: transparent;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: 1px solid #f0f2f5;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 12px;
  }
}

.inst-section {
  margin-bottom: 4px;

  &--flat {
    margin-bottom: 0;
  }
}

.inst-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;

  .el-icon {
    color: #1675d1;
    font-size: 14px;
  }
}

.inst-section-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  padding: 8px 10px;
  background: #fafafa;
  border-radius: 6px;
  border-left: 3px solid #1675d1;
}

.logic-cause-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.logic-cause-item {
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid #ebeef5;

  &:hover {
    background: #f0f6ff;
    border-color: #b3d8ff;
  }

  &--full {
    cursor: default;

    &:hover {
      background: #fff;
      border-color: #ebeef5;
    }
  }
}

.logic-cause-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  display: block;
  margin-bottom: 4px;
}

.logic-cause-desc {
  margin: 0 0 8px;
  font-size: 12px;
  color: #606266;
  line-height: 1.55;
}

.logic-cause-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.logic-cause-tag {
  font-size: 11px;
  color: #606266;
  background: #f4f4f5;
  padding: 3px 6px;
  border-radius: 4px;
  line-height: 1.3;
}

.logic-cause-more {
  font-size: 11px;
  color: #1675d1;
  padding: 1px 5px;
}

.defect-category-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;

  &--drawer {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

.defect-category-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition: background 0.15s;

  &:hover {
    background: #f0f6ff;
    border-color: #b3d8ff;
  }

  &--block {
    flex-direction: column;
    align-items: stretch;
    cursor: default;

    &:hover {
      background: #fafafa;
      border-color: #ebeef5;
    }
  }
}

.defect-category-head {
  display: flex;
  align-items: center;
  gap: 4px;
}

.defect-desc {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: #606266;
  line-height: 1.55;
}

.defect-no {
  font-size: 11px;
  color: #909399;
  flex-shrink: 0;
}

.defect-name {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
}

.defect-category-empty {
  grid-column: 1 / -1;
  font-size: 12px;
  color: #909399;
  padding: 6px 8px;
}

.field-instructions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-inst-item {
  font-size: 12px;
}

.field-inst-label {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.field-inst-content {
  color: #606266;
  line-height: 1.6;
}

.mt4 {
  margin-top: 4px;
}

.severity-inst-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.severity-inst-item {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
  border-left: 3px solid #dcdfe6;
  background: #fafafa;
}

.severity-inst-label {
  font-weight: 600;
  margin-bottom: 4px;
}

.severity-inst-content {
  color: #606266;
  line-height: 1.6;

  div + div {
    margin-top: 2px;
  }
}

.severity-inst-item--p0 {
  border-left-color: #f56c6c;
  .severity-inst-label { color: #f56c6c; }
}

.severity-inst-item--p1 {
  border-left-color: #e6a23c;
  .severity-inst-label { color: #e6a23c; }
}

.severity-inst-item--p2 {
  border-left-color: #409eff;
  .severity-inst-label { color: #409eff; }
}

.severity-inst-item--p3 {
  border-left-color: #67c23a;
  .severity-inst-label { color: #67c23a; }
}

.severity-inst-item--p4 {
  border-left-color: #909399;
  .severity-inst-label { color: #909399; }
}

.key-business-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.key-biz-group {
  font-size: 12px;
}

.key-biz-group-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.key-biz-item {
  color: #606266;
  line-height: 1.6;
  padding-left: 8px;
  border-left: 2px solid #dcdfe6;
  margin-bottom: 3px;
}

.tooltip-content {
  max-width: 280px;
}

.tooltip-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.tooltip-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 8px;
}

.tooltip-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tooltip-tag {
  font-size: 11px;
}

:deep(.el-popper) {
  max-width: 300px;
}
</style>
