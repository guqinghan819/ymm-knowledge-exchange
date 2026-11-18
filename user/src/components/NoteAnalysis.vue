<template>
  <section ref="panelRef" class="analysis-panel">
    <header class="analysis-header">
      <div>
        <h2>笔记分析</h2>
        <p>按发布时间筛选当前账号笔记，查看互动表现与频道分布</p>
      </div>
      <div class="header-actions">
        <div class="period-switch">
          <button
            v-for="option in periodOptions"
            :key="option.key"
            :class="{ active: selectedPeriodKey === option.key }"
            :aria-pressed="selectedPeriodKey === option.key"
            @click="selectedPeriodKey = option.key"
          >
            {{ option.label }}
          </button>
        </div>
        <button class="ghost-btn" :disabled="isLoading" @click="fetchAnalysis"><AppIcon name="refresh" :size="15" />刷新数据</button>
      </div>
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
      <div class="period-meta">
        <span>{{ currentPeriod?.startDate || '-' }} 至 {{ currentPeriod?.endDate || '-' }}</span>
        <strong>{{ selectedPeriodLabel }}</strong>
      </div>

      <div class="rank-grid">
        <RankList title="点赞数最高 Top 10" metric-label="点赞" :items="currentPeriod?.likeTopNotes || []" metric-key="likeCount" />
        <RankList title="收藏数最高 Top 10" metric-label="收藏" :items="currentPeriod?.collectTopNotes || []" metric-key="collectCount" />
      </div>

      <div class="lower-grid">
        <RankList title="评论数最多 Top 5" metric-label="评论" :items="currentPeriod?.commentTopNotes || []" metric-key="commentCount" compact />
        <div class="word-card">
          <div class="card-title">
            <h3>高频频道词云</h3>
            <span>{{ (currentPeriod?.channelWordCloud || []).length }} 个频道</span>
          </div>
          <div v-if="(currentPeriod?.channelWordCloud || []).length" ref="wordChartRef" class="word-chart" role="img" :aria-label="`${selectedPeriodLabel}频道词云，文字大小反映频道权重`"></div>
          <div v-else class="empty-box">暂无频道数据</div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { getNoteAnalysis } from '@/api/analysis'
import { getFileUrl } from '@/utils/utils'
import AppIcon from '@/components/AppIcon.vue'
import { chartBase, chartTooltip, channelColor } from '@/utils/chartTheme'

const RankList = defineComponent({
  props: {
    title: { type: String, required: true },
    metricLabel: { type: String, required: true },
    metricKey: { type: String, required: true },
    items: { type: Array, default: () => [] },
    compact: { type: Boolean, default: false }
  },
  setup(props) {
    const metricValue = item => Number(item?.[props.metricKey] || 0)
    return () => h('div', { class: ['rank-card', props.compact ? 'compact' : ''] }, [
      h('div', { class: 'card-title' }, [
        h('h3', props.title),
        h('span', `${props.items.length} 条`)
      ]),
      props.items.length
        ? h('div', { class: 'rank-list' }, props.items.map((item, index) => h('div', { class: 'rank-item', key: item.noteId || index }, [
            h('span', { class: ['rank-index', index < 3 ? 'top' : ''] }, index + 1),
            h('div', { class: 'cover' }, [
              item.cover
                ? (item.type === 1
                    ? h('video', { src: getFileUrl(item.cover), muted: true, preload: 'metadata' })
                    : h('img', { src: getFileUrl(item.cover), alt: item.title || '笔记封面' }))
                : h('span', item.type === 1 ? '视频' : '图文')
            ]),
            h('div', { class: 'note-info' }, [
              h('strong', { title: item.title || '无标题' }, item.title || '无标题'),
              h('span', item.channelName || '未归类')
            ]),
            h('div', { class: 'metric' }, [
              h('strong', metricValue(item)),
              h('span', props.metricLabel)
            ])
          ])))
        : h('div', { class: 'empty-box' }, '暂无笔记数据')
    ])
  }
})

const periodOptions = [
  { key: 'last7Days', label: '近 7 天' },
  { key: 'last30Days', label: '近 30 天' }
]

const analysis = ref({})
const selectedPeriodKey = ref('last7Days')
const wordChartRef = ref(null)
const wordChart = shallowRef(null)
const panelRef = ref(null)
let resizeObserver
let disposed = false
const isLoading = ref(false)
const loadError = ref(false)
let echartsModule = null
let wordCloudLoaded = false

const currentPeriod = computed(() => analysis.value?.[selectedPeriodKey.value] || null)
const selectedPeriodLabel = computed(() => periodOptions.find(item => item.key === selectedPeriodKey.value)?.label || '')

