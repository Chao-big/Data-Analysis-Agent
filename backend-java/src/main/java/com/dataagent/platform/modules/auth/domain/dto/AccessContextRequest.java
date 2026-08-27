package com.dataagent.platform.modules.auth.domain.dto;

import java.util.Set;

public record AccessContextRequest(
        Set<String> datasetIds
) {
}
