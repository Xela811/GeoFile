<template>
    <div class="file-list-container">
      <!-- 位置状态 -->
      <div class="location-status">
        <el-tag :type="location.type">{{ location.text }}</el-tag>
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件..."
          clearable
          :prefix-icon="Search"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- 文件列表 -->
      <el-table
        :data="fileList"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="fileName" label="文件名" width="300" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column prop="fileSize" label="大小" width="100" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="location" label="位置" width="150">
          <template #default="{ row }">
            <el-icon><Location /></el-icon>
            {{ row.location || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="handleDownload(row)">
              下载
            </el-button>
            <el-button type="danger" size="small" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </template>

  <script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { Search, Location } from '@element-plus/icons-vue'
  import { getFileList, type FileVO } from '@/api/file'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useFileStore } from '@/stores/fileStore'

  const router = useRouter()
  const fileStore = useFileStore()

  // 状态
  const fileList = ref<FileVO[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const searchKeyword = ref('')
  const location = ref({ type: 'info', text: '定位中...' })

  // 生命周期
  onMounted(() => {
    getLocationStatus()
    loadFileList()
  })

  // 方法
  const getLocationStatus = async () => {
    try {
      // 获取位置信息
      location.value = { type: 'success', text: '定位成功' }
    } catch {
      location.value = { type: 'warning', text: '定位失败' }
    }
  }

  const loadFileList = async () => {
    try {
      const res = await getFileList({
        page: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value
      }) as unknown as { list: FileVO[]; total: number }
      fileList.value = res.list
      total.value = res.total
    } catch {
      ElMessage.error('加载文件列表失败')
    }
  }

  const handleSearch = () => {
    currentPage.value = 1
    loadFileList()
  }

  const handlePageChange = () => {
    loadFileList()
  }

  const handleRowClick = (row: FileVO) => {
    router.push({ name: 'FileDetail', params: { id: row.id } })
  }

  const handleDownload = async (row: FileVO) => {
    try {
      await ElMessageBox.confirm(`确定要下载 "${row.fileName}" 吗？`)
      window.open(`/api/file/download/${row.id}`, '_blank')
    } catch {
      // 用户取消
    }
  }

  const handleDelete = async (row: FileVO) => {
    try {
      await ElMessageBox.confirm(`确定要删除 "${row.fileName}" 吗？`, '警告', {
        type: 'warning'
      })
      await fileStore.deleteFile(row.id)
      ElMessage.success('删除成功')
      loadFileList()
    } catch {
      // 用户取消或删除失败
    }
  }
  </script>

  <style scoped lang="scss">
  .file-list-container {
    background: white;
    padding: 20px;
    border-radius: 8px;
  }

  .location-status {
    margin-bottom: 20px;
  }

  .search-box {
    margin-bottom: 20px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  </style>