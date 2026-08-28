<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { logoutSession } from "../lib/auth";
import { workspaceStore } from "../lib/workspace-store";

const route = useRoute();
const router = useRouter();

const navigation = [
  { href: "/", label: "智能问数", helper: "对话工作台", icon: "home" },
  { href: "/datasets", label: "数据源", helper: "上传与授权", icon: "dataset" },
  { href: "/history", label: "任务历史", helper: "结果追踪", icon: "history" },
  { href: "/profile", label: "个人中心", helper: "账号与风控", icon: "user" },
] as const;

const pageMeta: Record<string, { title: string; description: string }> = {
  "/": {
    title: "智能问数",
    description: "通过自然语言发起分析，并在同一条对话流里查看图表、结论和审计信息。",
  },
  "/datasets": {
    title: "数据源管理",
    description: "统一管理 CSV、Excel 和只读 MySQL 数据源。",
  },
  "/history": {
    title: "任务历史",
    description: "查看任务状态、执行结果、事件流和 Trace 信息。",
  },
  "/profile": {
    title: "个人中心",
    description: "查看当前账号、租户信息和安全策略。",
  },
};

const isWorkbench = computed(() => route.path === "/");
const meta = computed(() => pageMeta[route.path] ?? pageMeta["/"]);
const userMenuOpen = ref(false);
const userMenuRef = ref<HTMLElement | null>(null);

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value;
}

function closeUserMenu() {
  userMenuOpen.value = false;
}

function handleWindowClick(event: MouseEvent) {
  const target = event.target;
  if (!(target instanceof Node)) {
    return;
  }

  if (!userMenuRef.value?.contains(target)) {
    closeUserMenu();
  }
}

function goToProfile() {
  closeUserMenu();
  router.push("/profile");
}

async function handleLogout() {
  closeUserMenu();
  await logoutSession();
  await router.replace("/login");
}

watch(
  () => route.fullPath,
  () => {
    closeUserMenu();
  },
);

onMounted(() => {
  window.addEventListener("click", handleWindowClick);
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleWindowClick);
});
</script>

