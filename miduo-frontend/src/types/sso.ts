export interface SsoStatusOutput {
  enabled: boolean
  callbackUrl: string
  appCode: string
}

export interface SsoCallbackInput {
  token: string
  state?: string
}

export interface SsoStateOutput {
  state: string
}

export interface SsoBridgeUrlOutput {
  bridgeUrl: string
}
