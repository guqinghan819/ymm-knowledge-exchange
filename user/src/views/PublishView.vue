<template>
  <div class="studio">
    <header class="studio-header">
      <RouterLink to="/home" class="brand" aria-label="雨绵绵首页">
        <img :src="logoSrc" alt="雨绵绵" />
        <span class="brand-divider"></span>
        <span>创作中心</span>
      </RouterLink>
      <div class="header-actions">
        <RouterLink to="/home" class="back-home"><AppIcon name="home" :size="16" /> 返回社区</RouterLink>
        <div class="account-menu" @keydown.esc="showLogout = false" @focusout="closeAccountMenu">
          <button v-if="publishUserInfo.nickname" class="account-button" type="button" :aria-expanded="showLogout" aria-controls="account-actions" @click="showLogout = !showLogout">
            <img v-if="avatarUrl && !avatarFailed" :src="avatarUrl" alt="" @error="avatarFailed = true" />
            <span v-else class="avatar-fallback">{{ avatarLetter }}</span>
            <span class="account-name">{{ publishUserInfo.nickname }}</span>
            <AppIcon name="chevron" class="account-chevron" :size="13" />
          </button>
          <RouterLink v-else to="/home?login=1" class="back-home">登录账号</RouterLink>
          <Transition name="popover-motion">
            <div v-if="showLogout" id="account-actions" class="account-popover">
              <button type="button" @click="handleLogout"><AppIcon name="logout" :size="17" />退出登录</button>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <div class="studio-layout">
      <aside class="studio-sidebar">
        <div class="sidebar-heading"><AppIcon name="edit" :size="18" /><span>创作工作台</span></div>
        <div class="create-menu">
          <button class="create-button" type="button" :aria-expanded="isPublishMenuOpen" aria-controls="create-options" @click="isPublishMenuOpen = !isPublishMenuOpen">
            <AppIcon name="plus" :size="20" /><span>发布笔记</span><AppIcon name="chevron" class="create-chevron" :class="{ open: isPublishMenuOpen }" :size="14" />
          </button>
          <div v-if="isPublishMenuOpen" id="create-options" class="create-options">
            <button type="button" :class="{ active: currentTab === 'publish-image' }" @click="switchTab('publish-image')"><AppIcon name="image" :size="17" />上传图文</button>
            <button type="button" :class="{ active: currentTab === 'publish-video' }" @click="switchTab('publish-video')"><AppIcon name="video" :size="17" />上传视频</button>
          </div>
        </div>
        <div class="nav-caption">内容与数据</div>
        <nav class="studio-nav" aria-label="创作中心导航">
          <button v-for="item in navigation" :key="item.key" type="button" :class="{ active: currentTab === item.key }" :aria-current="currentTab === item.key ? 'page' : undefined" @click="switchTab(item.key)">
            <AppIcon :name="item.icon" :size="19" /><span>{{ item.label }}</span><span v-if="currentTab === item.key" class="nav-dot"></span>
          </button>
        </nav>
        <div class="sidebar-bottom">
          <AppIcon name="book" :size="20" />
          <div><strong>你的经验，值得被看见</strong><p>记录所学，分享所得。</p></div>
        </div>
      </aside>

      <main class="studio-main">
        <div class="page-heading">
          <div><div class="breadcrumb">创作中心 <AppIcon name="chevron" :size="12" /> {{ currentTabLabel }}</div><h1>{{ currentTabLabel }}</h1></div>
          <span class="today">{{ today }}</span>
        </div>

        <Transition name="studio-content" mode="out-in">
          <div :key="currentTab" class="studio-content">
            <template v-if="currentTab === 'home'">
              <section class="overview-panel" aria-label="账号概览">
                <div class="profile-row">
                  <div class="profile-identity">
                    <img v-if="avatarUrl && !avatarFailed" :src="avatarUrl" class="profile-avatar" alt="当前用户头像" @error="avatarFailed = true" />
                    <span v-else class="profile-avatar avatar-fallback">{{ avatarLetter }}</span>
                    <div class="profile-copy"><h2>{{ publishUserInfo.nickname || '欢迎来到创作中心' }}<span v-if="publishUserInfo.nickname">，欢迎回来</span></h2><p>{{ publishUserInfo.introduce || '在这里整理灵感，让知识成为你的作品。' }}</p></div>
                  </div>
                  <span v-if="publishUserInfo.ymmId" class="profile-id">账号 {{ publishUserInfo.ymmId }}</span>
                </div>
                <div v-if="profileError" class="profile-error" role="status">详细数据加载失败<button type="button" :disabled="profileLoading" @click="loadPublishUserInfo">重新加载</button></div>
                <div class="stats-row" :aria-busy="profileLoading">
                  <div v-for="stat in stats" :key="stat.label" class="stat">
                    <div class="stat-label"><AppIcon :name="stat.icon" :size="16" />{{ stat.label }}</div>
                    <strong>{{ profileLoading || profileError ? '—' : formatNumber(stat.value) }}</strong>
                    <span class="stat-description">{{ stat.description }}</span>
                  </div>
                </div>
              </section>

              <div class="workspace-grid">
                <div class="creation-column">
                  <section class="creation-section" aria-labelledby="creation-title">
                    <div class="section-heading"><div><h2 id="creation-title">开始新的创作</h2><p>选一种方式，分享你的知识与灵感。</p></div><AppIcon name="edit" :size="20" /></div>
                    <div class="creation-cards">
                      <button type="button" class="creation-card image-card" @click="switchTab('publish-image')">
                        <div class="creation-art" aria-hidden="true">
                          <svg viewBox="0 0 240 130" fill="none">
                            <rect x="72" y="18" width="112" height="98" rx="9" fill="#8facd3" transform="rotate(9 72 18)" />
                            <rect x="51" y="12" width="112" height="98" rx="9" fill="#fff" transform="rotate(-5 51 12)" />
                            <path d="m64 81 24-26 20 18 16-16 30 28" stroke="#6d93c4" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
                            <circle cx="132" cy="36" r="8" fill="#b4cbed" />
                            <path d="m69 94 74-6" stroke="#d0def0" stroke-width="4" stroke-linecap="round" />
                            <path d="M191 39v18m-9-9h18" stroke="#99b7df" stroke-width="2" stroke-linecap="round" />
                            <circle cx="37" cy="76" r="4" stroke="#99b7df" stroke-width="2" />
                          </svg>
                        </div>
                        <div class="creation-copy"><span class="creation-type"><AppIcon name="image" :size="20" />图文笔记</span><p>用图片和文字，把经验讲清楚。</p><span class="creation-format">JPG / PNG / WEBP</span></div>
                        <span class="creation-link">开始写笔记<AppIcon name="arrow" :size="18" /></span>
                      </button>
                      <button type="button" class="creation-card video-card" @click="switchTab('publish-video')">
                        <div class="creation-art" aria-hidden="true">
                          <svg viewBox="0 0 240 130" fill="none">
                            <rect x="59" y="18" width="132" height="90" rx="10" fill="#c2d7d8" transform="rotate(7 59 18)" />
                            <rect x="45" y="17" width="139" height="87" rx="10" fill="#fff" stroke="#d4e0e1" />
                            <rect x="54" y="26" width="121" height="63" rx="5" fill="#e4eeec" />
                            <circle cx="115" cy="57" r="21" fill="#547f80" />
                            <path d="m110 47 14 10-14 10V47Z" fill="#fff" />
                            <path d="M60 97h87" stroke="#d9e5e3" stroke-width="3" stroke-linecap="round" />
                            <path d="M60 97h35" stroke="#719b98" stroke-width="3" stroke-linecap="round" />
                            <path d="M29 35v14m-7-7h14" stroke="#99b9b5" stroke-width="2" stroke-linecap="round" />
                          </svg>
                        </div>
                        <div class="creation-copy"><span class="creation-type"><AppIcon name="video" :size="20" />视频笔记</span><p>让过程更直观，让分享更生动。</p><span class="creation-format">MP4 / MOV</span></div>
                        <span class="creation-link">上传视频<AppIcon name="arrow" :size="18" /></span>
                      </button>
                    </div>
                  </section>

                  <section class="management-shortcut">
                    <span class="shortcut-icon"><AppIcon name="note" :size="23" /></span><div><h3>每一篇创作，都在这里</h3><p>查看发布状态，管理和编辑你的笔记。</p></div>
                    <button type="button" @click="switchTab('note-management')">管理笔记<AppIcon name="arrow" :size="17" /></button>
                  </section>
                </div>

                <aside class="writing-guide">
                  <div class="guide-title"><span><AppIcon name="bulb" :size="19" />创作小贴士</span><span class="guide-tag">写作指南</span></div>
                  <h3>好内容，从清晰的表达开始</h3>
                  <ul>
                    <li><AppIcon name="check" :size="16" /><div><strong>标题说清主题</strong><p>让读者一眼知道能收获什么。</p></div></li>
                    <li><AppIcon name="check" :size="16" /><div><strong>经验配上实例</strong><p>用步骤、图片或案例补充细节。</p></div></li>
                    <li><AppIcon name="check" :size="16" /><div><strong>选择合适的频道</strong><p>让内容被感兴趣的读者发现。</p></div></li>
                  </ul>
                  <div class="community-note"><AppIcon name="shield" :size="20" /><div><strong>真诚分享，尊重原创</strong><p>引用请注明来源，共建友好的知识社区。</p></div></div>
                </aside>
              </div>
              <p class="workspace-footer">雨绵绵 · 让知识在分享中生长</p>
            </template>
            <NoteManagement v-else-if="currentTab === 'note-management'" />
            <FanAnalysis v-else-if="currentTab === 'fan-analysis'" />
            <NoteAnalysis v-else-if="currentTab === 'note-analysis'" />
            <section v-else class="publish-panel">
              <div class="publish-tabs" role="group" aria-label="选择笔记类型">
                <button v-for="item in publishTypes" :key="item.key" type="button" :class="{ active: currentTab === item.key }" :aria-pressed="currentTab === item.key" @click="switchTab(item.key)"><AppIcon :name="item.icon" :size="18" />{{ item.label }}</button>
              </div>
              <PublishVideo v-if="currentTab === 'publish-video'" @published="switchTab('note-management')" />
              <PublishImage v-else @published="switchTab('note-management')" />
            </section>
          </div>
        </Transition>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { removeToken } from '@/utils/auth'
