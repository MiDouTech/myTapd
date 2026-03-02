import { ElMessage, ElMessageBox } from 'element-plus'

export function notifySuccess(message: string): void {
  ElMessage.success(message)
}

export function notifyError(message: string): void {
  ElMessage.error(message)
}

export function notifyWarning(message: string): void {
  ElMessage.warning(message)
}

export async function confirmAction(message: string, title = '提示'): Promise<void> {
  await ElMessageBox.confirm(message, title, {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
  })
}
