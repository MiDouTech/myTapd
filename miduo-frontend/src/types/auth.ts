export interface WecomLoginInput {
  code: string
}

export interface DevLoginInput {
  username: string
  password: string
}

export interface RefreshTokenInput {
  refreshToken: string
}

export interface LoginUserInfo {
  id: number
  name: string
  avatar?: string
  department?: string
  roles?: string[]
}

export interface LoginOutput {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo?: LoginUserInfo
}
