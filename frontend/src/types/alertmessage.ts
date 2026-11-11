export interface AlertMessage {
  id: number
  ruleId?: number
  targetType: 'STOCK' | 'INDEX' | string
  targetCode: string
  targetNameSnapshot?: string
  metricCode: string
  metricNameSnapshot?: string
  unitSnapshot?: string
  operator: string
  threshold: number
  triggerValue: number
  messageTitle: string
  messageContent: string
  dataTime?: string
  triggeredAt?: string
  readStatus?: string
  processStatus?: string
}
