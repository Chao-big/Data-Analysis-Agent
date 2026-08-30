package com.dataagent.platform.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.oss")
public record OssProperties(String endpoint, String bucket, String accessKeyId, String accessKeySecret) {
}
