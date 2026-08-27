package com.dataagent.platform.modules.audit;

import com.dataagent.platform.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping("/demo")
    public ApiResponse<Map<String, String>> demo() {
        return ApiResponse.ok(Map.of(
                "taskId", "task-demo-001",
                "reviewStatus", "PENDING"
        ));
    }
}

