<script setup lang="ts">
import type { Component } from 'vue'

export interface SidebarMenuItemModel {
  index: string
  title: string
  icon: Component
  children?: SidebarMenuItemModel[]
}

defineOptions({ name: 'SidebarMenuItem' })

defineProps<{
  item: SidebarMenuItemModel
}>()
</script>

<template>
  <el-sub-menu v-if="item.children?.length" :index="item.index">
    <template #title>
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.title }}</span>
    </template>
    <SidebarMenuItem v-for="child in item.children" :key="child.index" :item="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="item.index">
    <el-icon><component :is="item.icon" /></el-icon>
    <span>{{ item.title }}</span>
  </el-menu-item>
</template>
