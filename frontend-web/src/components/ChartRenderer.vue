<script setup lang="ts">
import * as echarts from "echarts";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    option: Record<string, unknown>;
    height?: number;
  }>(),
  {
    height: 280,
  },
);

const chartElement = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

function renderChart() {
  if (!chartElement.value) {
    return;
  }

  if (!chart) {
    chart = echarts.init(chartElement.value, undefined, { renderer: "svg" });
  }

  chart.setOption(props.option, true);
}

function resizeChart() {
  chart?.resize();
}

watch(
  () => props.option,
  () => {
    renderChart();
  },
  { deep: true },
);

onMounted(() => {
  renderChart();
  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeChart);
  chart?.dispose();
  chart = null;
});
</script>

<template>
  <div ref="chartElement" class="w-full" :style="{ height: `${height}px` }" />
</template>
