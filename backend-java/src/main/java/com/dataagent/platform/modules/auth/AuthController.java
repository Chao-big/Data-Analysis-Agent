package com.dataagent.platform.modules.auth;

import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/context-demo")
    public ApiResponse<TaskAccessContext> contextDemo() {
        TaskAccessContext context = new TaskAccessContext(
                "tenant-demo",
                "user-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
        return ApiResponse.ok(context);
    }
}

