import { computed, reactive } from "vue";
import { authSessionState } from "./auth";
import { currentUser as defaultCurrentUser, datasets, pickCharts, taskEvents, taskHistory } from "./mock-data";
import type { AnalysisTask, ChartPreference, ConnectionStatus, StreamEvent, StreamEventType, TaskStatus } from "./types";

const prohibitedKeywords = ["drop", "delete", "update", "alter", "truncate", "insert", "删除", "修改", "更新"];
const emptyKeywords = ["无结果", "没有数据", "空结果", "查不到"];
const failureKeywords = ["敏感", "越权", "失败", "error"];

const stageFlow: StreamEventType[] = [
  "task_started",
  "context_built",
  "sql_generated",
  "query_executed",
  "chart_ready",
  "task_finished",
];

export const dashboardStats = [
  { label: "今日任务", value: "28", helper: "成功率 89%" },
  { label: "授权数据集", value: "12", helper: "CSV / Excel / MySQL" },
  { label: "平均耗时", value: "17s", helper: "SSE 实时回传" },
  { label: "待人工审核", value: "2", helper: "高风险请求" },
];

export const datasetTypeLabel = {
  csv: "CSV",
  excel: "Excel",
  mysql: "MySQL",
} as const;

export const statusMeta: Record<TaskStatus, { label: string; tone: TaskStatus }> = {
  queued: { label: "排队中", tone: "queued" },
  running: { label: "执行中", tone: "running" },
  needs_review: { label: "待人工审核", tone: "needs_review" },
  succeeded: { label: "已完成", tone: "succeeded" },
  failed: { label: "已失败", tone: "failed" },
};

export const connectionMeta: Record<ConnectionStatus, { label: string; tone: ConnectionStatus }> = {
  idle: { label: "未连接", tone: "idle" },
  connecting: { label: "连接中", tone: "connecting" },
  open: { label: "已连接", tone: "open" },
  closed: { label: "已关闭", tone: "closed" },
  error: { label: "连接错误", tone: "error" },
};

const workspaceState = reactive<{
  question: string;
  selectedDatasetIds: string[];
  chartPreferences: ChartPreference[];
  tasks: AnalysisTask[];
  eventsByTaskId: Record<string, StreamEvent[]>;
  selectedTaskId: string | null;
  submitting: boolean;
  validationError: string | null;
  reviewPassword: string;
  reviewError: string | null;
  searchText: string;
  copiedTraceId: string | null;
  sqlExpanded: boolean;
}>({
  question: "分析过去 12 个月企业营收趋势，并输出区域排名与渠道占比。",
  selectedDatasetIds: ["dataset-enterprise-sales", "dataset-finance-mysql"],
  chartPreferences: ["line", "bar", "pie"] as ChartPreference[],
  tasks: [...taskHistory] as AnalysisTask[],
  eventsByTaskId: { ...taskEvents } as Record<string, StreamEvent[]>,
  selectedTaskId: taskHistory[0]?.taskId ?? null,
  submitting: false,
  validationError: null as string | null,
  reviewPassword: "",
  reviewError: null as string | null,
  searchText: "",
  copiedTraceId: null as string | null,
  sqlExpanded: false,
});

const timers: number[] = [];

function buildTimestamp(offsetSeconds: number) {
  return new Date(Date.now() + offsetSeconds * 1000).toISOString();
}

function buildEvent(
  taskId: string,
  traceId: string,
  eventType: StreamEventType,
  message: string,
  level: StreamEvent["level"],
  offsetSeconds: number,
): StreamEvent {
  return {
    taskId,
    traceId,
    eventType,
    message,
    level,
    timestamp: buildTimestamp(offsetSeconds),
  };
}

