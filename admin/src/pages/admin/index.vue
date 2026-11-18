<template>
  <div ref="dashboardRef" class="analysis-page">
    <header class="analysis-toolbar">
      <div>
        <h1>数据分析面板</h1>
        <p>{{ rangeText }}</p>
      </div>
      <div class="toolbar-actions">
        <el-radio-group v-model="periodKey" size="large">
          <el-radio-button label="last7Days">近 7 天</el-radio-button>
          <el-radio-button label="last30Days">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" :loading="loading" plain @click="loadDashboard">
          刷新
        </el-button>
      </div>
    </header>

    <el-skeleton v-if="loading" :rows="14" animated class="state-panel" />

    <el-result
      v-else-if="errorMessage"
      icon="error"
      title="分析数据加载失败"
      :sub-title="errorMessage"
      class="state-panel"
    >
      <template #extra>
        <el-button type="primary" :icon="Refresh" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-result>

    <main v-else class="analysis-grid">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>新增粉丝 Top 10</h2>
            <p>按当前仍有效的关注关系统计</p>
          </div>
          <span>{{ activePeriod.days }} 天</span>
        </div>
        <el-empty v-if="!activePeriod.topFans.length" description="暂无粉丝增长数据" />
        <div v-else ref="fansChartRef" class="rank-chart"></div>
      </section>

      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>发布笔记 Top 10</h2>
            <p>统计未删除笔记，含待审核与已下架</p>
          </div>
          <span>{{ activePeriod.days }} 天</span>
        </div>
        <el-empty v-if="!activePeriod.topPublishers.length" description="暂无发布数据" />
        <div v-else ref="publishersChartRef" class="rank-chart"></div>
      </section>

      <section class="panel liked-panel">
        <div class="panel-head">
          <div>
            <h2>点赞作品 Top 10</h2>
            <p>全站累计点赞，公开且正常作品</p>
          </div>
          <span>累计</span>
        </div>
        <el-empty v-if="!analysis.topLikedNotes.length" description="暂无点赞作品" />
        <ol v-else class="liked-list">
          <li v-for="(note, index) in analysis.topLikedNotes" :key="note.noteId">
            <strong>{{ index + 1 }}</strong>
            <div class="note-cover">
              <el-image v-if="note.cover" :src="note.cover" fit="cover">
                <template #error>
                  <div class="cover-fallback">
                    <el-icon><Document /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="cover-fallback">
                <el-icon><VideoPlay v-if="note.type === 1" /><Document v-else /></el-icon>
              </div>
            </div>
            <div class="note-main">
              <p>{{ note.title || '未命名作品' }}</p>
              <div>
                <el-avatar :size="22" :src="note.creatorAvatar">
                  {{ firstChar(note.creatorName) }}
                </el-avatar>
                <span>{{ note.creatorName || '未知用户' }}</span>
              </div>
            </div>
            <em>{{ formatNumber(note.likeCount) }}</em>
          </li>
        </ol>
      </section>

      <section class="panel word-panel">
        <div class="panel-head">
          <div>
            <h2>频道词云</h2>
            <p>同一笔记绑定多个频道时分别计入</p>
          </div>
          <span>{{ activePeriod.channelWordCloud.length }} 个频道</span>
        </div>
        <el-empty v-if="!activePeriod.channelWordCloud.length" description="暂无频道数据" />
        <div v-else ref="wordCloudRef" class="word-cloud"></div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { Document, Refresh, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDashboardAnalysis } from '@/api/admin/dashboard'

const dashboardRef = ref(null)
const fansChartRef = ref(null)
const publishersChartRef = ref(null)
const wordCloudRef = ref(null)
const periodKey = ref('last7Days')
const loading = ref(false)
const errorMessage = ref('')

const emptyPeriod = (days) => ({
  days,
  startDate: '',
  endDate: '',
  topFans: [],
  topPublishers: [],
  channelWordCloud: []
})

const analysis = ref({
  last7Days: emptyPeriod(7),
  last30Days: emptyPeriod(30),
  topLikedNotes: []
})

