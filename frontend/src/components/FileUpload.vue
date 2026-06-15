<template>
  <div class="file-upload">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
          <el-tag type="isUploading ? 'warning' : 'info'">{{
            isUploading ? '上传中...' : '就绪'
          }}</el-tag>
        </div>
      </template>

      <!-- 拖拽上传区域 -->
      <el-upload
        ref="uploadRef"
        method="post"
        class="upload-area"
        drag
        :disabled="isUploading"
        :action="uploadUrl"
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-progress="handleProgress"
        :on-success="handleSuccess"
        :on-error="handleError"
        :multiple="multiple"
        :limit="limit"
        v-model:file-list="fileList"
        :on-remove="handleDelete"
        :accept="accept"
        :show-file-list="false"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text" v-if="!isUploading">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            {{ tip }}
          </div>
        </template>
      </el-upload>

      <!-- 上传进度 -->
      <div v-if="fileList.length > 0" class="progress-section">
        <div v-for="file in fileList" :key="file.uid" class="upload-item">
          <div class="item-content">
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-details">
                <div class="name-row">
                  <div class="file-name-wrapper">
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                  </div>
                  <span class="file-size">{{ formatFileSize(file.size ?? 0) }}</span>
                </div>

                <el-progress
                  :percentage="file.percentage || 0"
                  :status="
                    file.status === 'success'
                      ? 'success'
                      : file.status === 'fail'
                        ? 'exception'
                        : ''
                  "
                  :stroke-width="12"
                  class="custom-progress"
                />

                <div class="upload-meta" v-if="file.status === 'uploading'">
                  <span v-if="(file as any).uploadSpeed">速度: {{ (file as any).uploadSpeed }}</span>
                  <span v-if="(file as any).remainingTime">预计剩余: {{ (file as any).remainingTime }}</span>
                </div>
              </div>
            </div>

            <div
              class="item-actions"
              v-if="file.status !== 'success' && file.status !== 'uploading'"
            >
              <el-button
                type="danger"
                icon="Delete"
                circle
                size="small"
                @click="handleDelete(file as any)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="upload-actions">
        <el-button
          v-if="!isUploading"
          type="primary"
          :disabled="fileList.length === 0"
          @click="startUpload"
          :icon="Upload"
        >
          开始上传
        </el-button>
        <el-button v-else type="danger" @click="handleStopRequest" :icon="CloseBold">
          停止上传
        </el-button>
        <el-button
          v-if="fileList.length > 0"
          :disabled="isUploading"
          @click="handleClear"
          :icon="Delete"
        >
          清空列表
        </el-button>
      </div>
    </el-card>

    <!-- 上传成功/失败回调 -->
    <slot name="success" :files="successFiles"></slot>
    <slot name="error" :error="uploadError"></slot>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance, UploadUserFile, UploadRawFile, UploadFile } from 'element-plus'
import { UploadFilled, Upload, Delete, Document, CloseBold } from '@element-plus/icons-vue'
import { sha256 } from 'js-sha256'
import HashWorker from '../workers/hash.worker?worker'

// 开发测试配置
const FIXED_LAT = 38.914
const FIXED_LNG = 121.614

// 获取坐标（开发测试模式使用固定坐标）
const getCoordinates = (): { lat: number; lng: number } | null => {
  // 检查localStorage中是否标记为固定坐标模式
  const savedLocationStr = localStorage.getItem('userLocation')
  if (savedLocationStr) {
    try {
      const locationData = JSON.parse(savedLocationStr)
      if (locationData.useFixedCoords) {
        console.log('使用固定坐标:', { lat: FIXED_LAT, lng: FIXED_LNG })
        return { lat: FIXED_LAT, lng: FIXED_LNG }
      }
    } catch (error) {
      console.error('解析位置信息失败:', error)
    }
  }

  // 检查当前是否为固定坐标模式
  const useFixedCoords = localStorage.getItem('useFixedCoords') === 'true'
  if (useFixedCoords) {
    console.log('使用固定坐标:', { lat: FIXED_LAT, lng: FIXED_LNG })
    return { lat: FIXED_LAT, lng: FIXED_LNG }
  }

  // 返回null表示不使用固定坐标
  return null
}

