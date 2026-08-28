<script setup lang="ts">
import type { ConnectionStatus, StreamEvent } from "../lib/types";
import { formatStage } from "../lib/workspace-store";
import StatusBadge from "./StatusBadge.vue";

defineProps<{
  events: StreamEvent[];
  connectionStatus: ConnectionStatus;
}>();

const connectionLabelMap: Record<ConnectionStatus, string> = {
  idle: "未连接",
  connecting: "连接中",
  open: "已连接",
  closed: "已关闭",
  error: "连接错误",
};
</script>

<template>
  <section class="rounded-[28px] border border-slate-200 bg-white/92 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-xl font-semibold text-slate-900">SSE 任务事件流</h2>
        <p class="mt-2 text-sm leading-7 text-slate-500">
          展示任务创建、上下文构建、SQL 生成、查询执行、图表生成、完成或失败等关键节点。
        </p>
      </div>
      <StatusBadge :label="connectionLabelMap[connectionStatus]" :tone="connectionStatus" />
    </div>

    <div class="mt-5 space-y-4">
      <template v-if="events.length">
        <div v-for="(event, index) in events" :key="`${event.taskId}-${event.eventType}-${event.timestamp}`" class="flex gap-4">
          <div class="flex w-4 flex-col items-center">
            <span
              :class="[
                'mt-2 h-3 w-3 rounded-full',
                event.level === 'error'
                  ? 'bg-rose-500'
                  : event.level === 'warning'
                    ? 'bg-amber-400'
                    : event.level === 'success'
                      ? 'bg-emerald-500'
                      : 'bg-cyan-500',
              ]"
            />
            <span v-if="index < events.length - 1" class="mt-2 min-h-12 w-px flex-1 bg-slate-200" />
          </div>
          <div class="flex-1 rounded-[22px] border border-slate-200 bg-slate-50/70 p-4">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <StatusBadge :label="formatStage(event.eventType)" :tone="event.level" />
              <span class="text-xs text-slate-400">{{ new Date(event.timestamp).toLocaleString("zh-CN") }}</span>
            </div>
            <p class="mt-3 text-sm leading-7 text-slate-700">{{ event.message }}</p>
            <p class="mt-2 text-xs text-slate-400">Trace ID: {{ event.traceId }}</p>
          </div>
        </div>
      </template>

      <div v-else class="rounded-[24px] border border-dashed border-slate-300 bg-slate-50/80 p-6 text-sm leading-7 text-slate-500">
        尚未提交任务。提交后这里会实时展示 `task_started`、`context_built`、`sql_generated`、`query_executed`、
        `chart_ready`、`task_finished` 或 `task_failed`。
      </div>
    </div>
  </section>
</template>
