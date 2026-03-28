export interface BasicSettingsOutput {
  systemName: string
  timezone: string
  workTimeStart: string
  workTimeEnd: string
  defaultPageSize: number
}

export interface BasicSettingsUpdateInput {
  systemName: string
  timezone: string
  workTimeStart: string
  workTimeEnd: string
  defaultPageSize: number
}
