package com.dataagent.platform.modules.dataset.service.impl;

import com.dataagent.platform.common.storage.OssStorageService;
import com.dataagent.platform.common.web.ApiException;
import com.dataagent.platform.common.web.ApiStatusCode;
import com.dataagent.platform.modules.dataset.domain.dto.UploadResultResponse;
import com.dataagent.platform.modules.dataset.service.DatasetService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DatasetServiceImpl implements DatasetService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx", "xls");

    private final OssStorageService ossStorageService;

    public DatasetServiceImpl(OssStorageService ossStorageService) {
        this.ossStorageService = ossStorageService;
    }

    @Override
    public UploadResultResponse upload(MultipartFile file, String datasetName, String description) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (datasetName == null || datasetName.isBlank()) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "数据集名称不能为空");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "文件格式不支持，仅支持 csv / xlsx /xls");
        }

        String datasetId = "dataset-" + UUID.randomUUID().toString().substring(0, 8);
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String objectKey = "datasets/" + month + "/" + datasetId + "." + extension;
        ossStorageService.upload(file, objectKey);

        return new UploadResultResponse(datasetId, extension.toUpperCase(Locale.ROOT), "REGISTERING");
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
