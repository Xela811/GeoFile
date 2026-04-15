<template>
  <div class="file-upload">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
          <el-tag type="info">{{ isUploading ? '上传中...' : '就绪' }}</el-tag>
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
        :on-remove="handleRemove"
        :accept="accept"
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
      <div v-if="showProgress" class="progress-section">
        <div v-for="file in fileList" :key="file.uid" class="upload-item">
          <div class="file-info">
            <el-icon><Document /></el-icon>
            <div class="file-name">{{ file.name }}</div>
          </div>
          <el-progress
            :percentage="calculateProgress(file)"
            :status="file.status"
            :indeterminate="file.status === 'uploading'"
            :stroke-width="20"
          />
          <el-button
            v-if="file.status === 'uploading'"
            type="danger"
            size="small"
            text
            @click="handleCancelUpload(file.uid)"
          >
            取消
          </el-button>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div v-if="showActions" class="upload-actions">
        <el-button
          type="primary"
          :loading="isUploading"
          :disabled="fileList.length === 0"
          @click="startUpload"
          :icon="Upload"
        >
          开始上传
        </el-button>
        <el-button v-if="fileList.length > 0" @click="handleClear" :icon="Delete">
          清空列表
        </el-button>
      </div>

      <!-- 分片上传对话框 -->
      <el-dialog
        v-model="showChunkDialog"
        title="大文件分片上传"
        width="500px"
        :close-on-click-modal="false"
      >
        <div class="chunk-upload-container">
          <el-form label-width="100px">
            <el-form-item label="文件名">
              <span>{{ currentFile?.name }}</span>
            </el-form-item>
            <el-form-item label="文件大小">
              <span>{{ formatFileSize(currentFile?.size || 0) }}</span>
            </el-form-item>
            <el-form-item label="总分片数">
              <el-input-number v-model="chunkUploadInfo.totalChunks" :min="1" readonly />
            </el-form-item>
            <el-form-item label="已上传">
              <el-progress
                :percentage="chunkUploadInfo.progress"
                :status="chunkUploadInfo.status"
              />
              <div class="chunk-info">
                已上传: {{ chunkUploadInfo.uploadedChunks }} / {{ chunkUploadInfo.totalChunks }}
              </div>
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <el-button @click="showChunkDialog = false">取消</el-button>
          <el-button type="primary" @click="handleStartChunkUpload" :loading="isChunkUploading">
            开始分片上传
          </el-button>
        </template>
      </el-dialog>
    </el-card>

    <!-- 上传成功/失败回调 -->
    <slot name="success" :files="successFiles"></slot>
    <slot name="error" :error="uploadError"></slot>
  </div>
</template>

<script setup lang="ts">
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
const isChunkUploading = ref(false)

// 分片上传状态
const showChunkDialog = ref(false)
const currentFile = ref<UploadRawFile | null>(null)
const chunkUploadInfo = ref({
  uploadId: '',
  totalChunks: 0,
  uploadedChunks: 0,
  progress: 0,
  status: 'uploading' as 'uploading' | 'completed' | 'error',
})

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
const maxSize = 1024 * 1024 * 1024 // 1GB

