"use client";

import { StreamEvent } from "../lib/types";

const demoEvents: StreamEvent[] = [
  {
    eventType: "task_started",
    taskId: "task-demo-001",
    traceId: "trace-demo-001",
    timestamp: "2026-08-27T12:00:00Z",
    message: "Agent runtime bootstrapped",
  },
  {
    eventType: "sql_generated",
    taskId: "task-demo-001",
    traceId: "trace-demo-001",
    timestamp: "2026-08-27T12:00:02Z",
    message: "Generated read-only SQL for monthly revenue trend",
  },
  {
    eventType: "chart_ready",
    taskId: "task-demo-001",
    traceId: "trace-demo-001",
    timestamp: "2026-08-27T12:00:05Z",
    message: "Prepared line chart configuration",
  },
];

export function TaskStream() {
  return (
    <section className="rounded-3xl border border-stone-900/10 bg-white/80 p-6 shadow-lg shadow-stone-900/5 backdrop-blur">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold">SSE Task Stream</h2>
        <span className="rounded-full bg-teal-700 px-3 py-1 text-xs font-medium text-white">
          live
        </span>
      </div>
      <div className="space-y-3">
        {demoEvents.map((event) => (
          <div
            key={`${event.taskId}-${event.eventType}-${event.timestamp}`}
            className="rounded-2xl border border-stone-900/10 bg-stone-50 p-4"
          >
            <div className="text-sm font-semibold text-teal-800">{event.eventType}</div>
            <div className="mt-1 text-sm text-stone-700">{event.message}</div>
            <div className="mt-2 text-xs text-stone-500">{event.timestamp}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

