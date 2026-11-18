<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import logoSrc from '@/assets/logo.png'

const router = useRouter()
const storyRef = ref(null)
const storyProgress = ref(0)
const orbitReady = ref(false)
const viewport = reactive({ width: 1440, height: 900 })
let targetProgress = 0
let motionFrame = 0
let entryFrame = 0
let lastFrameTime = 0
let featureObserver = null
let reducedMotion = false

const showcaseItems = [
  { key: 'publish', label: '把经验整理成作品', title: '笔记发布', description: '支持图文与视频内容，将零散经验组织成清晰、完整、便于传播的知识笔记。', detail: '编辑、预览、频道与话题归档在同一条创作路径中完成。', routeLabel: '创作中心 / 图文发布', accent: '#2d5db1', imageUrl: '/pic/feature-preview.svg' },
  { key: 'rag-search', label: '让搜索给出有依据的答案', title: '搜索与 RAG 检索', description: '从站内知识中寻找相关内容，并通过引用来源帮助用户理解答案从何而来。', detail: '搜索结果、智能回答与参考笔记保持清晰关联。', routeLabel: '知识检索 / 引用溯源', accent: '#426b9f', imageUrl: '/pic/feature-preview.svg' },
  { key: 'publish-assistant', label: '从想法到初稿，只差一次对话', title: '笔记发布助手', description: '围绕主题梳理内容方向，辅助生成标题、正文结构与发布建议，让表达更加完整。', detail: '保留创作者的判断，把重复整理交给智能助手。', routeLabel: '智能创作 / 发布助手', accent: '#315a8d', imageUrl: '/pic/feature-preview.svg' },
  { key: 'ppt', label: '让知识自然成为演示', title: 'PPT 生成', description: '从主题与资料中提炼叙事结构，生成适合讲解与分享的演示内容。', detail: '先组织观点，再形成页面，让演示围绕内容而不是模板。', routeLabel: '内容工具 / 演示生成', accent: '#536c9f', imageUrl: '/pic/feature-preview.svg' },
  { key: 'polish', label: '让文字更准确，也更像你', title: 'AI 润色', description: '在不改变原意的前提下改善表达、结构和语气，让复杂内容更容易阅读。', detail: '可用于标题优化、段落调整与长文精炼。', routeLabel: '智能创作 / 文字润色', accent: '#264d7e', imageUrl: '/pic/feature-preview.svg' },
  { key: 'comments', label: '知识在回应中继续生长', title: '评论与交流', description: '围绕笔记展开有上下文的讨论，让补充、质疑和新观点沉淀在内容之下。', detail: '点赞、回复与多级讨论共同构成持续交流的现场。', routeLabel: '内容详情 / 评论互动', accent: '#385f91', imageUrl: '/pic/feature-preview.svg' },
]

const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, value))
const smoothstep = value => value * value * (3 - 2 * value)
const shrinkProgress = computed(() => smoothstep(clamp(storyProgress.value / 0.88)))
const heroVisible = computed(() => 1 - smoothstep(clamp(storyProgress.value / 0.62)))

const orbitStyle = computed(() => {
  const mobile = viewport.width <= 900
  const size = mobile ? Math.min(420, viewport.width * 0.82) : Math.min(570, viewport.width * 0.39)
  const initialX = mobile ? viewport.width * 0.5 : viewport.width * 0.75
  const initialY = mobile ? viewport.height * 0.7 : viewport.height * 0.54
  const targetX = mobile ? viewport.width - 53 : viewport.width * 0.5
  const targetY = mobile ? 106 : 112
  const progress = shrinkProgress.value
  const finalSize = mobile ? 72 : 76
  const scale = 1 + (finalSize / size - 1) * progress

  return {
    '--orbit-size': `${Math.round(size)}px`,
    '--orbit-left': `${Math.round(initialX)}px`,
    '--orbit-top': `${Math.round(initialY)}px`,
    '--orbit-shift-x': `${((targetX - initialX) * progress).toFixed(2)}px`,
    '--orbit-shift-y': `${((targetY - initialY) * progress).toFixed(2)}px`,
    '--orbit-scale': scale.toFixed(5),
    '--orbit-ring-opacity': (1 - smoothstep(clamp(progress * 1.08))).toFixed(3),
    '--orbit-core-opacity': (1 - smoothstep(clamp(progress * 1.2))).toFixed(3),
    '--orbit-mark-counter-scale': (1 + progress * 1.08).toFixed(4),
  }
})

