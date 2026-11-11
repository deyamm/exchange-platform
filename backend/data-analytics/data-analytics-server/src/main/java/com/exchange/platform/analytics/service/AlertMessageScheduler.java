package com.exchange.platform.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时扫描与手工 evaluate API 使用同一套幂等执行逻辑。 */
@Component
public class AlertMessageScheduler {
    private static final Logger log = LoggerFactory.getLogger(AlertMessageScheduler.class);
    private final AlertMessageService service;
    public AlertMessageScheduler(AlertMessageService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${alert.evaluation.fixed-delay-ms:60000}")
    public void evaluate() {
        try {
            int generated = service.evaluateEnabledRules();
            if (generated > 0) log.info("alert evaluation generated {} messages", generated);
        } catch (RuntimeException ex) {
            log.error("alert evaluation failed", ex);
        }
    }
}
