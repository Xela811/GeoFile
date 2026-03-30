<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <header class="top-nav">
      <div class="logo">GeoFile</div>
      <nav class="nav-menu">
        <router-link to="/" class="active">首页</router-link>
        <router-link to="/upload">上传文件</router-link>
      </nav>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 标题区域 -->
      <div class="page-header">
        <h1>GeoFile - 附近文件</h1>
        <p class="subtitle">基于地理位置的文件共享服务</p>
      </div>

      <!-- 模式切换 -->
      <div class="mode-switch">
        <el-switch
          v-model="useFixedCoordinates"
          active-text="使用固定坐标"
          inactive-text="使用实际GPS"
          active-color="#e6a23c"
          inactive-color="#67c23a"
        />
        <el-tag
          :type="useFixedCoordinates ? 'warning' : 'success'"
          size="small"
          style="margin-left: 12px"
        >
          {{ useFixedCoordinates ? '开发测试模式' : '生产模式' }}
        </el-tag>
      </div>

      <!-- 位置信息卡片 -->
      <el-card class="location-card" v-loading="locationLoading">
        <template #header>
          <div class="card-header">
            <span>📍 当前位置</span>
            <el-button
              type="primary"
              :icon="Location"
              :loading="locationLoading"
              @click="handleGetCurrentLocation"
            >
              {{ locationInfo ? '重新获取' : '获取当前位置' }}
            </el-button>
          </div>
        </template>

        <div v-if="locationInfo" class="location-info">
          <!-- 坐标信息 -->
          <div class="info-section">
            <div class="info-title">📍 坐标</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="纬度">{{
                locationInfo.lat.toFixed(6)
              }}</el-descriptions-item>
              <el-descriptions-item label="经度">{{
                locationInfo.lng.toFixed(6)
              }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 详细地址 -->
          <div class="info-section">
            <div class="info-title">🏠 详细地址</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="省份">{{ locationInfo.province }}</el-descriptions-item>
              <el-descriptions-item label="城市">{{ locationInfo.city }}</el-descriptions-item>
              <el-descriptions-item label="区县">{{ locationInfo.district }}</el-descriptions-item>
              <el-descriptions-item label="街道">{{ locationInfo.township }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 搜索半径 -->
          <div class="info-section">
            <div class="info-title">🔍 搜索范围</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="搜索半径">{{ searchRadius }}米</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 完整地址预览 -->
          <div v-if="locationInfo.formattedAddress" class="formatted-address">
            <h3>📄 完整地址预览</h3>
            <el-tag type="success" size="large">
              {{ locationInfo.formattedAddress }}
            </el-tag>
          </div>
        </div>

        <el-empty v-else description="点击上方按钮获取当前位置" />
      </el-card>

      <!-- 附近文件搜索 -->
      <el-card class="search-card" v-loading="filesLoading">
        <template #header>
          <div class="card-header">
            <span>附近文件 ({{ nearbyFiles.length }} / {{ paginationInfo.total || 0 }})</span>
            <el-button
              type="primary"
              :icon="Search"
              :disabled="!locationInfo?.lat || !locationInfo.lng"
              @click="handleSearch"
            >
              重新搜索
            </el-button>
          </div>
        </template>

        <!-- 搜索参数设置 -->
        <div class="search-params">
          <el-form :inline="true">
            <el-form-item label="搜索半径">
              <el-slider
                v-model="searchRadius"
                :min="100"
                :max="5000"
                :step="100"
                :format-tooltip="(val) => val + '米'"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :icon="Search"
                :loading="filesLoading"
                @click="handleSearch"
              >
                开始搜索
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 文件搜索框 -->
        <div class="file-search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名"
            :prefix-icon="Search"
            clearable
            style="width: 300px; margin-right: 12px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="searchFileType"
            placeholder="选择文件类型"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option label="全部类型" value="" />
            <el-option label="PDF文档" value="pdf" />
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
            <el-option label="音频" value="audio" />
            <el-option label="文档" value="document" />
            <el-option label="压缩包" value="zip" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-button type="primary" :icon="Search" :loading="filesLoading" @click="handleSearch">
            搜索
          </el-button>
          <el-button v-if="searchKeyword || searchFileType" @click="handleResetSearch">
            重置
          </el-button>
        </div>

        <!-- 文件列表 -->
        <div v-if="nearbyFiles.length > 0" class="file-list">
          <div v-for="file in nearbyFiles" :key="file.id" class="file-item">
            <div class="file-icon">
              <el-icon :size="40">
                <component :is="getFileIcon(file.fileType)" />
              </el-icon>
            </div>
            <div class="file-info">
              <div class="file-name">{{ file.fileName }}</div>
              <div class="file-meta">
                <span>{{ formatFileSize(file.fileSize) }}</span>
                <span class="divider">|</span>
                <span>{{ file.uploadTime }}</span>
                <span class="divider">|</span>
                <span>{{ locationService.formatDistance(file.distance!) }}距离</span>
              </div>
            </div>
            <div class="file-actions">
              <el-button type="primary" size="small" @click="viewFileDetail(file)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-if="nearbyFiles.length === 0"
          :description="locationInfo ? '附近没有找到文件，快上传您的文件吧！' : '请先获取位置信息'"
        >
          <template #extra v-if="locationInfo">
            <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
              立即上传文件
            </el-button>
          </template>
        </el-empty>

        <!-- 分页器 -->
        <div v-if="paginationInfo.total > 0" class="pagination-container">
          <el-pagination
            v-model:current-page="paginationInfo.pageNum"
            v-model:page-size="paginationInfo.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="paginationInfo.total"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

      <!-- 文件上传对话框 -->
      <el-dialog
        v-model="showUploadDialog"
        title="上传文件"
        width="500px"
        :close-on-click-modal="false"
      >
        <FileUpload @upload-success="handleUploadSuccess" />
      </el-dialog>

      <!-- 文件详情对话框 -->
      <el-dialog v-model="showDetailDialog" title="文件详情" width="600px">
        <div v-if="selectedFile" class="file-detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="文件名">{{ selectedFile.fileName }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{
              selectedFile.fileType
            }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{
              formatFileSize(selectedFile.fileSize)
            }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{
              selectedFile.uploadTime
            }}</el-descriptions-item>
            <el-descriptions-item label="有效期至">{{
              selectedFile.expireTime
            }}</el-descriptions-item>

            <!-- 下载次数显示 -->
            <el-descriptions-item label="已下载次数">
              <span style="font-weight: bold; color: #409eff">
                {{ selectedFile.downloadCount || 0 }} 次
              </span>
            </el-descriptions-item>

            <!-- 下载次数限制显示 -->
            <el-descriptions-item label="下载次数上限">
              <span
                v-if="selectedFile.maxDownloads && selectedFile.maxDownloads > 0"
                style="color: #67c23a"
              >
                {{ selectedFile.maxDownloads }} 次
              </span>
              <span v-else style="color: #909399"> 不限制 </span>
            </el-descriptions-item>

            <!-- 下载次数状态 -->
            <el-descriptions-item label="下载状态">
              <div v-if="isFileExpiredRealtime">
                <el-progress :percentage="100" status="exception" :stroke-width="8" />
                <div style="margin-top: 8px">
                  <el-alert
                    title="文件已过有效期 (已失效)"
                    type="error"
                    :closable="false"
                    show-icon
                    description="该文件的安全访问期限已截止，无法继续下载。"
                  />
                </div>
              </div>

              <div v-else-if="Number(selectedFile.maxDownloads || 0) > 0">
                <el-progress
                  :percentage="calculateDownloadProgress()"
                  :status="getDownloadProgressStatus()"
                  :stroke-width="8"
                >
                  <template #default="{ percentage }">
                    <span
                      :style="{
                        color: getDownloadProgressStatus() === 'exception' ? '#F56C6C' : '#409EFF',
                      }"
                    >
                      {{ percentage }}%
                    </span>
                  </template>
                </el-progress>
                <div
                  v-if="!isMaxedOut && countdownText"
                  style="
                    margin-top: 8px;
                    color: #e6a23c;
                    font-size: 13px;
                    display: flex;
                    align-items: center;
                    gap: 4px;
                  "
                >
                  <el-icon><Timer /></el-icon> {{ countdownText }}
                </div>
                <div v-if="isMaxedOut" style="margin-top: 8px">
                  <el-alert title="已达到下载上限" type="error" :closable="false" show-icon />
                </div>
                <div v-else style="margin-top: 8px">
                  <el-alert
                    :title="
                      '还可下载 ' +
                      (selectedFile.maxDownloads - (selectedFile.downloadCount || 0)) +
                      ' 次'
                    "
                    type="success"
                    :closable="false"
                    show-icon
                  />
                </div>
              </div>
              <div v-else style="color: #909399">
                <el-icon><Download /></el-icon>
                不限制下载次数
              </div>
            </el-descriptions-item>
          </el-descriptions>

          <div class="file-actions-dialog">
            <el-button
              type="primary"
              size="large"
              @click="downloadFile"
              :disabled="isFileExpiredRealtime || isMaxedOut"
            >
              <el-icon><Download /></el-icon>
              {{ isFileExpiredRealtime ? '文件已过期' : isMaxedOut ? '下载次数已满' : '下载文件' }}
            </el-button>
            <el-button size="large" @click="copyFileLink">
              <el-icon><Link /></el-icon>
              复制链接
            </el-button>
            <el-button
              v-if="selectedFile && isFileOwner(selectedFile.id)"
              type="danger"
              size="large"
              @click="handleDeleteFile(selectedFile)"
            >
              <el-icon><Delete /></el-icon>
              删除文件
            </el-button>
          </div>
        </div>
      </el-dialog>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Location,
  Search,
  Upload,
  Download,
  Link,
  Back,
  Document,
  Picture,
  VideoCamera,
  Headset,
  Delete,
  Timer,
} from '@element-plus/icons-vue'
import FileUpload from '@/components/FileUpload.vue'
import locationService, { LocationInfo, NearbyFile } from '@/services/locationService'

