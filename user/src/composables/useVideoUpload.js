import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import {
  abortVideoMultipart,
  completeVideoMultipart,
  getVideoMultipartStatus,
  initializeVideoMultipart,
  signVideoPart
} from '@/api/videoMultipart'

const MULTIPART_THRESHOLD = 20 * 1024 * 1024
const MAX_VIDEO_SIZE = 500 * 1024 * 1024
const MULTIPART_CONCURRENCY = 3
const MAX_PART_RETRIES = 3
const PART_UPLOAD_TIMEOUT = 2 * 60 * 1000
const RETRY_DELAYS = [1000, 2000, 4000]
const STORAGE_PREFIX = 'ymm:video-multipart:'

export function useVideoUpload() {
  const uploadStep = ref('upload')
  const isUploading = ref(false)
  const uploadProgress = ref(0)
  const uploadedBytes = ref(0)
  const totalUploadBytes = ref(0)
  const uploadMode = ref('')
  const uploadStatus = ref('idle')
  const uploadError = ref('')
  const uploadedFiles = ref([])
  const currentFile = ref(null)
  const currentTask = ref(null)
  const multipartEnabled = import.meta.env.VITE_COS_VIDEO_MULTIPART_ENABLED !== 'false'

  const activePartRequests = new Map()
  const completedParts = new Map()
  const inFlightPartBytes = new Map()
  let smallUploadController = null
  let multipartController = null
  let uploadRunId = 0
  let pauseRequested = false
  let localPreviewUrl = null

  const releasePreviewUrl = () => {
    if (localPreviewUrl) {
      URL.revokeObjectURL(localPreviewUrl)
      localPreviewUrl = null
    }
  }

  const uploadStatusText = computed(() => {
    if (uploadStatus.value === 'paused') return uploadError.value || '上传已暂停'
    if (uploadStatus.value === 'completing') return '分片已上传，正在合并...'
    return uploadMode.value === 'multipart' ? '视频分片上传中...' : '文件上传中，请稍候...'
  })

  const processUpload = async file => {
    if (!isSupportedVideo(file)) {
      ElMessage.warning('仅支持 MP4、MOV 格式的视频')
      return
    }
    if (file.size <= 0 || file.size > MAX_VIDEO_SIZE) {
      ElMessage.warning('视频大小必须在 500MB 以内')
      return
    }

    releasePreviewUrl()
    localPreviewUrl = URL.createObjectURL(file)
    uploadedFiles.value = [{
      file,
      name: file.name,
      type: 'video',
      url: localPreviewUrl,
      progress: 0,
      status: 'uploading'
    }]
    currentFile.value = file
    totalUploadBytes.value = file.size
    uploadedBytes.value = 0
    uploadProgress.value = 0
    uploadError.value = ''
    isUploading.value = true

    if (multipartEnabled && file.size >= MULTIPART_THRESHOLD) {
      uploadMode.value = 'multipart'
      try {
        await startMultipartUpload(file)
      } catch (error) {
        isUploading.value = false
        uploadStatus.value = 'idle'
        uploadStep.value = 'upload'
        ElMessage.error('无法创建视频分片上传任务，请稍后重试')
        console.error('创建视频分片上传任务失败:', error)
      }
      return
    }
    uploadMode.value = 'simple'
    await startSimpleUpload(file)
  }

  const startSimpleUpload = async file => {
    uploadStatus.value = 'uploading'
    smallUploadController = new AbortController()
    try {
      const res = await uploadFile(file, {
        signal: smallUploadController.signal,
        timeout: 10 * 60 * 1000,
        onUploadProgress: event => {
          const total = event.total || file.size
          uploadedBytes.value = Math.min(event.loaded, file.size)
          uploadProgress.value = Math.min(99, Math.floor((event.loaded / total) * 100))
        }
      })
      if (!res?.success) throw new Error('视频上传失败')
      finishUpload(res.data)
    } catch (error) {
      if (error?.name !== 'CanceledError' && error?.name !== 'AbortError') {
        handleSimpleUploadFailure(error)
      }
    } finally {
      smallUploadController = null
    }
  }

  const startMultipartUpload = async file => {
    let task = readSavedTask(file)
    if (task) {
      try {
        const response = await getVideoMultipartStatus(task.taskId)
        if (response?.data?.status === 'COMPLETED' && response.data.url) {
          removeSavedTask(file)
          finishUpload(response.data.url)
          return
        }
      } catch (error) {
        if (!isInvalidTaskError(error)) throw error
        removeSavedTask(file)
        task = null
      }
    }

    if (!task) {
      const response = await initializeVideoMultipart({
        fileName: file.name,
        fileSize: file.size,
        contentType: normalizedContentType(file),
        lastModified: file.lastModified
      })
      task = normalizeTask(response.data, file)
      saveTask(file, task)
    }
    currentTask.value = task
    await runMultipartUpload(file, task)
  }

  const runMultipartUpload = async (file, task) => {
    const runId = ++uploadRunId
    multipartController?.abort()
    const controller = new AbortController()
    multipartController = controller
    const requestConfig = { signal: controller.signal }
    pauseRequested = false
    uploadError.value = ''
    uploadStatus.value = 'uploading'
    isUploading.value = true
    try {
      let response
      try {
        response = await getVideoMultipartStatus(task.taskId, requestConfig)
      } catch (error) {
        if (runId !== uploadRunId || pauseRequested) throw createPauseError()
        if (!isInvalidTaskError(error)) throw error
        removeSavedTask(file)
        currentTask.value = null
        const initResponse = await initializeVideoMultipart({
          fileName: file.name,
          fileSize: file.size,
          contentType: normalizedContentType(file),
          lastModified: file.lastModified
        })
        if (runId !== uploadRunId || pauseRequested) throw createPauseError()
        const newTask = normalizeTask(initResponse.data, file)
        saveTask(file, newTask)
        currentTask.value = newTask
        return runMultipartUpload(file, newTask)
      }
      if (runId !== uploadRunId || pauseRequested) throw createPauseError()
      const status = response.data
      task.expiresAt = Number(status.expiresAt || task.expiresAt)
      saveTask(file, task)
      if (status.status === 'COMPLETED' && status.url) {
        removeSavedTask(file)
        finishUpload(status.url)
        return
      }

      completedParts.clear()
      for (const part of status.uploadedParts || []) {
        const number = Number(part.partNumber)
        const size = Number(part.size)
        if (Number.isInteger(number) && number >= 1 && number <= task.totalParts &&
          size === expectedPartSize(file, task, number)) {
          completedParts.set(number, size)
        }
      }
      inFlightPartBytes.clear()
      refreshMultipartProgress(file)

      const queue = []
      for (let partNumber = 1; partNumber <= task.totalParts; partNumber++) {
        if (!completedParts.has(partNumber)) queue.push(partNumber)
      }
      const workers = Array.from(
        { length: Math.min(MULTIPART_CONCURRENCY, queue.length) },
        () => multipartWorker(file, task, queue, runId, requestConfig)
      )
      await Promise.all(workers)
      if (runId !== uploadRunId || pauseRequested) throw createPauseError()

      uploadStatus.value = 'completing'
      uploadProgress.value = 99
      let url
      try {
        const completeResponse = await completeVideoMultipart(task.taskId, requestConfig)
        url = completeResponse.data
      } catch (error) {
        if (runId !== uploadRunId || pauseRequested) throw createPauseError()
        if (!isRetryableUploadError(error)) throw error
        // The response may be lost after COS has already finished merging the parts.
        try {
          const statusResponse = await getVideoMultipartStatus(task.taskId, requestConfig)
          if (statusResponse?.data?.status === 'COMPLETED') {
            url = statusResponse.data.url
          }
        } catch {
          // Keep the original completion error and the saved task for a later retry.
        }
        if (!url) throw error
      }
      if (runId !== uploadRunId || pauseRequested) throw createPauseError()
      if (typeof url !== 'string' || !url.trim()) throw new Error('合并接口未返回视频地址，请重试')
      removeSavedTask(file)
      currentTask.value = null
      finishUpload(url)
    } catch (error) {
      if (runId !== uploadRunId) return
      if (isPauseError(error) || pauseRequested) {
        uploadStatus.value = 'paused'
        return
      }
      pauseRequested = true
      controller.abort()
      abortActivePartRequests()
      refreshMultipartProgress(file)
      uploadError.value = getUploadErrorMessage(error)
      uploadStatus.value = 'paused'
      ElMessage.warning(`${uploadError.value}，任务已保留`)
      console.error('视频分片上传失败:', error)
    }
  }

  const multipartWorker = async (file, task, queue, runId, requestConfig) => {
    while (queue.length > 0) {
      if (pauseRequested || runId !== uploadRunId) throw createPauseError()
      const partNumber = queue.shift()
      await uploadPartWithRetry(file, task, partNumber, runId, requestConfig)
    }
  }

  const uploadPartWithRetry = async (file, task, partNumber, runId, requestConfig) => {
    let retry = 0
    while (true) {
      if (pauseRequested || runId !== uploadRunId) throw createPauseError()
      try {
        await uploadSinglePart(file, task, partNumber, runId, requestConfig)
        return
      } catch (error) {
        if (isPauseError(error) || pauseRequested || runId !== uploadRunId) throw createPauseError()
        if (!isRetryableUploadError(error) || retry >= MAX_PART_RETRIES) throw error
        await delay(RETRY_DELAYS[retry])
        retry += 1
      }
    }
  }

  const uploadSinglePart = async (file, task, partNumber, runId, requestConfig) => {
    const signResponse = await signVideoPart(task.taskId, partNumber, requestConfig)
    if (pauseRequested || runId !== uploadRunId) throw createPauseError()
    const partSize = Number(task.partSize)
    const start = (partNumber - 1) * partSize
    const end = Math.min(start + partSize, file.size)
    const blob = file.slice(start, end)

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      activePartRequests.set(partNumber, xhr)
      xhr.open('PUT', signResponse.data.uploadUrl, true)
      xhr.timeout = PART_UPLOAD_TIMEOUT
      const clearRequest = () => {
        if (activePartRequests.get(partNumber) === xhr) {
          activePartRequests.delete(partNumber)
          inFlightPartBytes.delete(partNumber)
          refreshMultipartProgress(file)
        }
      }
      xhr.upload.onprogress = event => {
        if (!event.lengthComputable || runId !== uploadRunId || pauseRequested) return
        const previous = inFlightPartBytes.get(partNumber) || 0
        inFlightPartBytes.set(partNumber, Math.min(blob.size, Math.max(previous, event.loaded)))
        refreshMultipartProgress(file)
      }
      xhr.onload = () => {
        clearRequest()
        if (runId !== uploadRunId || pauseRequested) return reject(createPauseError())
        if (xhr.status >= 200 && xhr.status < 300) {
          completedParts.set(partNumber, blob.size)
          refreshMultipartProgress(file)
          resolve()
        } else {
          reject(createPartUploadError(xhr, partNumber))
        }
      }
      xhr.onerror = () => {
        clearRequest()
        reject(new Error(`分片 ${partNumber} 网络错误`))
      }
      xhr.ontimeout = () => {
        clearRequest()
        reject(new Error(`分片 ${partNumber} 上传超时`))
      }
      xhr.onabort = () => {
        clearRequest()
        reject(createPauseError())
      }
      xhr.send(blob)
    })
  }

  const pauseUpload = () => {
    if (uploadMode.value !== 'multipart' || uploadStatus.value !== 'uploading') return
    pauseRequested = true
    uploadRunId += 1
    multipartController?.abort()
    uploadStatus.value = 'paused'
    abortActivePartRequests()
  }

  const continueUpload = async () => {
    if (!currentFile.value || !currentTask.value || uploadStatus.value !== 'paused') return
    await runMultipartUpload(currentFile.value, currentTask.value)
  }

  const cancelUpload = async () => {
    const file = currentFile.value
    const task = currentTask.value
    pauseRequested = true
    uploadRunId += 1
    multipartController?.abort()
    abortActivePartRequests()
    smallUploadController?.abort()
    if (task?.taskId) {
      try {
        await abortVideoMultipart(task.taskId)
      } catch (error) {
        console.error('取消COS视频上传任务失败:', error)
      }
    }
    if (file) removeSavedTask(file)
    resetUploadState()
  }

  const abortActivePartRequests = () => {
    for (const xhr of activePartRequests.values()) xhr.abort()
    activePartRequests.clear()
    inFlightPartBytes.clear()
  }

  const refreshMultipartProgress = file => {
    let sentBytes = 0
    for (const size of completedParts.values()) sentBytes += size
    for (const size of inFlightPartBytes.values()) sentBytes += size
    uploadedBytes.value = Math.min(file.size, sentBytes)
    uploadProgress.value = Math.min(99, Math.floor((uploadedBytes.value / file.size) * 100))
  }

  const finishUpload = url => {
    const fileObject = uploadedFiles.value[0]
    if (fileObject) {
      fileObject.url = url
      fileObject.progress = 100
      fileObject.status = 'done'
    }
    releasePreviewUrl()
    uploadedBytes.value = totalUploadBytes.value
    uploadProgress.value = 100
    uploadStatus.value = 'done'
    isUploading.value = false
    uploadStep.value = 'form'
    currentTask.value = null
  }

  const handleSimpleUploadFailure = error => {
    if (uploadedFiles.value[0]) uploadedFiles.value[0].status = 'error'
    isUploading.value = false
    uploadStatus.value = 'idle'
    uploadStep.value = 'upload'
    ElMessage.error('视频上传失败，请重新上传')
    console.error(error)
  }

  const resetUploadState = () => {
    releasePreviewUrl()
    uploadStep.value = 'upload'
    isUploading.value = false
    uploadStatus.value = 'idle'
    uploadMode.value = ''
    uploadError.value = ''
    uploadProgress.value = 0
    uploadedBytes.value = 0
    totalUploadBytes.value = 0
    uploadedFiles.value = []
    currentFile.value = null
    currentTask.value = null
    completedParts.clear()
    inFlightPartBytes.clear()
  }

  onBeforeUnmount(() => {
    pauseRequested = true
    uploadRunId += 1
    multipartController?.abort()
    abortActivePartRequests()
    smallUploadController?.abort()
    releasePreviewUrl()
  })

  return {
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
  }
}

