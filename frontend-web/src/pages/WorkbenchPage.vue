<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
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
import type { AnalysisTask, StreamEvent, StreamEventType } from "../lib/types";

const historyPanelExpanded = ref(true);
const editingTaskId = ref<string | null>(null);
const editingTitle = ref("");
const activeMenuTaskId = ref<string | null>(null);

const selectedTask = computed(() => workspaceStore.currentTask.value);
const selectedEvents = computed(() => workspaceStore.currentEvents.value);
const selectedDatasets = computed(() => workspaceStore.currentDatasets.value);
const heroChart = computed(() => selectedTask.value?.charts[0] ?? null);
const extraCharts = computed(() => selectedTask.value?.charts.slice(1) ?? []);
const workbenchGridClass = computed(() =>
  historyPanelExpanded.value ? "xl:grid-cols-[320px_minmax(0,1fr)]" : "xl:grid-cols-[minmax(0,1fr)]",
);

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

const currentConversationTitle = computed(() => {
  if (editingTaskId.value && editingTaskId.value === selectedTask.value?.taskId) {
    return editingTitle.value;
  }
  return selectedTask.value?.question ?? "新对话";
});

function toggleHistoryPanel() {
  historyPanelExpanded.value = !historyPanelExpanded.value;
}

function toggleTaskMenu(taskId: string, event: Event) {
  event.stopPropagation();
  activeMenuTaskId.value = activeMenuTaskId.value === taskId ? null : taskId;
}

function closeTaskMenu() {
  activeMenuTaskId.value = null;
}

function startRename(task: AnalysisTask | null | undefined, event?: Event) {
  event?.stopPropagation();
  if (!task) {
    return;
  }
  closeTaskMenu();
  editingTaskId.value = task.taskId;
  editingTitle.value = task.question;
}

function cancelRename() {
  editingTaskId.value = null;
  editingTitle.value = "";
}

function commitRename() {
  const taskId = editingTaskId.value;
  const title = editingTitle.value.trim();

  if (!taskId) {
    return;
  }

  if (!title) {
    cancelRename();
    return;
  }

  workspaceStore.state.tasks = workspaceStore.state.tasks.map((task) =>
    task.taskId === taskId
      ? {
          ...task,
          question: title,
        }
      : task,
  );

  cancelRename();
}

function deleteTask(taskId: string, event?: Event) {
  event?.stopPropagation();

  const remainingTasks = workspaceStore.state.tasks.filter((task) => task.taskId !== taskId);
  const nextEventsByTaskId = { ...workspaceStore.state.eventsByTaskId };
  delete nextEventsByTaskId[taskId];

  workspaceStore.state.tasks = remainingTasks;
  workspaceStore.state.eventsByTaskId = nextEventsByTaskId;

  if (workspaceStore.state.selectedTaskId === taskId) {
    workspaceStore.state.selectedTaskId = remainingTasks[0]?.taskId ?? null;
  }

  if (editingTaskId.value === taskId) {
    cancelRename();
  }

  closeTaskMenu();
}

function handleRenameKeydown(event: KeyboardEvent) {
  if (event.key === "Enter") {
    event.preventDefault();
    commitRename();
  }

  if (event.key === "Escape") {
    event.preventDefault();
    cancelRename();
  }
}

function isEditingTask(taskId: string) {
  return editingTaskId.value === taskId;
}

function isTaskMenuOpen(taskId: string) {
  return activeMenuTaskId.value === taskId;
}

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

function handleWindowClick() {
  closeTaskMenu();
}

onMounted(() => {
  window.addEventListener("click", handleWindowClick);
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleWindowClick);
});
</script>

