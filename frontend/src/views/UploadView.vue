<template>
  <div class="upload-container">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome"> 返回首页 </el-button>
    </div>
    <el-card>
      <template #header>
        <h2>上传文件</h2>
      </template>

      <el-upload
        drag
        action="/api/file/upload"
        :before-upload="beforeUpload"
        :on-progress="handleProgress"
        :on-success="handleSuccess"
        :on-error="handleError"
        multiple
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            支持 JPG、PNG、PDF、DOC、TXT 等格式，单个文件不超过 100MB
          </div>
        </template>
      </el-upload>

      <!-- 上传进度列表 -->
      <div v-if="uploadList.length > 0" class="upload-list">
        <div v-for="item in uploadList" :key="item.uid" class="upload-item">
          <div class="upload-info">
            <span>{{ item.name }}</span>
            <el-progress :percentage="item.progress" />
          </div>
          <el-button
            v-if="item.status === 'uploading'"
            type="danger"
            size="small"
            @click="handleCancelUpload(item.uid)"
          >
            取消
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled, Back } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router' // 2. 引入路由

const router = useRouter() // 初始化路由

// 3. 跳转函数
const goHome = () => {
  router.push('/') // 对应你路由配置中 HomeView.vue 的路径
}

interface UploadListItem {
  uid: string
  name: string
  progress: number
  status: 'uploading' | 'success' | 'error'
}

interface UploadProgressEvent {
  uid?: string
  percent?: number
}

const uploadList = ref<UploadListItem[]>([])

const beforeUpload = (file: File) => {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

const handleProgress = (event: UploadProgressEvent) => {
  const item = uploadList.value.find((i) => i.uid === event.uid)
  if (item) {
    item.progress = event.percent ?? 0
    item.status = 'uploading'
  }
}

const handleSuccess = (_response: unknown, uploadFile?: { uid?: string }) => {
  const item = uploadList.value.find((i) => i.uid === uploadFile?.uid)
  if (item) {
    item.progress = 100
    item.status = 'success'
  }
  ElMessage.success('上传成功')
}

const handleError = () => {
  ElMessage.error('上传失败')
}

const handleCancelUpload = (uid: string) => {
  uploadList.value = uploadList.value.filter((i) => i.uid !== uid)
}
</script>

<style scoped lang="scss">
.upload-container {
  max-width: 800px;
  margin: 0 auto;
}

.upload-list {
  margin-top: 20px;

  .upload-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 10px;

    .upload-info {
      flex: 1;

      span {
        display: block;
        margin-bottom: 8px;
      }
    }
  }
}
</style>