import { getUserDetail } from '@/api/user'
import { getFileUrl } from '@/utils/utils'
import logoSrc from '@/assets/logo.png'
import AppIcon from '@/components/AppIcon.vue'
import NoteManagement from '@/components/NoteManagement.vue'
import PublishVideo from '@/components/PublishVideo.vue'
import PublishImage from '@/components/PublishImage.vue'
import FanAnalysis from '@/components/FanAnalysis.vue'
import NoteAnalysis from '@/components/NoteAnalysis.vue'

const userStore = useUserStore()
const userDetail = ref({})
const publishUserInfo = computed(() => ({ ...userStore.userInfo, ...userDetail.value }))
const profileLoading = ref(false)
const profileError = ref(false)
const avatarFailed = ref(false)
const avatarUrl = computed(() => getFileUrl(publishUserInfo.value.avatar))
const avatarLetter = computed(() => (publishUserInfo.value.nickname || '创').slice(0, 1))
const showLogout = ref(false)
const isPublishMenuOpen = ref(false)
const currentTab = ref('home')
const navigation = [
  { key: 'home', label: '工作台概览', icon: 'grid' },
  { key: 'note-management', label: '笔记管理', icon: 'note' },
  { key: 'fan-analysis', label: '粉丝分析', icon: 'users' },
  { key: 'note-analysis', label: '笔记分析', icon: 'chart' },
]
const publishTypes = [
  { key: 'publish-image', label: '上传图文', icon: 'image' },
  { key: 'publish-video', label: '上传视频', icon: 'video' },
]
const currentTabLabel = computed(() => [...navigation, ...publishTypes].find(item => item.key === currentTab.value)?.label || '工作台概览')
const today = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const stats = computed(() => [
  { label: '关注', icon: 'user', value: publishUserInfo.value.followingTotal, description: '连接感兴趣的创作者' },
  { label: '粉丝', icon: 'users', value: publishUserInfo.value.fansTotal, description: '与你一起成长的读者' },
  { label: '获赞', icon: 'heart', value: publishUserInfo.value.likeTotal, description: '每一份认可，都是动力' },
  { label: '收藏', icon: 'bookmark', value: publishUserInfo.value.collectTotal, description: '让有用的经验被留存' },
])
const formatNumber = value => Number(value ?? 0).toLocaleString('zh-CN')

