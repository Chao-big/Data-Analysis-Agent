<script setup lang="ts">
defineProps<{
  eyebrow: string;
  title: string;
  alternateLabel: string;
  alternateActionText: string;
  alternateActionTo: {
    path: string;
    query?: Record<string, string>;
  };
}>();
</script>

<template>
  <main class="min-h-screen bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.14),transparent_28%),linear-gradient(180deg,#eef3f8_0%,#e7edf5_100%)] px-4 py-6 sm:px-6 lg:px-8">
    <div class="mx-auto flex min-h-[calc(100vh-48px)] max-w-[1280px] items-center justify-center">
      <section class="grid w-full overflow-hidden rounded-[36px] border border-white/70 bg-white/80 shadow-[0_30px_90px_rgba(15,23,42,0.12)] backdrop-blur lg:grid-cols-[1.02fr_0.98fr]">
        <aside class="relative overflow-hidden bg-[linear-gradient(180deg,#08101f_0%,#111c33_56%,#0c1728_100%)] px-7 py-8 text-slate-100 sm:px-9 lg:px-12 lg:py-12">
          <div class="absolute inset-x-0 top-0 h-1 bg-[linear-gradient(90deg,#22d3ee,#60a5fa,#2563eb)]" />
          <div class="absolute -left-16 top-16 h-44 w-44 rounded-full bg-[radial-gradient(circle,rgba(34,211,238,0.18),transparent_72%)] blur-[2px]" />
          <div class="absolute right-[-34px] top-14 h-56 w-56 rounded-full bg-[radial-gradient(circle,rgba(96,165,250,0.20),transparent_72%)]" />
          <div class="absolute bottom-[-100px] left-12 h-64 w-64 rounded-full border border-white/10 bg-[radial-gradient(circle,rgba(255,255,255,0.08),rgba(255,255,255,0.02))] blur-[2px]" />

          <div class="relative flex h-full flex-col justify-between">
            <div class="flex items-center gap-3">
              <span class="flex h-12 w-12 items-center justify-center rounded-2xl border border-white/10 bg-white/10 text-white shadow-[0_12px_26px_rgba(15,23,42,0.22)]">
                <svg viewBox="0 0 24 24" class="h-5 w-5 fill-none stroke-current stroke-[1.8]">
                  <path d="M12 3 4.5 7v5.5c0 4.1 2.8 7.9 7.5 8.5 4.7-.6 7.5-4.4 7.5-8.5V7L12 3Z" />
                  <path d="M9 12.5 11 14.5 15.5 10" />
                </svg>
              </span>
              <div>
                <div class="display-face text-[1.35rem] font-semibold tracking-[-0.04em] text-white">数据分析工作台</div>
                <div class="mt-1 text-[11px] uppercase tracking-[0.3em] text-slate-400">secure access</div>
              </div>
            </div>

            <div class="relative mt-16 max-w-[480px] lg:mt-0">
              <div class="text-[11px] font-semibold uppercase tracking-[0.28em] text-cyan-200/80">{{ eyebrow }}</div>
              <h1 class="display-face mt-4 text-[2.8rem] font-semibold leading-[1.04] tracking-[-0.06em] text-white sm:text-[3.2rem]">
                {{ title }}
              </h1>
            </div>
          </div>
        </aside>

        <div class="flex items-center bg-white/95 px-6 py-8 sm:px-8 lg:px-12 lg:py-12">
          <div class="w-full">
            <slot />
            <div class="mt-8 flex items-center justify-between gap-4 border-t border-[#edf2fb] pt-5 text-sm text-[#6f7f94]">
              <span>{{ alternateLabel }}</span>
              <RouterLink
                :to="alternateActionTo"
                class="inline-flex items-center gap-2 rounded-full border border-[#d7e3fb] bg-white px-4 py-2 font-medium text-[#2563eb] transition hover:border-[#bfcef6] hover:bg-[#f6f9ff]"
              >
                {{ alternateActionText }}
                <span aria-hidden="true">→</span>
              </RouterLink>
            </div>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
