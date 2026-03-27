import type { LoginOutput } from '@/types/auth'
import type { SsoBridgeUrlOutput, SsoCallbackInput, SsoStateOutput, SsoStatusOutput } from '@/types/sso'
import request from '@/utils/request'

/**
 * 查询 SSO 状态
 * 接口编号：API000410
 * 产品文档功能：SSO 登录 - 获取 SSO 配置状态
 */
export function getSsoStatus(): Promise<SsoStatusOutput> {
  return request.get<SsoStatusOutput>('/auth/sso/status')
}

/**
 * 生成 SSO state 参数
 * 接口编号：API000411
 * 产品文档功能：SSO 登录 - 生成 CSRF 防护 state
 */
export function generateSsoState(): Promise<SsoStateOutput> {
  return request.post<SsoStateOutput>('/auth/sso/state')
}

/**
 * SSO 回调登录
 * 接口编号：API000412
 * 产品文档功能：SSO 登录 - 第三方回调校验
 */
export function ssoCallback(data: SsoCallbackInput): Promise<LoginOutput> {
  return request.post<LoginOutput>('/auth/sso/callback', data)
}

/**
 * SSO 登出
 * 接口编号：API000413
 * 产品文档功能：SSO 登录 - 登出并吊销米多会话
 */
export function ssoLogout(): Promise<void> {
  return request.post<void>('/auth/sso/logout')
}

/**
 * 获取登录桥 URL
 * 接口编号：API000414
 * 产品文档功能：SSO 登录 - 会话失效后重新获取身份
 */
export function getSsoBridgeUrl(): Promise<SsoBridgeUrlOutput> {
  return request.get<SsoBridgeUrlOutput>('/auth/sso/bridge-url')
}