function buildScenarioTask({
  question,
  datasetIds,
  chartPreferences,
}: {
  question: string;
  datasetIds: string[];
  chartPreferences: ChartPreference[];
}) {
  const sequence = Date.now();
  const taskId = `task-${sequence}`;
  const traceId = `trace-${sequence}`;
  const relatedDatasetNames = datasets
    .filter((dataset) => datasetIds.includes(dataset.id))
    .map((dataset) => dataset.datasetName);
  const lowerQuestion = question.toLowerCase();

  const isProhibited = prohibitedKeywords.some((keyword) => lowerQuestion.includes(keyword));
  const isEmpty = emptyKeywords.some((keyword) => question.includes(keyword));
  const isFailure = failureKeywords.some((keyword) => question.includes(keyword) || lowerQuestion.includes(keyword));

  if (isProhibited) {
    return {
      task: {
        taskId,
        traceId,
        status: "needs_review",
        question,
        datasetIds,
        startedAt: new Date().toLocaleString("zh-CN"),
        duration: "审核中",
        connectionStatus: "open",
        chartPreferences,
        sqlText: "UPDATE finance.revenue_fact SET net_revenue = 0 WHERE order_month = '2026-08-01';",
        sqlReasoning: "问题包含写操作意图，系统仅允许 SELECT，已进入高风险审核。",
        finalConclusion: "请求已被拦截，等待管理员密码确认后展示详细说明。",
        warnings: ["仅允许 SELECT。", "禁止 INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE。"],
        reviewMessage: "请输入管理员密码确认你已知晓风险说明，系统不会执行写操作，只会返回拦截原因。",
        resultKind: "review",
        charts: [],
        relatedDatasetNames,
      } satisfies AnalysisTask,
      events: [
        buildEvent(taskId, traceId, "task_started", "任务已创建，开始权限与风险校验。", "info", 1),
        buildEvent(taskId, traceId, "context_built", "数据集上下文已准备完成。", "info", 2),
        buildEvent(taskId, traceId, "sql_generated", "检测到写操作请求，已阻断执行。", "warning", 3),
        buildEvent(taskId, traceId, "human_review_required", "需要管理员密码确认后展示详细拦截原因。", "warning", 4),
      ],
    };
  }

  if (isFailure) {
    return {
      task: {
        taskId,
        traceId,
        status: "failed",
        question,
        datasetIds,
        startedAt: new Date().toLocaleString("zh-CN"),
        finishedAt: new Date(Date.now() + 6000).toLocaleString("zh-CN"),
        duration: "6s",
        connectionStatus: "closed",
        chartPreferences,
        sqlReasoning: "任务在权限或字段层面被拦截，未生成可执行 SQL。",
        finalConclusion: "本次任务失败，原因是问题涉及未授权字段或不被允许的访问范围。",
        warnings: ["请检查字段是否在已授权数据集中。"],
        failureReason: "检测到越权字段访问或敏感数据分析请求，任务已终止。",
        resultKind: "blocked",
        charts: [],
        relatedDatasetNames,
      } satisfies AnalysisTask,
      events: [
        buildEvent(taskId, traceId, "task_started", "任务已创建。", "info", 1),
        buildEvent(taskId, traceId, "context_built", "开始校验字段授权范围。", "warning", 2),
        buildEvent(taskId, traceId, "task_failed", "越权访问被拦截，任务失败。", "error", 3),
      ],
    };
  }

  if (isEmpty) {
    return {
      task: {
        taskId,
        traceId,
        status: "succeeded",
        question,
        datasetIds,
        startedAt: new Date().toLocaleString("zh-CN"),
        finishedAt: new Date(Date.now() + 7000).toLocaleString("zh-CN"),
        duration: "7s",
        connectionStatus: "closed",
        chartPreferences,
        sqlText:
          "SELECT region, SUM(net_revenue) AS total_revenue FROM finance.revenue_fact WHERE order_month BETWEEN '2024-01-01' AND '2024-01-31' AND region = '东北' GROUP BY region;",
        sqlReasoning: "根据用户指定条件聚合查询，但结果为空。",
        finalConclusion: "查询已执行成功，但当前筛选条件下没有命中数据。",
        warnings: ["建议扩大时间范围或减少筛选条件。"],
        resultKind: "empty",
        charts: [],
        relatedDatasetNames,
      } satisfies AnalysisTask,
      events: [
        buildEvent(taskId, traceId, "task_started", "任务已创建。", "info", 1),
        buildEvent(taskId, traceId, "sql_generated", "只读 SQL 已生成。", "success", 2),
        buildEvent(taskId, traceId, "query_executed", "查询执行完成，返回 0 行结果。", "warning", 3),
        buildEvent(taskId, traceId, "task_finished", "已返回无结果提示。", "success", 4),
      ],
    };
  }

  return {
    task: {
      taskId,
      traceId,
      status: "succeeded",
      question,
      datasetIds,
      startedAt: new Date().toLocaleString("zh-CN"),
      finishedAt: new Date(Date.now() + 12000).toLocaleString("zh-CN"),
      duration: "12s",
      connectionStatus: "closed",
      chartPreferences,
      sqlText:
        "SELECT order_month, SUM(net_revenue) AS total_revenue FROM finance.revenue_fact WHERE order_month >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) GROUP BY order_month ORDER BY order_month ASC;",
      sqlReasoning: "围绕用户问题按月聚合营收，并准备趋势图、排名图与结构图。",
      finalConclusion:
        "营收整体呈上升趋势，华东和华南排名靠前，直营和经销渠道仍是主要贡献来源。若用于决策，建议进一步补充异常月份明细分析。",
      warnings: ["当前结果基于汇总数据，不包含退款与冲销明细。"],
      resultKind: "normal",
      charts: pickCharts(chartPreferences),
      relatedDatasetNames,
    } satisfies AnalysisTask,
    events: [
      buildEvent(taskId, traceId, "task_started", "任务已创建，开始准备分析上下文。", "info", 1),
      buildEvent(taskId, traceId, "context_built", "已完成权限校验与 Schema 摘要构建。", "info", 2),
      buildEvent(taskId, traceId, "sql_generated", "只读 SQL 已生成并通过校验。", "success", 3),
      buildEvent(taskId, traceId, "query_executed", "查询已执行完成，开始 Python 聚合分析。", "success", 4),
      buildEvent(taskId, traceId, "chart_ready", "图表配置已生成。", "success", 5),
      buildEvent(taskId, traceId, "task_finished", "最终结论已返回。", "success", 6),
    ],
  };
}