interface Props {
  uploadUrl?: string
  title?: string
  multiple?: boolean
  limit?: number
  showProgress?: boolean
  showActions?: boolean
  accept?: string
  tip?: string
  maxDownloads?: number // 最大下载次数
  validMinutes?: number // 有效时长（分钟）
  needCode?: boolean // 新增：是否需要验证码（私有/公开开关）
}

const props = withDefaults(defineProps<Props>(), {
  uploadUrl: '/api/file/upload',
  title: '文件上传',
  multiple: true,
  limit: 5,
  showProgress: true,
  showActions: true,
  accept: '',
  tip: '支持 JPG、PNG、PDF、DOC、TXT 等格式，单个文件不超过 100MB',
  maxDownloads: 1,
  validMinutes: 30,
  needCode: true, // 默认开启验证码，即私有模式
})

const emit = defineEmits<{
  success: [files: UploadUserFile[]]
  error: [error: Error]
  'upload-change': [files: UploadUserFile[]]
  'upload-success': [fileData: any, length?: number]
  'require-limit-config': []
  'update:loading': [value: boolean]
}>()

// 状态
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const isUploading = ref(false)

// 成功/失败的文件
const successFiles = ref<UploadUserFile[]>([])
const uploadError = ref<Error | null>(null)

// 监听文件列表变化
watch(
  fileList,
  (newList) => {
    emit('upload-change', newList)
  },
  { deep: true },
)

// 使用 watch 实时同步状态给父组件
watch(isUploading, (newVal) => {
  emit('update:loading', newVal)
})

// 文件验证
const allowedTypes = [
  'jpg',
  'jpeg',
  'png',
  'gif',
  'bmp',
  'webp',
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'txt',
  'zip',
  'rar',
  '7z',
  'mp4',
  'avi',
  'mov',
  'mkv',
  'mp3',
  'wav',
  'flac',
  'json',
  'xml',
  'csv',
]
const FORBIDDEN_EXTS = ['jsp', 'php', 'asp', 'aspx', 'sh', 'py', 'bat']
const maxSize = 3 * 1024 * 1024 * 1024 // 3GB

// 处理文件选择
const handleFileChange = (file: UploadUserFile) => {
  const fileName = file.name
  const extension = getExtension(fileName)

  // --- 逻辑 A: 黑名单硬性拦截 (脚本文件) ---
  if (FORBIDDEN_EXTS.includes(extension)) {
    ElMessage.error(`出于安全考虑，禁止上传脚本文件: .${extension}`)

    // 手动从文件列表中移除，防止 UI 显示残留
    const index = fileList.value.findIndex((f) => f.uid === file.uid)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
    return false
  }

  // --- 逻辑 B: 大小硬性拦截 ---
  if (file.size && file.size > maxSize) {
    ElMessage.error('文件大小超过限制（3GB）')

    const index = fileList.value.findIndex((f) => f.uid === file.uid)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
    return false
  }

  // --- 逻辑 C: 白名单软提醒 (不拦截上传) ---
  // 如果不在预览白名单内（如 exe, mat, psd），只做提醒，允许其上传
  if (!allowedTypes.includes(extension)) {
    console.info(`[GeoFile] 提示：文件格式 .${extension} 暂不支持在线预览，但可以正常传输和取件。`)
    // 这里不需要 return false，因为它不是致命错误
  }

  return true
}

// 分片计算 SHA256
const calculateSha256 = (file: File, onProgress?: (p: number) => void): Promise<string> => {
  return new Promise((resolve, reject) => {
    // 1. 创建 Worker 实例
    const worker = new HashWorker()

    // 2. 发送文件对象给 Worker 开始计算
    worker.postMessage({ file })

    // 3. 监听来自 Worker 的消息
    worker.onmessage = (e: MessageEvent) => {
      const { type, data } = e.data

      switch (type) {
        case 'progress':
          // data 是 0-100 的整数
          if (onProgress) onProgress(data)
          break
        case 'success':
          // data 是计算好的十六进制字符串
          resolve(data)
          worker.terminate() // 任务完成，必须销毁线程
          break
        case 'error':
          reject(new Error(data))
          worker.terminate() // 出错也销毁线程
          break
      }
    }

    // 4. 监听 Worker 本身的错误（比如脚本加载失败）
    worker.onerror = (err) => {
      console.error('Worker error:', err)
      reject(new Error('Hash calculation worker failed'))
      worker.terminate()
    }
  })
}