async function fetchAnalysis() {
  wordChart.value?.dispose()
  wordChart.value = null
  isLoading.value = true
  loadError.value = false
  try {
    const res = await getNoteAnalysis()
    analysis.value = res?.data || {}
    isLoading.value = false
    await nextTick()
    await renderWordCloud()
  } catch (error) {
    loadError.value = true
    isLoading.value = false
    console.error('获取笔记分析失败:', error)
  }
}

async function ensureWordCloud() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  if (!wordCloudLoaded) {
    await import('echarts-wordcloud')
    wordCloudLoaded = true
  }
  return echartsModule
}

async function renderWordCloud() {
  const echarts = await ensureWordCloud()
  if (disposed) return
  const words = currentPeriod.value?.channelWordCloud || []
  if (!wordChartRef.value || words.length === 0) {
    wordChart.value?.dispose()
    wordChart.value = null
    return
  }
  if (!wordChart.value) {
    wordChart.value = echarts.init(wordChartRef.value, null, { renderer: 'svg' })
  }
  wordChart.value.setOption({
    ...chartBase(),
    tooltip: chartTooltip(),
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        width: '100%',
        height: '100%',
        sizeRange: [14, 42],
        rotationRange: [0, 0],
        gridSize: 12,
        layoutAnimation: false,
        textStyle: {
          fontFamily: 'Microsoft YaHei, PingFang SC, sans-serif',
          fontWeight: 500,
          color: params => channelColor(params.name)
        },
        data: words.map(item => ({
          name: item.word,
          value: Number(item.weight ?? item.count ?? 0)
        }))
      }
    ]
  }, true)
}

function resizeCharts() {
  wordChart.value?.resize()
}

watch(selectedPeriodKey, async () => {
  await nextTick()
  await renderWordCloud()
})

onMounted(() => {
  fetchAnalysis()
  resizeObserver = new ResizeObserver(resizeCharts)
  if (panelRef.value) resizeObserver.observe(panelRef.value)
})

onBeforeUnmount(() => {
  disposed = true
  resizeObserver?.disconnect()
  wordChart.value?.dispose()
})
</script>


<style scoped src="../assets/studio-analysis.css"></style>
<style scoped>
.rank-grid, .lower-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.rank-grid { margin-bottom: 20px; }
.word-card, :deep(.rank-card) { min-width: 0; padding: 21px; border: 1px solid #e9eef4; border-radius: 11px; }
.card-title, :deep(.card-title) { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 18px; }
.card-title h3, :deep(.card-title h3) { margin: 0; color: #3d526c; font-size: 14px; font-weight: 600; }
.card-title span, :deep(.card-title span) { color: #9ca8b7; font-size: 10px; white-space: nowrap; }
:deep(.rank-list) { display: flex; flex-direction: column; gap: 4px; }
:deep(.rank-item) { display: grid; grid-template-columns: 20px 44px minmax(0, 1fr) 50px; align-items: center; gap: 10px; min-height: 68px; border-bottom: 1px solid #f1f4f8; }
:deep(.rank-item:last-child) { border-bottom: 0; }
:deep(.rank-index) { color: #a0acba; font-family: 'Segoe UI', sans-serif; font-size: 13px; font-weight: 600; text-align: center; }
:deep(.rank-index.top) { color: #577ea9; }
:deep(.cover) { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; overflow: hidden; border-radius: 7px; color: #98a7b8; background: #f0f4f9; font-size: 10px; }
:deep(.cover img), :deep(.cover video) { width: 100%; height: 100%; object-fit: cover; }
:deep(.note-info) { min-width: 0; }
:deep(.note-info strong), :deep(.note-info span) { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
:deep(.note-info strong) { color: #42556f; font-size: 12px; font-weight: 500; margin-bottom: 4px; }
:deep(.note-info span) { color: #a0adbc; font-size: 10px; }
:deep(.metric) { text-align: right; }
:deep(.metric strong), :deep(.metric span) { display: block; }
:deep(.metric strong) { color: #476589; font-family: 'Segoe UI', sans-serif; font-weight: 600; font-size: 16px; }
:deep(.metric span) { color: #a0acbb; font-size: 9px; }
.word-chart { width: 100%; height: 300px; }
.empty-box, :deep(.empty-box) { display: flex; align-items: center; justify-content: center; min-height: 180px; border-radius: 8px; color: #9cabbc; background: #f8fafd; font-size: 12px; }
@media (max-width: 1080px) { .rank-grid, .lower-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .word-card, :deep(.rank-card) { padding: 16px 12px; } :deep(.rank-item) { grid-template-columns: 18px 40px minmax(0, 1fr) 40px; gap: 8px; } :deep(.cover) { width: 40px; height: 40px; } .word-chart { height: 250px; } }
</style>
