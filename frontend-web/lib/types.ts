export type StreamEvent = {
  eventType: string;
  taskId: string;
  traceId: string;
  timestamp: string;
  message?: string;
};

export type TaskSummary = {
  taskId: string;
  status: "queued" | "running" | "needs_review" | "succeeded" | "failed";
  question: string;
};