export function getStageIndex(task: AnalysisTask | undefined, events: StreamEvent[]) {
  if (!task) {
    return 0;
  }

  if (!events.length) {
    return task.status === "queued" ? 0 : 1;
  }

  const lastEvent = events[events.length - 1];
  if (lastEvent.eventType === "task_failed") {
    return Math.max(stageFlow.indexOf("sql_generated"), 2);
  }

  if (lastEvent.eventType === "human_review_required") {
    return stageFlow.indexOf("sql_generated");
  }

  const index = stageFlow.indexOf(lastEvent.eventType);
  return index === -1 ? 0 : index;
}

export function formatStage(stage: StreamEventType) {
  switch (stage) {
    case "task_started":
      return "任务创建";
    case "context_built":
      return "上下文构建";
    case "sql_generated":
      return "SQL 生成";
    case "query_executed":
      return "查询执行";
    case "chart_ready":
      return "图表生成";
    case "task_finished":
      return "任务完成";
    case "task_failed":
      return "任务失败";
    case "human_review_required":
      return "人工审核";
    default:
      return stage;
  }
}

export function stageDescription(stage: StreamEventType) {
  switch (stage) {
    case "task_started":
      return "创建任务，记录问题、数据集与 Trace ID。";
    case "context_built":
      return "构建已授权数据集的 Schema 摘要与分析上下文。";
    case "sql_generated":
      return "生成候选 SQL，并执行只读校验与风险检查。";
    case "query_executed":
      return "执行查询，返回聚合结果或空结果。";
    case "chart_ready":
      return "根据结果生成图表配置与展示内容。";
    case "task_finished":
      return "返回最终结论、告警、耗时与可追踪信息。";
    case "task_failed":
      return "任务在权限或风控阶段被终止。";
    case "human_review_required":
      return "命中高风险规则，需要人工确认说明。";
    default:
      return "";
  }
}

export function toggleDataset(datasetId: string) {
  const current = workspaceState.selectedDatasetIds;
  workspaceState.selectedDatasetIds = current.includes(datasetId)
    ? current.filter((id) => id !== datasetId)
    : [...current, datasetId];
}

export function toggleChartPreference(preference: ChartPreference) {
  const current = workspaceState.chartPreferences;

  if (preference === "auto") {
    workspaceState.chartPreferences = current.includes("auto") ? [] : ["auto"];
    return;
  }

  const next = current.filter((item) => item !== "auto");
  if (next.includes(preference)) {
    const filtered = next.filter((item) => item !== preference);
    workspaceState.chartPreferences = filtered.length ? filtered : ["auto"];
    return;
  }

  workspaceState.chartPreferences = [...next, preference];
}

export function resetDraft() {
  workspaceState.selectedTaskId = null;
  workspaceState.question = "";
  workspaceState.selectedDatasetIds = [];
  workspaceState.chartPreferences = ["auto"];
  workspaceState.validationError = null;
  workspaceState.reviewError = null;
  workspaceState.reviewPassword = "";
}

export function selectTask(taskId: string | null) {
  workspaceState.selectedTaskId = taskId;
}

