<template>
  <div class="dashboard-page">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-title">待处理工单</div>
          <div class="stat-value">--</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-title">今日新建</div>
          <div class="stat-value">--</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-title">已完成</div>
          <div class="stat-value">--</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-title">SLA风险</div>
          <div class="stat-value">--</div>
        </el-card>
      </el-col>
    </el-row>
    <el-alert
      class="mt-16"
      title="当前为 Task012 占位页，可在 Task010 数据看板能力完善后替换真实统计。"
      type="info"
      show-icon
      :closable="false"
    />
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  .stat-title {
    color: #909399;
    font-size: 14px;
  }

  .stat-value {
    color: #1675d1;
    font-size: 32px;
    font-weight: 600;
    margin-top: 8px;
  }
}

.mt-16 {
  margin-top: 16px;
}
</style>
