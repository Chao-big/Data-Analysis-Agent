export type DatasetType = "csv" | "excel" | "mysql";

export type TaskStatus = "queued" | "running" | "needs_review" | "succeeded" | "failed";

export type ConnectionStatus = "idle" | "connecting" | "open" | "closed" | "error";

export type EventLevel = "info" | "warning" | "error" | "success";

export type StreamEventType =
  | "task_started"
  | "context_built"
  | "sql_generated"
  | "query_executed"
  | "chart_ready"
  | "task_finished"
  | "task_failed"
  | "human_review_required";

export type ChartPreference = "auto" | "line" | "bar" | "pie";

export type ChartConfig = {
  id: string;
  title: string;
  description: string;
  type: Exclude<ChartPreference, "auto">;
  option: Record<string, unknown>;
};

export type DatasetSummary = {
  id: string;
  datasetName: string;
  datasetType: DatasetType;
  sourceLocation: string;
  schemaSummary: string[];
  owner: string;
  tenantId: string;
  createdAt: string;
  permissionScope: string;
  rowCount: number;
  description: string;
  sampleRows: Record<string, string | number>[];
  status?: "active" | "draft";
};

export type StreamEvent = {
  eventType: StreamEventType;
  taskId: string;
  traceId: string;
  timestamp: string;
  message: string;
  level: EventLevel;
};

export type TaskSummary = {
  taskId: string;
  traceId: string;
  status: TaskStatus;
  question: string;
  datasetIds: string[];
  startedAt: string;
  finishedAt?: string;
  duration?: string;
};

export type AnalysisTask = TaskSummary & {
  connectionStatus: ConnectionStatus;
  chartPreferences: ChartPreference[];
  sqlText?: string;
  sqlReasoning?: string;
  finalConclusion?: string;
  warnings: string[];
  failureReason?: string;
  reviewMessage?: string;
  resultKind: "normal" | "empty" | "blocked" | "review";
  charts: ChartConfig[];
  relatedDatasetNames: string[];
};

export type UserProfile = {
  userId: string;
  username: string;
  displayName: string;
  role: string;
  organization: string;
  email: string;
  phone: string;
  tenantId: string;
  lastLoginAt: string;
  passwordPolicy: string;
};

export type LoginCredentials = {
  identifier: string;
  password: string;
};

export type AuthApiResponse<T> = {
  success: boolean;
  code: number;
  data: T | null;
  message: string;
};

export type UploadDatasetResult = {
  datasetId: string;
  sourceType: string;
  status: string;
};


export type AuthTokenPayload = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  refreshTokenExpiresIn: number;
  userId: string;
  username: string;
  nickname: string;
  avatarUrl: string | null;
  status: string;
  tenantId: string;
  roles: string[];
};

export type RegisterUserForm = {
  username: string;
  displayName: string;
  email: string;
  phone: string;
  password: string;
};

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  refreshTokenExpiresIn: number;
  userId: string;
  username: string;
  displayName: string;
  tenantId: string;
  roles: string[];
  email: string;
  phone: string;
  loginAt: string;
};