const router = useRouter()

// 位置信息
const locationInfo = ref<LocationInfo | null>(null)
const locationLoading = ref(false)

// 附近文件
const nearbyFiles = ref<NearbyFile[]>([])
const filesLoading = ref(false)
const searchRadius = ref(1000)

// 搜索参数
const searchKeyword = ref('')
const searchFileType = ref('')

// 分页信息
const paginationInfo = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  hasPrevious: false,
  hasNext: false,
})

// 开发测试配置
const useFixedCoordinates = ref(false)
const FIXED_LAT = 38.914
const FIXED_LNG = 121.614

// 上传对话框
const showUploadDialog = ref(false)

// 文件详情对话框
const showDetailDialog = ref(false)
const selectedFile = ref<NearbyFile | null>(null)

// 获取坐标（支持开发测试使用固定坐标）
const getCoordinates = async (): Promise<{ lat: number; lng: number }> => {
  // 开发测试模式：使用固定坐标
  if (useFixedCoordinates.value) {
    console.log('使用固定测试坐标:', { lat: FIXED_LAT, lng: FIXED_LNG })
    return { lat: FIXED_LAT, lng: FIXED_LNG }
  }

  // 生产模式：使用实际GPS坐标
  console.log('使用实际GPS坐标')
  const basicLocation = await locationService.getCurrentLocation()
  return { lat: basicLocation.lat, lng: basicLocation.lng }
}

