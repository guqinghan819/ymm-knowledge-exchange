<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCreatorById } from '@/api/user'
import { getUserCount } from '@/api/count'
import { getNotesByCursor } from '@/api/note'
import { getFileUrl } from '@/utils/utils'

const route = useRoute()
const router = useRouter()

const profile = ref(null)
const counts = ref({})
const notes = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const errorText = ref('')
let profileRequestVersion = 0

const actorId = () => String(route.params.actorId || '')

const normalizeNote = (item) => ({
  ...item,
  noteCountResponse: item.noteCountResponse || {}
})

const applyNoteResponse = (res, append, expectedUserId) => {
  const rows = Array.isArray(res?.data)
    ? res.data
        .filter(item => String(item?.findNoteDetailResponse?.creatorId) === expectedUserId)
        .map(normalizeNote)
    : []
  notes.value = append ? notes.value.concat(rows) : rows
  cursor.value = res?.nextCursor ?? null
  hasNext.value = !!res?.hasNext
}

const loadProfile = async () => {
  const requestVersion = ++profileRequestVersion
  const id = actorId()
  if (!id) {
    errorText.value = '用户不存在'
    return
  }

  loading.value = true
  errorText.value = ''
  profile.value = null
  counts.value = {}
  notes.value = []
  cursor.value = null
  hasNext.value = false
  loadingMore.value = false
  try {
    const [profileRes, countRes, noteRes] = await Promise.all([
      getCreatorById(id),
      getUserCount(id),
      getNotesByCursor({ cursor: null, limit: 20, userId: id })
    ])
    if (requestVersion !== profileRequestVersion) return
    if (!profileRes?.success || !profileRes.data) {
      throw new Error(profileRes?.message || '用户不存在')
    }
    if (String(profileRes.data.id) !== id) {
      throw new Error('用户资料与页面地址不匹配')
    }
    profile.value = profileRes.data
    counts.value = String(countRes?.data?.userId) === id ? countRes.data : {}
    applyNoteResponse(noteRes, false, id)
  } catch (error) {
    if (requestVersion !== profileRequestVersion) return
    errorText.value = error?.message || '用户主页加载失败'
  } finally {
    if (requestVersion === profileRequestVersion) loading.value = false
  }
}

const loadMore = async () => {
  if (!hasNext.value || loadingMore.value) return
  const requestVersion = profileRequestVersion
  const id = actorId()
  loadingMore.value = true
  try {
    const res = await getNotesByCursor({
      cursor: cursor.value,
      limit: 20,
      userId: id
    })
    if (requestVersion === profileRequestVersion) applyNoteResponse(res, true, id)
  } finally {
    if (requestVersion === profileRequestVersion) loadingMore.value = false
  }
}

const openNote = (item) => {
  const id = item?.findNoteDetailResponse?.id
  if (id) router.push({ name: 'note-detail', params: { id: String(id) } })
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push({ name: 'home' })
}

onMounted(loadProfile)
watch(() => route.params.actorId, loadProfile)
</script>

