<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  className?: string;
}>();
</script>

<template>
  <section
    :class="[
      'rounded-[28px] border border-slate-200 bg-white/90 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]',
      className ?? '',
    ]"
  >
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 class="text-xl font-semibold text-slate-900">{{ title }}</h2>
        <p v-if="description" class="mt-2 max-w-3xl text-sm leading-7 text-slate-500">{{ description }}</p>
      </div>
      <slot name="action" />
    </div>
    <div class="mt-5">
      <slot />
    </div>
  </section>
</template>
