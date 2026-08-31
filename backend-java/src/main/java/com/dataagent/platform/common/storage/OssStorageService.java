package com.dataagent.platform.common.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@EnableConfigurationProperties(OssProperties.class)
public class OssStorageService {

    private final OssProperties properties;
    private final OSS client;

    public OssStorageService(OssProperties properties) {
        this.properties = properties;
        this.client = new OSSClientBuilder().build(
                properties.endpoint(), properties.accessKeyId(), properties.accessKeySecret());
    }

    /**
     * 上传文件到 OSS，返回 objectKey（bucket 内的对象路径）。
     */
    public String upload(MultipartFile file, String objectKey) {
        try {
            client.putObject(properties.bucket(), objectKey, file.getInputStream());
            return objectKey;
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败: " + file.getOriginalFilename(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        client.shutdown();
    }
}
