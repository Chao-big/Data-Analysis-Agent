<script setup lang="ts">
import AppShell from "../components/AppShell.vue";
import SectionCard from "../components/SectionCard.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { datasetTypeLabel, workspaceStore } from "../lib/workspace-store";

const mysqlFields = [
  { label: "数据集名称", value: "finance_revenue_fact" },
  { label: "主机", value: "10.12.3.18" },
  { label: "端口", value: "3306" },
  { label: "数据库名", value: "finance" },
  { label: "用户名", value: "readonly_finance" },
  { label: "表名", value: "revenue_fact, region_snapshot" },
];
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-8">
      <SectionCard title="数据集管理" description="统一管理 CSV / Excel / MySQL 数据集，展示 Schema 摘要、样例预览、权限范围与接入状态。">
        <template #action>
          <StatusBadge :label="`${workspaceStore.datasets.length} 个数据集`" tone="default" />
        </template>

        <div class="grid gap-4 xl:grid-cols-2">
          <div v-for="dataset in workspaceStore.datasets" :key="dataset.id" class="rounded-[24px] border border-slate-200 bg-slate-50/70 p-5">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div class="text-base font-semibold text-slate-900">{{ dataset.datasetName }}</div>
                <p class="mt-2 text-sm leading-7 text-slate-500">{{ dataset.description }}</p>
              </div>
              <div class="flex flex-wrap gap-2">
                <StatusBadge :label="datasetTypeLabel[dataset.datasetType]" tone="default" />
                <StatusBadge label="已授权" tone="succeeded" />
              </div>
            </div>

            <div class="mt-4 grid gap-3 sm:grid-cols-2">
              <div v-for="item in [
                ['source_location', dataset.sourceLocation],
                ['permission_scope', dataset.permissionScope],
                ['owner', dataset.owner],
                ['tenant_id', dataset.tenantId],
                ['created_at', dataset.createdAt],
                ['row_count', `${dataset.rowCount}`],
              ]" :key="item[0]" class="rounded-[18px] border border-slate-200 bg-white px-4 py-3">
                <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{{ item[0] }}</div>
                <div class="mt-2 text-sm text-slate-700">{{ item[1] }}</div>
              </div>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              <span v-for="field in dataset.schemaSummary" :key="field" class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs text-slate-500">
                {{ field }}
              </span>
            </div>

            <div class="mt-4 rounded-[20px] border border-slate-200 bg-white p-4">
              <div class="flex items-center justify-between gap-3">
                <div class="text-sm font-semibold text-slate-900">样例数据预览</div>
                <button class="text-xs font-semibold text-teal-700">查看完整样例</button>
              </div>
              <div class="mt-3 space-y-2 text-sm text-slate-600">
                <div v-for="(row, index) in dataset.sampleRows.slice(0, 2)" :key="index" class="rounded-2xl bg-slate-50 px-3 py-2">
                  {{ Object.entries(row).map(([key, value]) => `${key}: ${value}`).join(" / ") }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </SectionCard>

      <div class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <SectionCard title="上传 CSV / Excel" description="保留文件上传注册入口，强调名称、描述、格式提示、成功反馈与失败校验。">
          <template #action>
            <StatusBadge label="文件数据集" tone="default" />
          </template>

          <div class="rounded-[24px] border border-dashed border-teal-200 bg-teal-50/50 p-6">
            <div class="text-base font-semibold text-slate-900">拖拽上传文件</div>
            <p class="mt-2 text-sm leading-7 text-slate-500">
              支持 CSV / Excel。上传成功后自动生成 Schema 摘要与样例预览，并进入已授权数据集列表。
            </p>
            <div class="mt-5 grid gap-4">
              <label class="block">
                <span class="text-sm font-medium text-slate-700">数据集名称</span>
                <input value="school_exam_summary_2026" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none" />
              </label>
              <label class="block">
                <span class="text-sm font-medium text-slate-700">描述信息</span>
                <input value="学校考试汇总数据，用于分析班级和学科趋势。" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none" />
              </label>
              <label class="block">
                <span class="text-sm font-medium text-slate-700">文件格式提示</span>
                <input value="支持 .csv / .xlsx / .xls" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none" />
              </label>
            </div>
            <div class="mt-5 grid gap-3 sm:grid-cols-2">
              <button class="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700">查看格式说明</button>
              <button class="rounded-2xl bg-teal-600 px-4 py-3 text-sm font-semibold text-white">上传并注册</button>
            </div>
            <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              上传成功反馈：文件已通过校验，数据集已注册。
            </div>
            <div class="mt-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              失败校验提示：缺少表头、列名重复或格式不支持时，系统会阻止注册。
            </div>
          </div>
        </SectionCard>

        <SectionCard title="注册 MySQL 只读数据集" description="表单明确标注只读属性和字段校验，不暴露任何前端直连写入能力。">
          <template #action>
            <StatusBadge label="Read Only" tone="needs_review" />
          </template>

          <div class="grid gap-4 sm:grid-cols-2">
            <label v-for="field in mysqlFields" :key="field.label" class="block">
              <span class="text-sm font-medium text-slate-700">{{ field.label }}</span>
              <input :value="field.value" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none" />
            </label>
          </div>
          <div class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-7 text-amber-800">
            只读属性提示：系统仅接收只读凭据，后续分析仅允许生成和执行 SELECT 查询。
          </div>
          <div class="mt-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            表单校验错误提示：用户名不能为空，端口必须为数字，表名至少填写一个。
          </div>
          <div class="mt-5 grid gap-3 sm:grid-cols-2">
            <button class="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700">测试连接</button>
            <button class="rounded-2xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white">注册数据集</button>
          </div>
        </SectionCard>
      </div>
    </div>
  </AppShell>
</template>