const activePeriod = computed(() => analysis.value[periodKey.value] || emptyPeriod(7))
const rangeText = computed(() => {
  const period = activePeriod.value
  if (!period.startDate || !period.endDate) return '正在等待统计范围'
  return `${period.startDate} 至 ${period.endDate}，包含今天`
})

let fansChart
let publishersChart
let wordCloudChart
let resizeObserver

const reduceMotion = typeof window !== 'undefined'
  && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function normalizePeriod(period, days) {
  return {
    days: Number(period?.days || days),
    startDate: period?.startDate || '',
    endDate: period?.endDate || '',
    topFans: period?.topFans || [],
    topPublishers: period?.topPublishers || [],
    channelWordCloud: period?.channelWordCloud || []
  }
}

function normalizeAnalysis(data) {
  return {
    last7Days: normalizePeriod(data?.last7Days, 7),
    last30Days: normalizePeriod(data?.last30Days, 30),
    topLikedNotes: data?.topLikedNotes || []
  }
}

function firstChar(value) {
  return value ? String(value).slice(0, 1) : '用'
}

function formatNumber(value) {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return number.toLocaleString('zh-CN')
}

function rankName(item, index) {
  return `${index + 1}. ${item.nickname || `用户 ${item.userId || '-'}`}`
}

function getChart(element, currentChart) {
  if (!element) return null
  return currentChart || echarts.init(element, null, { renderer: 'svg' })
}

function rankingOption(items, color, unit) {
  const names = items.map(rankName).reverse()
  const values = items.map((item) => Number(item.count || 0)).reverse()
  return {
    color: [color],
    animationDuration: reduceMotion ? 0 : 420,
    grid: { left: 120, right: 56, top: 12, bottom: 16 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const row = params?.[0]
        return row ? `${row.name}<br/>${row.value} ${unit}` : ''
      }
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#e4eaf3' } },
      axisLabel: { color: '#718097' }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#354158', width: 110, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 18,
      label: {
        show: true,
        position: 'right',
        color: '#354158',
        formatter: `{c} ${unit}`
      },
      itemStyle: {
        borderRadius: [0, 6, 6, 0]
      }
    }]
  }
}

function channelColor(name) {
  const palette = ['#2859a6', '#426b9f', '#2f8164', '#866a32', '#695a9f', '#b55463']
  const sum = String(name || '').split('').reduce((total, char) => total + char.charCodeAt(0), 0)
  return palette[sum % palette.length]
}

function renderRankCharts() {
  const period = activePeriod.value
  if (period.topFans.length) {
    fansChart = getChart(fansChartRef.value, fansChart)
    fansChart?.setOption(rankingOption(period.topFans, '#2859a6', '人'), true)
  }
  if (period.topPublishers.length) {
    publishersChart = getChart(publishersChartRef.value, publishersChart)
    publishersChart?.setOption(rankingOption(period.topPublishers, '#426b9f', '篇'), true)
  }
}

function renderWordCloud() {
  const words = activePeriod.value.channelWordCloud
  if (!words.length) return
  wordCloudChart = getChart(wordCloudRef.value, wordCloudChart)
  wordCloudChart?.setOption({
    tooltip: {
      formatter: (params) => `${params.name}<br/>${params.value} 篇笔记`
    },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      sizeRange: [16, 54],
      rotationRange: [0, 0],
      rotationStep: 10,
      gridSize: 10,
      drawOutOfBound: false,
      layoutAnimation: !reduceMotion,
      textStyle: {
        fontFamily: 'Microsoft YaHei, PingFang SC, Arial, sans-serif',
        fontWeight: 700,
        color: (params) => channelColor(params.name)
      },
      emphasis: {
        focus: 'self',
        textStyle: {
          textShadowBlur: 4,
          textShadowColor: 'rgba(31, 41, 55, 0.18)'
        }
      },
      data: words.map((item) => ({
        name: item.name || '未命名频道',
        value: Number(item.count || 0)
      }))
    }]
  }, true)
}

async function renderCharts() {
  await nextTick()
  renderRankCharts()
  renderWordCloud()
}

