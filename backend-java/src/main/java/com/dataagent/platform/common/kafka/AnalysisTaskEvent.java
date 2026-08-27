package com.dataagent.platform.common.kafka;

public record AnalysisTaskEvent(
        String taskId,
        String tenantId,
        String userId,
        String traceId,
        String question
) {
}