function isSupportedVideo(file) {
  const extension = file.name.split('.').pop()?.toLowerCase()
  const type = normalizedContentType(file)
  return (extension === 'mp4' && type === 'video/mp4') ||
    (extension === 'mov' && type === 'video/quicktime')
}

function normalizedContentType(file) {
  if (file.type) return file.type.toLowerCase()
  return file.name.toLowerCase().endsWith('.mov') ? 'video/quicktime' : 'video/mp4'
}

function storageKey(file) {
  const fingerprint = `${file.name}:${file.size}:${file.lastModified}`
  return `${STORAGE_PREFIX}${encodeURIComponent(fingerprint)}`
}

function normalizeTask(task, file) {
  const partSize = Number(task?.partSize)
  const totalParts = Number(task?.totalParts)
  const expiresAt = Number(task?.expiresAt)
  if (!task?.taskId || !Number.isSafeInteger(partSize) || partSize <= 0 ||
    !Number.isSafeInteger(totalParts) || totalParts !== Math.ceil(file.size / partSize) ||
    !Number.isFinite(expiresAt)) {
    throw new Error('视频上传任务参数异常，请重新选择视频')
  }
  return { ...task, partSize, totalParts, expiresAt }
}

function expectedPartSize(file, task, partNumber) {
  return Math.min(Number(task.partSize), file.size - (partNumber - 1) * Number(task.partSize))
}