export function setQuestion(value: string) {
  workspaceState.question = value;
}

export function setSearchText(value: string) {
  workspaceState.searchText = value;
}

export function setReviewPassword(value: string) {
  workspaceState.reviewPassword = value;
}

export function setSqlExpanded(value: boolean) {
  workspaceState.sqlExpanded = value;
}

export function clearCopiedTraceId() {
  workspaceState.copiedTraceId = null;
}

export async function copyTraceId(traceId: string) {
  try {
    await navigator.clipboard.writeText(traceId);
    workspaceState.copiedTraceId = traceId;
    window.setTimeout(() => {
      if (workspaceState.copiedTraceId === traceId) {
        workspaceState.copiedTraceId = null;
      }
    }, 1600);
  } catch {
    workspaceState.copiedTraceId = null;
  }
}

export function submitTask(payload?: {
  question: string;
  datasetIds: string[];
  chartPreferences: ChartPreference[];
}) {
  const nextQuestion = payload?.question.trim() ?? workspaceState.question.trim();
  const nextDatasetIds = payload?.datasetIds ?? workspaceState.selectedDatasetIds;
  const nextChartPreferences = payload?.chartPreferences?.length ? payload.chartPreferences : workspaceState.chartPreferences;

  if (!nextDatasetIds.length) {
    workspaceState.validationError = "请至少选择一个数据集后再提交任务。";
    return false;
  }

  if (!nextQuestion) {
    workspaceState.validationError = "分析问题不能为空。";
    return false;
  }

  workspaceState.validationError = null;
  workspaceState.submitting = true;
  workspaceState.sqlExpanded = false;

  const normalizedPreferences: ChartPreference[] = nextChartPreferences.length ? nextChartPreferences : ["auto"];
  const scenario = buildScenarioTask({
    question: nextQuestion,
    datasetIds: nextDatasetIds,
    chartPreferences: normalizedPreferences,
  });

  const pendingTask: AnalysisTask = {
    ...scenario.task,
    status: "queued",
    duration: "排队中",
    connectionStatus: "connecting",
    charts: [],
    sqlText: undefined,
    sqlReasoning: undefined,
    finalConclusion: undefined,
    failureReason: undefined,
    reviewMessage: undefined,
    warnings: [],
  };

  workspaceState.question = nextQuestion;
  workspaceState.selectedDatasetIds = [...nextDatasetIds];
  workspaceState.chartPreferences = normalizedPreferences;
  workspaceState.tasks = [pendingTask, ...workspaceState.tasks];
  workspaceState.selectedTaskId = pendingTask.taskId;
  workspaceState.eventsByTaskId = {
    ...workspaceState.eventsByTaskId,
    [pendingTask.taskId]: [],
  };

  const createdTimers = scenario.events.map((event, index) =>
    window.setTimeout(() => {
      workspaceState.eventsByTaskId = {
        ...workspaceState.eventsByTaskId,
        [pendingTask.taskId]: [...(workspaceState.eventsByTaskId[pendingTask.taskId] ?? []), event],
      };

      workspaceState.tasks = workspaceState.tasks.map((task) => {
        if (task.taskId !== pendingTask.taskId) {
          return task;
        }

        if (index === 0) {
          return { ...task, status: "running", connectionStatus: "open", duration: "执行中" };
        }

        if (event.eventType === "human_review_required") {
          return {
            ...task,
            ...scenario.task,
            status: "needs_review",
            connectionStatus: "open",
            duration: "审核中",
          };
        }

        if (event.eventType === "task_failed") {
          return {
            ...task,
            ...scenario.task,
            status: "failed",
            connectionStatus: "closed",
          };
        }

        if (event.eventType === "task_finished") {
          return {
            ...task,
            ...scenario.task,
            connectionStatus: "closed",
          };
        }

        if (event.eventType === "chart_ready") {
          return {
            ...task,
            ...scenario.task,
            status: "running",
            connectionStatus: "open",
            charts: scenario.task.charts,
          };
        }

        return {
          ...task,
          ...scenario.task,
          status: "running",
          connectionStatus: "open",
          charts: event.eventType === "query_executed" ? scenario.task.charts : task.charts,
        };
      });

      if (index === scenario.events.length - 1) {
        workspaceState.submitting = false;
      }
    }, 800 * (index + 1)),
  );

  timers.push(...createdTimers);
  return true;
}

