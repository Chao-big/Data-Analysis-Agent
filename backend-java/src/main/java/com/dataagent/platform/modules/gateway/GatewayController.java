package com.dataagent.platform.modules.gateway;

import com.dataagent.platform.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("backend-java-monolith");
    }
}

