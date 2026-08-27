package com.dataagent.platform.modules.job;

import com.dataagent.platform.common.kafka.AnalysisTaskEvent;
import com.dataagent.platform.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @GetMapping("/demo")
    public ApiResponse<AnalysisTaskEvent> demo() {
        AnalysisTaskEvent event = new AnalysisTaskEvent(
                "task-demo-001",
                "tenant-demo",
                "user-demo",
                "trace-demo-001",
                "Compare monthly revenue for the last 6 months"
        );
        return ApiResponse.ok(event);
    }
}

