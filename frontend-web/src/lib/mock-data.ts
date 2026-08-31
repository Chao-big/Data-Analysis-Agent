import type {
  AnalysisTask,
  ChartConfig,
  ChartPreference,
  DatasetSummary,
  StreamEvent,
  StreamEventType,
  UserProfile,
} from "./types";

function buildEvent(
  taskId: string,
  traceId: string,
  eventType: StreamEventType,
  message: string,
  level: StreamEvent["level"],
  timestamp: string,
): StreamEvent {
  return {
    eventType,
    taskId,
    traceId,
    timestamp,
    message,
    level,
  };
}

function buildLineChart(): ChartConfig {
  return {
    id: "chart-line-revenue",
    title: "近 12 个月营收走势",
    description: "观察月度营收的抬升节奏、波动节点与异常月份。",
    type: "line",
    option: {
      tooltip: { trigger: "axis" },
      grid: { left: 28, right: 18, top: 40, bottom: 26 },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: ["1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"],
      },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#dbe7eb" } } },
      series: [
        {
          type: "line",
          smooth: true,
          data: [182, 194, 208, 223, 236, 249, 268, 259, 281, 296, 314, 336],
          lineStyle: { color: "#0f8b8d", width: 3 },
          areaStyle: { color: "rgba(15,139,141,0.12)" },
          itemStyle: { color: "#0f8b8d" },
        },
      ],
    },
  };
}

function buildBarChart(): ChartConfig {
  return {
    id: "chart-bar-region",
    title: "区域营收排名",
    description: "比较主要业务区域的营收规模与贡献梯度。",
    type: "bar",
    option: {
      tooltip: { trigger: "axis" },
      grid: { left: 28, right: 18, top: 40, bottom: 24 },
      xAxis: {
        type: "category",
        data: ["华东", "华南", "华北", "西南", "校园业务"],
      },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#dbe7eb" } } },
      series: [
        {
          type: "bar",
          data: [336, 312, 288, 244, 221],
          itemStyle: {
            color: "#2cb9b0",
            borderRadius: [10, 10, 0, 0],
          },
        },
      ],
    },
  };
}

function buildPieChart(): ChartConfig {
  return {
    id: "chart-pie-channel",
    title: "渠道贡献结构",
    description: "展示直营、经销、校园合作与线上渠道的占比结构。",
    type: "pie",
    option: {
      tooltip: { trigger: "item" },
      legend: { bottom: 0 },
      series: [
        {
          type: "pie",
          radius: ["42%", "70%"],
          label: { formatter: "{b} {d}%" },
          data: [
            { name: "直营", value: 38 },
            { name: "经销", value: 27 },
            { name: "校园合作", value: 18 },
            { name: "线上", value: 17 },
          ],
        },
      ],
    },
  };
}

export const chartLibrary = {
  line: buildLineChart(),
  bar: buildBarChart(),
  pie: buildPieChart(),
};

export const datasets: DatasetSummary[] = [
  {
    id: "dataset-school-score",
    datasetName: "school_exam_summary_2026",
    datasetType: "excel",
    sourceLocation: "/uploads/school/school_exam_summary_2026.xlsx",
    schemaSummary: ["month", "class_name", "subject", "avg_score", "student_count"],
    owner: "王老师",
    tenantId: "tenant-school",
    createdAt: "2026-08-20 09:12",
    permissionScope: "school.score.read",
    rowCount: 1260,
    description: "学校阶段考试汇总数据，适合做学科趋势和班级表现分析。",
    sampleRows: [
      { month: "2026-03", class_name: "高二(1)班", subject: "数学", avg_score: 108.4, student_count: 46 },
      { month: "2026-04", class_name: "高二(2)班", subject: "英语", avg_score: 114.1, student_count: 43 },
    ],
    status: "active",
  },
  {
    id: "dataset-enterprise-sales",
    datasetName: "enterprise_sales_monthly",
    datasetType: "csv",
    sourceLocation: "/uploads/enterprise/enterprise_sales_monthly.csv",
    schemaSummary: ["month", "region", "channel", "revenue", "orders", "margin_rate"],
    owner: "运营中心",
    tenantId: "tenant-enterprise",
    createdAt: "2026-08-21 15:08",
    permissionScope: "enterprise.sales.read",
    rowCount: 2480,
    description: "企业月度销售与订单汇总数据，适合营收趋势、区域排名和渠道结构分析。",
    sampleRows: [
      { month: "2026-05", region: "华东", channel: "直营", revenue: 286000, orders: 1820, margin_rate: 0.36 },
      { month: "2026-06", region: "华南", channel: "经销", revenue: 259000, orders: 1610, margin_rate: 0.31 },
    ],
    status: "active",
  },
  {
    id: "dataset-campus-library",
    datasetName: "campus_library_borrowing",
    datasetType: "csv",
    sourceLocation: "/uploads/school/campus_library_borrowing.csv",
    schemaSummary: ["date", "grade", "book_category", "borrow_count", "return_rate"],
    owner: "图书馆",
    tenantId: "tenant-school",
    createdAt: "2026-08-24 11:30",
    permissionScope: "school.library.read",
    rowCount: 965,
    description: "校园图书借阅数据，用于分析借阅趋势和年级阅读偏好。",
    sampleRows: [
      { date: "2026-08-01", grade: "高一", book_category: "文学", borrow_count: 86, return_rate: 0.96 },
      { date: "2026-08-02", grade: "高二", book_category: "科学", borrow_count: 72, return_rate: 0.94 },
    ],
    status: "active",
  },
  {
    id: "dataset-finance-mysql",
    datasetName: "finance_revenue_fact",
    datasetType: "mysql",
    sourceLocation: "mysql://readonly@10.12.3.18:3306/finance.revenue_fact",
    schemaSummary: ["order_month", "product_line", "net_revenue", "gross_margin", "region"],
    owner: "财务部",
    tenantId: "tenant-enterprise",
    createdAt: "2026-08-26 10:05",
    permissionScope: "finance.revenue.readonly",
    rowCount: 6820,
    description: "只读 MySQL 财务事实表，适合多维营收分析与图表输出。",
    sampleRows: [
      { order_month: "2026-06", product_line: "企业服务", net_revenue: 402100, gross_margin: 0.38, region: "华东" },
      { order_month: "2026-07", product_line: "校园业务", net_revenue: 221400, gross_margin: 0.24, region: "华中" },
    ],
    status: "active",
  },
];