// 获取当前位置
const handleGetCurrentLocation = async () => {
  try {
    locationLoading.value = true

    if (useFixedCoordinates.value) {
      ElMessage.info('开发测试模式：使用固定坐标')
    } else {
      ElMessage.info('生产模式：使用实际GPS坐标')
    }

    // 1. 获取经纬度坐标（使用固定的或实际的）
    const { lat, lng } = await getCoordinates()

    console.log('使用的坐标:', { lat, lng })

    // 2. 获取详细地址信息
    ElMessage.info('正在解析地址信息...')

    const detailedAddress = await locationService.getDetailedAddress(lat, lng)

    // 3. 合并位置信息
    locationInfo.value = {
      ...{ lat, lng },
      ...detailedAddress,
      useFixedCoords: useFixedCoordinates.value,
      updateTime: new Date().toISOString(),
    }

    // 4. 保存到localStorage
    localStorage.setItem('userLocation', JSON.stringify(locationInfo.value))

    ElMessage.success('位置信息获取成功')
    handleSearch()
  } catch (error: any) {
    ElMessage.error(error.message || '获取位置失败')
    console.error('获取位置失败', error)
  } finally {
    locationLoading.value = false
  }
}

// 搜索附近文件
const handleSearch = async () => {
  if (!locationInfo.value?.lat || !locationInfo.value?.lng) {
    ElMessage.warning('请先获取位置信息')
    return
  }

  console.log('开始搜索附近文件:', {
    lat: locationInfo.value.lat,
    lng: locationInfo.value.lng,
    radius: searchRadius.value,
    keyword: searchKeyword.value,
    fileType: searchFileType.value,
    pageNum: paginationInfo.value.pageNum,
    pageSize: paginationInfo.value.pageSize,
  })

  try {
    filesLoading.value = true
    ElMessage.info('正在搜索附近文件...')

    const result = await locationService.getNearbyFiles(
      locationInfo.value.lat,
      locationInfo.value.lng,
      searchRadius.value,
      undefined,
      searchKeyword.value,
      searchFileType.value,
      paginationInfo.value.pageNum,
      paginationInfo.value.pageSize,
    )

    console.log('搜索结果:', result)

    // 检查返回的数据结构
    if (result && result.files) {
      nearbyFiles.value = result.files

      // 更新分页信息
      if (result.total !== undefined) {
        paginationInfo.value.total = result.total
        paginationInfo.value.totalPages = Math.ceil(result.total / paginationInfo.value.pageSize)
        paginationInfo.value.hasPrevious = paginationInfo.value.pageNum > 1
        paginationInfo.value.hasNext =
          paginationInfo.value.pageNum < paginationInfo.value.totalPages
      } else {
        paginationInfo.value.total = result.files.length
        paginationInfo.value.totalPages = 1
        paginationInfo.value.hasPrevious = false
        paginationInfo.value.hasNext = false
      }

      const count = result.count || result.files.length
      ElMessage.success(`找到 ${count} 个附近文件`)
    } else {
      console.warn('返回的数据结构不符合预期:', result)
      nearbyFiles.value = []
      paginationInfo.value.total = 0
      paginationInfo.value.totalPages = 0
      paginationInfo.value.hasPrevious = false
      paginationInfo.value.hasNext = false
      ElMessage.warning('搜索完成，但没有找到文件')
    }
  } catch (error: any) {
    console.error('搜索附近文件失败:', error)
    ElMessage.error(error.message || '搜索附近文件失败，请检查网络连接')
    nearbyFiles.value = [] // 失败时清空文件列表
    paginationInfo.value.total = 0
  } finally {
    // 确保无论成功或失败都会关闭加载状态
    if (filesLoading.value === true) {
      filesLoading.value = false
    }
  }
}