// 采样哈希：取首、中、尾各 1MB 快速计算一个特征值
const calculateSampleHash = async (file: File): Promise<string> => {
  const size = file.size
  const sampleLimit = 3 * 1024 * 1024 // 3MB 阈值
  let combinedBlob: Blob

  if (size <= sampleLimit) {
    // 小文件直接全量读取，结果 100% 准确
    combinedBlob = file
  } else {
    // 大文件取首、中、尾各 1MB
    const offset = 1 * 1024 * 1024
    combinedBlob = new Blob([
      file.slice(0, offset),
      file.slice(Math.floor(size / 2) - 512 * 1024, Math.floor(size / 2) + 512 * 1024),
      file.slice(size - offset, size),
    ])
  }

  const arrayBuffer = await combinedBlob.arrayBuffer()
  return sha256(arrayBuffer)
}

/*const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  // 检查配置限制逻辑 (保持原样)
  if (props.maxDownloads === 0 || props.validMinutes === 0) {
    emit('require-limit-config')
    return
  }

  isUploading.value = true
  emit('update:loading', true)
  let sharedUploadToken = ''

  try {
    // 1. 获取位置信息和 Token (保持原逻辑)
    const savedLocationStr = localStorage.getItem('userLocation')
    const locationData = JSON.parse(savedLocationStr || '{}')

    // 获取 Token 的请求 (建议保持 fetch 或改为 axios)
    //const tokenRes = await fetch('/api/verification/token/download')
    //const { data: token } = await tokenRes.json()

    // 计算总上传大小，用于进度权重分配
    const totalBatchSize = fileList.value.reduce((sum, f) => sum + (f.size || 0), 0)

    // 2. 准备 FormData
    const formData = new FormData()
    let hasFilesToUpload = false // 标记是否还有需要真正网络上传的文件

    // 遍历文件列表进行“预检”
    for (const file of fileList.value) {
      if (!file.raw) continue

      file.status = 'uploading'

      // --- 1. 采样快检 (毫秒级) ---
      // 计算采样哈希（几乎瞬发）

      const currentSampleHash = await calculateSampleHash(file.raw as File)

      const quickCheck = await axios.post('/api/file/quick-check', {
        size: file.raw.size,
        sampleHash: currentSampleHash,
      })
      // 【核心修改】无论快检命中与否，都计算全量 Hash
      // 这样保证了：快检命中 -> 尝试秒传；快检不命中 -> 物理上传时带着 Hash 减少服务器计算
      const fileHash = await calculateSha256(file.raw as File, (p) => {
        file.percentage = Math.floor(p * 0.4)
      })

      let isSecSuccess = false

      if (quickCheck.data.data.canPotentiallySecUpload && fileHash) {
        const checkRes = await axios.post('/api/file/sec-upload', {
          hash: fileHash,
          fileName: file.raw.name,
          // 携带地理位置和配置，确保秒传也能生成提取码
          lat: locationData.useFixedCoords ? FIXED_LAT : locationData.lat,
          lng: locationData.useFixedCoords ? FIXED_LNG : locationData.lng,
          maxDownloads: props.maxDownloads,
          validMinutes: props.validMinutes,
          needCode: props.needCode,
          uploadToken: sharedUploadToken,
        })

        if (checkRes.data.code === 200 && checkRes.data.data) {
          // --- 分支 A: 秒传成功 ---
          file.status = 'success'
          file.percentage = 100
          // 如果这是第一个成功的秒传，记录下后端生成的 Token
          // 这样后续的文件（无论是秒传还是普通上传）都能沿用它
          if (!sharedUploadToken) {
            sharedUploadToken = checkRes.data.data.uploadToken
          }
          // 直接触发成功回调，传入后端返回的 FileVO (包含 downloadCode)
          emit('upload-success', checkRes.data, fileList.value.length)
          isSecSuccess = true
        }
      }
      if (!isSecSuccess) {
        // --- 分支 B: 秒传失败，准备正常上传 ---
        formData.append('files', file.raw)
        formData.append('sampleHashes', currentSampleHash)
        // 新增：将算好的全量 Hash 传给后端
        formData.append('fullHashes', fileHash)
        hasFilesToUpload = true
      }
    }
    if (hasFilesToUpload) {
      // 将 sharedUploadToken 塞进 FormData
      // 如果之前的秒传已经生成了 Token，后端会沿用；如果还没生成，后端会在这里生成
      if (sharedUploadToken) {
        formData.append('providedToken', sharedUploadToken)
      }
      // 添加经纬度和业务参数
      formData.append(
        'lat',
        (locationData.useFixedCoords ? FIXED_LAT : locationData.lat).toString(),
      )
      formData.append(
        'lng',
        (locationData.useFixedCoords ? FIXED_LNG : locationData.lng).toString(),
      )
      // --- 业务参数处理 ---
      if (props.maxDownloads !== undefined && props.maxDownloads > 0) {
        formData.append('maxDownloads', props.maxDownloads.toString())
      }
      if (props.validMinutes !== undefined && props.validMinutes > 0) {
        formData.append('validMinutes', props.validMinutes.toString())
      }
      if (props.needCode !== undefined) {
        formData.append('needCode', props.needCode.toString())
        console.log('添加验证码开关到表单:', { needCode: props.needCode })
      }
      console.log('发起批量上传，文件数量:', fileList.value.length)

      // 3. 使用 Axios 执行请求并监听进度
      const response = await axios.post('/api/file/upload/batch-with-location', formData, {
        // --- 核心：监听上传进度 ---
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && totalBatchSize > 0) {
            const totalLoaded = progressEvent.loaded
            let accumulatedSize = 0

            fileList.value.forEach((file) => {
              // 如果文件已经通过秒传成功了，跳过进度计算
              if (file.status === 'success') {
                accumulatedSize += file.size || 0
                return
              }
              const fileSize = file.size || 0
              const startThreshold = accumulatedSize
              const endThreshold = accumulatedSize + fileSize

              if (totalLoaded >= endThreshold) {
                // 该文件已完全上传
                file.percentage = 100
              } else if (totalLoaded > startThreshold) {
                // 进度正落在此文件区间内
                const fileLoaded = totalLoaded - startThreshold
                //file.percentage = Math.round((fileLoaded / fileSize) * 100)
                // 映射逻辑：将上传进度 (0-100) 映射到总进度的 (40-100)
                const uploadP = Math.round((fileLoaded / fileSize) * 100)
                file.percentage = 40 + Math.floor(uploadP * 0.6)
              } else {
                // 还没排到该文件
                file.percentage = 40
              }
              accumulatedSize += fileSize
            })
          }
        },
      })

      // 4. 处理响应结果
      const result = response.data
      if (result.code === 200) {
        //ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)

        fileList.value.forEach((f) => {
          f.status = 'success'
          f.percentage = 100
        })
        // 如果前面没生成 Token，这里是最后的同步机会
        if (!sharedUploadToken) sharedUploadToken = response.data.data[0].uploadToken
        if (result.data) emit('upload-success', result, fileList.value.length)
        emit('success', fileList.value)
      } else {
        throw new Error(result.message || '上传失败')
      }
    }
  } catch (e: any) {
    console.error('上传错误:', e)
    fileList.value.forEach((f) => {
      if (f.status === 'uploading') f.status = 'fail'
    })
    ElMessage.error(e.message || '上传过程中发生错误')
    emit('error', e)
  } finally {
    isUploading.value = false
    emit('update:loading', false)
  }
}*/
const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  // 检查配置限制逻辑 (保持原样)
  if (props.maxDownloads === 0 || props.validMinutes === 0) {
    emit('require-limit-config')
    return
  }

  isUploading.value = true
  emit('update:loading', true)
  let sharedUploadToken = ''

  try {
    // 1. 获取位置信息
    const savedLocationStr = localStorage.getItem('userLocation')
    const locationData = JSON.parse(savedLocationStr || '{}')
    const latVal = locationData.useFixedCoords ? FIXED_LAT : locationData.lat
    const lngVal = locationData.useFixedCoords ? FIXED_LNG : locationData.lng

    // 2. 准备普通批量上传的 FormData（用于装不需要分片的小文件）
    const batchFormData = new FormData()
    let hasSmallFilesToUpload = false 

    // 定义触发分片上传的阈值：100MB（超过100M的文件走分片，小于走普通批量，可根据喜好调整）
    const CHUNK_THRESHOLD = 100 * 1024 * 1024 
    const CHUNK_SIZE = 20 * 1024 * 1024 // 每个切片大小定为 20MB

    // 遍历文件列表进行“预检”与分流处理
    for (const file of fileList.value) {
      if (!file.raw) continue

      file.status = 'uploading'

      // --- 采样快检 ---
      const currentSampleHash = await calculateSampleHash(file.raw as File)

      const quickCheck = await axios.post('/api/file/quick-check', {
        size: file.raw.size,
        sampleHash: currentSampleHash,
      })

      // 计算全量 Hash (进度映射到 0% - 40%)
      const fileHash = await calculateSha256(file.raw as File, (p) => {
        file.percentage = Math.floor(p * 0.4)
      })

      let isSecSuccess = false

      // 尝试秒传
      if (quickCheck.data.data.canPotentiallySecUpload && fileHash) {
        const checkRes = await axios.post('/api/file/sec-upload', {
          hash: fileHash,
          fileName: file.raw.name,
          lat: latVal,
          lng: lngVal,
          maxDownloads: props.maxDownloads,
          validMinutes: props.validMinutes,
          needCode: props.needCode,
          uploadToken: sharedUploadToken,
        })

        if (checkRes.data.code === 200 && checkRes.data.data) {
          // 秒传成功分支
          file.status = 'success'
          file.percentage = 100
          if (!sharedUploadToken) {
            sharedUploadToken = checkRes.data.data.uploadToken
          }
          emit('upload-success', checkRes.data, fileList.value.length)
          isSecSuccess = true
        }
      }

      // --- 秒传失败，进入物理上传流程 ---
      if (!isSecSuccess) {
        const fileRaw = file.raw as File

        if (fileRaw.size > CHUNK_THRESHOLD) {
          console.log(`文件 [${fileRaw.name}] 启动分片【并发】上传模式...`)
          const totalChunks = Math.ceil(fileRaw.size / CHUNK_SIZE)

          // 🌟🌟🌟【并发核心修改点：构建分片任务队列】
          const chunkTasks: number[] = []
          for (let i = 0; i < totalChunks; i++) {
            chunkTasks.push(i)
          }

          // 记录当前已成功完成的切片数，用来平滑进度条
          let completedChunksCount = 0
          // 设定同时上传的并发数（根据国内网络情况，3~4 个并发是最优解，不容易被 CF 或 Nginx 掐断）
          const CONCURRENCY_LIMIT = 4 

          // 异步执行器
          const runUploadWorker = async () => {
            while (chunkTasks.length > 0) {
              const i = chunkTasks.shift() // 抢占式获取一个分片编号
              if (i === undefined) break

              const start = i * CHUNK_SIZE
              const end = Math.min(fileRaw.size, start + CHUNK_SIZE)
              const chunkBlob = fileRaw.slice(start, end)

              const chunkFormData = new FormData()
              chunkFormData.append('chunk', chunkBlob, fileRaw.name)
              chunkFormData.append('identifier', fileHash)
              chunkFormData.append('chunkNumber', i.toString())

              //  多个分片同时在网络通道中
              await axios.post('/api/file/upload/chunk', chunkFormData)

              completedChunksCount++
              // 平滑进度条：映射到 (40% - 90%)
              const chunkProgress = completedChunksCount / totalChunks
              file.percentage = 40 + Math.floor(chunkProgress * 50)
            }
          }

          //  开启多通道跑批并发
          const workers = []
          for (let w = 0; w < Math.min(CONCURRENCY_LIMIT, totalChunks); w++) {
            workers.push(runUploadWorker())
          }
          await Promise.all(workers) // 等待所有分片线程并发全部冲完

          // 所有分片传输完毕，向后端发起【收网合并】指令
          console.log(`文件 [${fileRaw.name}] 分片并发上传完毕，发起高效合并...`)
          const mergeResponse = await axios.post('/api/file/upload/merge', {
            identifier: fileHash,
            sampleHash: currentSampleHash,
            fileName: fileRaw.name,
            lat: latVal,
            lng: lngVal,
            maxDownloads: props.maxDownloads,
            validMinutes: props.validMinutes,
            needCode: props.needCode,
            providedToken: sharedUploadToken || null
          })

          const mergeResult = mergeResponse.data
          if (mergeResult.code === 200) {
            file.status = 'success'
            file.percentage = 100
            
            if (!sharedUploadToken && mergeResult.data && mergeResult.data.length > 0) {
              sharedUploadToken = mergeResult.data[0].uploadToken
            }
            emit('upload-success', mergeResult, fileList.value.length)
          } else {
            throw new Error(mergeResult.message || `文件 [${fileRaw.name}] 合并失败`)
          }

        } else {
          batchFormData.append('files', fileRaw)
          batchFormData.append('sampleHashes', currentSampleHash)
          batchFormData.append('fullHashes', fileHash)
          hasSmallFilesToUpload = true
        }
        /*if (fileRaw.size > CHUNK_THRESHOLD) {
          // ========= 【核心新增】分支 B1: 大文件执行前端切片上传 =========
          console.log(`文件 [${fileRaw.name}] 超过阈值，启动分片上传模式...`)
          const totalChunks = Math.ceil(fileRaw.size / CHUNK_SIZE)

          for (let i = 0; i < totalChunks; i++) {
            const start = i * CHUNK_SIZE
            const end = Math.min(fileRaw.size, start + CHUNK_SIZE)
            const chunkBlob = fileRaw.slice(start, end)

            const chunkFormData = new FormData()
            // 必须与后端 @RequestParam 命名的参数完全对应
            chunkFormData.append('chunk', chunkBlob, fileRaw.name)
            chunkFormData.append('identifier', fileHash)
            chunkFormData.append('chunkNumber', i.toString())

            // 发送单片
            await axios.post('/api/file/upload/chunk', chunkFormData)

            // 平滑进度条：分片上传进度映射到总进度的 (40% - 90%)，预留10%给后端合并
            const chunkProgress = (i + 1) / totalChunks
            file.percentage = 40 + Math.floor(chunkProgress * 50)
          }

          // 所有分片传输完毕，向后端发起【收网合并】指令
          console.log(`文件 [${fileRaw.name}] 分片传输完毕，发起合并请求...`)
          const mergeResponse = await axios.post('/api/file/upload/merge', {
            identifier: fileHash,
            sampleHash: currentSampleHash,
            fileName: fileRaw.name,
            lat: latVal,
            lng: lngVal,
            maxDownloads: props.maxDownloads,
            validMinutes: props.validMinutes,
            needCode: props.needCode,
            providedToken: sharedUploadToken || null // 沿用已生成的批量Token
          })

          const mergeResult = mergeResponse.data
          if (mergeResult.code === 200) {
            file.status = 'success'
            file.percentage = 100
            
            // 提取批次 Token
            if (!sharedUploadToken && mergeResult.data && mergeResult.data.length > 0) {
              sharedUploadToken = mergeResult.data[0].uploadToken
            }
            emit('upload-success', mergeResult, fileList.value.length)
          } else {
            throw new Error(mergeResult.message || `文件 [${fileRaw.name}] 合并失败`)
          }

        } else {
          // ========= 分支 B2: 小文件依旧塞进 batchFormData，走原有的批量接口 =========
          batchFormData.append('files', fileRaw)
          batchFormData.append('sampleHashes', currentSampleHash)
          batchFormData.append('fullHashes', fileHash)
          hasSmallFilesToUpload = true
        }*/
      }
    }

    // 3. 处理打包在 batchFormData 里的剩余小文件（如果有的话）
    if (hasSmallFilesToUpload) {
      if (sharedUploadToken) {
        batchFormData.append('providedToken', sharedUploadToken)
      }
      batchFormData.append('lat', latVal.toString())
      batchFormData.append('lng', lngVal.toString())
      
      if (props.maxDownloads !== undefined && props.maxDownloads > 0) {
        batchFormData.append('maxDownloads', props.maxDownloads.toString())
      }
      if (props.validMinutes !== undefined && props.validMinutes > 0) {
        batchFormData.append('validMinutes', props.validMinutes.toString())
      }
      if (props.needCode !== undefined) {
        batchFormData.append('needCode', props.needCode.toString())
      }

      console.log('发起剩余小文件的批量传统上传...')
      
      const totalBatchSize = fileList.value
        .filter(f => f.status === 'uploading' && (f.raw?.size || 0) <= CHUNK_THRESHOLD)
        .reduce((sum, f) => sum + (f.size || 0), 0)

      const response = await axios.post('/api/file/upload/batch-with-location', batchFormData, {
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && totalBatchSize > 0) {
            const totalLoaded = progressEvent.loaded
            let accumulatedSize = 0

            fileList.value.forEach((file) => {
              // 过滤掉秒传和已经通过分片上传成功的文件
              if (file.status === 'success' || (file.raw?.size || 0) > CHUNK_THRESHOLD) {
                if (file.status === 'success') accumulatedSize += file.size || 0
                return
              }
              const fileSize = file.size || 0
              const startThreshold = accumulatedSize
              const endThreshold = accumulatedSize + fileSize

              if (totalLoaded >= endThreshold) {
                file.percentage = 100
              } else if (totalLoaded > startThreshold) {
                const fileLoaded = totalLoaded - startThreshold
                const uploadP = Math.round((fileLoaded / fileSize) * 100)
                file.percentage = 40 + Math.floor(uploadP * 0.6)
              } else {
                file.percentage = 40
              }
              accumulatedSize += fileSize
            })
          }
        },
      })

      const result = response.data
      if (result.code === 200) {
        fileList.value.forEach((f) => {
          if (f.status === 'uploading') {
            f.status = 'success'
            f.percentage = 100
          }
        })
        if (!sharedUploadToken) sharedUploadToken = response.data.data[0].uploadToken
        if (result.data) emit('upload-success', result, fileList.value.length)
        emit('success', fileList.value)
      } else {
        throw new Error(result.message || '传统批量上传部分失败')
      }
    }

    // 如果运行到最后，列表里的所有文件都被妥善处理成功了
    if (fileList.value.every(f => f.status === 'success')) {
      emit('success', fileList.value)
    }

  } catch (e: any) {
    console.error('分片或批量上传捕获异常:', e)
    fileList.value.forEach((f) => {
      if (f.status === 'uploading') f.status = 'fail'
    })
    ElMessage.error(e.message || '文件上传或合并过程中发生错误')
    emit('error', e)
  } finally {
    isUploading.value = false
    emit('update:loading', false)
  }
}

