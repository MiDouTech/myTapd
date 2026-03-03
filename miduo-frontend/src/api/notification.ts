import type {
  NotificationPreferenceOutput,
  NotificationPreferenceUpdateInput,
} from '@/types/notification'
import request from '@/utils/request'

/**
 * 获取用户通知偏好
 * 接口编号：API000008
 * 产品文档功能：4.8 通知中心 - 通知偏好设置
 */
export function getNotificationPreferences(): Promise<NotificationPreferenceOutput[]> {
  return request.get<NotificationPreferenceOutput[]>('/notification/preference')
}

/**
 * 更新用户通知偏好
 * 接口编号：API000009
 * 产品文档功能：4.8 通知中心 - 更新通知偏好
 */
export function updateNotificationPreferences(data: NotificationPreferenceUpdateInput): Promise<void> {
  return request.put<void>('/notification/preference/update', data)
}
