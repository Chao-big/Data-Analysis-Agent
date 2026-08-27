package com.dataagent.platform.modules.dataset;

import com.dataagent.platform.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    @GetMapping("/demo")
    public ApiResponse<Map<String, Object>> demo() {
        return ApiResponse.ok(Map.of(
                "datasetId", "dataset-sales",
                "sourceType", "CSV",
                "status", "REGISTERED"
        ));
    }
}

