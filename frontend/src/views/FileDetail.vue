<template>
    <div class="file-detail-container" v-if="file">
      <el-card>
        <template #header>
          <div class="detail-header">
            <h2>{{ file.fileName }}</h2>
            <el-tag :type="getStatusType(file.status)">
              {{ getStatusText(file.status) }}
            </el-tag>
          </div>
        </template>

        <!-- 文件信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件类型">
            {{ file.fileType }}
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">
            {{ formatSize(file.fileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ file.uploadTime }}
          </el-descriptions-item>
          <el-descriptions-item label="位置">
            <el-icon><Location /></el-icon>
            {{ file.location || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="下载次数">
            {{ file.downloadCount }}
          </el-descriptions-item>
          <el-descriptions-item label="有效期">
            {{ file.expireTime }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 预览区 -->
        <div class="preview-area">
          <h3>文件预览</h3>
          <div v-if="canPreviewFile" class="preview-content">
            <el-image
              v-if="isImage(file.fileType)"
              :src="getFileUrl(file.id)"
              fit="contain"
            />
            <pre v-else>{{ file.content }}</pre>
          </div>
          <div v-else class="no-preview">
            该格式暂不支持预览
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" @click="handleDownload">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
          <el-button type="danger" @click="handleDelete">
            <el-icon><Delete /></el-icon>
            删除文件
          </el-button>
        </div>
      </el-card>
    </div>
  </template>

  <script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { Location, Download, Delete } from '@element-plus/icons-vue'
  import { getFileDetail, type FileDetailVO } from '@/api/file'
  import { useFileStore } from '@/stores/fileStore'
  import { ElMessage, ElMessageBox } from 'element-plus'

  const route = useRoute()
  const router = useRouter()
  const fileStore = useFileStore()

  const file = ref<FileDetailVO | null>(null)

  onMounted(async () => {
    const id = route.params.id as string
    await loadFileDetail(id)
  })

  const loadFileDetail = async (id: string) => {
    try {
      const res = await getFileDetail(id)
      file.value = res as unknown as FileDetailVO
    } catch {
      ElMessage.error('加载文件详情失败')
    }
  }

  const formatSize = (size: number) => {
    if (size < 1024) return size + ' B'
    if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  }

  const getStatusType = (status: number) => {
    const typeMap: Record<number, string> = {
      1: 'success',
      0: 'danger'
    }
    return typeMap[status] || 'info'
  }

  const getStatusText = (status: number) => {
    const textMap: Record<number, string> = {
      1: '正常',
      0: '已过期'
    }
    return textMap[status] ?? '未知'
  }

  const canPreviewText = (type: string) => {
    const t = type.toLowerCase()
    const ext = t.replace(/^.*\./, '').replace(/^.*\//, '')
    return ['txt', 'json', 'md', 'text/plain', 'application/json', 'text/markdown'].includes(t) || ['txt', 'json', 'md'].includes(ext)
  }

  const isImage = (type: string) => {
    const ext = type.toLowerCase().replace(/^.*\./, '').replace(/^.*\//, '')
    return ['jpg', 'jpeg', 'png', 'gif', 'image/jpeg', 'image/png', 'image/gif'].includes(type.toLowerCase()) || ['jpg', 'jpeg', 'png', 'gif'].includes(ext)
  }

  const canPreviewFile = computed(() => file.value && (isImage(file.value.fileType) || canPreviewText(file.value.fileType)))

  const getFileUrl = (id: string) => {
    return `/api/file/download/${id}`
  }

  const handleDownload = async () => {
    if (file.value) {
      window.open(getFileUrl(file.value.id), '_blank')
    }
  }

  const handleDelete = async () => {
    if (file.value) {
      try {
        await ElMessageBox.confirm(`确定要删除 "${file.value.fileName}" 吗？`, '警告', {
          type: 'warning'
        })
        await fileStore.deleteFile(file.value.id)
        ElMessage.success('删除成功')
        router.push({ name: 'FileList' })
      } catch {
        // 用户取消
      }
    }
  }
  </script>

  <style scoped lang="scss">
  .file-detail-container {
    max-width: 900px;
    margin: 0 auto;
  }

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0;
    }
  }

  .preview-area {
    margin-top: 20px;

    h3 {
      margin-bottom: 16px;
    }

    .preview-content {
      min-height: 300px;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 4px;
      text-align: center;

      pre {
        text-align: left;
        font-family: 'Courier New', monospace;
      }
    }

    .no-preview {
      padding: 20px;
      background: #f5f7fa;
      border-radius: 4px;
      text-align: center;
    }
  }

  .action-buttons {
    margin-top: 20px;
    display: flex;
    gap: 12px;
  }
  </style>