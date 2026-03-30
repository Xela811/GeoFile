<template>
  <div class="upload-page">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome"> 返回首页 </el-button>
    </div>

    <!-- 标题区域 -->
    <div class="page-header">
      <h1>文件上传</h1>
      <p class="subtitle">支持拖拽上传，大文件自动切换分片上传模式</p>
    </div>

    <!-- 可复用上传组件 -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2 style="margin: 0">文件列表</h2>
      <el-button type="primary" :icon="Back" @click="showLimitDialog">
        设置下载限制
      </el-button>
    </div>

    <FileUpload
      ref="uploadRef"
      title="上传文件"
      :multiple="true"
      :limit="10"
      :max-downloads="downloadLimitConfig.maxDownloads"
      :valid-minutes="downloadLimitConfig.validMinutes"
      tip="支持 JPG、PNG、PDF、DOC、DOCX、XLS、XLSX 等格式，单个文件不超过 100MB"
      @success="handleUploadSuccess"
      @upload-success="handleFileUploadSuccess"
      @error="handleUploadError"
      @require-limit-config="handleRequireLimitConfig"
    >
      <template #success="{ files }">
        <el-alert
          v-if="files.length > 0"
          title="上传成功"
          type="success"
          :closable="false"
          style="margin-top: 20px"
        >
          <template #default>
            <div v-for="file in files" :key="file.uid" class="success-item">
              <el-icon><SuccessFilled /></el-icon>
              <span>{{ file.name }}</span>
            </div>
          </template>
        </el-alert>
      </template>
    </FileUpload>

    <!-- 下载限制配置对话框 -->
    <el-dialog
      v-model="showDownloadLimitDialog"
      title="下载限制配置"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="120px">
        <el-form-item label="下载次数限制">
          <el-radio-group v-model="downloadLimitConfig.maxDownloads">
            <el-radio :value="1">1次</el-radio>
            <el-radio :value="5">5次</el-radio>
            <el-radio :value="10">10次</el-radio>
            <el-radio :value="50">50次</el-radio>
            <el-radio :value="100">100次</el-radio>
          </el-radio-group>
          <el-input-number
            v-model="downloadLimitConfig.maxDownloads"
            :min="1"
            :max="9999"
            style="margin-left: 12px; width: 150px"
          />
        </el-form-item>

        <el-form-item label="有效时长">
          <el-radio-group v-model="downloadLimitConfig.validMinutes">
            <el-radio :value="5">5分钟</el-radio>
            <el-radio :value="10">10分钟</el-radio>
            <el-radio :value="30">30分钟</el-radio>
            <el-radio :value="60">1小时</el-radio>
            <el-radio :value="1440">1天</el-radio>
          </el-radio-group>
          <el-input-number
            v-model="downloadLimitConfig.validMinutes"
            :min="1"
            :max="525600"
            style="margin-left: 12px; width: 150px"
          />
          <span style="margin-left: 12px; color: #909399; font-size: 12px">分钟</span>
        </el-form-item>

        <el-form-item label="配置说明">
          <el-alert
            :title="getLimitDescription()"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDownloadLimitDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmDownloadLimit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快捷操作 -->
    <!--<div class="quick-actions">
      <el-card>
        <template #header>
          <span>快捷操作</span>
        </template>
        <div class="action-buttons">
          <el-button type="primary" :icon="Upload" @click="triggerUpload">
            <el-icon><Upload /></el-icon>
            立即上传
          </el-button>
          <el-button @click="handleClear" :icon="Delete">
            <el-icon><Delete /></el-icon>
            清空列表
          </el-button>
        </div>
      </el-card>
    </div>-->
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, SuccessFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadUserFile } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const uploadRef = ref<InstanceType<typeof FileUpload>>()

// 下载限制配置
const showDownloadLimitDialog = ref(false)
const downloadLimitConfig = ref({
  maxDownloads: 1,
  validMinutes: 30, // 默认30分钟
})

// 返回首页
const goHome = () => {
  router.push('/')
}

// 显示下载限制配置对话框
const showLimitDialog = () => {
  showDownloadLimitDialog.value = true
}

// 确认下载限制配置
const confirmDownloadLimit = () => {
  const maxDesc = `最多下载 ${downloadLimitConfig.value.maxDownloads} 次`
  const minutesDesc = `${downloadLimitConfig.value.validMinutes} 分钟`

  ElMessage.success(`下载限制已设置: ${maxDesc}, ${minutesDesc}`)
  showDownloadLimitDialog.value = false
}

// 获取限制说明
const getLimitDescription = () => {
  const maxDesc = `最多下载 ${downloadLimitConfig.value.maxDownloads} 次，超过后无法下载`

  const minutes = downloadLimitConfig.value.validMinutes

  let timeDesc
  if (minutes === 0) {
    timeDesc = '文件永久有效'
  } else {
    timeDesc = `文件有效期 ${minutes} 分钟`
  }

  return `${maxDesc}，${timeDesc}`
}

// 上传成功（保存上传令牌）
const handleUploadSuccess = (files: UploadUserFile[]) => {
  console.log('上传成功:', files)
  ElMessage.success(`成功上传 ${files.length} 个文件`)
}

// 上传成功回调（包含 uploadToken）
const handleFileUploadSuccess = (fileData: any) => {
  console.log('收到上传文件数据:', fileData)

  if (fileData && fileData.id && fileData.uploadToken) {
    // 保存上传令牌到 localStorage
    saveUploadToken(fileData.id, fileData.uploadToken)
    ElMessage.success('文件上传成功并已保存操作令牌')
  } else {
    console.warn('上传文件数据不完整:', fileData)
  }
}

// 监听需要配置下载限制的事件
const handleRequireLimitConfig = () => {
  showDownloadLimitDialog.value = true
}

// 保存上传令牌
const saveUploadToken = (fileId: number, uploadToken: string) => {
  try {
    const myFiles = getMyUploadedFiles()
    myFiles[fileId] = uploadToken
    localStorage.setItem('myUploadedFiles', JSON.stringify(myFiles))
    console.log('已保存上传令牌:', fileId, uploadToken)
  } catch (e) {
    console.error('保存上传令牌失败:', e)
  }
}

// 获取我上传的文件列表
const getMyUploadedFiles = (): Record<number, string> => {
  try {
    const stored = localStorage.getItem('myUploadedFiles')
    return stored ? JSON.parse(stored) : {}
  } catch (e) {
    console.error('获取上传文件列表失败:', e)
    return {}
  }
}

// 检查是否是文件上传者
const isFileOwner = (fileId: number): boolean => {
  const myFiles = getMyUploadedFiles()
  return !!myFiles[fileId]
}

// 上传失败
const handleUploadError = (error: Error) => {
  console.error('上传失败:', error)
}

// 立即上传
//const triggerUpload = () => {
//  if (uploadRef.value) {
//    uploadRef.value.startUpload()
//  }
//}

// 清空列表
//const handleClear = () => {
//if (uploadRef.value) {
//uploadRef.value.clear()
//ElMessage.info('已清空文件列表')
//}
//}
</script>

<style scoped lang="scss">
.upload-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  margin-bottom: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-size: 32px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 10px 0;
  }

  .subtitle {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.quick-actions {
  margin-top: 20px;

  .action-buttons {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}

.success-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  span {
    color: #67c23a;
  }
}
</style>
