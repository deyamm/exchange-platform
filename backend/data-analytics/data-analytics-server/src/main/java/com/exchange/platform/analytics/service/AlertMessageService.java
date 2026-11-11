package com.exchange.platform.analytics.service;

import java.util.List;
import com.exchange.platform.analytics.api.response.AlertMessageView;

public interface AlertMessageService {
    /** 批量评估当前有效指标规则；返回本次新生成的消息数。 */
    int evaluateEnabledRules();
    List<AlertMessageView> listRecent(String targetType, String targetCode, int limit);
}