// 文件上传成功
const handleUploadSuccess = (uploadedFile: any) => {
  console.log('handleUploadSuccess 收到数据:', uploadedFile)
  showUploadDialog.value = false
  ElMessage.success('文件上传成功！')

  // 存储上传令牌到localStorage（用于免登录删除）
  if (uploadedFile && uploadedFile.id && uploadedFile.uploadToken) {
    saveUploadToken(uploadedFile.id, uploadedFile.uploadToken)
  } else {
    console.warn('上传令牌数据不完整:', uploadedFile)
  }

  // 如果位置信息已获取，则重新搜索附近文件
  if (locationInfo.value?.lat && locationInfo.value?.lng) {
    handleSearch()
  }
}

// 存储上传令牌
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
    return {}
  }
}

// 检查是否是文件上传者
const isFileOwner = (fileId: number): boolean => {
  const myFiles = getMyUploadedFiles()
  return !!myFiles[fileId]
}

// 获取文件的上传令牌
const getFileUploadToken = (fileId: number): string | undefined => {
  const myFiles = getMyUploadedFiles()
  return myFiles[fileId]
}

// 删除文件
const handleDeleteFile = async (file: NearbyFile) => {
  if (!file || !file.id) return

  const uploadToken = getFileUploadToken(file.id)
  if (!uploadToken) {
    ElMessage.warning('无法删除：未找到上传令牌')
    return
  }

  try {
    const confirmed = await ElMessageBox.confirm(
      '确定要删除这个文件吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    ).catch(() => false)

    if (!confirmed) return

    ElMessage.info('正在删除文件...')

    const response = await fetch(`/api/file/delete/${file.id}?uploadToken=${uploadToken}`, {
      method: 'DELETE',
    })

    const result = await response.json()

    console.log('删除文件响应:', result)

    if (result.code === 200) {
      ElMessage.success('文件删除成功')

      // 从本地存储中移除
      const myFiles = getMyUploadedFiles()
      delete myFiles[file.id]
      localStorage.setItem('myUploadedFiles', JSON.stringify(myFiles))

      // 从文件列表中移除
      nearbyFiles.value = nearbyFiles.value.filter((f) => f.id !== file.id)

      // 关闭详情对话框（如果正在查看该文件）
      if (selectedFile.value && selectedFile.value.id === file.id) {
        showDetailDialog.value = false
        selectedFile.value = null
      }

      // 显示成功消息后重新搜索（确保从数据库获取最新数据）
      setTimeout(() => {
        handleSearch()
      }, 500)
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    console.error('删除文件失败:', error)
    ElMessage.error('删除文件失败')
  }
}

// 查看文件详情
const viewFileDetail = (file: NearbyFile) => {
  console.log('当前点击的文件详情数据:', file)
  selectedFile.value = file
  showDetailDialog.value = true
}

// 下载文件
const downloadFile = async () => {
  // 1. 基础校验
  if (!selectedFile.value) {
    ElMessage.warning('请先选择一个文件')
    return
  }

  // 2. 直接从当前选中的文件对象中获取 token
  const token = selectedFile.value.downloadToken

  if (!token) {
    ElMessage.error('该文件缺少下载凭证，请刷新列表重试')
    return
  }

  try {
    ElMessage.success('开始下载文件...')

    // 3. 使用 fetch 获取文件，这样可以捕获错误
    const response = await fetch(`/api/file/download/${selectedFile.value.id}?token=${token}`)

    if (!response.ok) {
      // 处理 HTTP 错误状态码
      const errorData = await response.json().catch(() => null)

      if (response.status === 400) {
        ElMessage.error(errorData?.message || '文件已过期或无效，无法下载')
      } else if (response.status === 404) {
        ElMessage.error('文件不存在')
      } else {
        ElMessage.error(`下载失败: ${response.status} ${response.statusText}`)
      }
      return
    }

    // 4. 获取文件 blob
    const blob = await response.blob()

    // 5. 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = selectedFile.value.fileName || 'file'
    document.body.appendChild(a)
    a.click()

    // 6. 清理
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('下载成功')

    // 7. 下载成功后重新获取文件列表，更新下载次数
    //await handleSearch()
    // --- 核心修改部分 ---

    // 1. 前端先行：立刻让界面上的次数 +1
    // 这样进度条和描述项会瞬间更新，不会出现“变为0”的情况
    if (selectedFile.value.downloadCount !== undefined) {
      selectedFile.value.downloadCount += 1
    } else {
      selectedFile.value.downloadCount = 1
    }

    ElMessage.success('下载成功')

    // 2. 延迟同步：给后端数据库写入留出 1 秒缓冲时间，再刷新列表
    // 这样可以避免 handleSearch 拿到还没更新完的旧数据（或 0）
    setTimeout(() => {
      handleSearch()
    }, 1000)
  } catch (error: any) {
    console.error('下载文件失败:', error)

    // 检查是否是过期错误
    if (
      error.message &&
      error.message.includes('network') &&
      error.message.includes('failed to fetch')
    ) {
      ElMessage.error('网络请求失败，请检查网络连接')
    } else if (error.message && error.message.includes('expired')) {
      ElMessage.error('文件已过期，无法下载')
    } else {
      ElMessage.error('下载失败: ' + (error.message || '未知错误'))
    }
  }
}

// 复制文件链接
const copyFileLink = () => {
  if (!selectedFile.value) return

  const url =
    window.location.origin +
    `/api/file/download/${selectedFile.value.id}?token=${selectedFile.value.downloadToken}`

  navigator.clipboard
    .writeText(url)
    .then(() => {
      ElMessage.success('链接已复制到剪贴板')
    })
    .catch(() => {
      ElMessage.error('复制失败')
    })
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 获取文件图标
const getFileIcon = (fileType: string) => {
  if (fileType.includes('pdf')) return Document
  if (fileType.includes('image')) return Picture
  if (fileType.includes('video')) return VideoCamera
  if (fileType.includes('audio')) return Headset
  return Document
}

// 格式化距离
const formatDistance = (meters: number): string => {
  if (meters < 1000) return meters + 'm'
  return (meters / 1000).toFixed(2) + 'km'
}

// 计算下载进度
/*const calculateDownloadProgress = (): number => {
  if (
    !selectedFile.value ||
    !selectedFile.value.maxDownloads ||
    selectedFile.value.maxDownloads === 0
  ) {
    return 0
  }
  const current = selectedFile.value.downloadCount || 0
  const max = selectedFile.value.maxDownloads
  return Math.round((current / max) * 100)
}*/

// 获取下载进度状态
/*const getDownloadProgressStatus = (): string | undefined => {
  if (
    !selectedFile.value ||
    !selectedFile.value.maxDownloads ||
    selectedFile.value.maxDownloads === 0
  ) {
    return undefined
  }
  const current = selectedFile.value.downloadCount || 0
  const max = selectedFile.value.maxDownloads
  return current >= max ? 'exception' : undefined
}*/
// 1. 判断是否过期 (增加严格的 null/undefined 检查和格式兼容)
const isExpired = () => {
  // 必须确保 selectedFile 及其 expireTime 存在且不是空字符串
  if (!selectedFile.value || !selectedFile.value.expireTime) return false

  try {
    // 兼容 iOS/Safari 的日期格式 (将 - 替换为 /)
    const expireStr = selectedFile.value.expireTime.replace(/-/g, '/')
    const expireDate = new Date(expireStr)

    // 如果日期解析失败，返回 false 防止误杀
    if (isNaN(expireDate.getTime())) return false

    return new Date() > expireDate
  } catch (e) {
    return false
  }
}

// 2. 修改进度条状态逻辑 (增加对 undefined 的防御)
const getDownloadProgressStatus = () => {
  // 优先级 1: 过期判断
  if (isExpired()) return 'exception' // 红色

  const file = selectedFile.value
  if (!file) return 'success'

  // 优先级 2: 次数判断 (将 undefined 视为 0)
  const max = Number(file.maxDownloads || 0)
  const current = Number(file.downloadCount || 0)

  if (max > 0 && current >= max) {
    return 'exception' // 红色
  }

  return 'success' // 绿色
}

// 3. 计算百分比 (增加对 undefined 的防御)
const calculateDownloadProgress = () => {
  if (isExpired()) return 100 // 过期直接满进度条变红

  const file = selectedFile.value
  if (!file || !file.maxDownloads || file.maxDownloads <= 0) return 0

  const current = Number(file.downloadCount || 0)
  const max = Number(file.maxDownloads)
  const percent = Math.round((current / max) * 100)
  return percent > 100 ? 100 : percent
}

// 监听模式切换
watch(useFixedCoordinates, (newVal) => {
  if (newVal) {
    ElMessage.warning('已切换到开发测试模式：使用固定坐标')
  } else {
    ElMessage.success('已切换到生产模式：使用实际GPS坐标')
  }
})

// 处理页码变化
const handleCurrentChange = (page: number) => {
  paginationInfo.value.pageNum = page
  handleSearch()
}

// 处理每页大小变化
const handleSizeChange = (size: number) => {
  paginationInfo.value.pageSize = size
  paginationInfo.value.pageNum = 1 // 重置到第一页
  handleSearch()
}

// 重置搜索
const handleResetSearch = () => {
  searchKeyword.value = ''
  searchFileType.value = ''
  paginationInfo.value.pageNum = 1
  handleSearch()
}

// 1. 定义一个实时更新的当前时间戳
const nowTimestamp = ref(Date.now())
let timer: any = null

// 2. 增强版的过期判断（将你原有的逻辑改为依赖 nowTimestamp）
// 注意：这里我们把它改成一个变量，这样 Vue 就能实时追踪它
const isFileExpiredRealtime = computed(() => {
  if (!selectedFile.value || !selectedFile.value.expireTime) return false

  try {
    const expireStr = selectedFile.value.expireTime.replace(/年|月/g, '/').replace(/日/g, '').trim()
    const expireDate = new Date(expireStr)
    if (isNaN(expireDate.getTime())) return false

    // 核心修改：使用 nowTimestamp.value 进行对比，实现“挂机自动变红”
    return nowTimestamp.value > expireDate.getTime()
  } catch (e) {
    return false
  }
})

// 3. 倒计时计算属性
const countdownText = computed(() => {
  // 如果已过期或已达上限，直接返回空，不显示倒计时
  if (isFileExpiredRealtime.value || isMaxedOut.value) return ''

  if (!selectedFile.value?.expireTime) return ''

  const expireStr = selectedFile.value.expireTime.replace(/年|月/g, '/').replace(/日/g, '').trim()
  const expireDate = new Date(expireStr).getTime()

  const diff = Math.floor((expireDate - nowTimestamp.value) / 1000)
  if (diff <= 0) return ''

  const h = Math.floor(diff / 3600)
  const m = Math.floor((diff % 3600) / 60)
  const s = diff % 60
  return `剩余有效期：${h > 0 ? h + '时' : ''}${m}分${s}秒`
})

// 4. 判断次数是否超限
const isMaxedOut = computed(() => {
  const file = selectedFile.value
  if (!file || !file.maxDownloads || file.maxDownloads <= 0) return false
  return (file.downloadCount || 0) >= file.maxDownloads
})

// 5. 管理定时器
onMounted(() => {
  timer = setInterval(() => {
    nowTimestamp.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 初始化
onMounted(() => {
  console.log('首页初始化...')

  // 检查是否已经有位置信息（可以从localStorage读取）
  const savedLocation = localStorage.getItem('userLocation')
  if (savedLocation) {
    try {
      const locationData = JSON.parse(savedLocation)
      locationInfo.value = locationData

      console.log('从localStorage读取位置信息:', locationData)

      // 如果有位置信息，自动搜索附近文件
      if (locationData.lat && locationData.lng) {
        ElMessage.info('已从缓存读取位置信息，开始搜索附近文件...')
        handleSearch()
      } else {
        console.warn('位置信息不完整:', locationData)
        ElMessage.info('已从缓存读取位置信息，但坐标不完整，请重新获取位置')
      }
    } catch (error) {
      console.error('解析位置信息失败:', error)
      ElMessage.warning('读取位置信息失败，请重新获取位置')
    }
  } else {
    console.log('localStorage中没有位置信息')
    ElMessage.info('请先获取当前位置')
  }
})
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.top-nav {
  background: white;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  .logo {
    font-size: 24px;
    font-weight: bold;
    color: #409eff;
  }

  .nav-menu {
    display: flex;
    gap: 20px;

    a {
      color: #606266;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover,
      &.active {
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }
}

.main-content {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;

  h1 {
    font-size: 36px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 10px 0;
  }

  .subtitle {
    font-size: 16px;
    color: #909399;
    margin: 0;
  }
}

.mode-switch {
  margin-bottom: 20px;
}

.location-card,
.files-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.location-info {
  .info-section {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .info-title {
      font-size: 14px;
      font-weight: 600;
      color: #606266;
      margin-bottom: 8px;
    }
  }
}

.search-card {
  margin-top: 20px;
}

.search-params {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.file-search-box {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.file-list {
  display: grid;
  gap: 12px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
  }

  .file-icon {
    width: 56px;
    height: 56px;
    background: #ecf5ff;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #409eff;
    margin-right: 12px;
    flex-shrink: 0;
  }

  .file-info {
    flex: 1;
    min-width: 0;

    .file-name {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .file-meta {
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 8px;

      .divider {
        color: #dcdfe6;
      }
    }
  }

  .file-actions {
    flex-shrink: 0;
  }
}

.file-detail {
  .file-actions-dialog {
    margin-top: 24px;
    display: flex;
    gap: 12px;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
