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
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
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
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
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
                  <span v-if="file.uploadSpeed">速度: {{ file.uploadSpeed }}</span>
                  <span v-if="file.remainingTime">预计剩余: {{ file.remainingTime }}</span>
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
                @click="handleDelete(file)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="upload-actions">
        <el-button
          type="primary"
          :loading="isUploading"
          :disabled="fileList.length === 0"
          @click="startUpload"
          :icon="Upload"
        >
          开始上传
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
import { ElMessage } from 'element-plus'
import type { UploadInstance, UploadUserFile, UploadRawFile, UploadFile } from 'element-plus'
import { UploadFilled, Upload, Delete, Document } from '@element-plus/icons-vue'

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
  'upload-success': [fileData: any]
  'require-limit-config': []
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
const maxSize = 5 * 1024 * 1024 * 1024 // 5GB

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
    ElMessage.error('文件大小超过限制（5GB）')

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

// 开始上传 (批量改写版)
/*const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  // 检查是否需要配置下载限制
  if (props.maxDownloads === 0 || props.validMinutes === 0) {
    emit('require-limit-config')
    return
  }

  isUploading.value = true

  try {
    // 1. 获取位置信息逻辑 (保持原样)
    const savedLocationStr = localStorage.getItem('userLocation')
    let locationData = null
    if (savedLocationStr) {
      try {
        locationData = JSON.parse(savedLocationStr)
        if (locationData.useFixedCoords) {
          ElMessage.warning('开发测试模式：使用固定坐标上传文件')
        }
      } catch (error) {
        console.error('解析位置信息失败:', error)
      }
    }

    if (!locationData || !locationData.lat || !locationData.lng) {
      ElMessage.info('未检测到位置信息，文件上传成功但不记录位置')
    }

    // 2. 获取Token (保持原样)
    const tokenRes = await fetch('/api/file/generate-download-token/{fileId}')
    const { data: token } = await tokenRes.json()

    // 3. 统一使用批量上传接口
    const uploadUrl = '/api/file/upload/batch-with-location'

    // 4. 创建 FormData 并封装所有数据
    const formData = new FormData()

    // --- 核心修改：将所有文件添加到同一个 Key (files) 中 ---
    fileList.value.forEach((file) => {
      if (file.raw) {
        formData.append('files', file.raw) // 对应后端 @RequestParam("files")
      }
    })

    // --- 位置参数处理 (保持原逻辑) ---
    let uploadLat = locationData?.lat
    let uploadLng = locationData?.lng
    let uploadRadius = locationData?.radius || 1000

    if (locationData?.useFixedCoords) {
      uploadLat = FIXED_LAT
      uploadLng = FIXED_LNG
      uploadRadius = 1000
    }

    if (uploadLat && uploadLng) {
      formData.append('lat', uploadLat.toString())
      formData.append('lng', uploadLng.toString())
      formData.append('radius', uploadRadius.toString())
    }

    // --- 业务参数处理 (保持原逻辑) ---
    if (props.maxDownloads !== undefined && props.maxDownloads > 0) {
      formData.append('maxDownloads', props.maxDownloads.toString())
    }
    if (props.validMinutes !== undefined && props.validMinutes > 0) {
      formData.append('validMinutes', props.validMinutes.toString())
    }
    if (props.needCode !== undefined) {
      formData.append('needCode', props.needCode.toString())
    }
    if (props.needCode !== undefined) {
      formData.append('needCode', props.needCode.toString())
      console.log('添加验证码开关到表单:', { needCode: props.needCode })
    }
    console.log('发起批量上传，文件数量:', fileList.value.length)

    // 5. 执行单次批量上传请求
    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    })

    const result: { code: number; data?: any[]; message?: string } = await response.json()

    if (result.code === 200) {
      // 批量上传成功
      ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)

      // 传递完整 Result，便于父组件从 data 中遍历全部 FileVO 并保存每个 uploadToken
      if (result.data && result.data.length > 0) {
        emit('upload-success', result)
      }

      emit('success', fileList.value)
    } else {
      ElMessage.error(`上传失败: ${result.message || '未知错误'}`)
      // 将所有文件标记为失败
      fileList.value.forEach((f) => (f.status = 'fail'))
    }
  } catch (e) {
    const err = e as Error
    uploadError.value = err
    ElMessage.error('上传过程中发生错误: ' + err.message)
    emit('error', err)
  } finally {
    isUploading.value = false
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

  try {
    // 1. 获取位置信息和 Token (保持原逻辑)
    const savedLocationStr = localStorage.getItem('userLocation')
    const locationData = JSON.parse(savedLocationStr || '{}')

    // 获取 Token 的请求 (建议保持 fetch 或改为 axios)
    const tokenRes = await fetch('/api/verification/token/download')
    const { data: token } = await tokenRes.json()

    // 计算总上传大小，用于进度权重分配
    const totalBatchSize = fileList.value.reduce((sum, f) => sum + (f.size || 0), 0)

    // 2. 准备 FormData
    const formData = new FormData()
    fileList.value.forEach((file) => {
      if (file.raw) {
        formData.append('files', file.raw)
        // 初始化进度条为 0，状态为上传中
        file.status = 'uploading'
        file.percentage = 0
      }
    })

    // 添加经纬度和业务参数 (保持原逻辑)
    formData.append('lat', (locationData.useFixedCoords ? FIXED_LAT : locationData.lat).toString())
    formData.append('lng', (locationData.useFixedCoords ? FIXED_LNG : locationData.lng).toString())
    // ... 其他参数 append 同你之前的逻辑
    // --- 业务参数处理 (保持原逻辑) ---
    if (props.maxDownloads !== undefined && props.maxDownloads > 0) {
      formData.append('maxDownloads', props.maxDownloads.toString())
    }
    if (props.validMinutes !== undefined && props.validMinutes > 0) {
      formData.append('validMinutes', props.validMinutes.toString())
    }
    if (props.needCode !== undefined) {
      formData.append('needCode', props.needCode.toString())
    }
    if (props.needCode !== undefined) {
      formData.append('needCode', props.needCode.toString())
      console.log('添加验证码开关到表单:', { needCode: props.needCode })
    }
    console.log('发起批量上传，文件数量:', fileList.value.length)

    // 3. 使用 Axios 执行请求并监听进度
    const response = await axios.post('/api/file/upload/batch-with-location', formData, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      // --- 核心：监听上传进度 ---
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && totalBatchSize > 0) {
          //const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)

          // 更新所有正在上传的文件的进度条
          /*fileList.value.forEach((file) => {
            if (file.status === 'uploading') {
              file.percentage = percentCompleted
            }
          })*/
          const totalLoaded = progressEvent.loaded
          let accumulatedSize = 0

          fileList.value.forEach((file) => {
            const fileSize = file.size || 0
            const startThreshold = accumulatedSize
            const endThreshold = accumulatedSize + fileSize

            if (totalLoaded >= endThreshold) {
              // 该文件已完全上传
              file.percentage = 100
            } else if (totalLoaded > startThreshold) {
              // 进度正落在此文件区间内
              const fileLoaded = totalLoaded - startThreshold
              file.percentage = Math.round((fileLoaded / fileSize) * 100)
            } else {
              // 还没排到该文件
              file.percentage = 0
            }
            accumulatedSize += fileSize
          })
        }
      },
    })

    // 4. 处理响应结果
    const result = response.data
    if (result.code === 200) {
      ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)

      fileList.value.forEach((f) => {
        f.status = 'success'
        f.percentage = 100
      })

      if (result.data) emit('upload-success', result)
      emit('success', fileList.value)
    } else {
      throw new Error(result.message || '上传失败')
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