const stageStyle = computed(() => ({
  '--hero-opacity': heroVisible.value.toFixed(3),
  '--hero-shift': `${(storyProgress.value * -24).toFixed(2)}px`,
}))

const readTargetProgress = () => {
  if (!storyRef.value) return 0
  const rect = storyRef.value.getBoundingClientRect()
  return clamp(-rect.top / Math.max(1, rect.height - window.innerHeight))
}

const animateProgress = timestamp => {
  const elapsed = lastFrameTime ? Math.min(48, timestamp - lastFrameTime) : 16
  lastFrameTime = timestamp
  const difference = targetProgress - storyProgress.value
  const follow = 1 - Math.exp(-elapsed * 0.013)
  storyProgress.value += difference * follow

  if (Math.abs(difference) > 0.00025) {
    motionFrame = window.requestAnimationFrame(animateProgress)
  } else {
    storyProgress.value = targetProgress
    motionFrame = 0
    lastFrameTime = 0
  }
}

const requestStoryUpdate = () => {
  targetProgress = readTargetProgress()
  if (reducedMotion) {
    storyProgress.value = targetProgress
    return
  }
  if (!motionFrame) motionFrame = window.requestAnimationFrame(animateProgress)
}

const updateViewport = () => {
  viewport.width = window.innerWidth
  viewport.height = window.innerHeight
  requestStoryUpdate()
}

const enterCommunity = () => router.push({ name: 'home', query: { login: '1' } })

onMounted(async () => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  viewport.width = window.innerWidth
  viewport.height = window.innerHeight
  targetProgress = readTargetProgress()
  storyProgress.value = targetProgress
  window.addEventListener('scroll', requestStoryUpdate, { passive: true })
  window.addEventListener('resize', updateViewport, { passive: true })

  if (reducedMotion) {
    orbitReady.value = true
  } else {
    entryFrame = window.requestAnimationFrame(() => {
      entryFrame = window.requestAnimationFrame(() => { orbitReady.value = true })
    })
  }

  await nextTick()
  const featureElements = document.querySelectorAll('[data-feature]')
  if ('IntersectionObserver' in window && !reducedMotion) {
    featureObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const shouldReveal = entry.isIntersecting && entry.intersectionRatio >= 0.16
        entry.target.classList.toggle('is-visible', shouldReveal)
      })
    }, { threshold: [0, 0.16], rootMargin: '0px 0px -8% 0px' })
    featureElements.forEach(element => featureObserver.observe(element))
  } else {
    featureElements.forEach(element => element.classList.add('is-visible'))
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', requestStoryUpdate)
  window.removeEventListener('resize', updateViewport)
  featureObserver?.disconnect()
  if (motionFrame) window.cancelAnimationFrame(motionFrame)
  if (entryFrame) window.cancelAnimationFrame(entryFrame)
})
</script>