function isRetryableUploadError(error) {
  if (error?.errorCode) return false
  const status = Number(error?.response?.status || error?.status)
  if (status) return status >= 500 || status === 408 || status === 429
  return error?.name !== 'CanceledError' && error?.name !== 'AbortError' && !isPauseError(error)
}

function createPartUploadError(xhr, partNumber) {
  const code = xhr.responseText?.match(/<Code>([^<]+)<\/Code>/)?.[1]
  const message = xhr.responseText?.match(/<Message>([^<]+)<\/Message>/)?.[1]
  const detail = code ? `${code}${message ? `（${message}）` : ''}` : `HTTP ${xhr.status}`
  return Object.assign(new Error(`分片 ${partNumber} 上传失败：${detail}`), {
    status: xhr.status,
    cosCode: code,
    cosRequestId: xhr.responseText?.match(/<RequestId>([^<]+)<\/RequestId>/)?.[1]
  })
}

function getUploadErrorMessage(error) {
  const status = Number(error?.response?.status || error?.status || error?.errorCode)
  if (status === 401) return '登录状态失效，请登录后继续上传'
  if (status >= 500) return `上传服务暂时不可用（HTTP ${status}），请稍后继续`
  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') {
    return '服务器响应超时，请点击继续确认上传结果'
  }
  return error?.response?.data?.message || error?.message || '视频上传失败，请重试'
}

