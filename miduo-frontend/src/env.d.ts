/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_USE_PROXY: string
  readonly VITE_API_PROXY_TARGET: string
  readonly VITE_WECOM_OAUTH_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