<template>
  <main class="profile-page">
    <div class="page-shell">
      <button type="button" class="back-button" @click="goBack">← 返回</button>

      <div v-if="loading" class="page-state">正在加载用户主页...</div>
      <div v-else-if="errorText" class="page-state error-state">
        <p>{{ errorText }}</p>
        <button type="button" @click="loadProfile">重新加载</button>
      </div>

      <template v-else-if="profile">
        <section class="profile-hero">
          <div class="hero-glow"></div>
          <div class="profile-avatar-wrap">
            <img
              v-if="profile.avatar"
              class="profile-avatar"
              :src="getFileUrl(profile.avatar)"
              :alt="profile.nickname" />
            <span v-else class="profile-avatar avatar-fallback">
              {{ profile.nickname?.slice(0, 1) || '用' }}
            </span>
          </div>
          <div class="profile-copy">
            <p class="profile-label">用户主页</p>
            <h1>{{ profile.nickname || '用户' }}</h1>
            <p class="profile-intro">{{ profile.introduce || '这个人还没有填写简介' }}</p>
            <div class="profile-stats">
              <span><strong>{{ counts.followingTotal || 0 }}</strong> 关注</span>
              <span><strong>{{ counts.fansTotal || 0 }}</strong> 粉丝</span>
              <span><strong>{{ Number(counts.likeTotal || 0) + Number(counts.collectTotal || 0) }}</strong> 获赞与收藏</span>
            </div>
          </div>
        </section>

        <section class="works-section">
          <header class="works-header">
            <div>
              <p>公开内容</p>
              <h2>TA 的笔记</h2>
            </div>
            <span>{{ counts.noteTotal || notes.length }} 篇</span>
          </header>

          <div v-if="notes.length" class="note-grid">
            <article
              v-for="item in notes"
              :key="item.findNoteDetailResponse.id"
              class="note-card"
              tabindex="0"
              @click="openNote(item)"
              @keydown.enter="openNote(item)">
              <div class="note-cover">
                <video
                  v-if="item.findNoteDetailResponse.type === 1 && item.findNoteDetailResponse.videoUri"
                  :src="item.findNoteDetailResponse.videoUri"
                  muted
                  preload="metadata"></video>
                <img
                  v-else-if="item.findNoteDetailResponse.imgUris?.[0]"
                  :src="getFileUrl(item.findNoteDetailResponse.imgUris[0])"
                  :alt="item.findNoteDetailResponse.title"
                  loading="lazy" />
                <span v-else>暂无封面</span>
              </div>
              <div class="note-copy">
                <h3>{{ item.findNoteDetailResponse.title || '无标题' }}</h3>
                <div class="note-meta">
                  <span aria-label="点赞数"><AppIcon name="heart" :size="14" /> {{ item.noteCountResponse.likeTotal || 0 }}</span>
                  <span aria-label="收藏数"><AppIcon name="bookmark" :size="14" /> {{ item.noteCountResponse.collectTotal || 0 }}</span>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="empty-notes">TA 还没有发布公开笔记</div>

          <button
            v-if="hasNext"
            type="button"
            class="load-more"
            :disabled="loadingMore"
            @click="loadMore">
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </button>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 28px;
  color: #172435;
  background: #f4f7fb;
}

.page-shell { max-width: 1080px; margin: 0 auto; }
.back-button {
  margin-bottom: 18px;
  padding: 8px 0;
  color: #506176;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
}

