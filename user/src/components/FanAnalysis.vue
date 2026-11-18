<template>
  <section ref="panelRef" class="analysis-panel">
    <header class="analysis-header">
      <div>
        <h2>粉丝分析</h2>
        <p>查看近 7 天与近 30 天新增粉丝走势</p>
      </div>
      <button class="ghost-btn" :disabled="isLoading" @click="fetchAnalysis"><AppIcon name="refresh" :size="15" />刷新数据</button>
    </header>

    <div v-if="isLoading" class="analysis-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="loadError" class="analysis-state error">
      <span>加载失败，请稍后重试</span>
      <button @click="fetchAnalysis">重试</button>
    </div>

    <template v-else>
      <div class="period-cards">
        <button
          v-for="option in periodOptions"
          :key="option.key"
          class="period-card"
          :class="{ active: selectedPeriodKey === option.key }"
          :aria-pressed="selectedPeriodKey === option.key"
          @click="selectedPeriodKey = option.key"
        >
          <span class="period-label"><AppIcon name="users" :size="16" />{{ option.label }}新增粉丝</span>
          <strong>{{ getPeriod(option.key)?.addedTotal ?? 0 }}</strong>
          <span class="period-range">{{ getPeriod(option.key)?.startDate || '-' }} 至 {{ getPeriod(option.key)?.endDate || '-' }}</span>
        </button>
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <h3>{{ selectedPeriodLabel }}新增粉丝</h3>
          <span>每日新增</span>
        </div>
        <div v-if="selectedPeriod?.dailyAdded?.length" ref="chartRef" class="fan-chart" role="img" :aria-label="`${selectedPeriodLabel}每日新增粉丝趋势，共新增 ${selectedPeriod?.addedTotal ?? 0} 人`"></div>
        <div v-else class="analysis-state"><AppIcon name="chart" :size="24" />该时段暂无粉丝数据</div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { getFanAnalysis } from '@/api/analysis'
import AppIcon from '@/components/AppIcon.vue'
import { chartBase, chartTooltip } from '@/utils/chartTheme'

const periodOptions = [
  { key: 'last7Days', label: '近 7 天' },
  { key: 'last30Days', label: '近 30 天' }
]

const analysis = ref({})
const selectedPeriodKey = ref('last7Days')
const chartRef = ref(null)
const chartInstance = shallowRef(null)
const panelRef = ref(null)
let resizeObserver
let disposed = false
const isLoading = ref(false)
const loadError = ref(false)
let echartsModule = null

const selectedPeriod = computed(() => getPeriod(selectedPeriodKey.value))
const selectedPeriodLabel = computed(() => periodOptions.find(item => item.key === selectedPeriodKey.value)?.label || '')

function getPeriod(key) {
  return analysis.value?.[key] || null
}

async function fetchAnalysis() {
  chartInstance.value?.dispose()
  chartInstance.value = null
  isLoading.value = true
  loadError.value = false
  try {
    const res = await getFanAnalysis()
    analysis.value = res?.data || {}
    isLoading.value = false
    await nextTick()
    await renderChart()
  } catch (error) {
    loadError.value = true
    isLoading.value = false
    console.error('获取粉丝分析失败:', error)
  }
}

async function ensureEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
}

async function renderChart() {
  const echarts = await ensureEcharts()
  if (disposed) return
  if (!chartRef.value || !selectedPeriod.value) {
    chartInstance.value?.dispose()
    chartInstance.value = null
    return
  }
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartRef.value, null, { renderer: 'svg' })
  }

  const dailyAdded = selectedPeriod.value.dailyAdded || []
  const dates = dailyAdded.map(item => item.date?.slice(5) || '')
  const counts = dailyAdded.map(item => Number(item.count || 0))

  chartInstance.value.setOption({
    ...chartBase(),
    color: ['#6d91bb', '#365f91'],
    grid: { left: 12, right: 16, top: 42, bottom: 16, containLabel: true },
    legend: { top: 0, right: 0, itemWidth: 12, itemHeight: 8, textStyle: { color: '#7d8a9c', fontSize: 11 } },
    tooltip: { ...chartTooltip(), trigger: 'axis', valueFormatter: value => `${value} 人` },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5ebf2' } },
      axisLabel: { color: '#8795a7', fontSize: 11, margin: 14, hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eaf0f5', type: 'dashed' } },
      axisLabel: { color: '#8795a7', fontSize: 11 }
    },
    series: [
      {
        name: '新增粉丝',
        type: 'bar',
        barMaxWidth: 22,
        data: counts,
        itemStyle: { borderRadius: [5, 5, 0, 0], opacity: 0.75 },
        emphasis: { itemStyle: { opacity: 1 } }
      },
      {
        name: '新增趋势',
        type: 'line',
        smooth: false,
        symbolSize: 5,
        lineStyle: { width: 2 },
        data: counts
      }
    ]
  }, true)
}

function resizeChart() {
  chartInstance.value?.resize()
}

watch(selectedPeriodKey, async () => {
  await nextTick()
  await renderChart()
})

onMounted(() => {
  fetchAnalysis()
  resizeObserver = new ResizeObserver(resizeChart)
  if (panelRef.value) resizeObserver.observe(panelRef.value)
})

onBeforeUnmount(() => {
  disposed = true
  resizeObserver?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<style scoped src="../assets/studio-analysis.css"></style>
