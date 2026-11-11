import http from '/@/utils/http'
import type { AlertMessage } from '/@/types/alertmessage'

export function queryRecentAlertMessages(targetType: string, targetCode: string, limit = 5): Promise<AlertMessage[]> | any {
  return http.get<AlertMessage[]>('/api/v1/analytics/alert-messages/recent', { params: { targetType, targetCode, limit } })
}