.page-state {
  display: grid;
  min-height: 60vh;
  place-items: center;
  color: #718096;
}
.error-state { align-content: center; gap: 12px; }
.error-state button, .load-more {
  padding: 10px 20px;
  color: #2b5fa5;
  border: 1px solid #cbd9e8;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}

.profile-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 245px;
  gap: 30px;
  overflow: hidden;
  padding: 38px 44px;
  color: #fff;
  border-radius: 28px;
  background: linear-gradient(135deg, #132d4e, #285b96 58%, #65a9c8);
  box-shadow: 0 24px 55px rgba(30, 69, 113, 0.2);
}
.hero-glow {
  position: absolute;
  top: -130px;
  right: -70px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
}
.profile-avatar-wrap { position: relative; z-index: 1; flex: none; }
.profile-avatar {
  display: grid;
  width: 112px;
  height: 112px;
  place-items: center;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
  background: #eef4fa;
  box-shadow: 0 14px 35px rgba(10, 30, 55, 0.3);
}
.avatar-fallback { color: #285b96; font-size: 38px; font-weight: 700; }
.profile-copy { position: relative; z-index: 1; min-width: 0; }
.profile-label { margin: 0 0 6px; color: rgba(255,255,255,.68); font-size: 12px; letter-spacing: .18em; }
.profile-copy h1 { margin: 0; font-size: 32px; }
.profile-intro { max-width: 650px; margin: 10px 0 20px; color: rgba(255,255,255,.78); line-height: 1.65; }
.profile-stats { display: flex; flex-wrap: wrap; gap: 24px; color: rgba(255,255,255,.76); font-size: 14px; }
.profile-stats strong { margin-right: 4px; color: #fff; font-size: 18px; }

.works-section {
  margin-top: 24px;
  padding: 30px;
  border: 1px solid #e5ebf2;
  border-radius: 24px;
  background: #fff;
}
.works-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 22px; }
.works-header p { margin: 0 0 4px; color: #91a0b2; font-size: 12px; }
.works-header h2 { margin: 0; font-size: 23px; }
.works-header > span { color: #8290a2; font-size: 14px; }
.note-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; }
.note-card {
  overflow: hidden;
  border: 1px solid #e7ecf2;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease;
}
.note-card:hover { transform: translateY(-3px); box-shadow: 0 14px 28px rgba(31, 57, 86, .12); }
.note-card:focus-visible { outline: 3px solid rgba(43, 95, 165, .22); outline-offset: 3px; }
.note-cover { display: grid; height: 210px; place-items: center; overflow: hidden; color: #9aa6b4; background: #edf2f7; }
.note-cover img, .note-cover video { width: 100%; height: 100%; object-fit: cover; }
.note-copy { padding: 14px; }
.note-copy h3 { min-height: 42px; margin: 0; overflow: hidden; font-size: 15px; line-height: 1.4; }
.note-meta { display: flex; gap: 14px; margin-top: 11px; color: #8793a3; font-size: 12px; }
.empty-notes { display: grid; min-height: 220px; place-items: center; color: #98a3b2; }
.load-more { display: block; margin: 24px auto 0; }
.load-more:disabled { opacity: .55; cursor: not-allowed; }

@media (max-width: 900px) {
  .note-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .profile-page { padding: 16px; }
  .profile-hero { align-items: flex-start; flex-direction: column; padding: 28px 24px; }
  .profile-avatar { width: 84px; height: 84px; }
  .profile-copy h1 { font-size: 26px; }
  .works-section { padding: 20px 16px; }
  .note-grid { gap: 12px; }
  .note-cover { height: 160px; }
}

/* Shared user-facing visual language */
.profile-page {
  min-height: 100vh;
  padding: 30px 24px 56px;
  color: var(--ymm-text);
  background:
    linear-gradient(rgba(40,89,166,.035) 1px, transparent 1px),
    var(--ymm-canvas);
  background-size: 100% 36px;
}
.page-shell { max-width: 1180px; }
.back-button {
  margin-bottom: 18px;
  padding: 8px 12px;
  border: 1px solid var(--ymm-line);
  border-radius: 9px;
  color: var(--ymm-primary);
  background: #fff;
  font-weight: 650;
}
.back-button:hover { background: var(--ymm-primary-soft); }
.page-state,
.works-section {
  border-color: var(--ymm-line);
  border-radius: var(--ymm-radius-lg);
  background: #fff;
}
.error-state button,
.load-more {
  border-radius: 9px;
  background: var(--ymm-primary);
}
.profile-hero {
  min-height: 272px;
  border: 1px solid #254f91;
  border-radius: var(--ymm-radius-lg);
  background: #1f4e91;
  box-shadow: var(--ymm-shadow-soft);
}
.hero-glow {
  border: 1px solid rgba(255,255,255,.16);
  background: transparent;
}
.profile-avatar {
  border-width: 3px;
  box-shadow: 0 14px 32px rgba(9,28,57,.28);
}
.profile-label {
  letter-spacing: 0;
  font-weight: 600;
}
.profile-copy h1,
.works-header h2,
.note-copy h3 {
  font-family: var(--ymm-font-serif);
}
.works-section {
  padding: 28px;
  box-shadow: none;
}
.works-header p { color: var(--ymm-primary); }
.works-header h2 { color: var(--ymm-ink); }
.note-grid { gap: 16px; }
.note-card {
  border-color: var(--ymm-line-soft);
  border-radius: var(--ymm-radius-md);
  box-shadow: 0 1px 0 rgba(23,35,59,.03);
}
.note-card:hover {
  border-color: #b9c9df;
  box-shadow: var(--ymm-shadow-soft);
}
.note-copy h3 { color: var(--ymm-ink); line-height: 1.5; }
.note-meta { color: var(--ymm-muted); }

@media (max-width: 600px) {
  .profile-page { padding: 14px 12px 34px; }
  .profile-hero { min-height: 0; padding: 24px 20px; }
  .works-section { margin-top: 14px; padding: 20px 14px; }
  .note-grid { gap: 10px; }
  .note-cover { height: 150px; }
  .note-copy { padding: 11px; }
  .note-copy h3 { font-size: 13px; }
}
</style>
