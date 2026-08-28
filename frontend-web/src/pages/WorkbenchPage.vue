<script setup lang="ts">
import { computed } from "vue";
import AppShell from "../components/AppShell.vue";
import ChartRenderer from "../components/ChartRenderer.vue";
import StatusBadge from "../components/StatusBadge.vue";
import {
  datasetTypeLabel,
  formatStage,
  stageDescription,
  statusMeta,
  workspaceStore,
} from "../lib/workspace-store";
import type { AnalysisTask, ChartPreference, StreamEvent, StreamEventType } from "../lib/types";

const chartOptions: { label: string; value: ChartPreference }[] = [
  { label: "自动", value: "auto" },
  { label: "折线", value: "line" },
  { label: "柱状", value: "bar" },
  { label: "饼图", value: "pie" },
];

const selectedTask = computed(() => workspaceStore.currentTask.value);
const selectedEvents = computed(() => workspaceStore.currentEvents.value);
const selectedDatasets = computed(() => workspaceStore.currentDatasets.value);
const quickDatasets = computed(() => workspaceStore.datasets.slice(0, 6));
const heroChart = computed(() => selectedTask.value?.charts[0] ?? null);
const extraCharts = computed(() => selectedTask.value?.charts.slice(1) ?? []);

const resultTitle = computed(() => {
  if (!selectedTask.value) {
    return "开始新的分析对话";
  }

  if (selectedTask.value.resultKind === "review") {
    return "请求触发人工复核";
  }

  if (selectedTask.value.status === "failed") {
    return "任务执行失败";
  }

  if (selectedTask.value.resultKind === "empty") {
    return "查询完成，但没有命中数据";
  }

  return "分析结果已返回";
});

const resultBody = computed(() => {
  if (!selectedTask.value) {
    return "在底部输入业务问题，系统会按消息流返回结果，执行过程只在需要时展开查看。";
  }

  if (selectedTask.value.resultKind === "review") {
    return selectedTask.value.reviewMessage ?? "当前请求命中了风控规则，需要管理员确认说明后再查看拦截原因。";
  }

  if (selectedTask.value.status === "failed") {
    return selectedTask.value.failureReason ?? "任务未能完成，可按需展开过程明细查看原因。";
  }

  if (selectedTask.value.resultKind === "empty") {
    return "建议放宽筛选范围、扩大时间区间，或调整问题描述后重新发起分析。";
  }

  return selectedTask.value.finalConclusion ?? "任务完成后，这里会显示面向业务的结论摘要。";
});

const processPreview = computed(() => {
  if (!selectedTask.value) {
    return "等待新的分析问题";
  }

  if (selectedTask.value.resultKind === "review") {
    return "请求触发风控规则，等待人工复核";
  }

  if (selectedTask.value.status === "failed") {
    return "分析过程被中断，点击查看执行细节";
  }

  const lastEvent = selectedEvents.value[selectedEvents.value.length - 1];
  if (!lastEvent) {
    return "已接收问题，准备开始分析";
  }

  switch (lastEvent.eventType) {
    case "task_started":
      return "正在理解分析需求";
    case "context_built":
      return "正在整理数据上下文";
    case "sql_generated":
      return "已生成只读 SQL，准备执行查询";
    case "query_executed":
      return "查询已完成，正在汇总结果";
    case "chart_ready":
      return "图表已生成，正在组织回答";
    case "task_finished":
      return "结果已整理完成";
    case "human_review_required":
      return "需要人工复核后才能继续说明";
    case "task_failed":
      return "分析过程已终止";
    default:
      return "查看执行过程";
  }
});

function taskStatusLabel(task: AnalysisTask) {
  return statusMeta[task.status].label;
}

function taskStatusTone(task: AnalysisTask) {
  return statusMeta[task.status].tone;
}

function formatTaskTime(task: AnalysisTask) {
  return task.finishedAt ?? task.startedAt;
}