// 处理文件选择
const handleFileChange = (file: UploadUserFile) => {
  /*// 验证文件类型
  const extension = getExtension(file.name)
  if (!allowedTypes.includes(extension)) {
    ElMessage.error(`不支持的文件类型: ${extension}`)
    return false
  }

  if (file.size && file.size > maxSize) {
    ElMessage.error('文件大小超过限制（100MB）')
    return false
  }

  return true*/
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
    ElMessage.error('文件大小超过限制（1GB）')

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

// 开始上传
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
    // 获取位置信息
    const savedLocationStr = localStorage.getItem('userLocation')
    let locationData = null
    if (savedLocationStr) {
      try {
        locationData = JSON.parse(savedLocationStr)
        console.log('从 localStorage 获取位置信息:', locationData)

        // 如果是固定坐标模式，添加警告
        if (locationData.useFixedCoords) {
          ElMessage.warning('开发测试模式：使用固定坐标上传文件，位置可能不准确')
        }
      } catch (error) {
        console.error('解析位置信息失败:', error)
        locationData = null
      }
    }

    // 如果没有位置信息，显示提示
    if (!locationData || !locationData.lat || !locationData.lng) {
      ElMessage.info('未检测到位置信息，文件上传成功但不记录位置')
    }

    // 获取Token
    const tokenRes = await fetch('/api/file/generate-download-token/{fileId}')
    const { data: token } = await tokenRes.json()

    // 决定使用哪个上传接口
    const useLocationApi = locationData && locationData.lat && locationData.lng
    const uploadUrl = useLocationApi ? '/api/file/upload-with-location' : props.uploadUrl

    console.log('使用上传接口:', uploadUrl)
    console.log('位置信息:', locationData)

    // 逐个上传文件
    const currentSuccessFiles: UploadUserFile[] = []

    for (let i = 0; i < fileList.value.length; i++) {
      const file = fileList.value[i]
      if (!file) continue

      try {
        // 创建FormData
        const formData = new FormData()
        formData.append('file', file.raw!)

        // --- 核心修改：如果存在位置信息，则附加到表单中 ---
        let uploadLat = locationData?.lat
        let uploadLng = locationData?.lng
        let uploadRadius = locationData?.radius || 1000

        // 如果在固定坐标模式，使用固定坐标
        if (locationData?.useFixedCoords) {
          uploadLat = FIXED_LAT
          uploadLng = FIXED_LNG
          uploadRadius = 1000 // 固定模式使用默认1000米
          console.log('使用固定坐标添加到表单:', {
            lat: uploadLat,
            lng: uploadLng,
            radius: uploadRadius,
          })
        } else if (locationData) {
          console.log('添加位置参数到表单:', {
            lat: locationData.lat,
            lng: locationData.lng,
            radius: locationData.radius,
          })
        }

        if (uploadLat && uploadLng) {
          formData.append('lat', uploadLat.toString())
          formData.append('lng', uploadLng.toString())
          formData.append('radius', uploadRadius.toString())
        }

        // 添加下载限制参数
        if (props.maxDownloads !== undefined && props.maxDownloads > 0) {
          formData.append('maxDownloads', props.maxDownloads.toString())
          console.log('添加下载限制到表单:', { maxDownloads: props.maxDownloads })
        }

        // 添加有效时长参数（分钟）
        if (props.validMinutes !== undefined && props.validMinutes > 0) {
          formData.append('validMinutes', props.validMinutes.toString())
          console.log('添加有效时长到表单:', { validMinutes: props.validMinutes })
        }

        // --- 4. 核心修改：添加“是否需要验证码”参数 ---
        // 即使 needCode 是布尔值，formData 也会将其转为字符串 "true" 或 "false"
        // 后端 Spring Boot 的 Boolean 类型会自动识别这两个字符串
        if (props.needCode !== undefined) {
          formData.append('needCode', props.needCode.toString())
          console.log('添加验证码开关到表单:', { needCode: props.needCode })
        }

        // 上传文件
        const response = await fetch(uploadUrl, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
          body: formData,
        })

        const result: { code: number; data?: any; message?: string } = await response.json()

        console.log('后端完整响应:', result)
        console.log('result.code:', result.code)
        console.log('result.data:', result.data)
        console.log('result.message:', result.message)

        if (result.code === 200) {
          currentSuccessFiles.push(file)
          ElMessage.success(`${file.name} 上传成功`)

          // 发送 upload-success 事件，传递后端返回的文件数据（包含 uploadToken）
          if (result.data) {
            console.log('上传成功，文件数据:', result.data)
            console.log('文件数据中的 uploadToken:', result.data.uploadToken)
            emit('upload-success', result.data)
          } else {
            console.warn('result.data 为空，可能是后端未返回数据')
          }
        } else {
          ElMessage.error(`${file.name}: ${result.message || '上传失败'}`)
          file.status = 'fail'
        }
      } catch (e) {
        console.error(e)
        ElMessage.error(`${file.name}: 上传失败`)
        file.status = 'fail'
      }
    }

    // 发送成功事件
    if (currentSuccessFiles.length > 0) {
      successFiles.value = currentSuccessFiles
      emit('success', currentSuccessFiles)
      ElMessage.success(`成功上传 ${currentSuccessFiles.length} 个文件`)
    }
  } catch (e) {
    const err = e as Error
    uploadError.value = err
    ElMessage.error('上传失败: ' + err.message)
    emit('error', err)
  } finally {
    isUploading.value = false
  }
}*/

// 开始上传 (批量改写版)
const startUpload = async () => {
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
const handleRemove = (file: UploadUserFile) => {
  ElMessage.info(`删除文件: ${file.name}`)
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
  /*const parts = filename.split('.')
  if (parts.length === 0) return ''
  const ext = parts.length > 1 ? (parts[parts.length - 1] ?? '') : ''
  return ext.toLowerCase()*/
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

// 开始分片上传
const handleStartChunkUpload = async () => {
  if (!currentFile.value) return

  isChunkUploading.value = true
  showChunkDialog.value = false

  try {
    // 1. 初始化分片上传
    const initRes = await fetch('/api/file/upload/init', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('downloadToken') || ''}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        fileName: currentFile.value!.name,
        fileSize: currentFile.value!.size.toString(),
        chunkSize: '2097152', // 2MB
      }),
    })

    const initResult = await initRes.json()
    if (initResult.code !== 200) {
      throw new Error(initResult.message)
    }

    const token = await getDownloadToken()

    // 2. 逐个上传分片
    for (let i = chunkUploadInfo.value.uploadedChunks; i < chunkUploadInfo.value.totalChunks; i++) {
      chunkUploadInfo.value.uploadedChunks = i
      chunkUploadInfo.value.progress = Math.round((i / chunkUploadInfo.value.totalChunks) * 100)

      // 创建分片
      const start = i * 2097152
      const end = Math.min(start + 2097152, currentFile.value!.size)
      const chunk = currentFile.value!.slice(start, end)

      // 上传分片
      const chunkFormData = new FormData()
      chunkFormData.append('file', chunk)
      chunkFormData.append('fileName', currentFile.value!.name)
      chunkFormData.append('chunkIndex', i.toString())
      chunkFormData.append('totalChunks', chunkUploadInfo.value.totalChunks.toString())

      const chunkRes = await fetch('/api/file/upload/chunk', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: chunkFormData,
      })

      if (chunkRes.status !== 200) {
        throw new Error('分片上传失败')
      }
    }

    // 3. 合并分片
    const mergeRes = await fetch('/api/file/upload/merge', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('downloadToken') || ''}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        fileName: currentFile.value!.name,
        totalChunks: chunkUploadInfo.value.totalChunks.toString(),
      }),
    })

    const mergeResult = await mergeRes.json()
    if (mergeResult.code === 200) {
      ElMessage.success('分片上传成功')
    } else {
      throw new Error(mergeResult.message)
    }
  } catch (error) {
    ElMessage.error('分片上传失败: ' + (error as Error).message)
  } finally {
    isChunkUploading.value = false
  }
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

  .upload-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 10px;

    .file-info {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;

      .file-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
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

.chunk-upload-container {
  padding: 20px 0;
}

.chunk-info {
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}
</style>
