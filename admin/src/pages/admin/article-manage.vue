<template>
  <div class="article-management">
    <!-- 搜索和操作区域 -->
    <div class="search-and-actions mb-4 p-4 bg-white rounded shadow">
      <el-row :gutter="20" class="items-center">
        <el-col :span="18">
          <el-form :model="searchForm" label-width="80px" class="flex flex-wrap" @submit.prevent="handleSearch">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px">
                <el-option label="待审核" :value="0" />
                <el-option label="正常" :value="1" />
                <el-option label="已删除" :value="2" />
                <el-option label="已下架" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="作者ID">
              <el-input v-model="searchForm.creatorId" placeholder="请输入作者ID" clearable style="width: 220px" />
            </el-form-item>
          </el-form>
        </el-col>

        <el-col :span="6" class="flex justify-end">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 文章列表 -->
    <div class="article-list bg-white rounded shadow table-container">
      <el-table
        :data="articleList"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        class="article-table"
        @row-click="openDetail"
      >
        <el-table-column prop="id" label="文章ID" width="160" show-overflow-tooltip />
        <el-table-column label="封面" width="90">
          <template #default="scope">
            <div v-if="scope.row.type === 1" class="video-cover-wrapper">
              <video
                v-if="scope.row.cover && !videoCoverErrors[scope.row.id]"
                :src="fileUrl(scope.row.cover)"
                class="video-cover"
                preload="metadata"
                muted
                playsinline
                aria-hidden="true"
                @loadedmetadata="seekVideoCoverFrame"
                @error="videoCoverErrors[scope.row.id] = true"
              />
              <div v-else class="video-cover-fallback">视频</div>
              <span class="video-play-icon" aria-hidden="true">▶</span>
            </div>
            <el-image
              v-else-if="scope.row.cover"
              :src="fileUrl(scope.row.cover)"
              class="cover-preview"
              :preview-src-list="[fileUrl(scope.row.cover)]"
              preview-teleported
              fit="cover"
              @click.stop
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <span v-else class="text-xs text-gray-400">无封面</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <button type="button" class="article-title-link" @click.stop="openDetail(scope.row)">{{ scope.row.title || '未命名笔记' }}</button>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="150">
          <template #default="scope">
            <div>{{ scope.row.creatorName || '-' }}</div>
            <div class="text-xs text-gray-400">ID: {{ scope.row.creatorId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === 0 ? 'primary' : 'warning'" effect="plain">
              {{ scope.row.type === 0 ? '图文' : '视频' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ reviewText(scope.row) }}</div>
            <div v-if="scope.row.reviewReason" class="text-xs text-gray-500">原因：{{ scope.row.reviewReason }}</div>
            <div v-if="scope.row.reviewInternalError" class="text-xs text-red-500">内部：{{ scope.row.reviewInternalError }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="topicName" label="话题" width="120" show-overflow-tooltip />
        <el-table-column label="赞/藏/评" width="130">
          <template #default="scope">
            <span class="text-xs">{{ scope.row.likeCount }} / {{ scope.row.collectCount }} / {{ scope.row.commentCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === 0">
              <el-button size="small" type="success" @click.stop="handlePass(scope.row)">通过</el-button>
              <el-button size="small" type="danger" @click.stop="handleReject(scope.row)">驳回</el-button>
            </template>
            <template v-else-if="scope.row.status === 1">
              <el-button v-if="scope.row.reviewMethod === 'AI'" size="small" type="danger" @click.stop="handleReject(scope.row)">复核驳回</el-button>
              <el-button size="small" type="warning" @click.stop="handleDown(scope.row)">下架</el-button>
              <el-button size="small" type="danger" @click.stop="handleDelete(scope.row)">删除</el-button>
            </template>
            <template v-else-if="scope.row.status === 3">
              <el-button v-if="scope.row.reviewMethod === 'AI'" size="small" type="success" @click.stop="handlePass(scope.row)">复核通过</el-button>
              <el-button v-else size="small" type="success" @click.stop="handleRestore(scope.row)">恢复</el-button>
              <el-button size="small" type="danger" @click.stop="handleDelete(scope.row)">删除</el-button>
            </template>
            <span v-else class="text-xs text-gray-400">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 游标分页 -->
      <div class="pagination-container p-4 flex justify-between items-center">
        <span class="text-sm text-gray-500">共 {{ total }} 篇</span>
        <div class="flex items-center gap-3">
          <el-button size="small" :disabled="currentPage === 0" @click="handlePrev">上一页</el-button>
          <span class="text-sm text-gray-600">第 {{ currentPage + 1 }} 页</span>
          <el-button size="small" type="primary" :disabled="!hasNext" @click="handleNext">下一页</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailDialogVisible" title="笔记内容" width="760px" append-to-body @closed="closeDetail">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailNote">
          <h2 class="detail-title">{{ detailNote.title || '未命名笔记' }}</h2>
          <div class="detail-meta">
            <span>{{ detailNote.creatorName || '未知作者' }}（ID: {{ detailNote.creatorId }}）</span>
            <el-tag :type="statusTagType(detailNote.status)" size="small">{{ statusText(detailNote) }}</el-tag>
            <span v-if="detailNote.topicName">话题：{{ detailNote.topicName }}</span>
            <span v-if="detailNote.channelName">频道：{{ detailNote.channelName }}</span>
          </div>
          <div class="detail-meta">
            <span>审核：{{ reviewText(detailNote) }}</span>
            <span v-if="detailNote.reviewedAt">审核时间：{{ detailNote.reviewedAt }}</span>
            <span v-if="detailNote.reviewTaskStatus">任务：{{ detailNote.reviewTaskStatus }}</span>
          </div>
          <p v-if="detailNote.reviewReason" class="detail-content">驳回原因：{{ detailNote.reviewReason }}</p>
          <p v-if="detailNote.reviewInternalError" class="detail-content">内部失败信息：{{ detailNote.reviewInternalError }}</p>
          <p class="detail-content">{{ detailNote.content || '暂无正文内容' }}</p>
          <video v-if="detailNote.type === 1 && detailNote.videoUri" class="detail-video" :src="fileUrl(detailNote.videoUri)" controls preload="metadata" />
          <div v-if="detailNote.imgUris?.length" class="detail-images">
            <el-image v-for="(uri, index) in detailNote.imgUris" :key="`${uri}-${index}`" :src="fileUrl(uri)" :preview-src-list="detailImageUrls" :initial-index="index" preview-teleported fit="contain" />
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog
      v-model="downDialogVisible"
      title="下架笔记"
      width="500px"
      :close-on-click-modal="!downSubmitting"
      :close-on-press-escape="!downSubmitting"
      :show-close="!downSubmitting"
      @closed="resetDownForm"
    >
      <p class="down-note-title">确认下架「{{ downTarget?.title || '未命名笔记' }}」</p>
      <el-form
        ref="downFormRef"
        :model="downForm"
        :rules="downFormRules"
        :disabled="downSubmitting"
        label-width="90px"
      >
        <el-form-item label="下架原因" prop="reasonCode">
          <el-select
            v-model="downForm.reasonCode"
            placeholder="请选择下架原因"
            :loading="reasonsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in reasonOptions"
              :key="item.code"
              :label="item.label"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明" prop="reasonDetail">
          <el-input
            v-model="downForm.reasonDetail"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            :placeholder="downForm.reasonCode === 'OTHER' ? '选择“其他”时必须填写' : '可补充具体违规内容或处理依据'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="downSubmitting" @click="downDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="downSubmitting" @click="submitDown">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getArticleList,
  getArticleDetail,
  checkArticlePass,
  checkArticleReject,
  getDownReasonOptions,
  downArticle,
  restoreArticle,
  deleteArticle
} from '@/api/admin/article'

// 搜索表单
const searchForm = reactive({
  status: null,
  creatorId: ''
})

// 列表与游标分页状态
const articleList = ref([])
const loading = ref(false)
const total = ref(0)
const hasNext = ref(false)
const nextCursor = ref(null)
const currentPage = ref(0)        // 当前页索引(从0开始)
const pageCursors = ref([null])   // 每一页的起始游标，第一页为 null

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailNote = ref(null)
let detailRequestId = 0
const fileUrl = (url) => {
  if (!url) return ''
  const cleaned = url.replace(/^\[|\]$/g, '')
  return /^(https?:|blob:|data:)/.test(cleaned) ? cleaned : `/api${cleaned.startsWith('/') ? '' : '/'}${cleaned}`
}
const detailImageUrls = computed(() => (detailNote.value?.imgUris || []).map(fileUrl))
const videoCoverErrors = reactive({})
const seekVideoCoverFrame = (event) => {
  const video = event.currentTarget
  if (Number.isFinite(video.duration) && video.duration > 0.1) video.currentTime = 0.1
}

const openDetail = async (row) => {
  const requestId = ++detailRequestId
  detailNote.value = null
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const res = await getArticleDetail(row.id)
    if (requestId !== detailRequestId) return
    if (res?.success && res.data) detailNote.value = res.data
    else ElMessage.error(res?.message || '获取笔记内容失败')
  } catch (error) {
    if (requestId === detailRequestId) ElMessage.error('获取笔记内容失败')
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false
  }
}
const closeDetail = () => {
  detailRequestId++
  detailNote.value = null
  detailLoading.value = false
}

const downDialogVisible = ref(false)
const downSubmitting = ref(false)
const reasonsLoading = ref(false)
const reasonOptions = ref([])
const downTarget = ref(null)
const downFormRef = ref()
const downForm = reactive({
  reasonCode: '',
  reasonDetail: ''
})
const validateReasonDetail = (_rule, value, callback) => {
  if (downForm.reasonCode === 'OTHER' && !String(value || '').trim()) {
    callback(new Error('选择“其他”时必须填写补充说明'))
    return
  }
  callback()
}
const downFormRules = {
  reasonCode: [{ required: true, message: '请选择下架原因', trigger: 'change' }],
  reasonDetail: [
    { max: 200, message: '补充说明最多200个字符', trigger: 'blur' },
    { validator: validateReasonDetail, trigger: ['blur', 'change'] }
  ]
}

// 状态映射
const statusText = (row) => row.status === 3 && row.reviewReason ? '未通过' : ({ 0: '待审核', 1: '正常', 2: '已删除', 3: '已下架' }[row.status] ?? '未知')
const statusTagType = (status) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[status] ?? 'info')
const reviewText = (row) => {
  // 审核来源可能未在历史数据中记录，不能据此把已通过文章当作待审。
  if (row.status === 0) {
    if (row.reviewTaskStatus === 'HUMAN_REQUIRED') return '待人工复核'
    return row.type === 0 ? 'AI审核中' : '待人工审核'
  }
  const method = { AI: 'AI审核', MANUAL: '人工审核' }[row.reviewMethod]
  if (row.status === 1) return method ? `${method}通过` : '已通过（审核来源未记录）'
  return method || '审核来源未记录'
}

// 构造请求参数
const buildParams = (cursor) => ({
  status: (searchForm.status === null || searchForm.status === '' || searchForm.status === undefined)
    ? null
    : searchForm.status,
  creatorId: searchForm.creatorId ? String(searchForm.creatorId).trim() : null,
  cursor: cursor ?? null,
  limit: 20
})

// 加载列表
const loadList = async (cursor) => {
  loading.value = true
  try {
    const res = await getArticleList(buildParams(cursor))
    if (res.success) {
      articleList.value = res.data || []
      nextCursor.value = res.nextCursor ?? null
      hasNext.value = res.hasNext ?? false
      total.value = res.total ?? 0
    } else {
      ElMessage.error(res.message || '获取文章列表失败')
    }
  } catch (error) {
    ElMessage.error('获取文章列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  pageCursors.value = [null]
  currentPage.value = 0
  loadList(null)
}

// 重置
const handleReset = () => {
  searchForm.status = null
  searchForm.creatorId = ''
  handleSearch()
}

const loadDownReasons = async () => {
  reasonsLoading.value = true
  try {
    const res = await getDownReasonOptions()
    if (res.success) {
      reasonOptions.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取下架原因失败')
    }
  } catch (error) {
    ElMessage.error('获取下架原因失败')
    console.error(error)
  } finally {
    reasonsLoading.value = false
  }
}

// 上一页
const handlePrev = () => {
  if (currentPage.value <= 0) return
  currentPage.value--
  loadList(pageCursors.value[currentPage.value])
}

// 下一页
const handleNext = () => {
  if (!hasNext.value) return
  if (pageCursors.value.length <= currentPage.value + 1) {
    pageCursors.value.push(nextCursor.value)
  }
  currentPage.value++
  loadList(pageCursors.value[currentPage.value])
}

// 通用操作确认
const confirmAction = async (row, message, actionFn, successText) => {
  try {
    await ElMessageBox.confirm(message, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    ElMessage.info('已取消操作')
    return
  }
  try {
    const res = await actionFn(row.id)
    if (res.success) {
      ElMessage.success(successText)
      // 重新加载当前页
      loadList(pageCursors.value[currentPage.value])
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error(error)
  }
}

const handlePass = (row) => confirmAction(row, `确定审核通过文章「${row.title}」吗？`, checkArticlePass, '审核通过成功')
const handleReject = async (row) => {
  let reason
  try {
    const answer = await ElMessageBox.prompt(`请输入驳回「${row.title}」的原因（作者可见）`, '人工审核驳回', {
      confirmButtonText: '确认驳回', cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: value => {
        if (!value?.trim()) return '必须填写驳回原因'
        if (value.trim().length > 200) return '原因最多200字'
        return true
      }
    })
    reason = answer.value.trim()
  } catch { return }
  try {
    const res = await checkArticleReject(row.id, reason)
    if (res.success) { ElMessage.success('驳回成功'); loadList(pageCursors.value[currentPage.value]) }
    else ElMessage.error(res.message || '驳回失败')
  } catch (error) { ElMessage.error('驳回失败'); console.error(error) }
}
const handleDown = async (row) => {
  downTarget.value = row
  downForm.reasonCode = ''
  downForm.reasonDetail = ''
  downDialogVisible.value = true
  if (!reasonOptions.value.length) {
    await loadDownReasons()
  }
}

const submitDown = async () => {
  if (downSubmitting.value || !downTarget.value) return
  const valid = await downFormRef.value?.validate().catch(() => false)
  if (!valid) return

  downSubmitting.value = true
  try {
    const res = await downArticle({
      noteId: downTarget.value.id,
      reasonCode: downForm.reasonCode,
      reasonDetail: downForm.reasonDetail.trim() || null
    })
    if (res.success) {
      ElMessage.success('下架成功，通知将发送给作者')
      downDialogVisible.value = false
      loadList(pageCursors.value[currentPage.value])
    } else {
      ElMessage.error(res.message || '下架失败')
    }
  } catch (error) {
    ElMessage.error('下架失败')
    console.error(error)
  } finally {
    downSubmitting.value = false
  }
}

const resetDownForm = () => {
  downTarget.value = null
  downForm.reasonCode = ''
  downForm.reasonDetail = ''
  downFormRef.value?.clearValidate()
}
const handleRestore = (row) => confirmAction(row, `确定恢复上架文章「${row.title}」吗？`, restoreArticle, '恢复成功')
const handleDelete = (row) => confirmAction(row, `确定强制删除文章「${row.title}」吗？删除后不可恢复。`, deleteArticle, '删除成功')

onMounted(() => {
  loadList(null)
  loadDownReasons()
})
</script>

<style scoped>
.article-management {
  padding: 20px;
}

.search-and-actions {
  margin-bottom: 20px;
}

.search-and-actions .el-form-item {
  margin-right: 20px;
  margin-bottom: 0;
}

.table-container {
  overflow-x: auto;
  max-width: 100%;
}

.article-table {
  min-width: 1100px;
}

.article-table :deep(.el-table__row) { cursor: pointer; }
.article-title-link { padding: 0; border: 0; background: none; color: #409eff; cursor: pointer; font: inherit; text-align: left; }
.article-title-link:hover { text-decoration: underline; }
.detail-body { min-height: 120px; max-height: 65vh; overflow-y: auto; }
.detail-title { margin: 0 0 12px; font-size: 20px; line-height: 1.5; }
.detail-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; color: #909399; font-size: 13px; }
.detail-content { margin: 22px 0; color: #303133; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; }
.detail-video { display: block; width: 100%; max-height: 420px; border-radius: 8px; background: #000; }
.detail-images { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.detail-images .el-image { width: 100%; max-height: 360px; border-radius: 8px; background: #f5f7fa; cursor: zoom-in; }

.cover-preview {
  width: 60px;
  height: 60px;
  border-radius: 4px;
}

.video-cover-wrapper { position: relative; width: 60px; height: 60px; overflow: hidden; border-radius: 4px; background: #263545; }
.video-cover { display: block; width: 100%; height: 100%; object-fit: cover; pointer-events: none; }
.video-cover-fallback { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #c7d2df; font-size: 11px; }
.video-play-icon { position: absolute; top: 50%; left: 50%; display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; transform: translate(-50%, -50%); background: #0009; color: #fff; font-size: 10px; pointer-events: none; }

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

.down-note-title {
  margin: 0 0 18px;
  color: #606266;
  line-height: 1.6;
}
</style>