export function retryCurrentTask() {
  const task = currentTask.value;
  if (!task) {
    return;
  }

  submitTask({
    question: task.question,
    datasetIds: task.datasetIds,
    chartPreferences: task.chartPreferences,
  });
}

export function confirmReview() {
  const task = currentTask.value;

  if (!task || task.status !== "needs_review") {
    return;
  }

  if (workspaceState.reviewPassword !== "admin") {
    workspaceState.reviewError = "管理员密码错误。当前原型默认密码为 admin。";
    return;
  }

  workspaceState.reviewError = null;
  const finalEvent = buildEvent(
    task.taskId,
    task.traceId,
    "task_failed",
    "管理员已确认风险说明。系统继续保持阻断，不执行任何写操作。",
    "error",
    1,
  );

  workspaceState.eventsByTaskId = {
    ...workspaceState.eventsByTaskId,
    [task.taskId]: [...(workspaceState.eventsByTaskId[task.taskId] ?? []), finalEvent],
  };

  workspaceState.tasks = workspaceState.tasks.map((item) =>
    item.taskId === task.taskId
      ? {
          ...item,
          status: "failed",
          connectionStatus: "closed",
          duration: "已拦截",
          resultKind: "blocked",
          failureReason: "高风险写操作请求已确认并保持拦截。平台不会执行任何非 SELECT SQL。",
          finalConclusion: "风险说明已确认，但任务不会继续执行。请改写为只读分析问题。",
          warnings: ["密码确认仅用于审计留痕，不会放行危险 SQL。", "建议改写为查询、汇总或趋势分析问题。"],
        }
      : item,
  );
  workspaceState.reviewPassword = "";
}

export const filteredTasks = computed(() => {
  const keyword = workspaceState.searchText.trim().toLowerCase();
  if (!keyword) {
    return workspaceState.tasks;
  }

  return workspaceState.tasks.filter((task) => {
    return (
      task.question.toLowerCase().includes(keyword) ||
      task.taskId.toLowerCase().includes(keyword) ||
      task.traceId.toLowerCase().includes(keyword)
    );
  });
});

export const groupedTasks = computed(() => {
  return [
    { title: "今天", items: filteredTasks.value.slice(0, 2) },
    { title: "7 天内", items: filteredTasks.value.slice(2, 5) },
    { title: "更早以前", items: filteredTasks.value.slice(5) },
  ].filter((group) => group.items.length > 0);
});

export const currentTask = computed(() => workspaceState.tasks.find((task) => task.taskId === workspaceState.selectedTaskId));

export const currentEvents = computed(() =>
  currentTask.value ? workspaceState.eventsByTaskId[currentTask.value.taskId] ?? [] : [],
);

export const currentStageIndex = computed(() => getStageIndex(currentTask.value, currentEvents.value));

export const currentStatus = computed(() => (currentTask.value ? statusMeta[currentTask.value.status] : null));

export const currentConnection = computed(() =>
  currentTask.value ? connectionMeta[currentTask.value.connectionStatus] : connectionMeta.idle,
);

export const currentDatasets = computed(() =>
  datasets.filter((dataset) => workspaceState.selectedDatasetIds.includes(dataset.id)),
);

export const workspaceStore = {
  state: workspaceState,
  datasets,
  get currentUser() {
    const session = authSessionState.session;
    if (!session) {
      return defaultCurrentUser;
    }

    return {
      ...defaultCurrentUser,
      userId: session.userId,
      username: session.username,
      nickname: session.nickname || session.displayName,
      displayName: session.displayName,
      avatarUrl: session.avatarUrl ?? defaultCurrentUser.avatarUrl,
      status: session.status || defaultCurrentUser.status,
      tenantId: session.tenantId,
      email: session.email || defaultCurrentUser.email,
      phone: session.phone || defaultCurrentUser.phone,
      gender: session.gender ?? defaultCurrentUser.gender,
      lastLoginIp: null,
      lastLoginAt: new Date(session.loginAt).toLocaleString("zh-CN"),
    };
  },
  dashboardStats,
  stageFlow,
  currentTask,
  currentEvents,
  currentStageIndex,
  currentStatus,
  currentConnection,
  currentDatasets,
  filteredTasks,
  groupedTasks,
  toggleDataset,
  toggleChartPreference,
  resetDraft,
  selectTask,
  setQuestion,
  setSearchText,
  setReviewPassword,
  setSqlExpanded,
  clearCopiedTraceId,
  copyTraceId,
  submitTask,
  retryCurrentTask,
  confirmReview,
};