async function loadPublishUserInfo() {
  const ymmId = userStore.userInfo?.ymmId
  if (!ymmId) return
  profileLoading.value = true
  profileError.value = false
  try {
    const res = await getUserDetail(ymmId)
    if (res?.success) userDetail.value = res.data || {}
  } catch (error) {
    profileError.value = true
    console.error('获取发布页用户信息失败:', error)
  } finally {
    profileLoading.value = false
  }
}
function switchTab(tab) {
  currentTab.value = tab
  isPublishMenuOpen.value = tab.startsWith('publish-')
}
function closeAccountMenu(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) showLogout.value = false
}
async function handleLogout() {
  try {
    await logout()
    removeToken()
    userStore.logout()
    localStorage.clear()
    ElMessage.success('已退出登录')
    window.location.href = '/'
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}
onMounted(loadPublishUserInfo)
</script>

<style scoped>
.studio { --studio-blue: #365f91; --studio-ink: #24374d; --studio-muted: #758498; --studio-line: #e5eaf0; min-height: 100vh; background: #f3f5f8; color: var(--studio-ink); font-family: "PingFang SC", "Microsoft YaHei", sans-serif; }
button, a { -webkit-tap-highlight-color: transparent; }
button { font: inherit; cursor: pointer; }
button:disabled { cursor: wait; opacity: .65; }
.studio-header { position: sticky; top: 0; z-index: 30; height: 76px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 0 32px; background: #fff; border-bottom: 1px solid var(--studio-line); }
.brand { display: flex; align-items: center; gap: 20px; color: var(--studio-ink); text-decoration: none; font-size: 17px; font-weight: 600; white-space: nowrap; }
.brand img { width: 100px; height: 55px; object-fit: contain; }
.brand-divider { width: 1px; height: 21px; background: #dde4ed; }
.header-actions { display: flex; align-items: center; gap: 30px; }
.back-home { display: flex; align-items: center; gap: 8px; text-decoration: none; color: #627187; font-size: 13px; }
.back-home:hover { color: var(--studio-blue); }
.account-menu { position: relative; }
.account-button { display: flex; align-items: center; gap: 10px; border: 0; background: transparent; color: var(--studio-ink); }
.account-button img, .account-button .avatar-fallback { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; font-size: 13px; }
.account-name { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.avatar-fallback { display: inline-flex; align-items: center; justify-content: center; background: #e7eef8; color: var(--studio-blue); font-weight: 600; }
.account-chevron { transform: rotate(90deg); color: #8c98a9; }
.account-popover { position: absolute; top: calc(100% + 12px); right: 0; padding: 6px; width: 148px; border: 1px solid var(--studio-line); border-radius: 10px; background: #fff; box-shadow: 0 8px 28px #22364a12; }
.account-popover button { display: flex; align-items: center; gap: 9px; width: 100%; padding: 10px; border: 0; border-radius: 6px; color: #536477; background: transparent; font-size: 13px; }
.account-popover button:hover { background: #f2f5fa; }
.studio-layout { display: grid; grid-template-columns: 222px minmax(0, 1fr); min-height: calc(100vh - 76px); }
.studio-sidebar { position: sticky; top: 76px; height: calc(100vh - 76px); display: flex; flex-direction: column; padding: 32px 20px 24px; border-right: 1px solid var(--studio-line); background: #fff; }
.sidebar-heading { display: flex; align-items: center; gap: 9px; padding: 0 10px; margin-bottom: 26px; font-weight: 600; font-size: 14px; }
.sidebar-heading > svg { color: #7792b3; }
.create-button { width: 100%; display: flex; align-items: center; gap: 10px; min-height: 46px; padding: 0 16px; border: 1px solid var(--studio-blue); border-radius: 9px; color: #fff; background: var(--studio-blue); font-size: 14px; font-weight: 600; box-shadow: 0 4px 9px #365f911a; transition: background .18s; }
.create-button:hover { background: #294e7a; }
.create-chevron { margin-left: auto; transition: transform .18s; }
.create-chevron.open { transform: rotate(90deg); }
.create-options { display: grid; gap: 3px; padding: 8px 0 0 12px; }
.create-options button { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border: 0; border-radius: 7px; text-align: left; color: #62748b; background: transparent; font-size: 13px; }
.create-options button:hover, .create-options button.active { color: var(--studio-blue); background: #f0f4fa; }
.nav-caption { margin: 32px 12px 12px; color: #8c98a7; font-size: 11px; }
.studio-nav { display: grid; gap: 8px; }
.studio-nav button { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 46px; padding: 10px 14px; border: 0; border-radius: 8px; background: transparent; color: #68778b; text-align: left; font-size: 14px; transition: background .18s, color .18s; }
.studio-nav button:hover { background: #f7f9fb; color: var(--studio-blue); }
.studio-nav button.active { background: #edf2f9; color: #315b8e; font-weight: 600; }
.nav-dot { width: 5px; height: 5px; border-radius: 50%; background: #5279a8; margin-left: auto; }
.sidebar-bottom { display: flex; align-items: flex-start; gap: 9px; padding: 24px 6px 0; margin-top: auto; color: #8a9aad; }
.sidebar-bottom strong { font-size: 11px; font-weight: 500; }
.sidebar-bottom p { margin: 4px 0 0; font-size: 10px; color: #9ba6b4; }
.studio-main { width: 100%; max-width: 1500px; min-width: 0; margin: 0 auto; padding: 30px 40px 24px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 24px; gap: 16px; }
.breadcrumb { display: flex; align-items: center; gap: 9px; color: #8b96a6; font-size: 11px; margin-bottom: 12px; }
.page-heading h1 { font-size: 26px; font-weight: 650; line-height: 1.3; letter-spacing: -.6px; }
.today { color: #8895a6; font-size: 12px; padding-bottom: 4px; white-space: nowrap; }
.overview-panel { padding: 27px 30px 25px; background: #fff; border: 1px solid var(--studio-line); border-radius: 14px; }
.profile-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding-bottom: 26px; }
.profile-identity { display: flex; align-items: center; gap: 16px; min-width: 0; }
.profile-avatar { width: 58px; height: 58px; flex: none; object-fit: cover; border-radius: 50%; font-size: 24px; border: 4px solid #f2f5f9; }
.profile-copy { min-width: 0; }
.profile-copy h2 { font-size: 18px; font-weight: 600; overflow-wrap: anywhere; }
.profile-copy h2 span { font-weight: 400; }
.profile-copy p { margin-top: 5px; color: var(--studio-muted); font-size: 12px; line-height: 1.8; overflow-wrap: anywhere; }
.profile-id { flex: none; color: #8d98a7; font-size: 11px; }
.stats-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding-top: 24px; border-top: 1px solid #eef1f5; }
.stat { padding-left: 30px; min-width: 0; }
.stat:first-child { padding-left: 0; }
.stat + .stat { border-left: 1px solid #edf0f4; }
.stat-label { display: flex; align-items: center; gap: 7px; color: #7c8b9d; font-size: 12px; }
.stat-label svg { color: #8e9eb2; }
.stat strong { display: block; margin: 8px 0 2px; font-family: "Segoe UI", "PingFang SC", sans-serif; font-size: 32px; font-weight: 600; line-height: 1.35; letter-spacing: -.8px; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.stat-description { font-size: 10px; color: #9aa5b2; }
.profile-error { display: flex; gap: 10px; padding: 0 0 12px; color: #ab6655; font-size: 12px; }
.profile-error button { border: 0; background: none; color: var(--studio-blue); }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 286px; gap: 28px; margin-top: 30px; align-items: start; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.section-heading h2 { font-size: 17px; font-weight: 600; }
.section-heading p { margin-top: 5px; font-size: 12px; color: #8995a5; }
.section-heading > svg { color: #99a8ba; }
.creation-cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.creation-card { position: relative; overflow: hidden; display: flex; flex-direction: column; min-width: 0; padding: 0; text-align: left; border: 1px solid #dce5f0; border-radius: 13px; color: var(--studio-ink); background: #fff; transition: border-color .2s, box-shadow .2s; }
.creation-card:hover { border-color: #91accd; box-shadow: 0 6px 20px #2845670b; }
.creation-art { display: grid; place-items: center; width: 100%; height: 145px; background: #e9f0fa; }
.creation-art svg { height: 128px; width: 232px; max-width: 100%; transition: transform .25s ease; }
.creation-card:hover .creation-art svg { transform: translateY(-3px); }
.video-card { border-color: #dce5e5; }
.video-card .creation-art { background: #ecf2f1; }
.creation-copy { padding: 20px 22px 16px; }
.creation-type { display: flex; align-items: center; gap: 9px; font-size: 17px; font-weight: 600; }
.creation-type svg { color: #5d80ad; }
.video-card .creation-type svg { color: #638983; }
.creation-copy p { margin: 9px 0 12px; color: #7c8999; font-size: 12px; }
.creation-format { font-family: "Segoe UI", sans-serif; font-size: 10px; color: #9aa5b4; }
.creation-link { display: flex; align-items: center; justify-content: space-between; margin: auto 22px 0; padding: 14px 0 17px; border-top: 1px solid #edf1f5; color: var(--studio-blue); font-size: 12px; font-weight: 600; }
.video-card .creation-link { color: #527e77; }
.management-shortcut { display: flex; align-items: center; gap: 14px; margin-top: 20px; padding: 21px 23px; border: 1px solid var(--studio-line); border-radius: 11px; background: #fff; }
.shortcut-icon { display: grid; place-items: center; width: 43px; height: 43px; flex: none; border-radius: 10px; color: #6d86a5; background: #f0f4f9; }
.management-shortcut h3 { font-size: 13px; font-weight: 600; }
.management-shortcut p { margin-top: 5px; font-size: 11px; color: #93a0ae; }
.management-shortcut button { display: flex; align-items: center; gap: 8px; margin-left: auto; padding: 8px 0 8px 10px; flex: none; border: 0; color: #5b779a; background: transparent; font-size: 12px; }
.writing-guide { margin-top: 4px; padding: 22px; border: 1px solid var(--studio-line); border-radius: 13px; background: #fff; }
.guide-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.guide-title > span:first-child { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 600; }
.guide-title svg { color: #7e94b0; }
.guide-tag { padding: 3px 7px; border-radius: 4px; color: #8d9aad; background: #f3f6fa; font-size: 9px; }
.writing-guide h3 { margin: 22px 0 20px; font-size: 14px; font-weight: 600; }
.writing-guide ul { display: grid; gap: 22px; padding: 0; list-style: none; }
.writing-guide li { display: flex; align-items: flex-start; gap: 9px; }
.writing-guide li > svg { margin-top: 2px; color: #819bbd; }
.writing-guide li strong { font-size: 12px; font-weight: 500; }
.writing-guide li p { margin-top: 5px; color: #91a0b0; font-size: 11px; line-height: 1.8; }
.community-note { display: flex; align-items: flex-start; gap: 9px; margin-top: 27px; padding-top: 20px; border-top: 1px solid #edf1f5; color: #8598af; }
.community-note strong { font-size: 11px; font-weight: 500; color: #70849e; }
.community-note p { margin-top: 5px; font-size: 10px; line-height: 1.8; }
.workspace-footer { margin-top: 28px; text-align: center; color: #a4adba; font-size: 10px; }
.publish-panel { background: #fff; border: 1px solid var(--studio-line); border-radius: 14px; padding: 12px 28px 28px; min-width: 0; }
.publish-tabs { display: flex; gap: 28px; margin-bottom: 24px; border-bottom: 1px solid var(--studio-line); }
.publish-tabs button { display: flex; align-items: center; gap: 8px; padding: 17px 4px; margin-bottom: -1px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #8290a1; font-size: 14px; }
.publish-tabs button.active { border-bottom-color: var(--studio-blue); color: var(--studio-blue); font-weight: 600; }
.studio-content { min-width: 0; }
.studio-content-enter-active, .studio-content-leave-active { transition: opacity .15s ease; }
.studio-content-enter-from, .studio-content-leave-to { opacity: 0; }
.studio-main :deep(.analysis-panel), .studio-main :deep(.note-management) { border: 1px solid var(--studio-line); border-radius: 14px; box-shadow: none; }
.studio-main :deep(.upload-box) { background: #f8fafc; border-color: #cddbeb; border-radius: 12px; }
.studio-main :deep(.upload-icon) { color: #6586ae; }
.studio-main :deep(.upload-tips) { color: #8a98a9; }
@media (min-width: 1700px) { .studio-main { padding-top: 40px; } .creation-art { height: 175px; } .creation-art svg { height: 145px; } }
@media (max-width: 1250px) { .studio-main { padding: 28px; } .workspace-grid { grid-template-columns: minmax(0, 1fr); gap: 22px; } .writing-guide { display: none; } .profile-id { display: none; } }
@media (max-width: 900px) { .studio-layout { grid-template-columns: 186px minmax(0, 1fr); } .studio-sidebar { padding: 26px 12px; } .studio-main { padding: 24px; } .overview-panel { padding: 22px; } .stat { padding-left: 16px; } .stat-description { display: none; } .creation-copy { padding: 18px 16px 14px; } .creation-link { margin-inline: 16px; } .management-shortcut { padding: 18px; } .management-shortcut p { max-width: 170px; } }
@media (max-width: 680px) {
  .studio-header { height: 64px; padding: 0 16px; gap: 10px; }
  .brand { gap: 12px; font-size: 14px; }.brand img { width: 78px; height: 44px; }.brand-divider { height: 17px; }
  .header-actions { gap: 16px; }.header-actions > .back-home { display: none; }.account-name, .account-chevron { display: none; }
  .studio-layout { display: block; }.studio-sidebar { position: static; height: auto; padding: 12px 16px; border-right: 0; border-bottom: 1px solid var(--studio-line); }
  .sidebar-heading, .nav-caption, .sidebar-bottom { display: none; }.create-menu { position: relative; }.create-button { min-height: 40px; justify-content: center; }.create-chevron { margin-left: 6px; }
  .create-options { grid-template-columns: 1fr 1fr; padding: 8px 0 0; }.create-options button { justify-content: center; background: #f6f8fb; }
  .studio-nav { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 4px; padding-top: 10px; }.studio-nav button { flex-direction: column; justify-content: center; gap: 5px; min-width: 0; min-height: 54px; padding: 8px 3px; font-size: 11px; white-space: nowrap; }.studio-nav button svg { width: 17px; height: 17px; }.nav-dot { display: none; }
  .studio-main { padding: 22px 16px; }.page-heading { margin-bottom: 18px; }.page-heading h1 { font-size: 23px; }.breadcrumb { display: none; }.today { font-size: 10px; }
  .overview-panel { padding: 20px; }.profile-row { padding-bottom: 20px; }.profile-avatar { width: 48px; height: 48px; font-size: 20px; }.profile-identity { gap: 12px; }.profile-copy h2 { font-size: 15px; }.profile-copy h2 span { display: none; }.profile-copy p { font-size: 11px; }
  .stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px 0; padding-top: 20px; }.stat:nth-child(3) { padding-left: 0; border-left: 0; }.stat strong { font-size: 28px; margin-top: 5px; }.stat-description { display: inline; font-size: 9px; }
  .workspace-grid { margin-top: 24px; }.section-heading h2 { font-size: 16px; }.creation-cards { gap: 12px; }.creation-art { height: 110px; }.creation-art svg { width: 180px; height: 100px; }.creation-copy { padding: 15px 13px 12px; }.creation-type { gap: 6px; font-size: 15px; }.creation-type svg { width: 17px; height: 17px; }.creation-copy p { font-size: 11px; min-height: 36px; }.creation-format { font-size: 9px; }.creation-link { margin-inline: 13px; font-size: 11px; }
  .management-shortcut { padding: 17px 14px; gap: 10px; }.shortcut-icon { width: 35px; height: 35px; }.management-shortcut h3 { font-size: 12px; }.management-shortcut p { font-size: 10px; max-width: 150px; }.management-shortcut button { gap: 4px; font-size: 11px; padding-left: 0; }.management-shortcut button svg { display: none; }
  .publish-panel { padding: 8px 14px 20px; }.publish-tabs { gap: 24px; }.studio-main :deep(.analysis-panel), .studio-main :deep(.note-management) { padding: 18px 14px; }
}
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition: none !important; } }
</style>
