import type { LoginOutput, RefreshTokenInput, WecomLoginInput } from '@/types/auth'
import request from '@/utils/request'

/**
 * 企微扫码登录
 * 接口编号：API000400
 * 产品文档功能：4.6.1 企微扫码登录
 */
export function wecomLogin(data: WecomLoginInput): Promise<LoginOutput> {
  return request.post<LoginOutput>('/auth/wecom/login', data)
}

/**
 * 刷新 Token
 * 接口编号：API000401
 * 产品文档功能：4.6.1 Token管理（双Token机制）
 */
export function refreshToken(data: RefreshTokenInput): Promise<LoginOutput> {
  return request.post<LoginOutput>('/auth/refresh', data)
}
