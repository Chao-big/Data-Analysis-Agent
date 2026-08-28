# Java Dataset 模块 API 文档

## 1. 模块基础信息

1. 基础路径：`/api/datasets`
2. 返回包装：`ApiResponse<T>`

## 2. 上传文件并注册数据集

### `POST /api/datasets/upload`

请求类型：`multipart/form-data`

表单字段：

1. `file`
2. `datasetName`
3. `description`

成功响应：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "datasetId": "dataset-file-001",
    "sourceType": "CSV",
    "status": "REGISTERING"
  }
}
```

## 3. 注册 MySQL 数据集

### `POST /api/datasets/mysql/register`

请求体：

```json
{
  "datasetName": "sales_mysql",
  "host": "127.0.0.1",
  "port": 3306,
  "database": "demo",
  "username": "readonly_user",
  "password": "******",
  "tableNames": ["sales"]
}
```

## 4. 获取数据集列表

### `GET /api/datasets`

查询参数：

1. `keyword`
2. `sourceType`
3. `status`

## 5. 获取数据集详情

### `GET /api/datasets/{datasetId}`

响应示例：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "datasetId": "dataset-sales",
    "datasetName": "sales",
    "sourceType": "CSV",
    "schemaSummary": [
      {"name": "order_month", "type": "string"},
      {"name": "revenue", "type": "decimal"}
    ],
    "status": "REGISTERED"
  }
}
```

## 6. 获取数据预览

### `GET /api/datasets/{datasetId}/preview`

查询参数：

1. `limit`

## 7. 授权数据集

### `POST /api/datasets/{datasetId}/grant`

请求体：

```json
{
  "roleIds": ["ANALYST"],
  "userIds": ["user-001"]
}
```

## 8. 调试接口

### `GET /api/datasets/demo`

用途：开发期返回演示数据集对象。

## 9. 错误码约定

1. `400`：文件格式不支持
2. `403`：无访问权限
3. `409`：数据集名称重复
4. `422`：Schema 解析失败