<template>
  <main class="portal-page">
    <section ref="storyRef" class="scroll-story">
      <div class="story-sticky" :style="stageStyle">
        <header class="portal-header">
          <div class="portal-brand"><img :src="logoSrc" alt="雨绵绵" /><span>知识交流社区</span></div>
          <p>让经验被看见，让答案有出处</p>
        </header>

        <div class="hero-copy">
          <p class="hero-intro">每一次认真分享，都让知识抵达更远</p>
          <h1>让值得留下的知识，<br />被更多人看见。</h1>
          <p class="hero-description">雨绵绵连接真实经验、专业见解与好奇的人。发现有价值的内容，记录你的思考，也让每一次交流成为下一次探索的起点。</p>
          <button class="portal-login" type="button" @click="enterCommunity"><span>登录进入</span><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h13M14 7l5 5-5 5"></path></svg></button>
          <p class="hero-capabilities">内容创作　智能检索　可信交流</p>
        </div>

        <div class="knowledge-orbit" :style="orbitStyle" aria-hidden="true">
          <div class="orbit-surface" :class="{ ready: orbitReady }">
            <span class="orbit-ring ring-one"></span><span class="orbit-ring ring-two"></span><span class="orbit-ring ring-three"></span>
            <span class="orbit-axis axis-horizontal"></span><span class="orbit-axis axis-vertical"></span><span class="orbit-core-ring"></span>
            <span class="orbit-mark">知</span><span class="orbit-caption">知识在交流中生长</span>
            <span class="orbit-word word-one">经验</span><span class="orbit-word word-two">观点</span><span class="orbit-word word-three">方法</span>
          </div>
        </div>

        <div class="scroll-hint" :class="{ hidden: storyProgress > 0.08 }" aria-hidden="true"><span></span>向下滚动，了解雨绵绵</div>
        <p class="story-transition" :class="{ visible: storyProgress > 0.72 }">六种能力，沿着一次完整的知识旅程展开</p>
      </div>
    </section>

    <section class="feature-stream" aria-label="产品功能展示">
      <header class="feature-heading" data-feature>
        <p>从记录到交流</p>
        <h2>不是六张轮播图，<br />而是六段真实的使用路径。</h2>
      </header>

      <article
        v-for="(item, index) in showcaseItems"
        :key="item.key"
        class="feature-row"
        :class="{ reverse: index % 2 === 1 }"
        :style="{ '--item-accent': item.accent }"
        data-feature
      >
        <div class="feature-copy">
          <div class="feature-count"><span>{{ String(index + 1).padStart(2, '0') }}</span><i></i><span>{{ String(showcaseItems.length).padStart(2, '0') }}</span></div>
          <p class="feature-label">{{ item.label }}</p>
          <h3>{{ item.title }}</h3>
          <div class="description-rule"></div>
          <strong>{{ item.description }}</strong>
          <p class="feature-detail">{{ item.detail }}</p>
        </div>

        <figure class="showcase-screen">
          <figcaption class="screen-bar">
            <span class="window-dots"><i></i><i></i><i></i></span><span class="screen-route">{{ item.routeLabel }}</span><span class="screen-status">展示</span>
          </figcaption>
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="`${item.title}操作界面`" />
          <div v-else class="screen-placeholder">
            <div class="placeholder-sidebar"><span></span><span></span><span></span><span></span></div>
            <div class="placeholder-canvas">
              <div class="placeholder-heading"><span>{{ item.title }}</span><i></i></div>
              <div class="placeholder-hero-block"><span class="placeholder-sequence">{{ String(index + 1).padStart(2, '0') }}</span><div><i></i><i></i><i></i></div></div>
              <div class="placeholder-panels"><span></span><span></span><span></span></div><p>展示图等待管理端配置</p>
            </div>
          </div>
        </figure>
      </article>
    </section>

    <section class="portal-values" aria-label="平台价值">
      <div class="values-heading"><p>知识不是终点</p><h2>它在被分享、被理解、被回应时继续生长。</h2></div>
      <div class="values-list">
        <article><span>发现</span><p>从真实分享中获得启发，让搜索不只给出结果，也呈现经验。</p></article>
        <article><span>分享</span><p>用图文与视频记录所知，让个人经验成为可以传递的知识。</p></article>
        <article><span>交流</span><p>围绕内容展开讨论，在不同视角之间建立有价值的连接。</p></article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.portal-page { color: #17243b; background: #f7f9fc; }
.scroll-story { position: relative; height: 185vh; }
.story-sticky { position: sticky; top: 0; width: 100%; height: 100vh; min-height: 680px; overflow: hidden; isolation: isolate; background: linear-gradient(rgba(45,93,177,.035) 1px,transparent 1px),#f7f9fc; background-size: 100% 38px; }
.story-sticky::before,.story-sticky::after { content: ''; position: absolute; top: 0; bottom: 0; width: 1px; z-index: -1; background: #dce4ef; }.story-sticky::before { left: clamp(28px,5vw,84px); }.story-sticky::after { right: clamp(28px,5vw,84px); }
.portal-header { position: relative; z-index: 20; min-height: 92px; margin: 0 clamp(28px,5vw,84px); padding: 17px 0; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #dce4ef; background: rgba(247,249,252,.82); backdrop-filter: blur(14px); }
.portal-brand { display: flex; align-items: center; gap: 17px; }.portal-brand img { width: 130px; height: auto; display: block; }.portal-brand span { padding-left: 17px; border-left: 1px solid #cbd5e4; color: #31415c; font-family: var(--ymm-font-serif); font-size: 15px; font-weight: 700; }.portal-header>p { margin: 0; color: #758198; font-size: 13px; }
.hero-copy { position: absolute; top: 50%; left: clamp(28px,5vw,84px); z-index: 3; width: min(48vw,710px); opacity: var(--hero-opacity); transform: translateY(calc(-46% + var(--hero-shift))); will-change: opacity,transform; }
.hero-intro { margin: 0 0 24px; color: #2d5db1; font-size: 14px; font-weight: 700; }.hero-copy h1 { margin: 0; color: #102a54; font-family: var(--ymm-font-serif); font-size: clamp(48px,5.2vw,76px); line-height: 1.18; letter-spacing: -.045em; }.hero-description { max-width: 620px; margin: 28px 0 0; color: #637086; font-size: clamp(15px,1.15vw,17px); line-height: 1.95; }
.portal-login { width: 168px; height: 52px; margin-top: 34px; padding: 0 19px 0 23px; border: 1px solid #102a54; border-radius: 10px; display: inline-flex; align-items: center; justify-content: space-between; color: #fff; background: #102a54; box-shadow: 0 14px 34px rgba(16,42,84,.2); cursor: pointer; font-size: 15px; font-weight: 700; transition: transform .22s ease,background .22s ease,box-shadow .22s ease; }.portal-login svg { width: 19px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; transition: transform .22s ease; }.portal-login:hover { transform: translateY(-2px); background: #2d5db1; box-shadow: 0 18px 38px rgba(16,42,84,.26); }.portal-login:hover svg { transform: translateX(3px); }.portal-login:focus-visible { outline: 3px solid rgba(45,93,177,.3); outline-offset: 4px; }.hero-capabilities { margin: 17px 0 0; color: #8a95a7; font-size: 12px; }
.knowledge-orbit { position: absolute; left: var(--orbit-left); top: var(--orbit-top); z-index: 12; width: var(--orbit-size); height: var(--orbit-size); color: #fff; transform: translate(-50%,-50%) translate3d(var(--orbit-shift-x),var(--orbit-shift-y),0) scale(var(--orbit-scale)); transform-origin: center; will-change: transform; }
.orbit-surface { position: absolute; inset: 0; overflow: hidden; border: 1px solid #bfd0e5; border-radius: 50%; background: #2d5db1; box-shadow: 0 28px 70px rgba(32,67,124,.22); opacity: 0; transform: scale(.14); transform-origin: center; transition: opacity .28s ease,transform 1.25s cubic-bezier(.16,1,.3,1); will-change: transform; }.orbit-surface.ready { opacity: 1; transform: scale(1); }
.orbit-ring { position: absolute; left: 50%; top: 50%; border: 1px solid rgba(255,255,255,.24); border-radius: 50%; opacity: var(--orbit-ring-opacity); transform: translate(-50%,-50%); }.ring-one { width: 76%; height: 76%; animation: ring-grow .8s .1s cubic-bezier(.16,1,.3,1) both; }.ring-two { width: 52%; height: 52%; animation: ring-grow .8s .28s cubic-bezier(.16,1,.3,1) both; }.ring-three { width: 28%; height: 28%; animation: ring-grow .8s .46s cubic-bezier(.16,1,.3,1) both; }
.orbit-axis { position: absolute; background: rgba(255,255,255,.14); opacity: var(--orbit-ring-opacity); }.axis-horizontal { left: 8%; top: 50%; width: 84%; height: 1px; }.axis-vertical { top: 8%; left: 50%; width: 1px; height: 84%; }.orbit-core-ring { position: absolute; left: 50%; top: 50%; width: 25%; height: 25%; border: 1px solid rgba(255,255,255,.65); border-radius: 50%; opacity: var(--orbit-core-opacity); transform: translate(-50%,-50%); }.orbit-mark { position: absolute; left: 50%; top: 50%; font-family: var(--ymm-font-serif); font-size: clamp(76px,7.6vw,112px); line-height: 1; transform: translate(-50%,-50%) scale(var(--orbit-mark-counter-scale)); transform-origin: center; }.orbit-caption { position: absolute; left: 50%; top: 64%; font-size: 14px; font-weight: 600; opacity: var(--orbit-core-opacity); transform: translateX(-50%); white-space: nowrap; }.orbit-word { position: absolute; color: rgba(255,255,255,.78); font-size: 16px; font-weight: 600; letter-spacing: .04em; opacity: var(--orbit-ring-opacity); }.word-one { top: 21%; left: 28%; }.word-two { top: 34%; right: 17%; }.word-three { bottom: 19%; left: 26%; }
.scroll-hint { position: absolute; left: 50%; bottom: 23px; z-index: 14; display: flex; align-items: center; gap: 10px; color: #7f8b9f; font-size: 11px; opacity: 1; transform: translateX(-50%); transition: opacity .3s ease; }.scroll-hint.hidden { opacity: 0; }.scroll-hint span { position: relative; width: 17px; height: 27px; border: 1px solid #9aa7b9; border-radius: 10px; }.scroll-hint span::after { content: ''; position: absolute; left: 50%; top: 5px; width: 2px; height: 6px; border-radius: 2px; background: #2d5db1; transform: translateX(-50%); animation: scroll-dot 1.5s ease-in-out infinite; }
.story-transition { position: absolute; left: 50%; bottom: 16vh; margin: 0; color: #64738a; font-family: var(--ymm-font-serif); font-size: 17px; opacity: 0; transform: translate(-50%,18px); transition: opacity .42s ease,transform .55s cubic-bezier(.16,1,.3,1); white-space: nowrap; }.story-transition.visible { opacity: 1; transform: translate(-50%,0); }
.feature-stream { position: relative; padding: 90px clamp(28px,7vw,118px) 120px; overflow: hidden; background: #f7f9fc; }.feature-stream::before { content: ''; position: absolute; left: clamp(28px,5vw,84px); top: 0; bottom: 0; width: 1px; background: #dce4ef; }
.feature-heading { max-width: 900px; margin: 0 auto 110px; text-align: center; opacity: 0; transform: translateY(24px); transition: opacity .7s ease,transform .85s cubic-bezier(.16,1,.3,1); }.feature-heading.is-visible { opacity: 1; transform: none; }.feature-heading>p { margin: 0 0 16px; color: #2d5db1; font-size: 13px; font-weight: 700; }.feature-heading h2 { margin: 0; color: #102a54; font-family: var(--ymm-font-serif); font-size: clamp(38px,4.2vw,64px); font-weight: 600; line-height: 1.35; }
.feature-row { width: min(100%,1360px); min-height: min(920px,92vh); margin: 0 auto; display: grid; grid-template-columns: minmax(300px,.68fr) minmax(560px,1.32fr); align-items: center; gap: clamp(54px,7vw,118px); }.feature-row.reverse { grid-template-columns: minmax(560px,1.32fr) minmax(300px,.68fr); }.feature-row.reverse .feature-copy { order: 2; }.feature-row.reverse .showcase-screen { order: 1; }
.feature-copy { opacity: 0; transform: translateY(34px); transition: opacity .72s ease,transform .9s cubic-bezier(.16,1,.3,1); }.showcase-screen { width: 100%; margin: 0; overflow: hidden; border: 1px solid #cbd7e7; border-radius: 14px; background: #fff; box-shadow: 0 32px 80px rgba(25,52,93,.16),0 4px 14px rgba(25,52,93,.07); opacity: 0; transform: translateY(54px) scale(.985); transition: opacity .72s ease .08s,transform 1s cubic-bezier(.16,1,.3,1) .08s; }.feature-row.is-visible .feature-copy,.feature-row.is-visible .showcase-screen { opacity: 1; transform: none; }
.feature-count { display: flex; align-items: center; gap: 10px; margin-bottom: 32px; color: #8290a4; font-size: 11px; font-weight: 700; }.feature-count span:first-child { color: var(--item-accent); }.feature-count i { width: 44px; height: 1px; background: #cbd5e4; }.feature-label { margin: 0 0 13px; color: var(--item-accent); font-size: 13px; font-weight: 700; }.feature-copy h3 { margin: 0; color: #102a54; font-family: var(--ymm-font-serif); font-size: clamp(42px,4.3vw,66px); line-height: 1.12; }.description-rule { width: 58px; height: 3px; margin: 25px 0; background: var(--item-accent); }.feature-copy strong { display: block; max-width: 500px; color: #35445d; font-size: 17px; font-weight: 600; line-height: 1.8; }.feature-detail { max-width: 460px; margin: 15px 0 0; color: #7b8799; font-size: 13px; line-height: 1.75; }
.screen-bar { height: 47px; padding: 0 16px; display: grid; grid-template-columns: 80px 1fr 80px; align-items: center; border-bottom: 1px solid #e1e7f0; color: #778397; background: #fbfcfe; font-size: 11px; }.window-dots { display: flex; gap: 6px; }.window-dots i { width: 7px; height: 7px; border-radius: 50%; background: #c7d1df; }.window-dots i:first-child { background: var(--item-accent); }.screen-route { overflow: hidden; text-align: center; text-overflow: ellipsis; white-space: nowrap; }.screen-status { justify-self: end; color: var(--item-accent); font-weight: 700; }.showcase-screen>img { width: 100%; height: auto; display: block; object-fit: contain; object-position: top; }
.screen-placeholder { aspect-ratio: 16/10; display: grid; grid-template-columns: 18% 1fr; background: #f4f7fb; }.placeholder-sidebar { padding: 12% 17%; display: flex; flex-direction: column; gap: 18px; background: #102a54; }.placeholder-sidebar span { width: 100%; height: 5px; border-radius: 4px; background: rgba(255,255,255,.26); }.placeholder-sidebar span:first-child { width: 72%; background: rgba(255,255,255,.9); }.placeholder-canvas { position: relative; padding: 6% 7%; overflow: hidden; }.placeholder-heading { display: flex; align-items: center; justify-content: space-between; color: #263956; font-family: var(--ymm-font-serif); font-size: clamp(16px,1.8vw,27px); font-weight: 700; }.placeholder-heading i { width: 16%; height: 8px; border-radius: 5px; background: #dce5f1; }.placeholder-hero-block { height: 44%; margin-top: 7%; padding: 6%; display: flex; align-items: flex-end; justify-content: space-between; color: #fff; background: var(--item-accent); }.placeholder-sequence { font-family: var(--ymm-font-serif); font-size: clamp(44px,5vw,82px); opacity: .9; }.placeholder-hero-block>div { width: 38%; display: grid; gap: 8px; }.placeholder-hero-block i { height: 5px; display: block; background: rgba(255,255,255,.5); }.placeholder-hero-block i:nth-child(2) { width: 78%; }.placeholder-hero-block i:nth-child(3) { width: 54%; }.placeholder-panels { margin-top: 5%; display: grid; grid-template-columns: repeat(3,1fr); gap: 3%; }.placeholder-panels span { aspect-ratio: 2/1; border: 1px solid #d7e0ec; background: #fff; }.placeholder-canvas>p { position: absolute; right: 5%; bottom: 4%; margin: 0; color: #9aa5b5; font-size: 10px; }
.portal-values { min-height: 72vh; padding: 110px clamp(28px,8vw,130px); display: grid; grid-template-columns: 1fr 1.2fr; align-items: center; gap: 9vw; color: #fff; background: #102a54; }.values-heading p { margin: 0 0 16px; color: #9eb7dc; font-size: 13px; }.values-heading h2 { max-width: 620px; margin: 0; font-family: var(--ymm-font-serif); font-size: clamp(36px,4vw,62px); font-weight: 600; line-height: 1.35; }.values-list { border-top: 1px solid rgba(255,255,255,.2); }.values-list article { padding: 24px 0; display: grid; grid-template-columns: 100px 1fr; border-bottom: 1px solid rgba(255,255,255,.2); }.values-list span { font-family: var(--ymm-font-serif); font-size: 20px; }.values-list p { margin: 0; color: rgba(255,255,255,.66); font-size: 13px; line-height: 1.75; }
@keyframes ring-grow { from { opacity: 0; transform: translate(-50%,-50%) scale(.25); } to { opacity: var(--orbit-ring-opacity); transform: translate(-50%,-50%) scale(1); } } @keyframes scroll-dot { 0%,100% { opacity: .25; transform: translate(-50%,0); } 50% { opacity: 1; transform: translate(-50%,7px); } }
@media (max-width:1050px) { .feature-row { grid-template-columns: minmax(250px,.7fr) minmax(440px,1.3fr); gap: 38px; }.feature-row.reverse { grid-template-columns: minmax(440px,1.3fr) minmax(250px,.7fr); }.feature-copy strong { font-size: 15px; } }
@media (max-width:900px) {
  .scroll-story { height: 175vh; }.story-sticky { min-height: 650px; }.portal-header { min-height: 74px; margin: 0 18px; }.portal-brand { gap: 10px; }.portal-brand img { width: 96px; }.portal-brand span { padding-left: 10px; font-size: 12px; }.portal-header>p { display: none; }.story-sticky::before { left: 18px; }.story-sticky::after { right: 18px; }
  .hero-copy { top: 122px; left: 18px; width: calc(100% - 36px); transform: translateY(var(--hero-shift)); }.hero-intro { margin-bottom: 17px; font-size: 12px; }.hero-copy h1 { font-size: clamp(38px,10.8vw,58px); line-height: 1.2; }.hero-description { max-width: 560px; margin-top: 21px; font-size: 14px; line-height: 1.75; }.portal-login { width: 160px; height: 48px; margin-top: 24px; }.hero-capabilities { margin-top: 13px; }.story-transition { bottom: 12vh; font-size: 14px; }
  .feature-stream { padding: 72px 18px 90px; }.feature-stream::before { left: 18px; }.feature-heading { margin-bottom: 72px; padding: 0 18px; }.feature-row,.feature-row.reverse { min-height: auto; margin-bottom: 118px; grid-template-columns: 1fr; gap: 30px; }.feature-row.reverse .feature-copy,.feature-row.reverse .showcase-screen { order: initial; }.feature-copy { padding-left: 30px; }.feature-count { margin-bottom: 22px; }.feature-copy h3 { font-size: clamp(34px,9vw,48px); }.description-rule { margin: 18px 0; }.feature-copy strong { font-size: 14px; line-height: 1.7; }.showcase-screen { width: min(100%,680px); justify-self: center; }
  .portal-values { min-height: auto; padding: 80px 24px; grid-template-columns: 1fr; gap: 56px; }
}
@media (max-width:560px) {
  .story-sticky { min-height: 620px; }.hero-copy { top: 106px; }.hero-copy h1 { font-size: clamp(34px,10vw,48px); }.hero-description { display: -webkit-box; overflow: hidden; -webkit-line-clamp: 3; -webkit-box-orient: vertical; }.story-transition { width: calc(100% - 80px); text-align: center; white-space: normal; }
  .feature-stream { padding-top: 62px; }.feature-heading h2 { font-size: 34px; }.feature-row,.feature-row.reverse { margin-bottom: 96px; }.feature-copy { padding-left: 30px; }.feature-copy h3 { font-size: 32px; }.feature-detail { display: none; }
  .screen-bar { height: 38px; grid-template-columns: 54px 1fr 54px; padding: 0 10px; font-size: 9px; }.window-dots { gap: 4px; }.window-dots i { width: 5px; height: 5px; }.showcase-screen { border-radius: 10px; }.placeholder-canvas>p { display: none; }.values-heading h2 { font-size: 34px; }.values-list article { grid-template-columns: 72px 1fr; }
}
@media (prefers-reduced-motion:reduce) { .orbit-ring,.scroll-hint span::after { animation: none; }.orbit-surface,.feature-heading,.feature-copy,.showcase-screen,.story-transition { transition: none; } }
</style>