<template>
  <AppShell>
    <div :class="['grid h-full min-h-0 gap-3 overflow-hidden', workbenchGridClass]">
      <section
        v-if="historyPanelExpanded"
        class="flex min-h-0 flex-col overflow-hidden rounded-[32px] border border-white/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.97),rgba(244,249,251,0.95))] shadow-[0_24px_60px_rgba(15,23,42,0.05)]"
      >
        <div class="border-b border-[#dde8ee] px-4 py-4">
          <div class="flex items-center justify-between gap-3">
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-[0.24em] text-[#16807d]">Recent Conversations</div>
              <h2 class="mt-1 text-lg font-semibold text-[#102038]">最近对话</h2>
            </div>
            <button
              type="button"
              class="rounded-full border border-[#d8e6ed] bg-white px-3 py-1.5 text-xs font-semibold text-[#5d7287] transition hover:bg-[#f7fafc]"
              @click="toggleHistoryPanel"
            >
              收起
            </button>
          </div>
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
                    'group relative w-full rounded-[24px] border px-4 py-4 text-left transition',
                    task.taskId === workspaceStore.state.selectedTaskId
                      ? 'border-[#bfe8e3] bg-[linear-gradient(135deg,rgba(232,250,247,0.98),rgba(242,248,255,0.98))] shadow-[0_14px_26px_rgba(15,139,141,0.08)]'
                      : 'border-[#dce8ee] bg-white/92 hover:border-[#cbdde6] hover:bg-white',
                  ]"
                  @click="workspaceStore.selectTask(task.taskId)"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="min-w-0 flex-1">
                      <div v-if="isEditingTask(task.taskId)" class="space-y-2" @click.stop>
                        <input
                          v-model="editingTitle"
                          class="h-10 w-full rounded-[14px] border border-[#c9dae3] bg-white px-3 text-sm text-[#102038] outline-none focus:border-[#6fcac0] focus:ring-2 focus:ring-[#e3f6f3]"
                          @blur="commitRename"
                          @keydown="handleRenameKeydown"
                        />
                      </div>
                      <p v-else class="line-clamp-2 text-sm font-semibold leading-6 text-[#102038]">{{ task.question }}</p>
                    </div>
                    <div class="relative flex items-center" @click.stop>
                      <div
                        :class="[
                          'transition-transform duration-200',
                          isTaskMenuOpen(task.taskId) ? '-translate-x-1.5' : 'group-hover:-translate-x-1.5',
                        ]"
                      >
                        <StatusBadge :label="taskStatusLabel(task)" :tone="taskStatusTone(task)" />
                      </div>
                      <button
                        type="button"
                        :class="[
                          'flex h-8 items-center justify-center overflow-hidden rounded-full transition-all duration-200',
                          isTaskMenuOpen(task.taskId)
                            ? 'ml-2 w-8 border border-[#d8e6ed] bg-white text-[#6c8196] opacity-100 shadow-[0_6px_18px_rgba(15,23,42,0.06)]'
                            : 'ml-0 w-0 border border-transparent bg-transparent text-transparent opacity-0 pointer-events-none group-hover:ml-2 group-hover:w-8 group-hover:border-[#d8e6ed] group-hover:bg-white group-hover:text-[#6c8196] group-hover:opacity-100 group-hover:pointer-events-auto group-hover:shadow-[0_6px_18px_rgba(15,23,42,0.06)]',
                        ]"
                        @click="toggleTaskMenu(task.taskId, $event)"
                      >
                        <svg viewBox="0 0 20 20" class="h-4 w-4 fill-current">
                          <circle cx="5" cy="10" r="1.4" />
                          <circle cx="10" cy="10" r="1.4" />
                          <circle cx="15" cy="10" r="1.4" />
                        </svg>
                      </button>

                      <div
                        v-if="isTaskMenuOpen(task.taskId)"
                        class="absolute right-0 top-10 z-20 min-w-[148px] rounded-[18px] border border-[#dbe7ed] bg-white p-2 shadow-[0_18px_40px_rgba(15,23,42,0.12)]"
                      >
                        <button
                          type="button"
                          class="flex w-full items-center gap-2 rounded-[12px] px-3 py-2 text-sm font-medium text-[#102038] transition hover:bg-[#f4f8fa]"
                          @click="startRename(task, $event)"
                        >
                          <svg viewBox="0 0 20 20" class="h-4 w-4 fill-none stroke-current stroke-[1.8]">
                            <path d="m13.8 3.2 3 3-8.7 8.7-3.7.7.7-3.7Z" />
                            <path d="M11.8 5.2 14.8 8.2" />
                          </svg>
                          <span>重命名</span>
                        </button>
                        <button
                          type="button"
                          class="mt-1 flex w-full items-center gap-2 rounded-[12px] px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-50"
                          @click="deleteTask(task.taskId, $event)"
                        >
                          <svg viewBox="0 0 20 20" class="h-4 w-4 fill-none stroke-current stroke-[1.8]">
                            <path d="M4.5 6.5h11" />
                            <path d="M7.5 6.5v8" />
                            <path d="M12.5 6.5v8" />
                            <path d="M6.5 6.5 7 4.5h6l.5 2" />
                            <path d="M6 6.5v9a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1v-9" />
                          </svg>
                          <span>删除对话</span>
                        </button>
                      </div>
                    </div>
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
              没有匹配到历史对话。可以直接在底部发起一个新问题。
            </div>
          </div>
        </div>
      </section>

      <section class="flex min-h-0 flex-col overflow-hidden rounded-[32px] border border-white/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.94),rgba(244,249,251,0.98))] shadow-[0_24px_60px_rgba(15,23,42,0.05)]">
        <div class="border-b border-[#dde8ee] px-5 py-3">
          <div class="grid grid-cols-[auto_1fr_auto] items-center gap-3">
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="flex h-9 w-9 items-center justify-center rounded-full border border-[#d8e6ed] bg-white text-[#4f6478] transition hover:bg-[#f7fafc]"
                @click="toggleHistoryPanel"
              >
                <svg viewBox="0 0 20 20" class="h-4 w-4 fill-none stroke-current stroke-[1.8]">
                  <rect x="2.5" y="3.5" width="15" height="13" rx="2" />
                  <path d="M7 3.5v13" />
                </svg>
              </button>
              <button
                type="button"
                class="flex h-9 w-9 items-center justify-center rounded-full border border-[#d8e6ed] bg-white text-[#4f6478] transition hover:bg-[#f7fafc] disabled:cursor-not-allowed disabled:opacity-50"
                @click="workspaceStore.resetDraft()"
              >
                <svg viewBox="0 0 20 20" class="h-4 w-4 fill-none stroke-current stroke-[1.8]">
                  <path d="M10 4v12" />
                  <path d="M4 10h12" />
                </svg>
              </button>
            </div>

            <div class="truncate text-center text-sm font-semibold text-[#102038]">数据分析工作台</div>
            <div />
          </div>
        </div>

        <div class="border-b border-[#e3edf1] px-6 py-4">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="truncate text-xl font-semibold text-[#102038]">{{ currentConversationTitle }}</div>
              <div class="mt-1 text-xs text-[#8194a7]">
                {{ selectedTask ? selectedTask.traceId : "输入问题后会自动创建一条新对话" }}
              </div>
            </div>

            <button
              v-if="false"
              type="button"
              class="rounded-full border border-[#d8e6ed] bg-white px-3 py-1.5 text-xs font-semibold text-[#5d7287] transition hover:bg-[#f7fafc]"
              @click="startRename(selectedTask)"
            >
              修改标题
            </button>
          </div>

          <div v-if="false" class="mt-3 flex flex-wrap gap-3">
            <input
              v-model="editingTitle"
              class="h-11 min-w-[280px] flex-1 rounded-[16px] border border-[#c9dae3] bg-white px-4 text-sm text-[#102038] outline-none focus:border-[#6fcac0] focus:ring-2 focus:ring-[#e3f6f3]"
              @keydown="handleRenameKeydown"
            />
            <button
              type="button"
              class="rounded-[16px] bg-[#16213a] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#111b31]"
              @click="commitRename"
            >
              保存
            </button>
            <button
              type="button"
              class="rounded-[16px] border border-[#d8e6ed] bg-white px-4 py-2.5 text-sm font-semibold text-[#4f6478] transition hover:bg-[#f7fafc]"
              @click="cancelRename"
            >
              取消
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
                  class="w-full"
                  :open="workspaceStore.state.sqlExpanded"
                  @toggle="workspaceStore.setSqlExpanded(($event.target as HTMLDetailsElement).open)"
                >
                  <summary class="flex cursor-pointer list-none items-center gap-3 rounded-[18px] px-1 py-1 text-sm text-[#5f7388]">
                    <span :class="['flex h-7 w-7 items-center justify-center rounded-full border text-[13px] font-semibold', processMarkerClasses()]">
                      {{ workspaceStore.state.sqlExpanded ? "-" : "✓" }}
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
            <div class="max-w-[680px] text-center">
              <h2 class="display-face text-[2.4rem] font-semibold leading-[1.08] text-[#102038]">数据分析工作台</h2>
              <p class="mt-4 text-base leading-8 text-[#627588]">
                直接在底部输入问题，系统会按消息流返回结论、图表和提示。
              </p>
            </div>
          </div>
        </div>

        <div class="bg-[linear-gradient(180deg,rgba(255,255,255,0.3),rgba(239,247,249,0.9))] px-4 pb-4 pt-2">
          <div class="mx-auto max-w-[980px]">
            <div class="relative">
              <textarea
                :value="workspaceStore.state.question"
                rows="3"
                class="w-full rounded-[26px] border border-[#d8e6ed] bg-[linear-gradient(180deg,#ffffff_0%,#f9fbfc_100%)] px-4 py-3 pr-16 text-sm leading-7 text-[#102038] shadow-[0_14px_32px_rgba(15,23,42,0.04)] outline-none transition placeholder:text-[#95a6b6] focus:border-[#6fcac0] focus:ring-4 focus:ring-[#e3f6f3]"
                placeholder="例如：分析过去 12 个月企业营收趋势，并输出区域排名与渠道占比。"
                @input="workspaceStore.setQuestion(($event.target as HTMLTextAreaElement).value)"
              />
              <button
                type="button"
                class="absolute bottom-3 right-3 flex h-10 w-10 items-center justify-center rounded-full bg-[#16213a] text-white transition hover:bg-[#111b31] disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="workspaceStore.state.submitting"
                @click="workspaceStore.submitTask()"
              >
                <svg viewBox="0 0 20 20" class="h-4 w-4 fill-none stroke-current stroke-[2]">
                  <path d="M10 15V5" />
                  <path d="m5.5 9.5 4.5-4.5 4.5 4.5" />
                </svg>
              </button>
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