function saveTask(file, task) {
  localStorage.setItem(storageKey(file), JSON.stringify({
    taskId: task.taskId,
    partSize: Number(task.partSize),
    totalParts: Number(task.totalParts),
    expiresAt: Number(task.expiresAt),
    fileName: file.name,
    fileSize: file.size,
    lastModified: file.lastModified
  }))
}

function readSavedTask(file) {
  try {
    const raw = localStorage.getItem(storageKey(file))
    if (!raw) return null
    const task = JSON.parse(raw)
    if (!task.taskId || task.expiresAt <= Date.now() ||
      task.fileName !== file.name || Number(task.fileSize) !== file.size ||
      Number(task.lastModified) !== file.lastModified) {
      removeSavedTask(file)
      return null
    }
    return normalizeTask(task, file)
  } catch {
    removeSavedTask(file)
    return null
  }
}

function removeSavedTask(file) {
  localStorage.removeItem(storageKey(file))
}

function formatFileSize(bytes) {
  const value = Number(bytes) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function createPauseError() {
  return Object.assign(new Error('UPLOAD_PAUSED'), { code: 'UPLOAD_PAUSED' })
}

function isPauseError(error) {
  return error?.code === 'UPLOAD_PAUSED'
}

function isInvalidTaskError(error) {
  const code = error?.errorCode || error?.response?.data?.errorCode
  return code === 'OSS-21002' || code === 'OSS-21003'
}