export const currentUser: UserProfile = {
  userId: "user-admin-001",
  username: "admin",
  nickname: "系统管理员",
  displayName: "系统管理员",
  avatarUrl: null,
  role: "管理员 / 分析负责人",
  organization: "数据分析中心",
  email: "admin@data-agent.local",
  phone: "138-0000-0000",
  gender: 0,
  status: "ACTIVE",
  lastLoginIp: "127.0.0.1",
  tenantId: "tenant-enterprise",
  lastLoginAt: "2026-08-27 09:18",
  createdAt: "2026-08-27 08:00",
  updatedAt: "2026-08-28 17:28",
  remark: null,
  passwordPolicy: "高风险请求需要再次输入管理员密码确认，但平台不会放行任何非 SELECT SQL。",
};

export function pickCharts(preferences: ChartPreference[]) {
  if (!preferences.length || preferences.includes("auto")) {
    return [chartLibrary.line, chartLibrary.bar, chartLibrary.pie];
  }

  return preferences
    .filter((item): item is Exclude<ChartPreference, "auto"> => item !== "auto")
    .map((item) => chartLibrary[item]);
}

export const taskHistory: AnalysisTask[] = [
  {
    taskId: "task-20260827-0101",
    traceId: "trace-20260827-0101",
    status: "succeeded",
    question: "分析过去 12 个月企业营收趋势，并输出区域排名与渠道占比。",
    datasetIds: ["dataset-enterprise-sales", "dataset-finance-mysql"],
    startedAt: "2026-08-27 10:02:12",
    finishedAt: "2026-08-27 10:02:28",
    duration: "16s",
    connectionStatus: "closed",
    chartPreferences: ["line", "bar", "pie"],
    sqlText:
      "SELECT order_month, SUM(net_revenue) AS total_revenue FROM finance.revenue_fact WHERE order_month >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) GROUP BY order_month ORDER BY order_month;",
    sqlReasoning: "按月聚合净营收，适合做趋势分析，同时结合区域和渠道维度补充经营解释。",
    finalConclusion:
      "营收整体保持上升，1 月和 12 月增速最明显。华东与华南贡献最高，直营仍是主要营收来源，校园合作渠道在暑期后恢复明显。",
    warnings: ["结果基于汇总表，不包含退款与冲销明细。"],
    resultKind: "normal",
    charts: [chartLibrary.line, chartLibrary.bar, chartLibrary.pie],
    relatedDatasetNames: ["enterprise_sales_monthly", "finance_revenue_fact"],
  },
  {
    taskId: "task-20260827-0102",
    traceId: "trace-20260827-0102",
    status: "needs_review",
    question: "请帮我删除 2026 年 8 月营收异常数据并重新统计。",
    datasetIds: ["dataset-finance-mysql"],
    startedAt: "2026-08-27 11:15:44",
    duration: "审核中",
    connectionStatus: "open",
    chartPreferences: ["auto"],
    sqlText: "UPDATE finance.revenue_fact SET net_revenue = 0 WHERE order_month = '2026-08-01';",
    sqlReasoning: "检测到写操作意图，系统已拦截并等待管理员确认。",
    finalConclusion: "当前请求不符合只读分析规则，已进入人工复核。",
    warnings: ["仅允许 SELECT，禁止 UPDATE / DELETE / DROP / ALTER / TRUNCATE。"],
    reviewMessage: "如需查看详细拦截原因，请输入管理员密码确认。",
    resultKind: "review",
    charts: [],
    relatedDatasetNames: ["finance_revenue_fact"],
  },
  {
    taskId: "task-20260827-0103",
    traceId: "trace-20260827-0103",
    status: "failed",
    question: "分析未授权客户敏感字段的增长情况。",
    datasetIds: ["dataset-enterprise-sales"],
    startedAt: "2026-08-27 12:04:11",
    finishedAt: "2026-08-27 12:04:17",
    duration: "6s",
    connectionStatus: "closed",
    chartPreferences: ["auto"],
    sqlReasoning: "问题中引用了未授权字段，权限校验失败。",
    finalConclusion: "因越权访问被拦截，本次任务未执行查询。",
    failureReason: "用户无权访问 customer_phone、id_card_no 等敏感字段。",
    warnings: ["请改用已授权数据集或申请扩展权限。"],
    resultKind: "blocked",
    charts: [],
    relatedDatasetNames: ["enterprise_sales_monthly"],
  },
  {
    taskId: "task-20260827-0104",
    traceId: "trace-20260827-0104",
    status: "succeeded",
    question: "查询东北区域 2024 年 1 月的营收数据。",
    datasetIds: ["dataset-finance-mysql"],
    startedAt: "2026-08-27 13:01:09",
    finishedAt: "2026-08-27 13:01:15",
    duration: "6s",
    connectionStatus: "closed",
    chartPreferences: ["line"],
    sqlText:
      "SELECT region, SUM(net_revenue) AS total_revenue FROM finance.revenue_fact WHERE order_month BETWEEN '2024-01-01' AND '2024-01-31' AND region = '东北' GROUP BY region;",
    sqlReasoning: "根据用户指定区域和时间范围进行聚合。",
    finalConclusion: "查询已成功执行，但当前筛选条件下没有命中数据。",
    warnings: ["建议扩大时间范围或减少区域限制。"],
    resultKind: "empty",
    charts: [],
    relatedDatasetNames: ["finance_revenue_fact"],
  },
];