function resizeCharts() {
  fansChart?.resize()
  publishersChart?.resize()
  wordCloudChart?.resize()
}

function disposeCharts() {
  fansChart?.dispose()
  publishersChart?.dispose()
  wordCloudChart?.dispose()
  fansChart = null
  publishersChart = null
  wordCloudChart = null
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  disposeCharts()
  let shouldRender = false
  try {
    const response = await getDashboardAnalysis()
    if (response?.success === false) {
      throw new Error(response?.message || '接口返回失败')
    }
    analysis.value = normalizeAnalysis(response?.data || response)
    shouldRender = true
  } catch (error) {
    errorMessage.value = error?.message || '请稍后重试'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
  if (shouldRender) {
    await renderCharts()
  }
}

watch(periodKey, async () => {
  disposeCharts()
  await renderCharts()
})

onMounted(async () => {
  await loadDashboard()
  resizeObserver = new ResizeObserver(resizeCharts)
  if (dashboardRef.value) resizeObserver.observe(dashboardRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  disposeCharts()
})
</script>

<style scoped>
.analysis-page {
  min-height: calc(100vh - 92px);
  padding: 24px;
  color: var(--ymm-text);
  background:
    linear-gradient(90deg, rgba(40, 89, 166, 0.035) 1px, transparent 1px),
    linear-gradient(180deg, rgba(40, 89, 166, 0.03) 1px, transparent 1px),
    var(--ymm-canvas);
  background-size: 42px 42px;
}

.analysis-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  background: rgba(255, 255, 255, 0.9);
}

.analysis-toolbar h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.25;
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
}

.analysis-toolbar p,
.panel-head p {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ymm-muted);
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.state-panel {
  min-height: 460px;
  padding: 32px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  background: #fff;
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.panel {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  background: rgba(255, 255, 255, 0.94);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.3;
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
}

.panel-head > span {
  flex: 0 0 auto;
  padding: 5px 9px;
  border: 1px solid #c4d3e7;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
}

.rank-chart {
  width: 100%;
  height: 360px;
}

.liked-panel,
.word-panel {
  min-height: 458px;
}

.liked-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.liked-list li {
  display: grid;
  grid-template-columns: 32px 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 8px 10px;
  border: 1px solid var(--ymm-line-soft);
  border-radius: 9px;
  background: #fbfcfe;
}

.liked-list strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
  color: #fff;
  background: var(--ymm-ink);
}

.liked-list li:nth-child(1) strong {
  background: #d97706;
}

.liked-list li:nth-child(2) strong {
  background: var(--ymm-primary);
}

.liked-list li:nth-child(3) strong {
  background: var(--ymm-success);
}

.note-cover,
.note-cover :deep(.el-image) {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  overflow: hidden;
  background: #e9eef5;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--ymm-muted);
  background: #e9eef5;
}

.note-main {
  min-width: 0;
}

.note-main p {
  overflow: hidden;
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--ymm-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-main div {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--ymm-muted);
  font-size: 12px;
}

.note-main span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.liked-list em {
  min-width: 72px;
  text-align: right;
  font-style: normal;
  font-weight: 800;
  color: var(--ymm-primary);
}

.word-cloud {
  width: 100%;
  height: 374px;
}

:deep(.el-radio-button__inner) {
  letter-spacing: 0;
}

:deep(.el-button:focus-visible),
:deep(.el-radio-button__inner:focus-visible) {
  outline: 2px solid var(--ymm-primary);
  outline-offset: 2px;
}

@media (max-width: 1100px) {
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .analysis-page {
    padding: 14px;
  }

  .analysis-toolbar {
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .rank-chart {
    height: 320px;
  }

  .word-cloud {
    height: 330px;
  }

  .liked-list li {
    grid-template-columns: 28px 48px minmax(0, 1fr);
  }

  .note-cover,
  .note-cover :deep(.el-image) {
    width: 48px;
    height: 48px;
  }

  .liked-list em {
    grid-column: 3;
    min-width: 0;
    text-align: left;
  }
}
</style>
