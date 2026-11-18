<template>
  <div class="publish-video-container">
    <!-- Upload Step -->
    <div class="upload-area" v-if="uploadStep === 'upload'">
      <div class="upload-box" @click="triggerFileInput">
        <div class="upload-icon"><AppIcon name="cloud" /></div>
        <p>拖拽视频到此或点击上传</p>
        <button class="upload-btn">上传视频</button>
        <input 
          type="file" 
          id="video-upload-input" 
          hidden 
          accept=".mp4,.mov,video/mp4,video/quicktime"
          @change="handleFileSelect"
        />
      </div>
      <div class="upload-tips">
        <div>
          <h4>视频大小</h4>
          <p>支持时长2分钟以内，最大500MB的视频文件</p>
          <h4>视频格式</h4>
          <p>支持常用视频格式，推荐使用mp4、mov</p>
          <h4>视频分辨率</h4>
          <p>超过1080P的视频用网页端上传画质更清晰</p>
        </div>
      </div>
    </div>

    <!-- Uploading Overlay -->
    <div v-if="isUploading" class="global-loading-overlay">
      <div v-if="uploadStatus === 'uploading'" class="loading-spinner"></div>
      <p class="loading-text">{{ uploadStatusText }}</p>
      <div class="progress-bar-container">
        <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <p class="progress-text">{{ uploadProgress }}%</p>
      <p class="upload-size-text">{{ formatFileSize(uploadedBytes) }} / {{ formatFileSize(totalUploadBytes) }}</p>
      <p v-if="uploadStatus === 'paused'" class="resume-hint">任务已保留，可继续上传；刷新页面后请重新选择同一文件。</p>
      <div class="upload-actions">
        <button
          v-if="uploadMode === 'multipart' && uploadStatus === 'uploading'"
          class="upload-control-btn"
          @click="pauseUpload"
        >暂停</button>
        <button
          v-if="uploadMode === 'multipart' && uploadStatus === 'paused'"
          class="upload-control-btn primary"
          @click="continueUpload"
        >继续</button>
        <button class="upload-control-btn danger" @click="cancelUpload">取消</button>
      </div>
    </div>

    <!-- Form Step -->
    <div class="form-area" v-else-if="uploadStep === 'form'">
      <!-- File Preview Section -->
      <div class="file-preview">
        <div class="preview-box">
          <video v-if="uploadedFiles[0]" :src="uploadedFiles[0].url" controls class="preview-media"></video>
          <div v-else class="preview-placeholder">预览</div>
        </div>
        
        <!-- Video Info -->
        <div class="file-info" v-if="uploadedFiles[0]">
          <div class="file-name">{{ uploadedFiles[0].name }}</div>
          <div class="upload-success">上传成功 100%</div>
          <button class="reupload-btn" @click="resetUpload">重新上传</button>
        </div>
      </div>
      
      <div class="form-content">
        <div class="form-item">
          <label>标题</label>
          <input type="text" v-model="publishForm.title" placeholder="填写标题会有更多赞哦" class="form-input" />
        </div>
        <div class="form-item">
          <label>正文</label>
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
  </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getChannels } from '@/api/channel'
import { getTopics } from '@/api/topic'
import { publishNote } from '@/api/note'
import { useVideoUpload } from '@/composables/useVideoUpload'

const emit = defineEmits(['published'])
const isPublishing = ref(false)
const {
  uploadStep,
  isUploading,
  uploadProgress,
  uploadedBytes,
  totalUploadBytes,
  uploadMode,
  uploadStatus,
  uploadStatusText,
  uploadedFiles,
  processUpload,
  pauseUpload,
  continueUpload,
  cancelUpload,
  resetUploadState,
  formatFileSize
} = useVideoUpload()
const publishForm = ref({
  title: '',
  content: '',
  channelId: '',
  topicId: ''
})

const channels = ref([])
const topics = ref([])

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
  if (!uploadedFiles.value.length || !uploadedFiles.value[0].url) {
    return ElMessage.warning('请先上传视频')
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
  // topicId can be optional or required depending on business logic, assuming optional here or user must select if topics exist
  
  isPublishing.value = true
  try {
    const res = await publishNote({
      type: 1, // 1 for video
      imageUrls: [],
      videoUrl: uploadedFiles.value[0].url,
      title: publishForm.value.title,
      content: publishForm.value.content,
      topicId: publishForm.value.topicId || null
    })
    
    if (res && res.success) {
      ElMessage.success('发布成功')
      resetUpload()
      emit('published')
    }
  } catch (error) {
    console.error('发布失败:', error)
    // ElMessage.error('发布失败')
  } finally {
    isPublishing.value = false
  }
}

const triggerFileInput = () => {
  document.getElementById('video-upload-input').click()
}

const handleFileSelect = async (e) => {
  const files = Array.from(e.target.files)
  if (files.length === 0) return
  
  const file = files[0]
  e.target.value = ''
  await processUpload(file)
}

const resetUpload = () => {
  resetUploadState()
  publishForm.value = { title: '', content: '', channelId: '', topicId: '' }
}
</script>

<style scoped>
.publish-video-container {
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
  height: 400px;
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

.file-info {
  margin-top: 10px;
}

.file-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-success {
  font-size: 12px;
  color: #52c41a;
  margin-bottom: 8px;
}

.reupload-btn {
  font-size: 12px;
  color: #666;
  background: none;
  border: 1px solid #eee;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.reupload-btn:hover {
  color: #3370ff;
  border-color: #3370ff;
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

.form-item label {
  display: block;
  font-size: 16px;
  color: #333;
  margin-bottom: 12px;
  font-weight: 500;
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
  margin-bottom: 15px;
}

.progress-bar-container {
  width: 300px;
  height: 8px;
  background-color: #eee;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-bar {
  height: 100%;
  background-color: #3370ff;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
}

.upload-size-text,
.resume-hint {
  margin: 0 0 14px;
  font-size: 13px;
  color: #777;
}

.resume-hint {
  color: #b26a00;
}

.upload-actions {
  display: flex;
  gap: 12px;
}

.upload-control-btn {
  min-width: 82px;
  padding: 8px 18px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #333;
  cursor: pointer;
}

.upload-control-btn.primary {
  border-color: #3370ff;
  background: #3370ff;
  color: #fff;
}

.upload-control-btn.danger {
  border-color: #f56c6c;
  color: #f56c6c;
}
</style>
