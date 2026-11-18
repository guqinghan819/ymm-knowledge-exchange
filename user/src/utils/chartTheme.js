// SVG 图表与创作工作台共用的视觉规范。
export function chartBase() {
  return {
    animationDuration: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 350,
    textStyle: { fontFamily: 'Microsoft YaHei, PingFang SC, sans-serif' },
    aria: { enabled: true },
  }
}

export function chartTooltip() {
  return {
    confine: true,
    backgroundColor: '#ffffff',
    borderColor: '#e3eaf2',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: '#435870', fontSize: 12 },
    extraCssText: 'border-radius:8px;box-shadow:0 6px 24px rgba(36,55,77,.08)',
  }
}

export function channelColor(name) {
  const palette = ['#365f91', '#7192b8', '#52867e', '#8c9bb0', '#65799b', '#a98965']
  const hash = Array.from(String(name || '')).reduce((total, char) => total + char.charCodeAt(0), 0)
  return palette[hash % palette.length]
}