<template>
  <div class="h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,rgba(34,197,184,0.12),transparent_22%),radial-gradient(circle_at_bottom_right,rgba(14,116,144,0.1),transparent_26%),linear-gradient(180deg,#f6fbfc_0%,#edf4f6_54%,#f8fbfc_100%)]">
    <div class="flex h-full w-full gap-3 overflow-hidden p-0">
      <aside class="hidden min-h-0 w-[212px] shrink-0 flex-col rounded-[28px] border border-white/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.97),rgba(242,248,250,0.93))] p-3 shadow-[0_24px_60px_rgba(15,23,42,0.06)] lg:flex">
        <div class="rounded-[22px] border border-[#dce8ee] bg-[linear-gradient(145deg,#ffffff_0%,#edf8f7_100%)] px-4 py-4 text-center">
          <div class="whitespace-nowrap text-lg font-semibold text-[#102038]">数据分析工作台</div>
        </div>

        <nav class="mt-3 space-y-1.5">
          <RouterLink
            v-for="item in navigation"
            :key="item.href"
            :to="item.href"
            :class="[
              'flex items-center gap-3 rounded-[20px] border px-3 py-2.5 transition',
              route.path === item.href
                ? 'border-[#bfe8e3] bg-[linear-gradient(135deg,rgba(227,251,248,0.98),rgba(240,248,255,0.98))] shadow-[0_12px_24px_rgba(15,139,141,0.06)]'
                : 'border-transparent bg-white/45 hover:border-[#d7e5eb] hover:bg-white/90',
            ]"
          >
            <span
              :class="[
                'flex h-10 w-10 items-center justify-center rounded-2xl',
                route.path === item.href ? 'bg-white text-[#11827e]' : 'bg-[#eef4f7] text-[#70849a]',
              ]"
            >
              <svg v-if="item.icon === 'dataset'" viewBox="0 0 24 24" class="h-5 w-5 fill-none stroke-current stroke-[1.8]">
                <ellipse cx="12" cy="6" rx="6.5" ry="2.8" />
                <path d="M5.5 6v6c0 1.5 2.9 2.8 6.5 2.8s6.5-1.3 6.5-2.8V6" />
                <path d="M5.5 12v6c0 1.5 2.9 2.8 6.5 2.8s6.5-1.3 6.5-2.8v-6" />
              </svg>
              <svg v-else-if="item.icon === 'history'" viewBox="0 0 24 24" class="h-5 w-5 fill-none stroke-current stroke-[1.8]">
                <path d="M4 12a8 8 0 1 0 2.3-5.7" />
                <path d="M4 4v4h4" />
                <path d="M12 8v5l3 2" />
              </svg>
              <svg v-else-if="item.icon === 'user'" viewBox="0 0 24 24" class="h-5 w-5 fill-none stroke-current stroke-[1.8]">
                <circle cx="12" cy="8" r="3.2" />
                <path d="M5.5 18.5c1.6-2.8 4-4.2 6.5-4.2s4.9 1.4 6.5 4.2" />
              </svg>
              <svg v-else viewBox="0 0 24 24" class="h-5 w-5 fill-none stroke-current stroke-[1.8]">
                <path d="M4 10.5 12 4l8 6.5" />
                <path d="M6.5 9.5V20h11V9.5" />
                <path d="M10 20v-5h4v5" />
              </svg>
            </span>
            <span class="min-w-0">
              <span class="block text-sm font-semibold text-[#102038]">{{ item.label }}</span>
              <span class="block truncate text-xs text-[#7e91a5]">{{ item.helper }}</span>
            </span>
          </RouterLink>
        </nav>
      </aside>

      <div class="flex min-h-0 min-w-0 flex-1 flex-col gap-3 overflow-hidden">
        <header
          v-if="!isWorkbench"
          class="rounded-[28px] border border-white/80 bg-white/88 px-5 py-3.5 shadow-[0_18px_48px_rgba(15,23,42,0.05)] backdrop-blur"
        >
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-[0.28em] text-[#17827f]">Enterprise Analytics Workspace</div>
              <div class="display-face mt-1 text-[1.85rem] font-semibold leading-none text-[#102038]">{{ meta.title }}</div>
              <p class="mt-2 text-sm text-[#607489]">{{ meta.description }}</p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full border border-[#d7e6ed] bg-[#f7fafc] px-3 py-1 text-xs font-semibold text-[#53687f]">企业分析工作区</span>
              <span class="rounded-full border border-[#d7e6ed] bg-[#f7fafc] px-3 py-1 text-xs font-semibold text-[#53687f]">SSE 实时任务流</span>
              <span class="rounded-full border border-[#cceee7] bg-[#ebfaf7] px-3 py-1 text-xs font-semibold text-[#11827e]">只读 SQL 审计</span>
            </div>
          </div>
        </header>

        <main class="min-h-0 flex-1 overflow-hidden">
          <slot />
        </main>
      </div>

      <div ref="userMenuRef" class="fixed right-5 top-5 z-50">
        <button
          type="button"
          class="flex items-center gap-3 px-1 py-1 transition"
          @click.stop="toggleUserMenu"
        >
          <div class="flex h-11 w-11 items-center justify-center rounded-full bg-[linear-gradient(135deg,#0f8b8d,#34c7b8)] text-sm font-semibold text-white">
            {{ workspaceStore.currentUser.displayName.slice(0, 1) }}
          </div>
          <div class="min-w-0 text-left">
            <div class="max-w-[120px] truncate text-sm font-semibold leading-tight text-[#102038]">
              {{ workspaceStore.currentUser.displayName }}
            </div>
          </div>
          <svg
            viewBox="0 0 20 20"
            class="h-4 w-4 shrink-0 text-[#6b8196] transition"
            :class="userMenuOpen ? 'rotate-180' : ''"
          >
            <path d="M5.5 7.5 10 12l4.5-4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" />
          </svg>
        </button>

        <div
          v-if="userMenuOpen"
          class="mt-2 w-[220px] rounded-[22px] border border-white/80 bg-white/96 p-2 shadow-[0_20px_40px_rgba(15,23,42,0.12)] backdrop-blur"
        >
          <div class="border-b border-[#e7eef2] px-3 pb-3 pt-2">
            <div class="text-sm font-semibold text-[#102038]">{{ workspaceStore.currentUser.displayName }}</div>
            <div class="mt-1 text-xs text-[#7e91a5]">{{ workspaceStore.currentUser.username }}</div>
          </div>

          <div class="pt-2">
            <button
              type="button"
              class="flex w-full items-center justify-between rounded-[16px] px-3 py-2.5 text-left text-sm font-medium text-[#34495e] transition hover:bg-[#f4f8fa]"
              @click="goToProfile"
            >
              <span>个人中心</span>
              <svg viewBox="0 0 20 20" class="h-4 w-4 text-[#91a2b1]">
                <path d="M7.5 5.5 12 10l-4.5 4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" />
              </svg>
            </button>
            <button
              type="button"
              class="flex w-full items-center justify-between rounded-[16px] px-3 py-2.5 text-left text-sm font-medium text-[#34495e] transition hover:bg-[#fdf3f1] hover:text-[#b65446]"
              @click="handleLogout"
            >
              <span>退出登录</span>
              <svg viewBox="0 0 20 20" class="h-4 w-4 text-current">
                <path d="M8 5.5H6.5A1.5 1.5 0 0 0 5 7v6a1.5 1.5 0 0 0 1.5 1.5H8" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" />
                <path d="M10 7.5 13 10l-3 2.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" />
                <path d="M13 10H8.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
