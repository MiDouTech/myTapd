import type {
  NotificationOutput,
  NotificationPageInput,
  NotificationPreferenceOutput,
  NotificationPreferenceUpdateInput,
  NotificationUnreadCountOutput,
} from '@/types/notification'
import type { PageOutput } from '@/types/common'
import request from '@/utils/request'

function normalizePreference(item: NotificationPreferenceOutput): NotificationPreferenceOutput {
  return {
    ...item,
    siteEnabled: item.siteEnabled ?? 1,
    wecomEnabled: item.wecomEnabled ?? 1,
    emailEnabled: item.emailEnabled ?? 0,
  }
}

/**
 * 分页查询通知列表
 * 接口编号：API000004
 * 产品文档功能：4.8 通知中心 - 站内通知列表
 */
export function getNotificationPage(
  params: NotificationPageInput,
): Promise<PageOutput<NotificationOutput>> {
  return request.get<PageOutput<NotificationOutput>>('/notification/page', { params })
}

/**
 * 标记通知为已读
 * 接口编号：API000005
 * 产品文档功能：4.8 通知中心 - 标记通知已读
 */
export function markNotificationAsRead(id: number): Promise<void> {
  return request.put<void>(`/notification/read/${id}`)
}

/**
 * 标记全部通知为已读
 * 接口编号：API000006
 * 产品文档功能：4.8 通知中心 - 全部标记已读
 */
export function markAllNotificationsAsRead(): Promise<void> {
  return request.put<void>('/notification/read/all')
}

/**
 * 查询未读通知数量
 * 接口编号：API000007
 * 产品文档功能：4.8 通知中心 - 未读数量
 */
export function getNotificationUnreadCount(): Promise<NotificationUnreadCountOutput> {
  return request.get<NotificationUnreadCountOutput>('/notification/unread/count')
}

/**
 * 获取用户通知偏好
 * 接口编号：API000008
 * 产品文档功能：4.8 通知中心 - 通知偏好设置
 */
export async function getNotificationPreferences(): Promise<NotificationPreferenceOutput[]> {
  const result = await request.get<NotificationPreferenceOutput[]>('/notification/preference')
  return result.map(normalizePreference)
}

/**
 * 更新用户通知偏好
 * 接口编号：API000009
 * 产品文档功能：4.8 通知中心 - 更新通知偏好
 */
export function updateNotificationPreferences(data: NotificationPreferenceUpdateInput): Promise<void> {
  return request.put<void>('/notification/preference/update', data)
}
