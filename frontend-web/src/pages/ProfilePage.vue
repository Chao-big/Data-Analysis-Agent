<script setup lang="ts">
import AppShell from "../components/AppShell.vue";
import SectionCard from "../components/SectionCard.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { workspaceStore } from "../lib/workspace-store";

const recentActivities = [
  "2026-08-27 13:01 创建无结果分析任务并查看 Trace ID。",
  "2026-08-27 11:15 确认高风险写操作拦截说明。",
  "2026-08-27 10:02 查看营收趋势任务图表与 SQL 详情。",
];
</script>

<template>
  <AppShell>
    <div class="grid gap-6 pb-8 xl:grid-cols-[0.9fr_1.1fr]">
      <SectionCard title="个人中心" description="展示当前登录账号、所属组织、角色信息与基础安全策略。">
        <div class="rounded-[28px] bg-[linear-gradient(135deg,rgba(15,139,141,0.10),rgba(56,189,248,0.08))] p-6">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div class="text-2xl font-semibold text-slate-900">{{ workspaceStore.currentUser.displayName }}</div>
              <p class="mt-2 text-sm text-slate-600">{{ workspaceStore.currentUser.role }}</p>
            </div>
            <StatusBadge label="已登录" tone="succeeded" />
          </div>
          <p class="mt-4 text-sm leading-7 text-slate-700">
            当前账号支持工作台分析、数据集查看、任务追踪和风险审核说明确认。
          </p>
        </div>

        <div class="mt-5 grid gap-4 sm:grid-cols-2">
          <div v-for="item in [
            ['账号', workspaceStore.currentUser.username],
            ['组织', workspaceStore.currentUser.organization],
            ['邮箱', workspaceStore.currentUser.email],
            ['手机号', workspaceStore.currentUser.phone],
            ['租户', workspaceStore.currentUser.tenantId],
            ['最近登录', workspaceStore.currentUser.lastLoginAt],
          ]" :key="item[0]" class="rounded-[20px] border border-slate-200 bg-slate-50/80 px-4 py-3">
            <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{{ item[0] }}</div>
            <div class="mt-2 text-sm text-slate-700">{{ item[1] }}</div>
          </div>
        </div>
      </SectionCard>

      <div class="space-y-6">
        <SectionCard title="安全与风控" description="高风险分析请求会要求管理员密码确认，但不会执行写 SQL。">
          <div class="rounded-[24px] border border-amber-200 bg-amber-50 px-5 py-4 text-sm leading-7 text-amber-800">
            {{ workspaceStore.currentUser.passwordPolicy }}
          </div>
          <div class="mt-4 grid gap-3">
            <div v-for="item in [
              ['只读 SQL 策略', '仅允许 SELECT，其余写操作一律拦截。'],
              ['审计追踪', '所有任务都记录 task_id、trace_id、dataset_ids、sql_text 和状态。'],
              ['人工审核', '命中高风险规则时进入 needs_review，并展示密码确认入口。'],
            ]" :key="item[0]" class="rounded-[20px] border border-slate-200 bg-slate-50/80 px-4 py-3">
              <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{{ item[0] }}</div>
              <div class="mt-2 text-sm text-slate-700">{{ item[1] }}</div>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="最近活动" description="保留用户近期的任务查看与审核操作记录。">
          <div class="space-y-3">
            <div v-for="item in recentActivities" :key="item" class="rounded-[20px] border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm text-slate-600">
              {{ item }}
            </div>
          </div>
        </SectionCard>
      </div>
    </div>
  </AppShell>
</template>