// 处理上传进度
const handleProgress = (_event: ProgressEvent, file: UploadUserFile) => {
  file.status = 'uploading'
}

// 上传成功
const handleSuccess = (response: { code: number; data?: any }, file: UploadUserFile) => {
  if (response.code === 200) {
    file.status = 'success'

    // 将上传文件信息（包含uploadToken）传递给父组件
    if (response.data) {
      emit('upload-success', response.data)
    }
  } else {
    file.status = 'fail'
  }
}

// 上传失败
const handleError = (err: Error) => {
  uploadError.value = err
  ElMessage.error('上传失败')
}

// 取消上传
const handleCancelUpload = (uid: UploadUserFile['uid']) => {
  if (!uploadRef.value) return
  const file = fileList.value.find((f) => f.uid === uid)
  if (file) {
    uploadRef.value.abort(file as UploadFile)
  }
}

// 删除文件
const handleDelete = (file: UploadFile) => {
  // 核心：直接过滤数组，Vue 会自动处理响应式更新
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)

  // 提示
  ElMessage({
    message: `已移出待上传列表`,
    type: 'info',
    duration: 2000,
  })
}

// 清空列表
const handleClear = () => {
  fileList.value = []
}

// 停止并刷新的逻辑
const handleStopRequest = () => {
  ElMessageBox.confirm('确定要停止上传吗？这会清空当前进度并刷新页面以确保状态重置。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '点错了',
    type: 'warning',
    buttonSize: 'default',
  })
    .then(() => {
      // 用户点击了确定，直接刷新页面
      // 这是最干净的“取消”方式，会自动中断所有 HTTP 请求和计算任务
      window.location.reload()
    })
    .catch(() => {
      // 用户点击了取消，什么也不做，上传继续
      console.log('用户取消了停止操作')
    })
}