export const taskEvents: Record<string, StreamEvent[]> = {
  "task-20260827-0101": [
    buildEvent("task-20260827-0101", "trace-20260827-0101", "task_started", "任务已启动，开始校验数据集权限。", "info", "2026-08-27T10:02:12+08:00"),
    buildEvent("task-20260827-0101", "trace-20260827-0101", "context_built", "已完成 Schema 摘要与最小权限上下文构建。", "info", "2026-08-27T10:02:15+08:00"),
    buildEvent("task-20260827-0101", "trace-20260827-0101", "sql_generated", "只读 SQL 已生成并通过校验。", "success", "2026-08-27T10:02:18+08:00"),
    buildEvent("task-20260827-0101", "trace-20260827-0101", "query_executed", "查询已执行完成，返回 12 条聚合记录。", "success", "2026-08-27T10:02:22+08:00"),
    buildEvent("task-20260827-0101", "trace-20260827-0101", "chart_ready", "趋势图、排名图和占比图已生成。", "success", "2026-08-27T10:02:25+08:00"),
    buildEvent("task-20260827-0101", "trace-20260827-0101", "task_finished", "任务执行完成，最终结论已返回。", "success", "2026-08-27T10:02:28+08:00"),
  ],
  "task-20260827-0102": [
    buildEvent("task-20260827-0102", "trace-20260827-0102", "task_started", "任务已启动，开始检查权限和写操作风险。", "info", "2026-08-27T11:15:44+08:00"),
    buildEvent("task-20260827-0102", "trace-20260827-0102", "context_built", "数据集上下文已准备完成。", "info", "2026-08-27T11:15:46+08:00"),
    buildEvent("task-20260827-0102", "trace-20260827-0102", "sql_generated", "检测到 UPDATE 操作，触发高风险拦截。", "warning", "2026-08-27T11:15:47+08:00"),
    buildEvent("task-20260827-0102", "trace-20260827-0102", "human_review_required", "需要管理员密码确认后展示详细风险说明。", "warning", "2026-08-27T11:15:48+08:00"),
  ],
  "task-20260827-0103": [
    buildEvent("task-20260827-0103", "trace-20260827-0103", "task_started", "任务已启动。", "info", "2026-08-27T12:04:11+08:00"),
    buildEvent("task-20260827-0103", "trace-20260827-0103", "context_built", "授权范围校验中发现敏感字段。", "warning", "2026-08-27T12:04:14+08:00"),
    buildEvent("task-20260827-0103", "trace-20260827-0103", "task_failed", "越权访问被拦截，任务失败。", "error", "2026-08-27T12:04:17+08:00"),
  ],
  "task-20260827-0104": [
    buildEvent("task-20260827-0104", "trace-20260827-0104", "task_started", "任务已启动。", "info", "2026-08-27T13:01:09+08:00"),
    buildEvent("task-20260827-0104", "trace-20260827-0104", "sql_generated", "查询条件已生成。", "info", "2026-08-27T13:01:11+08:00"),
    buildEvent("task-20260827-0104", "trace-20260827-0104", "query_executed", "查询执行成功，但没有命中数据。", "warning", "2026-08-27T13:01:14+08:00"),
    buildEvent("task-20260827-0104", "trace-20260827-0104", "task_finished", "已返回无结果提示。", "success", "2026-08-27T13:01:15+08:00"),
  ],
};
