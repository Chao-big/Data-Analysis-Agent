<script setup lang="ts">
import { computed, ref } from "vue";
import AppShell from "../components/AppShell.vue";
import SectionCard from "../components/SectionCard.vue";
import StatusBadge from "../components/StatusBadge.vue";
import TaskStream from "../components/TaskStream.vue";
import { workspaceStore } from "../lib/workspace-store";
import type { TaskStatus } from "../lib/types";

const filters = ["全部", "queued", "running", "needs_review", "succeeded", "failed"] as const;

const filterLabelMap: Record<(typeof filters)[number], string> = {
  全部: "全部",
  queued: "排队中",
  running: "执行中",
  needs_review: "待人工审核",
  succeeded: "已完成",
  failed: "已失败",
};

const activeFilter = ref<(typeof filters)[number]>("全部");
const selectedTaskId = ref(workspaceStore.state.tasks[0]?.taskId ?? "");

const filteredTasks = computed(() => {
  if (activeFilter.value === "全部") {
    return workspaceStore.state.tasks;
  }

  return workspaceStore.state.tasks.filter((task) => task.status === activeFilter.value);
});

const currentTask = computed(() => filteredTasks.value.find((task) => task.taskId === selectedTaskId.value) ?? filteredTasks.value[0]);

function changeFilter(filter: (typeof filters)[number]) {
  activeFilter.value = filter;
  const nextTasks =
    filter === "全部" ? workspaceStore.state.tasks : workspaceStore.state.tasks.filter((task) => task.status === filter);
  if (nextTasks.length) {
    selectedTaskId.value = nextTasks[0].taskId;
  }
}
</script>

<template>
  <AppShell>
    <div class="grid gap-6 pb-8 xl:grid-cols-[0.95fr_1.05fr]">
      <SectionCard title="任务历史" description="按状态筛选历史任务，查看问题、状态、Trace ID、耗时和结果摘要。">
        <div class="flex flex-wrap gap-3">
          <button
            v-for="filter in filters"
            :key="filter"
            type="button"
            :class="[
              'rounded-full border px-4 py-2 text-sm font-medium transition',
              filter === activeFilter ? 'border-teal-200 bg-teal-50 text-teal-700' : 'border-slate-200 bg-white text-slate-600',
            ]"
            @click="changeFilter(filter)"
          >
            {{ filterLabelMap[filter] }}
          </button>
        </div>

        <div class="mt-5 space-y-4">
          <button
            v-for="task in filteredTasks"
            :key="task.taskId"
            type="button"
            :class="[
              'w-full rounded-[24px] border p-4 text-left transition',
              task.taskId === currentTask?.taskId ? 'border-teal-200 bg-teal-50/70' : 'border-slate-200 bg-slate-50/70 hover:border-slate-300',
            ]"
            @click="selectedTaskId = task.taskId"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="text-base font-semibold text-slate-900">{{ task.question }}</div>
              <StatusBadge :label="filterLabelMap[task.status as TaskStatus]" :tone="task.status" />
            </div>
            <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-500">
              <span>{{ task.taskId }}</span>
              <span>{{ task.traceId }}</span>
              <span>{{ task.duration }}</span>
            </div>
            <p class="mt-3 text-sm leading-7 text-slate-500">{{ task.finalConclusion }}</p>
          </button>

          <div v-if="!filteredTasks.length" class="rounded-[24px] border border-dashed border-slate-300 bg-slate-50/80 p-6 text-sm text-slate-500">
            当前筛选条件下没有任务记录。
          </div>
        </div>
      </SectionCard>

      <div class="space-y-6">
        <SectionCard title="任务详情" description="右侧查看当前任务的关键状态、失败原因、警告信息和关联数据集。">
          <template #action>
            <StatusBadge v-if="currentTask" :label="filterLabelMap[currentTask.status]" :tone="currentTask.status" />
          </template>

          <div v-if="currentTask" class="space-y-4">
            <div v-for="item in [
              ['Task ID', currentTask.taskId],
              ['Trace ID', currentTask.traceId],
              ['状态', filterLabelMap[currentTask.status]],
              ['开始时间', currentTask.startedAt],
              ['完成时间', currentTask.finishedAt ?? '--'],
              ['耗时', currentTask.duration ?? '--'],
              ['关联数据集', currentTask.relatedDatasetNames.join('、')],
              ...(currentTask.failureReason ? [['失败原因', currentTask.failureReason]] : []),
            ]" :key="item[0]" class="rounded-[20px] border border-slate-200 bg-slate-50/80 px-4 py-3">
              <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{{ item[0] }}</div>
              <div class="mt-2 text-sm leading-7 text-slate-700">{{ item[1] }}</div>
            </div>
          </div>

          <div v-else class="rounded-[24px] border border-dashed border-slate-300 bg-slate-50/80 p-6 text-sm text-slate-500">
            当前没有可查看的任务。
          </div>
        </SectionCard>

        <TaskStream :events="currentTask ? workspaceStore.state.eventsByTaskId[currentTask.taskId] ?? [] : []" :connection-status="currentTask?.connectionStatus ?? 'idle'" />
      </div>
    </div>
  </AppShell>
</template>