// 计算进度
const calculateProgress = (file: UploadUserFile) => {
  if (file.status === 'success') return 100
  if (file.status === 'fail') return 0
  return 0
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 获取文件扩展名
const getExtension = (filename: string) => {
  if (!filename) return ''

  // 1. 去除文件名首尾空格和末尾的点（防御 Windows 系统下的绕过漏洞）
  const trimmedName = filename.trim().replace(/\.+$/, '')

  // 2. 查找最后一个点的位置
  const lastDotIndex = trimmedName.lastIndexOf('.')

  // 3. 确保点不是文件名的第一个字符（隐藏文件），且点后面有内容
  if (lastDotIndex <= 0 || lastDotIndex === trimmedName.length - 1) {
    return ''
  }

  // 4. 截取并转为小写
  return trimmedName.substring(lastDotIndex + 1).toLowerCase()
}

// 获取下载Token
async function getDownloadToken() {
  const tokenRes = await fetch('/api/verification/token/download')
  const { data: token } = await tokenRes.json()
  return token
}

defineExpose({
  clear: handleClear,
  startUpload,
})
</script>

<style scoped lang="scss">
.file-upload {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  margin-bottom: 20px;
}

.progress-section {
  margin-top: 20px;
  max-height: 400px;
  overflow-y: auto;

  .upload-item {
    background: #ffffff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 12px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    }

    .item-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .file-info {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      flex: 1;

      .file-icon {
        font-size: 24px;
        color: #409eff;
        margin-top: 4px;
      }

      .file-details {
        flex: 1;
        min-width: 0;

        .name-row {
          display: flex;
          align-items: flex-start;
          gap: 12px;

          .file-name-wrapper {
            flex: 1;
            min-width: 0;

            .file-name {
              display: inline-block;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              font-size: 14px;
              font-weight: 500;
              color: #303133;

              white-space: normal; // 允许换行
              word-break: break-all; // 强制长单词/链接换行，防止撑开容器
              line-height: 1.4; // 增加行高防止多行时太挤
            }
          }

          .file-size {
            flex-shrink: 0;
            font-size: 12px;
            color: #909399;
            background: #f0f2f5;
            padding: 2px 6px;
            border-radius: 4px;
            white-space: nowrap;
          }
        }

        .custom-progress {
          margin: 8px 0;
        }

        .upload-meta {
          display: flex;
          gap: 16px;
          font-size: 11px;
          color: #67c23a; // 使用绿色强调速度信息
        }
      }
    }

    .item-actions {
      flex-shrink: 0;
      margin-left: 12px;
      display: flex;
      justify-content: center;
      align-items: center;
      align-self: flex-start;
      &.success-icon {
        width: 24px; // 成功的勾可以给一个较窄的宽度
      }
    }
  }
}

.upload-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
}
</style>
