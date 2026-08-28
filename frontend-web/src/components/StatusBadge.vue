<script setup lang="ts">
import type { ConnectionStatus, EventLevel, TaskStatus } from "../lib/types";

type BadgeTone = TaskStatus | ConnectionStatus | EventLevel | "default";

defineProps<{
  label: string;
  tone?: BadgeTone;
}>();

const toneClasses: Record<BadgeTone, string> = {
  default: "bg-slate-100 text-slate-700 border-slate-200",
  queued: "bg-slate-100 text-slate-700 border-slate-200",
  running: "bg-cyan-50 text-cyan-700 border-cyan-200",
  needs_review: "bg-amber-50 text-amber-700 border-amber-200",
  succeeded: "bg-emerald-50 text-emerald-700 border-emerald-200",
  failed: "bg-rose-50 text-rose-700 border-rose-200",
  idle: "bg-slate-100 text-slate-700 border-slate-200",
  connecting: "bg-slate-100 text-slate-700 border-slate-200",
  open: "bg-emerald-50 text-emerald-700 border-emerald-200",
  closed: "bg-amber-50 text-amber-700 border-amber-200",
  error: "bg-rose-50 text-rose-700 border-rose-200",
  info: "bg-sky-50 text-sky-700 border-sky-200",
  warning: "bg-amber-50 text-amber-700 border-amber-200",
  success: "bg-emerald-50 text-emerald-700 border-emerald-200",
};
</script>

<template>
  <span
    :class="[
      'inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em]',
      toneClasses[tone ?? 'default'],
    ]"
  >
    {{ label }}
  </span>
</template>
