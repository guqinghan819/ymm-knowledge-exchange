<template>
  <div class="publish-image-container">
    <!-- Upload Step -->
    <div class="upload-area" v-if="uploadStep === 'upload'">
      <div class="upload-box" @click="triggerFileInput">
        <div class="upload-icon"><AppIcon name="cloud" /></div>
        <p>拖拽图片到此或点击上传</p>
        <button class="upload-btn">上传图片</button>
        <input 
          type="file" 
          id="image-upload-input" 
          hidden 
          accept="image/*" 
          multiple 
          @change="handleFileSelect"
        />
      </div>
      <div class="upload-tips">
        <div>
          <h4>图片大小</h4>
          <p>支持上传的图片大小，最大32MB的图片文件</p>
          <h4>图片格式</h4>
          <p>支持上传的图片格式，推荐使用png、jpg、jpeg、webp</p>
          <h4>图片分辨率</h4>
          <p>不限制宽高比例，推荐上传3:4至2:1之间，分辨率不低于720*960的图片</p>
        </div>
      </div>
    </div>

    <!-- Uploading Overlay -->
    <div v-if="isUploading" class="global-loading-overlay">
      <div class="loading-spinner"></div>
      <p class="loading-text">图片上传中，请稍候...</p>
    </div>

    <!-- Form Step -->
    <div class="form-area" v-else-if="uploadStep === 'form'">
      <!-- File Preview Section -->
      <div class="file-preview">
        <div class="preview-box">
          <img v-if="uploadedFiles.length > 0" :src="uploadedFiles[0].url" class="preview-media" />
          <div v-else class="preview-placeholder">预览</div>
        </div>

        <!-- Image List (Add More) -->
        <div class="image-list">
          <div class="image-item" v-for="(img, index) in uploadedFiles" :key="index">
            <img :src="img.url" />
            <div class="image-overlay">
              <button type="button" class="remove-btn" aria-label="移除图片" @click.stop="removeImage(index)"><AppIcon name="close" :size="14" /></button>
              <button class="edit-btn-small" @click.stop="openEditModal(index, $event)">编辑</button>
            </div>
          </div>
          <div class="image-add-btn" v-if="uploadedFiles.length < 8" @click="addMoreImages" role="button" tabindex="0" aria-label="添加图片" @keydown.enter="addMoreImages" @keydown.space.prevent="addMoreImages">
            <AppIcon name="plus" :size="24" />
          </div>
          <input 
            type="file" 
            id="image-upload-input-more" 
            hidden 
            accept="image/*" 
            multiple 
            @change="handleFileSelect"
          />
        </div>
      </div>
      
      <div class="form-content">
        <div class="form-item">
          <div class="form-label-row">
            <label>标题</label>
            <div class="ai-action-group">
              <button
                type="button"
                class="ai-write-btn"
                :disabled="isAiWritingTitle || isAiWritingContent"
                @click="handleAiWriteTitle($event)"
              >
                {{ isAiWritingTitle ? 'AI生成中...' : 'AI写标题' }}
              </button>
            </div>
          </div>
          <input type="text" v-model="publishForm.title" placeholder="填写标题会有更多赞哦" class="form-input" />
        </div>
        <div class="form-item">
          <div class="form-label-row">
            <label>正文</label>
            <button
              type="button"
              class="ai-write-btn"
              :disabled="isAiWritingTitle || isAiWritingContent"
              @click="handleAiWriteContent($event)"
            >
              {{ isAiWritingContent ? 'AI生成中...' : 'AI写作' }}
            </button>
          </div>
          <div class="textarea-container">
            <textarea 
              v-model="publishForm.content" 
              placeholder="输入正文描述，真诚有价值的分享予人温暖" 
              class="form-textarea"
              maxlength="6000"
            ></textarea>
            <span class="char-count">{{ publishForm.content.length }}/6000</span>
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item flex-1">
            <label>频道</label>
            <select v-model="publishForm.channelId" class="form-select" @change="handleChannelChange">
              <option value="" disabled>请选择频道</option>
              <option v-for="channel in channels" :key="channel.id" :value="channel.id">{{ channel.name }}</option>
            </select>
          </div>
          <div class="form-item flex-1">
            <label>话题</label>
            <select v-model="publishForm.topicId" class="form-select" @mousedown="handleTopicClick">
              <option value="" disabled>请选择话题</option>
              <option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.name }}</option>
            </select>
          </div>
        </div>

        <button class="submit-btn" :disabled="isPublishing" @click="handlePublish">
          {{ isPublishing ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>

    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showAiModal" class="modal-overlay ai-title-overlay" :style="dialogMotionStyle">
        <div class="ai-title-modal dialog-panel">
        <div class="ai-title-header">
          <div>
            <div class="ai-title-name">{{ aiModalTitle }}</div>
            <div class="ai-title-subtitle">{{ aiModalSubtitle }}</div>
          </div>
        </div>
        <div class="ai-title-body">
          <div
            v-for="(step, index) in aiSteps"
            :key="index"
            class="ai-title-step"
            :class="{ active: index === aiSteps.length - 1 && isAiGenerating }"
          >
            <span
              v-if="index === aiSteps.length - 1 && isAiGenerating"
              class="ai-title-step-loading"
            ></span>
            <span v-else class="ai-title-step-dot"></span>
            <span>{{ step }}</span>
          </div>
        </div>
        </div>
      </div>
    </Transition>

    <!-- Edit Modal -->
    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showEditModal" class="modal-overlay" :style="dialogMotionStyle" @click.self="closeEditModal">
        <div class="edit-modal dialog-panel">
        <div class="edit-header">
          <div class="edit-title">图片编辑 {{ editingIndex + 1 }}/{{ uploadedFiles.length }}</div>
          <button class="close-icon" aria-label="关闭图片编辑" @click="closeEditModal"><AppIcon name="close" :size="20" /></button>
        </div>
        
        <div class="edit-body">
          <div class="edit-preview-area">
            <div class="canvas-container">
               <vue-cropper
                 ref="cropper"
                 :img="uploadedFiles[editingIndex]?.url"
                 :outputSize="1"
                 outputType="png"
                 :info="true"
                 :full="true"
                 :canMove="true"
                 :canMoveBox="true"
                 :fixed="false"
                 :fixedBox="false"
                 :original="false"
                 :autoCrop="true"
                 :centerBox="true"
                 :high="true"
                 mode="contain"
               ></vue-cropper>
            </div>
            <!-- Navigation Buttons -->
            <button class="nav-btn prev" @click="prevImage" v-if="uploadedFiles.length > 1 && editingIndex > 0">&lt;</button>
            <button class="nav-btn next" @click="nextImage" v-if="uploadedFiles.length > 1 && editingIndex < uploadedFiles.length - 1">&gt;</button>
          </div>
        </div>
        
        <div class="edit-footer">
          <button class="cancel-btn" @click="closeEditModal">取消</button>
          <button class="confirm-btn" @click="confirmEdit">确认</button>
        </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import { getChannels } from '@/api/channel'
import { getTopics } from '@/api/topic'
import { publishNote } from '@/api/note'
import { createNoteTitleStream, createNoteContentStream } from '@/api/agent'
import { useDialogMotionOrigin } from '@/utils/dialogMotion'
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'

const emit = defineEmits(['published'])
const { dialogMotionStyle, captureDialogOrigin, resetDialogOrigin } = useDialogMotionOrigin()
const uploadStep = ref('upload') // 'upload', 'form'
const isUploading = ref(false)
const isPublishing = ref(false)
const isAiWritingTitle = ref(false)
const isAiWritingContent = ref(false)
const aiTitleStep = ref('')
const aiSteps = ref([])
const showAiModal = ref(false)
const aiModalTitle = ref('')
const aiModalSubtitle = ref('')
const uploadedFiles = ref([]) // Array of { url, file, type, name, progress }
const publishForm = ref({
  title: '',
  content: '',
  channelId: '',
  topicId: ''
})

const channels = ref([])
const topics = ref([])

const isAiGenerating = computed(() => isAiWritingTitle.value || isAiWritingContent.value)

const getUploadedImageUrls = () => {
  return uploadedFiles.value
    .filter(file => file.status === 'done' && file.url)
    .map(file => file.url)
}

const openAiStepModal = (title, subtitle, firstStep) => {
  aiModalTitle.value = title
  aiModalSubtitle.value = subtitle
  aiTitleStep.value = firstStep
  aiSteps.value = [firstStep]
  showAiModal.value = true
}

const closeAiStepModal = () => {
  showAiModal.value = false
  aiTitleStep.value = ''
  aiSteps.value = []
}

const parseSsePayload = (dataText) => {
  try {
    return dataText ? JSON.parse(dataText) : {}
  } catch {
    return {}
  }
}

const readAiTextFromStream = async (stream, onStep) => {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let pendingData = ''
  let result = ''

  const dispatch = (payload) => {
    if (payload?.type === 'step') {
      onStep?.(payload.content || '')
    }
    if (payload?.type === 'text') {
      result += payload.content || ''
    }
    if (payload?.type === 'error') {
      throw new Error(payload.message || payload.content || 'AI生成失败')
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (value) {
      buffer += decoder.decode(value, { stream: true })
      while (true) {
        const idx = buffer.indexOf('\n')
        if (idx < 0) break
        let line = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 1)
        if (line.endsWith('\r')) line = line.slice(0, -1)
        if (!line) {
          if (pendingData) {
            dispatch(parseSsePayload(pendingData))
            pendingData = ''
          }
          continue
        }
        if (line.startsWith(':')) continue
        if (line.startsWith('data:')) pendingData += line.slice(5).trim()
      }
    }
    if (done) break
  }

  if (pendingData.trim()) {
    dispatch(parseSsePayload(pendingData))
  }

  return result.trim()
}

const createPublishAiTitle = async () => {
  const imageUrls = getUploadedImageUrls()
  if (imageUrls.length === 0) {
    throw new Error('请先上传图片')
  }

  const conversationId = `publish-image-title-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const resp = await createNoteTitleStream({
    imageUrls,
    title: publishForm.value.title,
    content: publishForm.value.content,
    conversationId
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`AI请求失败: ${resp.status}`)
  }
  return readAiTextFromStream(resp.body, (step) => {
    aiTitleStep.value = step
    aiSteps.value.push(step)
  })
}

const createPublishAiContent = async () => {
  const imageUrls = getUploadedImageUrls()
  if (imageUrls.length === 0) {
    throw new Error('请先上传图片')
  }

  const conversationId = `publish-image-content-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const resp = await createNoteContentStream({
    imageUrls,
    title: publishForm.value.title,
    content: publishForm.value.content,
    conversationId
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`AI请求失败: ${resp.status}`)
  }
  return readAiTextFromStream(resp.body, (step) => {
    aiTitleStep.value = step
    aiSteps.value.push(step)
  })
}

const normalizeAiTitle = (text) => {
  return text
    .replace(/^["“”'‘’\s]+|["“”'‘’\s]+$/g, '')
    .split('\n')
    .map(line => line.replace(/^\d+[.、]\s*/, '').trim())
    .find(Boolean) || ''
}

const handleAiWriteTitle = async (event) => {
  if (isAiWritingTitle.value || isAiWritingContent.value) return

  captureDialogOrigin(event)
  isAiWritingTitle.value = true
  openAiStepModal('AI写标题', '正在根据全部图片总结标题', '准备发送图片...')
  try {
    const title = normalizeAiTitle(await createPublishAiTitle())
    if (!title) {
      ElMessage.warning('AI暂未生成标题，请稍后重试')
      return
    }
    publishForm.value.title = title.slice(0, 20)
    ElMessage.success('标题已生成')
    closeAiStepModal()
  } catch (error) {
    console.error('AI生成标题失败:', error)
    ElMessage.error(error.message || 'AI生成标题失败')
  } finally {
    isAiWritingTitle.value = false
  }
}

const handleAiWriteContent = async (event) => {
  if (isAiWritingTitle.value || isAiWritingContent.value) return

  captureDialogOrigin(event)
  isAiWritingContent.value = true
  openAiStepModal('AI写作', '正在根据全部图片生成知识分享正文', '准备发送图片...')
  try {
    const content = await createPublishAiContent()
    if (!content) {
      ElMessage.warning('AI暂未生成正文，请稍后重试')
      return
    }
    publishForm.value.content = content.slice(0, 6000)
    ElMessage.success('正文已生成')
    closeAiStepModal()
  } catch (error) {
    console.error('AI生成正文失败:', error)
    ElMessage.error(error.message || 'AI生成正文失败')
  } finally {
    isAiWritingContent.value = false
  }
}

const loadChannels = async () => {
  try {
    const res = await getChannels()
    if (res && res.success) {
      channels.value = res.data
    }
  } catch (error) {
    console.error('获取频道失败:', error)
  }
}

onMounted(() => {
  loadChannels()
})

const handleChannelChange = async () => {
  publishForm.value.topicId = ''
  topics.value = []
  if (!publishForm.value.channelId) return
  
  try {
    const res = await getTopics(publishForm.value.channelId)
    if (res && res.success) {
      topics.value = res.data
    }
  } catch (error) {
    console.error('获取话题失败:', error)
  }
}

const handleTopicClick = (e) => {
  if (!publishForm.value.channelId) {
    e.preventDefault()
    ElMessage.warning('请先选择频道')
  }
}

const handlePublish = async () => {
  if (uploadedFiles.value.length === 0) {
    return ElMessage.warning('请先上传图片')
  }
  if (!publishForm.value.title.trim()) {
    return ElMessage.warning('请输入标题')
  }
  if (!publishForm.value.content.trim()) {
    return ElMessage.warning('请输入正文内容')
  }
  if (!publishForm.value.channelId) {
    return ElMessage.warning('请选择频道')
  }
  
  isPublishing.value = true
  try {
    const res = await publishNote({
      type: 0, // 0 for image
      imageUrls: uploadedFiles.value.map(f => f.url),
      videoUrl: null,
      title: publishForm.value.title,
      content: publishForm.value.content,
      topicId: publishForm.value.topicId || null
    })
    
    if (res && res.success) {
      ElMessage.success('发布成功')
      uploadStep.value = 'upload'
      uploadedFiles.value = []
      publishForm.value = { title: '', content: '', channelId: '', topicId: '' }
      emit('published')
    }
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    isPublishing.value = false
  }
}

// Edit Modal State
const showEditModal = ref(false)
const editingIndex = ref(0)
const cropper = ref(null)

const triggerFileInput = () => {
  document.getElementById('image-upload-input').click()
}

const addMoreImages = () => {
  if (uploadedFiles.value.length >= 8) {
    ElMessage.warning('最多只能上传8张图片')
    return
  }
  document.getElementById('image-upload-input-more').click()
}

const handleFileSelect = async (e) => {
  const files = Array.from(e.target.files)
  if (files.length === 0) return
  
  const remainingSlots = 8 - uploadedFiles.value.length
  const filesToUpload = files.slice(0, remainingSlots)
  
  if (filesToUpload.length === 0) {
    ElMessage.warning('最多只能上传8张图片')
    return
  }
  
  for (const file of filesToUpload) {
    await processUpload(file)
  }
}

const processUpload = async (file) => {
  const fileObj = {
    file,
    name: file.name,
    type: 'image',
    url: URL.createObjectURL(file),
    progress: 0,
    status: 'uploading'
  }
  
  uploadedFiles.value.push(fileObj)
  if (uploadedFiles.value.length === 1) {
    isUploading.value = true
  }
  
  try {
    const timer = setInterval(() => {
      if (fileObj.progress < 90) {
        fileObj.progress += 10
      }
    }, 100)
    
    const res = await uploadFile(file)
    clearInterval(timer)
    
    if (res && res.success) {
      fileObj.url = res.data
      fileObj.progress = 100
      fileObj.status = 'done'
      
      const allDone = uploadedFiles.value.every(f => f.status === 'done')
      if (allDone) {
        isUploading.value = false
        uploadStep.value = 'form'
      }
    } else {
      fileObj.status = 'error'
      isUploading.value = false
    }
  } catch (error) {
    fileObj.status = 'error'
    isUploading.value = false
    console.error(error)
  }
}

const removeImage = (index) => {
  uploadedFiles.value.splice(index, 1)
  if (uploadedFiles.value.length === 0) {
    uploadStep.value = 'upload'
  }
}

// Edit Modal Methods
const openEditModal = (index, event) => {
  captureDialogOrigin(event)
  editingIndex.value = index
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
}

const prevImage = () => {
  if (editingIndex.value > 0) {
    editingIndex.value--
  }
}

const nextImage = () => {
  if (editingIndex.value < uploadedFiles.value.length - 1) {
    editingIndex.value++
  }
}

const confirmEdit = () => {
  if (cropper.value) {
    cropper.value.getCropBlob(async (data) => {
      const originalFile = uploadedFiles.value[editingIndex.value].file
      const newFile = new File([data], originalFile.name, { type: originalFile.type })
      
      try {
        const res = await uploadFile(newFile)
        if (res && res.success) {
           uploadedFiles.value[editingIndex.value].url = res.data
           uploadedFiles.value[editingIndex.value].file = newFile
           closeEditModal()
           ElMessage.success('编辑已保存')
        }
      } catch (e) {
        console.error(e)
      }
    })
  } else {
    closeEditModal()
  }
}
</script>

<style scoped>
.publish-image-container {
  padding: 24px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.upload-box {
  width: 100%;
  max-width: 600px;
  height: 300px;
  border: 2px dashed #eee;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
  cursor: pointer;
  transition: border-color 0.2s;
  margin-bottom: 40px;
}

.upload-box:hover {
  border-color: #3370ff;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ccc;
}

.upload-box p {
  color: #999;
  font-size: 14px;
  margin-bottom: 20px;
}

.upload-btn {
  background-color: #3370ff;
  color: #fff;
  border: none;
  padding: 10px 30px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.upload-btn:hover {
  background-color: #285edb;
}

.upload-tips {
  width: 100%;
  max-width: 800px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #eee;
  padding-top: 30px;
}

.upload-tips h4 {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  margin-top: 0;
}

.upload-tips p {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-bottom: 16px;
}

/* Form Area */
.form-area {
  display: flex;
  gap: 30px;
}

.file-preview {
  width: 300px;
  flex-shrink: 0;
}

.preview-box {
  width: 100%;
  height: 400px; /* Portrait ratio */
  background-color: #000;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 10px;
  overflow: hidden;
}

.preview-media {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.image-item {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  border: 1px solid #eee;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-item .image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.2s;
  gap: 8px;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.edit-btn-small {
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.8);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  margin-top: 10px;
}

.edit-btn-small:hover {
  background: rgba(0, 0, 0, 0.8);
}

.remove-btn {
  padding: 0;
  border: 0;
  position: absolute;
  top: 4px;
  right: 4px;
  background-color: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 12px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
}

.image-add-btn {
  width: 60px;
  height: 60px;
  border: 1px dashed #ccc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #ccc;
  cursor: pointer;
}

.image-add-btn:hover {
  border-color: #3370ff;
  color: #3370ff;
}

.form-content {
  flex: 1;
}

.form-row {
  display: flex;
  gap: 20px;
}

.flex-1 {
  flex: 1;
}

.form-item {
  margin-bottom: 24px;
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.form-item label {
  display: block;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.ai-action-group {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.ai-step-text {
  color: #667085;
  font-size: 12px;
  white-space: nowrap;
}

.ai-write-btn {
  border: 1px solid rgba(51, 112, 255, 0.2);
  background: #f4f7ff;
  color: #3370ff;
  border-radius: 16px;
  padding: 6px 12px;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ai-write-btn:hover:not(:disabled) {
  background: #eaf0ff;
  border-color: rgba(51, 112, 255, 0.45);
}

.ai-write-btn:disabled {
  color: #8aa7ef;
  cursor: not-allowed;
  opacity: 0.75;
}

.ai-title-overlay {
  z-index: 3000;
  background-color: rgba(15, 23, 42, 0.28);
}

.ai-title-modal {
  width: 420px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.ai-title-header {
  padding: 20px 22px 14px;
  border-bottom: 1px solid #eef1f6;
}

.ai-title-name {
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.ai-title-subtitle {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.ai-title-body {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px 22px 22px;
}

.ai-title-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #475467;
  font-size: 14px;
  line-height: 1.6;
  padding: 6px 0;
}

.ai-title-step-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #d0d5dd;
  flex-shrink: 0;
}

.ai-title-step-loading {
  width: 12px;
  height: 12px;
  margin-top: 5px;
  border: 2px solid rgba(51, 112, 255, 0.2);
  border-top-color: #3370ff;
  border-radius: 50%;
  flex-shrink: 0;
  animation: ai-title-spin 0.8s linear infinite;
}

.ai-title-step.active {
  color: #3370ff;
}

@keyframes ai-title-spin {
  to {
    transform: rotate(360deg);
  }
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #3370ff;
}

.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 20px; /* More rounded corners */
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
  appearance: none; /* Remove default arrow */
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12' fill='none'%3E%3Cpath d='M2.5 4.5L6 8L9.5 4.5' stroke='%23666' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  background-color: #f8f9fa; /* Slight background color for smoothness */
  cursor: pointer;
  color: #333;
}

.form-select:hover {
  background-color: #f0f2f5;
  border-color: #d1d5db;
}

.form-select:focus {
  border-color: #3370ff;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.1);
}

.form-textarea {
  width: 100%;
  height: 200px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #3370ff;
}

.submit-btn {
  background-color: #3370ff;
  color: #fff;
  border: none;
  padding: 12px 48px;
  border-radius: 24px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-btn:hover {
  background-color: #285edb;
}

.submit-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.textarea-container {
  position: relative;
  width: 100%;
}

.char-count {
  position: absolute;
  bottom: 12px;
  right: 12px;
  font-size: 12px;
  color: #999;
}

/* Global Loading Overlay */
.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(255, 255, 255, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3370ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: #333;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.edit-modal {
  background-color: #fff;
  width: 900px;
  height: 700px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-header {
  height: 60px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.edit-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.close-icon {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
}

.edit-body {
  flex: 1;
  background-color: #f0f0f0;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.edit-preview-area {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.canvas-container {
  width: 100%;
  height: 100%;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid #ddd;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  z-index: 10;
  font-size: 18px;
  color: #666;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.nav-btn:hover {
  background-color: #fff;
  color: #3370ff;
}

.nav-btn.prev {
  left: 20px;
}

.nav-btn.next {
  right: 20px;
}

.edit-footer {
  height: 70px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  background-color: #fff;
}

.cancel-btn {
  padding: 8px 24px;
  border-radius: 20px;
  border: 1px solid #ddd;
  background-color: #fff;
  color: #666;
  cursor: pointer;
}

.confirm-btn {
  padding: 8px 24px;
  border-radius: 20px;
  border: none;
  background-color: #3370ff;
  color: #fff;
  cursor: pointer;
}

.confirm-btn:hover {
  background-color: #285edb;
}
</style>