function formatEventTime(timestamp: string) {
  return new Date(timestamp).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function stageState(index: number) {
  if (!selectedTask.value) {
    return "pending";
  }

  if (selectedTask.value.status === "failed" && index === workspaceStore.currentStageIndex.value) {
    return "failed";
  }

  if (index < workspaceStore.currentStageIndex.value) {
    return "done";
  }

  if (index === workspaceStore.currentStageIndex.value) {
    return "current";
  }

  return "pending";
}

function stageStateLabel(index: number) {
  const state = stageState(index);

  if (state === "failed") {
    return "失败";
  }

  if (state === "done") {
    return "完成";
  }

  if (state === "current") {
    return "进行中";
  }

  return "待执行";
}

function stageStateClasses(index: number) {
  const state = stageState(index);

  if (state === "failed") {
    return "border-rose-200 bg-rose-50 text-rose-700";
  }

  if (state === "done") {
    return "border-emerald-200 bg-emerald-50 text-emerald-700";
  }

  if (state === "current") {
    return "border-cyan-200 bg-cyan-50 text-cyan-700";
  }

  return "border-[#d9e6ec] bg-white text-[#7f93a7]";
}

function sqlAuditLabel(task: AnalysisTask | null) {
  if (!task) {
    return "等待生成";
  }

  if (task.resultKind === "review" || task.resultKind === "blocked") {
    return "风控拦截";
  }

  return "只读校验通过";
}

function sqlAuditTone(task: AnalysisTask | null) {
  if (!task) {
    return "default";
  }

  if (task.resultKind === "review" || task.resultKind === "blocked") {
    return "needs_review";
  }

  return "succeeded";
}

function eventTone(event: StreamEvent) {
  return event.level;
}

function eventLabel(eventType: StreamEventType) {
  return formatStage(eventType);
}

function processMarkerClasses() {
  if (!selectedTask.value) {
    return "border-[#d8e6ed] bg-white text-[#71859a]";
  }

  if (selectedTask.value.status === "failed" || selectedTask.value.resultKind === "blocked") {
    return "border-rose-200 bg-rose-50 text-rose-700";
  }

  if (selectedTask.value.resultKind === "review") {
    return "border-amber-200 bg-amber-50 text-amber-700";
  }

  return "border-emerald-200 bg-emerald-50 text-emerald-700";
}
</script>

<template>
  <AppShell>
    <div class="grid min-h-[calc(100vh-32px)] gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
      <section class="flex min-h-0 flex-col overflow-hidden rounded-[32px] border border-white/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.97),rgba(244,249,251,0.95))] shadow-[0_24px_60px_rgba(15,23,42,0.05)]">
        <div class="border-b border-[#dbe7ed] px-5 py-5">
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-[0.24em] text-[#16807d]">Conversation Workspace</div>
              <h1 class="display-face mt-1 text-[2rem] font-semibold leading-none text-[#102038]">智能问数</h1>
              <p class="mt-2 text-sm leading-7 text-[#64788c]">按对话组织分析任务，而不是在多个面板之间来回切换。</p>
            </div>
            <button
              type="button"
              class="rounded-[18px] border border-[#bde7e2] bg-[#ebfaf7] px-4 py-2 text-sm font-semibold text-[#127f7b] transition hover:bg-[#ddf6f1]"
              @click="workspaceStore.resetDraft()"
            >
              新建对话
            </button>
          </div>

          <label class="mt-4 block">
            <span class="sr-only">搜索任务</span>
            <input
              :value="workspaceStore.state.searchText"
              class="h-12 w-full rounded-[20px] border border-[#d8e5eb] bg-white px-4 text-sm text-[#14253d] outline-none transition placeholder:text-[#94a4b5] focus:border-[#6fcac0] focus:ring-4 focus:ring-[#e3f6f3]"
              placeholder="搜索问题、Task ID 或 Trace ID"
              @input="workspaceStore.setSearchText(($event.target as HTMLInputElement).value)"
            />
          </label>
        </div>

        <div class="min-h-0 flex-1 overflow-y-auto px-4 py-4">
          <div class="space-y-5">
            <div v-for="group in workspaceStore.groupedTasks.value" :key="group.title">
              <div class="mb-3 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-[#8a9aab]">
                <span>{{ group.title }}</span>
                <span class="h-px flex-1 bg-[#dce8ee]" />
              </div>

              <div class="space-y-2">
                <button
                  v-for="task in group.items"
                  :key="task.taskId"
                  type="button"
                  :class="[
                    'w-full rounded-[24px] border px-4 py-4 text-left transition',
                    task.taskId === workspaceStore.state.selectedTaskId
                      ? 'border-[#bfe8e3] bg-[linear-gradient(135deg,rgba(232,250,247,0.98),rgba(242,248,255,0.98))] shadow-[0_14px_26px_rgba(15,139,141,0.08)]'
                      : 'border-[#dce8ee] bg-white/92 hover:border-[#cbdde6] hover:bg-white',
                  ]"
                  @click="workspaceStore.selectTask(task.taskId)"
                >
                  <div class="flex items-start justify-between gap-3">
                    <p class="line-clamp-2 text-sm font-semibold leading-6 text-[#102038]">{{ task.question }}</p>
                    <StatusBadge :label="taskStatusLabel(task)" :tone="taskStatusTone(task)" />
                  </div>
                  <p class="mt-3 line-clamp-1 text-xs text-[#7f93a7]">{{ task.traceId }}</p>
                  <div class="mt-2 flex items-center justify-between gap-3 text-xs text-[#8a9cad]">
                    <span>{{ formatTaskTime(task) }}</span>
                    <span>{{ task.duration ?? "--" }}</span>
                  </div>
                </button>
              </div>
            </div>

            <div
              v-if="!workspaceStore.groupedTasks.value.length"
              class="rounded-[24px] border border-dashed border-[#cadbe4] bg-[#f8fbfc] px-4 py-5 text-sm leading-7 text-[#65788d]"
            >
              没有匹配到历史对话。可以清空搜索后重试，或者直接发起一个新问题。
            </div>
          </div>
        </div>

        <div class="border-t border-[#dbe7ed] px-4 py-4">
          <div class="rounded-[24px] border border-[#dce8ee] bg-white/92 p-4">
            <div class="flex items-center justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-[#102038]">本次提问数据源</div>
                <div class="mt-1 text-xs text-[#8293a5]">点选后会作为底部输入框的当前上下文。</div>
              </div>
              <RouterLink to="/datasets" class="text-xs font-semibold text-[#14807d]">管理</RouterLink>
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button
                v-for="dataset in quickDatasets"
                :key="dataset.id"
                type="button"
                :class="[
                  'rounded-full border px-3 py-2 text-xs font-medium transition',
                  workspaceStore.state.selectedDatasetIds.includes(dataset.id)
                    ? 'border-[#bde7e2] bg-[#ebfaf7] text-[#127f7b]'
                    : 'border-[#d8e5eb] bg-[#f7fafc] text-[#5f7285]',
                ]"
                @click="workspaceStore.toggleDataset(dataset.id)"
              >
                {{ dataset.datasetName }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="flex min-h-0 flex-col overflow-hidden rounded-[32px] border border-white/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.94),rgba(244,249,251,0.98))] shadow-[0_24px_60px_rgba(15,23,42,0.05)]">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[#dde8ee] px-4 py-3.5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-full border border-[#d8e6ed] bg-white px-3 py-1 text-xs font-semibold text-[#5c7288]">对话工作台</span>
            <StatusBadge v-if="workspaceStore.currentStatus.value" :label="workspaceStore.currentStatus.value.label" :tone="workspaceStore.currentStatus.value.tone" />
            <StatusBadge :label="workspaceStore.currentConnection.value.label" :tone="workspaceStore.currentConnection.value.tone" />
            <span v-if="selectedTask" class="rounded-full border border-[#d8e6ed] bg-white px-3 py-1 text-xs font-semibold text-[#5c7288]">
              {{ selectedTask.taskId }}
            </span>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <button
              type="button"
              :disabled="!selectedTask"
              class="rounded-[18px] border border-[#d8e6ed] bg-white px-3.5 py-2 text-xs font-semibold text-[#4c6279] transition hover:bg-[#f7fafc] disabled:cursor-not-allowed disabled:opacity-50"
              @click="selectedTask && workspaceStore.copyTraceId(selectedTask.traceId)"
            >
              {{ workspaceStore.state.copiedTraceId && selectedTask?.traceId === workspaceStore.state.copiedTraceId ? "已复制 Trace ID" : "复制 Trace ID" }}
            </button>
            <button
              type="button"
              :disabled="!selectedTask || workspaceStore.state.submitting"
              class="rounded-[18px] border border-[#d8e6ed] bg-white px-3.5 py-2 text-xs font-semibold text-[#4c6279] transition hover:bg-[#f7fafc] disabled:cursor-not-allowed disabled:opacity-50"
              @click="workspaceStore.retryCurrentTask()"
            >
              重新执行
            </button>
          </div>
        </div>

        <div class="min-h-0 flex-1 overflow-y-auto px-4 py-4">
          <div v-if="selectedTask" class="mx-auto max-w-[1040px] space-y-4">
            <div class="flex justify-end">
              <article class="max-w-[760px] rounded-[28px] bg-[#152239] px-5 py-5 text-white shadow-[0_20px_40px_rgba(21,34,57,0.18)]">
                <div class="flex items-center justify-between gap-3">
                  <span class="text-[11px] font-semibold uppercase tracking-[0.24em] text-[#86f0e2]">Business Question</span>
                  <span class="text-xs text-white/65">{{ formatTaskTime(selectedTask) }}</span>
                </div>
                <p class="mt-3 text-base leading-8">{{ selectedTask.question }}</p>
                <div v-if="selectedTask.relatedDatasetNames.length" class="mt-4 flex flex-wrap gap-2">
                  <span
                    v-for="datasetName in selectedTask.relatedDatasetNames"
                    :key="datasetName"
                    class="rounded-full border border-white/12 bg-white/10 px-3 py-1 text-xs font-medium text-white/78"
                  >
                    {{ datasetName }}
                  </span>
                </div>
              </article>
            </div>

            <div class="flex items-start gap-3">
              <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#0f8b8d,#34c7b8)] text-sm font-semibold text-white shadow-[0_14px_24px_rgba(15,139,141,0.24)]">
                AI
              </div>

              <div class="min-w-0 flex-1 space-y-3">
                <details
                  class="max-w-[460px]"
                  :open="workspaceStore.state.sqlExpanded"
                  @toggle="workspaceStore.setSqlExpanded(($event.target as HTMLDetailsElement).open)"
                >
                  <summary class="flex cursor-pointer list-none items-center gap-3 rounded-[18px] px-1 py-1 text-sm text-[#5f7388]">
                    <span :class="['flex h-7 w-7 items-center justify-center rounded-full border text-[13px] font-semibold', processMarkerClasses()]">
                      {{ workspaceStore.state.sqlExpanded ? "−" : "✓" }}
                    </span>
                    <span class="font-medium text-[#43586e]">{{ processPreview }}</span>
                    <svg
                      viewBox="0 0 20 20"
                      :class="[
                        'ml-auto h-4 w-4 stroke-[#8ea1b3] stroke-[1.8] transition',
                        workspaceStore.state.sqlExpanded ? 'rotate-180' : '',
                      ]"
                      fill="none"
                    >
                      <path d="m5 8 5 5 5-5" />
                    </svg>
                  </summary>

                  <div class="mt-3 rounded-[24px] border border-[#dce8ee] bg-white/94 p-5">
                    <div class="flex flex-wrap gap-2">
                      <div
                        v-for="(stage, index) in workspaceStore.stageFlow"
                        :key="stage"
                        :class="['rounded-full border px-3 py-2 text-xs font-semibold transition', stageStateClasses(index)]"
                      >
                        {{ formatStage(stage) }} · {{ stageStateLabel(index) }}
                      </div>
                    </div>

                    <div class="mt-4 space-y-3">
                      <template v-if="selectedEvents.length">
                        <div
                          v-for="event in selectedEvents"
                          :key="`${event.taskId}-${event.eventType}-${event.timestamp}`"
                          class="rounded-[20px] border border-[#e3edf1] bg-[#f8fbfc] p-4"
                        >
                          <div class="flex flex-wrap items-center justify-between gap-3">
                            <div class="flex items-center gap-2">
                              <StatusBadge :label="eventLabel(event.eventType)" :tone="eventTone(event)" />
                              <span class="text-xs text-[#8b9dad]">{{ stageDescription(event.eventType) }}</span>
                            </div>
                            <span class="text-xs text-[#8799aa]">{{ formatEventTime(event.timestamp) }}</span>
                          </div>
                          <p class="mt-3 text-sm leading-7 text-[#102038]">{{ event.message }}</p>
                        </div>
                      </template>
                      <div
                        v-else
                        class="rounded-[20px] border border-dashed border-[#cfdee6] bg-[#f8fbfc] px-4 py-5 text-sm leading-7 text-[#65788d]"
                      >
                        提交任务后，这里会显示当前分析过程。
                      </div>
                    </div>

                    <div class="mt-4 rounded-[22px] bg-[#132038] px-4 py-4 text-white">
                      <div class="flex flex-wrap items-center gap-2">
                        <StatusBadge label="SQL" tone="default" />
                        <StatusBadge :label="sqlAuditLabel(selectedTask)" :tone="sqlAuditTone(selectedTask)" />
                      </div>
                      <pre
                        class="mono-face mt-3 overflow-x-auto whitespace-pre-wrap break-words text-sm leading-7 text-white/88"
                      >{{ selectedTask.sqlText ?? "-- 当前任务还没有生成可展示的 SQL" }}</pre>
                      <p v-if="selectedTask.sqlReasoning" class="mt-3 text-sm leading-7 text-white/72">
                        {{ selectedTask.sqlReasoning }}
                      </p>
                    </div>
                  </div>
                </details>

                <article class="rounded-[28px] border border-[#dce8ee] bg-white/94 p-5">
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div class="text-[11px] font-semibold uppercase tracking-[0.22em] text-[#8da0b0]">Assistant Reply</div>
                      <h2 class="mt-1 text-[1.45rem] font-semibold text-[#102038]">{{ resultTitle }}</h2>
                    </div>
                    <StatusBadge :label="sqlAuditLabel(selectedTask)" :tone="sqlAuditTone(selectedTask)" />
                  </div>
                  <p class="mt-3 text-sm leading-8 text-[#566b81]">{{ resultBody }}</p>
                  <div v-if="selectedTask.warnings.length" class="mt-3 flex flex-wrap gap-2">
                    <span
                      v-for="warning in selectedTask.warnings"
                      :key="warning"
                      class="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700"
                    >
                      {{ warning }}
                    </span>
                  </div>
                </article>

                <article
                  v-if="selectedTask.resultKind === 'review'"
                  class="rounded-[28px] border border-amber-200 bg-amber-50 p-5"
                >
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div class="text-[11px] font-semibold uppercase tracking-[0.22em] text-amber-700">Manual Review</div>
                      <h3 class="mt-1 text-xl font-semibold text-amber-900">管理员确认说明</h3>
                    </div>
                    <StatusBadge label="仅记录确认，不放行 SQL" tone="needs_review" />
                  </div>
                  <p class="mt-3 text-sm leading-7 text-amber-900/85">
                    这里只记录你已经知晓风险说明，系统仍然不会执行任何非 SELECT SQL。
                  </p>
                  <div class="mt-3.5 grid gap-3 sm:grid-cols-[1fr_156px]">
                    <input
                      :value="workspaceStore.state.reviewPassword"
                      type="password"
                      class="h-12 rounded-[18px] border border-amber-200 bg-white px-4 text-sm text-[#102038] outline-none transition focus:border-amber-300"
                      placeholder="输入管理员密码"
                      @input="workspaceStore.setReviewPassword(($event.target as HTMLInputElement).value)"
                    />
                    <button
                      type="button"
                      class="rounded-[18px] bg-amber-500 px-4 py-3 text-sm font-semibold text-white transition hover:bg-amber-600"
                      @click="workspaceStore.confirmReview()"
                    >
                      确认说明
                    </button>
                  </div>
                  <p v-if="workspaceStore.state.reviewError" class="mt-3 text-sm text-rose-600">
                    {{ workspaceStore.state.reviewError }}
                  </p>
                </article>

                <article
                  v-if="heroChart || extraCharts.length"
                  class="rounded-[28px] border border-[#dce8ee] bg-white/94 p-5"
                >
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div class="text-[11px] font-semibold uppercase tracking-[0.22em] text-[#8da0b0]">Visual Result</div>
                      <h3 class="mt-1 text-[1.35rem] font-semibold text-[#102038]">{{ heroChart?.title ?? "图表结果" }}</h3>
                      <p class="mt-2 text-sm leading-7 text-[#65788d]">
                        {{ heroChart?.description ?? "分析图表会跟随结论直接出现在对话流中。" }}
                      </p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                      <span
                        v-for="dataset in selectedDatasets"
                        :key="dataset.id"
                        class="rounded-full border border-[#d8e6ed] bg-[#f7fafc] px-3 py-1 text-xs font-semibold text-[#5e7286]"
                      >
                        {{ datasetTypeLabel[dataset.datasetType] }}
                      </span>
                    </div>
                  </div>

                  <div
                    v-if="heroChart"
                    class="mt-3 rounded-[24px] border border-[#e3edf1] bg-white px-3 py-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)]"
                  >
                    <ChartRenderer :option="heroChart.option" />
                  </div>

                  <div v-if="extraCharts.length" class="mt-3 grid gap-3 xl:grid-cols-2">
                    <div
                      v-for="chart in extraCharts"
                      :key="chart.id"
                      class="rounded-[22px] border border-[#e3edf1] bg-[#f8fbfc] p-4"
                    >
                      <div class="flex items-center justify-between gap-3">
                        <div>
                          <h4 class="text-lg font-semibold text-[#102038]">{{ chart.title }}</h4>
                          <p class="mt-1 text-sm text-[#66798d]">{{ chart.description }}</p>
                        </div>
                        <span class="rounded-full border border-[#d8e6ed] bg-white px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-[#5f7286]">
                          {{ chart.type }}
                        </span>
                      </div>
                      <div class="mt-3 rounded-[18px] border border-[#e3edf1] bg-white p-3">
                        <ChartRenderer :option="chart.option" />
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>

          <div v-else class="flex h-full min-h-[420px] items-center justify-center px-4">
            <div class="max-w-[720px] text-center">
              <div class="text-[11px] font-semibold uppercase tracking-[0.28em] text-[#16807d]">Conversation First</div>
              <h2 class="display-face mt-3 text-[2.6rem] font-semibold leading-[1.04] text-[#102038]">把分析过程放回一条对话里</h2>
              <p class="mt-4 text-base leading-8 text-[#627588]">
                这里不再堆叠很多模块。你只需要在底部输入问题，系统会按消息流返回结论、图表和提示，过程明细按需展开。
              </p>
            </div>
          </div>
        </div>

        <div class="border-t border-[#dde8ee] bg-[linear-gradient(180deg,rgba(255,255,255,0.74),rgba(239,247,249,0.96))] p-4">
          <div class="mx-auto max-w-[980px] rounded-[28px] border border-[#dce8ee] bg-white/94 p-4 shadow-[0_16px_36px_rgba(15,23,42,0.04)]">
            <div class="flex flex-wrap items-center gap-2 text-xs">
              <span class="font-semibold uppercase tracking-[0.18em] text-[#8ca0b1]">当前数据源</span>
              <span
                v-for="dataset in selectedDatasets"
                :key="dataset.id"
                class="rounded-full border border-[#bfe8e3] bg-[#ebfaf7] px-3 py-1 font-semibold text-[#127f7b]"
              >
                {{ dataset.datasetName }}
              </span>
              <span v-if="!selectedDatasets.length" class="text-[#8ca0b1]">尚未选择数据源</span>
            </div>

            <textarea
              :value="workspaceStore.state.question"
              rows="4"
              class="mt-4 w-full rounded-[24px] border border-[#d8e6ed] bg-[linear-gradient(180deg,#ffffff_0%,#f9fbfc_100%)] px-4 py-3 text-sm leading-7 text-[#102038] outline-none transition placeholder:text-[#95a6b6] focus:border-[#6fcac0] focus:ring-4 focus:ring-[#e3f6f3]"
              placeholder="例如：分析过去 12 个月企业营收趋势，并输出区域排名与渠道占比。"
              @input="workspaceStore.setQuestion(($event.target as HTMLTextAreaElement).value)"
            />

            <div class="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <div class="text-xs font-semibold uppercase tracking-[0.18em] text-[#8ca0b1]">图表偏好</div>
                <div class="mt-2 flex flex-wrap gap-2">
                  <button
                    v-for="item in chartOptions"
                    :key="item.value"
                    type="button"
                    :class="[
                      'rounded-full border px-3.5 py-2 text-xs font-semibold transition',
                      workspaceStore.state.chartPreferences.includes(item.value)
                        ? 'border-[#bde7e2] bg-[#ebfaf7] text-[#127f7b]'
                        : 'border-[#d8e6ed] bg-[#f7fafc] text-[#5f7285]',
                    ]"
                    @click="workspaceStore.toggleChartPreference(item.value)"
                  >
                    {{ item.label }}
                  </button>
                </div>
              </div>

              <div class="flex flex-wrap gap-3">
                <button
                  type="button"
                  class="rounded-[20px] border border-[#d8e6ed] bg-white px-5 py-3 text-sm font-medium text-[#4f6478] transition hover:bg-[#f7fafc]"
                  @click="workspaceStore.resetDraft()"
                >
                  清空草稿
                </button>
                <button
                  type="button"
                  class="rounded-[20px] bg-[#16213a] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#111b31]"
                  @click="workspaceStore.submitTask()"
                >
                  {{ workspaceStore.state.submitting ? "正在提交分析..." : "发送分析请求" }}
                </button>
              </div>
            </div>

            <p v-if="workspaceStore.state.validationError" class="mt-3 text-sm text-rose-600">
              {{ workspaceStore.state.validationError }}
            </p>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>
