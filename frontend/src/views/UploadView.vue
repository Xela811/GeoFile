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
    <FileUpload
      ref="uploadRef"
      title="上传文件"
      :multiple="true"
      :limit="10"
      tip="支持 JPG、PNG、PDF、DOC、DOCX、XLS、XLSX 等格式，单个文件不超过 100MB"
      @success="handleUploadSuccess"
      @error="handleUploadError"
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

    <!-- 快捷操作 -->
    <div class="quick-actions">
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Upload, Delete, SuccessFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadUserFile } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const uploadRef = ref<InstanceType<typeof FileUpload>>()
uploadRef.value?.clear() // 清空列表
uploadRef.value?.startUpload() // 开始上传

// 返回首页
const goHome = () => {
  router.push('/')
}

// 上传成功
const handleUploadSuccess = (files: UploadUserFile[]) => {
  console.log('上传成功:', files)
  ElMessage.success(`成功上传 ${files.length} 个文件`)
}

// 上传失败
const handleUploadError = (error: Error) => {
  console.error('上传失败:', error)
}

// 立即上传
const triggerUpload = () => {
  if (uploadRef.value) {
    uploadRef.value.startUpload()
  }
}

// 清空列表
const handleClear = () => {
  if (uploadRef.value) {
    uploadRef.value.clear()
    ElMessage.info('已清空文件列表')
  }
}
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
