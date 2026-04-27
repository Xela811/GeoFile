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
            <span>🔍 发现文件 ({{ nearbyFiles.length }} / {{ paginationInfo.total || 0 }})</span>
            <div class="code-extract-area">
              <el-input
                v-model="extractCode"
                placeholder="输入取件码提取文件"
                :prefix-icon="Key"
                clearable
                @keyup.enter="handleExtractByCode"
              >
                <template #append>
                  <el-button @click="handleExtractByCode" :loading="extractLoading">提取</el-button>
                </template>
              </el-input>
            </div>
            <el-button
              type="primary"
              :icon="Search"
              :disabled="!locationInfo?.lat || !locationInfo.lng"
              @click="handleSearch('public')"
            >
              搜索附近文件
            </el-button>
          </div>
        </template>

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
            style="width: 130px"
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
          <div class="search-btn-group">
            <el-button
              style="width: 65px"
              type="primary"
              :icon="Search"
              :loading="filesLoading"
              @click="handleSearch"
            >
              搜索
            </el-button>
            <el-button
              style="width: 50px"
              v-if="searchKeyword || searchFileType"
              @click="handleResetSearch"
            >
              重置
            </el-button>
          </div>
        </div>

        <!-- 文件列表 -->
        <div v-if="nearbyFiles.length > 0" class="file-list">
          <div v-for="file in nearbyFiles" :key="file.id" class="file-item">
            <div class="file-icon">
              <el-icon :size="40">
                <component :is="getFileIcon(file.fileType)" />
              </el-icon>
              <div
                v-if="file.isPrivate === 1"
                class="preview-mask"
                @click.stop="handlePreview(file)"
              >
                🔎预览
              </div>
            </div>
            <div class="file-info">
              <div class="file-name">{{ file.fileName }}</div>
              <div class="file-meta">
                <span
                  ><el-icon><Document /></el-icon>{{ formatFileSize(file.fileSize) }}</span
                >
                <span v-if="file.isPrivate !== 1"
                  ><el-icon><Location /></el-icon
                  >{{ locationService.formatDistance(file.distance!) }}距离</span
                >
                <span
                  ><el-icon><Clock /></el-icon>{{ file.uploadTime }}</span
                >
              </div>
            </div>
            <div class="file-actions">
              <el-button type="primary" size="small" @click="viewFileDetail(file)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>

        <el-dialog
          v-model="previewVisible"
          :title="'正在预览: ' + activeFile?.fileName"
          width="70%"
          top="5vh"
          destroy-on-close
          class="preview-dialog"
        >
          <div v-loading="previewLoading" class="preview-body">
            <template v-if="isImage(activeFile?.fileType)">
              <div class="img-wrapper">
                <img :src="previewUrl" @load="previewLoading = false" class="preview-content-img" />
              </div>
            </template>

            <template v-else-if="isDocOrText(activeFile?.fileType)">
              <iframe
                :src="previewUrl"
                class="preview-iframe"
                @load="previewLoading = false"
              ></iframe>
            </template>

            <template v-else-if="isVideo(activeFile?.fileType)">
              <div class="video-wrapper">
                <video
                  :src="previewUrl"
                  controls
                  playsinline
                  class="preview-video"
                  @canplay="previewLoading = false"
                >
                  您的浏览器不支持视频播放
                </video>
              </div>
            </template>

            <template v-else-if="isAudio(activeFile?.fileType)">
              <div class="audio-wrapper">
                <audio :src="previewUrl" controls autoplay @canplay="previewLoading = false">
                  您的浏览器不支持音频播放
                </audio>
              </div>
            </template>

            <template v-else-if="isArchive(activeFile?.fileType)">
              <div class="archive-preview">
                <div class="archive-header">
                  <el-icon><FolderOpened /></el-icon>
                  <span>压缩包文件结构</span>
                </div>
                <el-scrollbar height="500px">
                  <el-tree
                    :data="archiveTree"
                    :props="{ label: 'name', children: 'children' }"
                    indent="20"
                  >
                    <template #default="{ node, data }">
                      <div class="custom-tree-node">
                        <el-icon v-if="data.directory" color="#e6a23c"><Folder /></el-icon>
                        <el-icon v-else color="#409eff"><Document /></el-icon>
                        <span class="node-label">{{ node.label }}</span>
                        <span v-if="!data.directory" class="node-size">
                          {{ formatFileSize(data.size) }}
                        </span>
                      </div>
                    </template>
                  </el-tree>
                </el-scrollbar>
              </div>
            </template>

            <template v-else-if="isInstaller(activeFile?.fileType)">
              <div class="installer-info-box" style="text-align: center; padding: 40px 0">
                <el-icon size="80" color="#409eff"><Platform /></el-icon>
                <h3 style="margin-top: 20px">
                  程序安装包 ({{ activeFile?.fileType?.toUpperCase() }})
                </h3>
                <p style="color: #909399; margin-bottom: 24px">
                  该文件为可执行程序，为了您的系统安全，请下载到本地后运行。
                </p>
                <el-descriptions :column="1" border style="max-width: 400px; margin: 0 auto 20px">
                  <el-descriptions-item label="文件名">{{
                    activeFile?.fileName
                  }}</el-descriptions-item>
                  <el-descriptions-item label="大小">{{
                    formatFileSize(activeFile?.fileSize)
                  }}</el-descriptions-item>
                </el-descriptions>
                <el-button type="primary" size="large" @click="downloadFileFromPreview">
                  立即下载
                </el-button>
              </div>
            </template>

            <el-result v-else icon="warning" title="当前格式不支持在线预览">
              <template #extra>
                <el-button type="primary" @click="downloadFileFromPreview">下载原文件</el-button>
              </template>
            </el-result>
          </div>

          <template #footer>
            <div class="preview-footer-info">
              <el-tag size="small" type="info">格式: {{ activeFile?.fileType }}</el-tag>
              <span class="warning-text">提示：预览操作将消耗一次下载额度</span>
            </div>
          </template>
        </el-dialog>

        <!-- 空状态 -->
        <el-empty
          v-if="nearbyFiles.length === 0"
          :description="
            locationInfo ? '附近没有找到文件，快点击上传您的文件吧！' : '请先获取位置信息'
          "
        >
          <template #description>
            <p v-if="locationInfo">
              附近没有找到文件，快
              <router-link to="/upload" class="clickable-link">点击上传您的文件</router-link> 吧！
            </p>
            <p v-else>
              我们需要您的位置信息来发现附近的文件
              <el-button type="primary" link @click="handleGetCurrentLocation">立即获取</el-button>
            </p>
          </template>
        </el-empty>

        <!-- 分页器 -->
        <div v-if="paginationInfo.total > 0" class="pagination-container">
          <el-pagination
            v-model:current-page="paginationInfo.pageNum"
            v-model:page-size="paginationInfo.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="paginationInfo.total"
            layout=" sizes, prev, pager, next, jumper"
            background
            small
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
      <el-dialog v-model="showDetailDialog" title="文件详情" width="95%" style="max-width: 600px">
        <div v-if="selectedFile" class="file-detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="文件名"
              ><div class="long-text-wrapper">
                {{ selectedFile.fileName }}
              </div></el-descriptions-item
            >
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
            <el-popover
              placement="top"
              :width="220"
              trigger="click"
              popper-style="padding: 15px; text-align: center;"
            >
              <template #reference>
                <el-button size="large" type="info" plain>
                  <el-icon><View /></el-icon>
                  二维码分享
                </el-button>
              </template>

              <div>
                <div v-if="isMaxedOut" class="status-alert error">
                  <el-icon><Warning /></el-icon>
                  <span>下载次数已达上限</span>
                </div>
                <div v-else-if="currentFileIsPrivate" class="status-alert private">
                  <el-icon><Lock /></el-icon>
                  <span>私密文件：扫码即下</span>
                </div>
                <div v-else class="status-alert public">
                  <el-icon><Location /></el-icon>
                  <span>公开文件：1km地理限制</span>
                </div>

                <div class="qr-wrapper" v-if="!isMaxedOut">
                  <qrcode-vue
                    :value="currentDownloadUrl"
                    :size="180"
                    level="H"
                    render-as="svg"
                    :image-settings="{
                      src: currentFileIsPrivate ? LOCK_ICON : LOCATION_ICON,
                      width: 40,
                      height: 40,
                      excavate: true,
                    }"
                  />
                  <p style="margin-top: 10px; font-size: 12px; color: #909399">
                    {{ currentFileIsPrivate ? '此链接已授权，扫码直接下载' : '请在范围内扫码下载' }}
                  </p>
                </div>
              </div>
            </el-popover>
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
import { useRouter, useRoute } from 'vue-router'
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
  Memo,
  TakeawayBox,
  Delete,
  Timer,
  Key,
} from '@element-plus/icons-vue'
import FileUpload from '@/components/FileUpload.vue'
import locationService from '@/services/locationService'
import type { LocationInfo, NearbyFile } from '@/services/locationService'
import { reconcileMyUploadedFiles } from '@/services/reconcileService'
import QrcodeVue from 'qrcode.vue'

const router = useRouter()
const route = useRoute()

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

/** 取件码私有列表视图：为 true 时翻页/删项不调用附近公开搜索 */
const isExtractListView = ref(false)
const activeExtractCode = ref('')
const extractedFilesFull = ref<NearbyFile[]>([])

// 开发测试配置
const useFixedCoordinates = ref(false)
const FIXED_LAT = 38.914
const FIXED_LNG = 121.614

// 上传对话框
const showUploadDialog = ref(false)

// 文件详情对话框
const showDetailDialog = ref(false)
const selectedFile = ref<NearbyFile | null>(null)

// 定义图标地址
const LOCK_ICON = 'https://api.iconify.design/ep:lock.svg?color=%2367c23a' // 绿色锁
const LOCATION_ICON = 'https://api.iconify.design/ep:location.svg?color=%23e6a23c' // 橙色定位针

// 计算当前选中文件的下载链接
const currentDownloadUrl = computed(() => {
  if (!selectedFile.value) return ''
  // 保持和你 copyFileLink 函数中的逻辑一致
  return `${window.location.origin}/api/file/download/${selectedFile.value.id}?token=${selectedFile.value.downloadToken}`
})

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

// 搜索附近文件（会退出「取件码私有列表」视图）
const handleSearch = async (mode = 'keep') => {
  if (!locationInfo.value?.lat || !locationInfo.value?.lng) {
    ElMessage.warning('请先获取位置信息')
    return
  }

  if (mode === 'public') {
    activeExtractCode.value = '' // 只有明确说要搜附近时，才清空码
    extractCode.value = ''
  }

  /*isExtractListView.value = false
  activeExtractCode.value = ''
  extractedFilesFull.value = []*/

  console.log('开始搜索附近文件:', {
    lat: locationInfo.value.lat,
    lng: locationInfo.value.lng,
    radius: searchRadius.value,
    keyword: searchKeyword.value,
    fileType: searchFileType.value,
    pageNum: paginationInfo.value.pageNum,
    pageSize: paginationInfo.value.pageSize,
    activeExtractCode: activeExtractCode.value,
  })

  try {
    filesLoading.value = true
    if (mode === 'public') {
      ElMessage.info('正在搜索附近文件...')
    }

    const result = await locationService.getNearbyFiles(
      locationInfo.value.lat,
      locationInfo.value.lng,
      searchRadius.value,
      undefined,
      searchKeyword.value,
      searchFileType.value,
      paginationInfo.value.pageNum,
      paginationInfo.value.pageSize,
      undefined, // sortBy (如果没传，补个 undefined)
      undefined, // sortOrder (如果没传，补个 undefined)
      activeExtractCode.value,
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

      const msg = activeExtractCode.value ? '已筛选私有文件列表' : `找到 ${result.count} 个附近文件`
      ElMessage.success(msg)
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

/** 取件码模式：根据全量列表与当前页做前端分页，写入 nearbyFiles */
const applyExtractPage = () => {
  const total = extractedFilesFull.value.length
  paginationInfo.value.total = total
  const pageSize = paginationInfo.value.pageSize
  let pageNum = paginationInfo.value.pageNum
  const maxPage = Math.max(1, Math.ceil(total / pageSize) || 1)
  if (pageNum > maxPage) {
    pageNum = maxPage
    paginationInfo.value.pageNum = pageNum
  }
  const start = (pageNum - 1) * pageSize
  nearbyFiles.value = extractedFilesFull.value.slice(start, start + pageSize)
  paginationInfo.value.totalPages = maxPage
  paginationInfo.value.hasPrevious = pageNum > 1
  paginationInfo.value.hasNext = pageNum < maxPage
}

// 文件上传成功
const handleUploadSuccess = (uploadedFile: any) => {
  console.log('handleUploadSuccess 收到数据:', uploadedFile)
  showUploadDialog.value = false
  ElMessage.success('文件上传成功！')

  // 存储上传令牌到localStorage（用于免登录删除）
  /*if (uploadedFile && uploadedFile.id && uploadedFile.uploadToken) {
    saveUploadToken(uploadedFile.id, uploadedFile.uploadToken)
  } else {
    console.warn('上传令牌数据不完整:', uploadedFile)
  }*/
  // 支持 Result{ data }、FileVO[]、单个 FileVO（与 FileUpload 各路径对齐）
  const raw = uploadedFile && uploadedFile.data !== undefined ? uploadedFile.data : uploadedFile
  let items: { id?: number; uploadToken?: string }[] = []
  if (Array.isArray(raw)) {
    items = raw
  } else if (raw && raw.id != null && raw.uploadToken) {
    items = [raw]
  }
  if (items.length > 0) {
    console.log(`保存 ${items.length} 个文件的上传令牌`)
    items.forEach((file) => {
      if (file.id != null && file.uploadToken) {
        saveUploadToken(Number(file.id), file.uploadToken)
      }
    })
  } else {
    console.warn('上传成功但未提取到有效的 ID 或 Token:', uploadedFile)
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

      // 从文件列表中移除（取件码视图：同步全量列表并仅刷新当前页，不触发附近公开搜索）
      if (isExtractListView.value) {
        extractedFilesFull.value = extractedFilesFull.value.filter((f) => f.id !== file.id)
        if (extractedFilesFull.value.length === 0) {
          isExtractListView.value = false
          activeExtractCode.value = ''
          nearbyFiles.value = []
          paginationInfo.value.total = 0
          paginationInfo.value.totalPages = 0
          paginationInfo.value.hasPrevious = false
          paginationInfo.value.hasNext = false
        } else {
          const pageSize = paginationInfo.value.pageSize
          const maxPage = Math.max(1, Math.ceil(extractedFilesFull.value.length / pageSize) || 1)
          if (paginationInfo.value.pageNum > maxPage) {
            paginationInfo.value.pageNum = maxPage
          }
          applyExtractPage()
        }
      } else {
        nearbyFiles.value = nearbyFiles.value.filter((f) => f.id !== file.id)
      }

      // 关闭详情对话框（如果正在查看该文件）
      if (selectedFile.value && selectedFile.value.id === file.id) {
        showDetailDialog.value = false
        selectedFile.value = null
      }
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
    /*setTimeout(() => {
      handleSearch()
    }, 1000)*/
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

// 后端 fileType 为扩展名（如 jpg、zip），不是 MIME；按扩展名 + MIME 字符串兼容
const IMAGE_EXTS = new Set([
  'jpg',
  'jpeg',
  'png',
  'gif',
  'webp',
  'bmp',
  'svg',
  'ico',
  'avif',
  'heic',
])
const VIDEO_EXTS = new Set(['mp4', 'webm', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'm4v', 'mpeg', 'mpg'])
const AUDIO_EXTS = new Set(['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a', 'wma', 'opus'])
const ARCHIVE_EXTS = new Set(['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz', 'tgz', 'lzma', 'zst'])
const TEXT_CODE_EXTS = new Set([
  'txt',
  'md',
  'log',
  'csv',
  'json',
  'xml',
  'yaml',
  'yml',
  'ini',
  'env',
  'ts',
  'js',
  'vue',
  'html',
  'htm',
  'css',
  'scss',
  'less',
  'java',
  'py',
  'go',
  'rs',
  'c',
  'cpp',
  'h',
  'sql',
  'properties',
  'conf',
])
const INSTALLER_EXTS = new Set(['exe', 'msi', 'apk', 'dmg', 'pkg'])
const NATIVE_VIDEO_EXTS = new Set(['mp4', 'webm', 'ogg'])

const getFileIcon = (fileType: string) => {
  const raw = (fileType || '').toLowerCase().trim()
  if (!raw) return Document
  if (raw.includes('/')) {
    if (raw.startsWith('image/')) return Picture
    if (raw.startsWith('video/')) return VideoCamera
    if (raw.startsWith('audio/')) return Headset
    if (raw === 'application/pdf' || raw.includes('pdf')) return Document
  }
  const ext = raw.replace(/^\./, '')
  if (ext === 'pdf') return Document
  if (IMAGE_EXTS.has(ext)) return Picture
  if (VIDEO_EXTS.has(ext)) return VideoCamera
  if (AUDIO_EXTS.has(ext)) return Headset
  if (ARCHIVE_EXTS.has(ext)) return TakeawayBox
  if (TEXT_CODE_EXTS.has(ext)) return Memo
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
  if (isExtractListView.value) {
    applyExtractPage()
  } else {
    handleSearch()
  }
}

// 处理每页大小变化
const handleSizeChange = (size: number) => {
  paginationInfo.value.pageSize = size
  paginationInfo.value.pageNum = 1 // 重置到第一页
  if (isExtractListView.value) {
    applyExtractPage()
  } else {
    handleSearch()
  }
}

// 重置搜索
const handleResetSearch = () => {
  searchKeyword.value = ''
  searchFileType.value = ''

  paginationInfo.value.pageNum = 1
  if (activeExtractCode.value) {
    ElMessage.info('已重置筛选条件，继续查看私有列表')
  }
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

  // 对账清理 myUploadedFiles，避免后端定时下架后本地仍残留上传令牌
  void reconcileMyUploadedFiles()

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

// 新增：提取码相关变量
const extractCode = ref('')
const extractLoading = ref(false)

// 封装提取逻辑
const checkAndExtract = () => {
  // 新增：如果当前不在首页或分享路径，不执行提取逻辑
  if (route.path !== '/' && !route.path.startsWith('/s/')) {
    return
  }

  // 1. 尝试从 Vue Router 对象获取 (常规做法)
  let codeFromUrl = route.params.code || route.query.code

  // 2. 如果 Router 没拿到 (Hash 模式下的冷启动 bug)，直接解析原生 Hash 字符串
  if (!codeFromUrl) {
    const hash = window.location.hash // 获取类似 "#/s/0WHNW"
    if (hash && hash.includes('/s/')) {
      // 这里的逻辑：分割字符串，取最后一个斜杠后面的部分
      const parts = hash.split('/')
      codeFromUrl = parts[parts.length - 1]
    }
  }

  console.log('--- 提取码解析结果 ---')
  console.log('原生 Hash:', window.location.hash)
  console.log('解析到的 Code:', codeFromUrl)

  if (codeFromUrl) {
    // 自动填充输入框
    extractCode.value = codeFromUrl

    // 触发后端接口请求
    setTimeout(() => {
      handleExtractByCode()
    }, 500)
  }
}

onMounted(() => {
  checkAndExtract()
})

// 关键：监听路由变化。
// 如果用户从 /s/A 页面直接点击另一个链接跳到 /s/B，onMounted 不会重新触发
watch(
  () => route.params.code,
  (newCode) => {
    if (newCode) {
      checkAndExtract()
    }
  },
)

/**
 * 通过取件码提取文件
 * 逻辑：后端根据 code 在 Redis 找到 batchUploadToken，再返回对应的文件列表
 */
const handleExtractByCode = async () => {
  const code = extractCode.value?.trim()
  if (!code) {
    ElMessage.warning('请输入取件码')
    return
  }

  extractLoading.value = true
  try {
    /*const response = await fetch(`/api/file/extract/${extractCode.value}`, {
      method: 'GET',
    })

    const result = await response.json()

    if (result.code === 200) {
      if (result.data && result.data.length > 0) {
        isExtractListView.value = true
        activeExtractCode.value = extractCode.value.trim()
        extractedFilesFull.value = result.data
        paginationInfo.value.pageNum = 1
        applyExtractPage()

        ElMessage.success(`成功提取 ${result.data.length} 个文件`)
      } else {
        isExtractListView.value = false
        activeExtractCode.value = ''
        extractedFilesFull.value = []
        nearbyFiles.value = []
        paginationInfo.value.total = 0
        ElMessage.info('该取件码下没有有效文件')
      }
    } else {
      ElMessage.error(result.message || '提取失败，请检查取件码是否正确')
    }*/
    // 这意味着后续的所有搜索（包括分页和筛选）都会带上这个码
    activeExtractCode.value = code

    // 3. 重置分页信息，因为这是从第一页重新开始显示提取的内容
    paginationInfo.value.pageNum = 1

    // 4. 直接复用 handleSearch 逻辑
    // 因为你在 handleSearch 内部已经加入了 locationService.getNearbyFiles(..., activeExtractCode.value)
    await handleSearch()

    // 5. 特殊处理：如果在 handleSearch 后发现没有结果，可能码是错的
    if (nearbyFiles.value.length === 0) {
      activeExtractCode.value = '' // 清空码，恢复状态
    }
  } catch (error) {
    console.error('提取文件出错:', error)
    ElMessage.error('网络错误，请稍后再试')
  } finally {
    extractLoading.value = false
  }
}

// --- 新增预览相关的响应式变量 ---
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const activeFile = ref<NearbyFile | null>(null)
const showInstallerInfo = ref(false)

// --- 复用你已有的常量进行逻辑判断 ---
const isImage = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '')
  return IMAGE_EXTS.has(ext)
}

const isDocOrText = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '')
  // 包含 PDF 以及你定义的全部文本/代码格式
  return ext === 'pdf' || TEXT_CODE_EXTS.has(ext)
}

const isVideo = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '').trim()
  return VIDEO_EXTS.has(ext)
}

const isAudio = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '').trim()
  return AUDIO_EXTS.has(ext)
}

const isInstaller = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '').trim()
  return INSTALLER_EXTS.has(ext)
}

// --- 重构后的预览入口函数 ---
const handlePreview = async (file: NearbyFile) => {
  if (!file) return

  // 判断逻辑：如果设置了最大下载次数(maxDownloads > 0)，且当前次数已达到或超过上限
  if (file.maxDownloads > 0 && file.downloadCount >= file.maxDownloads) {
    ElMessage.error('该文件预览/下载次数已达上限，无法访问')
    return
  }

  activeFile.value = file
  showInstallerInfo.value = false // 重置安装包标志
  previewUrl.value = '' // 清空预览链接
  archiveTree.value = [] // 清空压缩包树
  previewLoading.value = false // 确保不会带着上次的 loading 状态打开
  if (!file.downloadToken) {
    ElMessage.warning('该文件需要验证取件码后方可预览')
    return
  }

  const fileType = (file.fileType || '').toLowerCase().replace(/^\./, '').trim()
  if (isInstaller(file.fileType)) {
    // 不去 fetch 后端，直接展示一个精美的安装包图标和下载按钮
    showInstallerInfo.value = true
    previewVisible.value = true
    return
  }
  if (isArchive(file.fileType)) {
    previewLoading.value = true
    previewVisible.value = true // 先打开弹窗显示加载状态
    try {
      // 请求后端构建好的树形结构
      const response = await fetch(`/api/file/archive/list/${file.id}?token=${file.downloadToken}`)
      if (!response.ok) throw new Error('解析失败')
      archiveTree.value = await response.json()
    } catch (error) {
      ElMessage.error('无法读取压缩包目录')
      previewVisible.value = false
    } finally {
      previewLoading.value = false
    }
    return
  }

  // --- D. 核心判定：浏览器是否能“真正”渲染该文件 ---
  const isNativeVideo = NATIVE_VIDEO_EXTS.has(fileType)
  const isNativeAudio = ['mp3', 'wav', 'ogg', 'm4a', 'flac'].includes(fileType) // 常见浏览器原生支持
  const isNativeDoc = fileType === 'pdf' || TEXT_CODE_EXTS.has(fileType)
  const isNativeImage = IMAGE_EXTS.has(fileType)

  // 只有满足原生支持条件的，才允许进入预览流
  const canDirectPreview = isNativeImage || isNativeVideo || isNativeAudio || isNativeDoc

  if (canDirectPreview) {
    // 1. 只有能预览的才消耗下载次数
    if (typeof file.downloadCount === 'number') {
      file.downloadCount++
    }
    // 2. 执行加载
    previewLoading.value = true
    previewUrl.value = `/api/file/preview/${file.id}?token=${file.downloadToken}&t=${Date.now()}`
    previewVisible.value = true
  } else {
    // --- E. 拦截逻辑：针对 avi, mkv, docx 等已知但不支持预览的格式 ---
    // 这解决了你说的“一直加载”的问题
    const tipMsg = `文件 ".${fileType}" 是专有格式，暂不支持在线预览。`
    /*if (VIDEO_EXTS.has(fileType)) {
      tipMsg = `视频格式 ".${fileType}" 编码受限，浏览器无法直接播放。`;
    }*/

    ElMessageBox.confirm(`${tipMsg}建议您直接下载到本地查看。`, '提示', {
      confirmButtonText: '立即下载',
      cancelButtonText: '我知道了',
      type: 'info',
      distinguishCancelAndClose: true,
    })
      .then(() => {
        selectedFile.value = file
        downloadFile()
      })
      .catch(() => {
        // 保持安静
      })
  }
}

// 预览窗口内的快捷下载
const downloadFileFromPreview = () => {
  if (activeFile.value) {
    // 逻辑：关闭预览，开启详情弹窗来触发下载（或者直接调用你的 downloadFile）
    previewVisible.value = false
    selectedFile.value = activeFile.value
    downloadFile()
  }
}

const archiveTree = ref<any[]>([])

// 增加压缩包判断
const isArchive = (fileType: string) => {
  const ext = (fileType || '').toLowerCase().replace(/^\./, '').trim()
  return ARCHIVE_EXTS.has(ext)
}

const currentFileIsPrivate = computed(() => {
  return selectedFile.value?.isPrivate || false
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
  /* 新增：允许内容在空间不足时换行，并设置间距 */
  flex-wrap: wrap;
  gap: 12px;
  > span {
    order: 1;
    white-space: nowrap;
  }
  .code-extract-area {
    order: 2;
    /* 设定一个理想宽度 */
    flex-basis: 220px;
    /* 如果剩余空间不足以容纳它和按钮，它会尝试占据更大空间并换行 */
    flex-grow: 1;
    max-width: 280px;
    .el-input-group__append {
      background-color: var(--el-color-primary);
      color: white;
      &:hover {
        background-color: var(--el-color-primary-light-3);
      }
    }
  }
  .el-button {
    order: 3;
  }
}

/* 关键：针对移动端（小屏）的重排逻辑 */
@media (max-width: 768px) {
  .card-header {
    /* 1. 让按钮的 order 变小，排到标题后面去 */
    .el-button {
      order: 2;
    }

    /* 2. 让输入框的 order 变大，掉到最下面 */
    .code-extract-area {
      order: 3;
      flex: 1 1 100%; /* 强制占据 100% 宽度，独占一行 */
      max-width: none; /* 移除大屏时的宽度限制 */
      margin-top: 4px; /* 给第二行加点间距 */
    }

    /* 3. 确保标题和按钮分布在第一行的两头 */
    > span {
      flex: 1; /* 标题占据剩余空间，把按钮推向右侧 */
    }
  }
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
  .search-btn-group {
    display: flex;
    align-items: center;
    /* 1. 控制按钮之间的物理间距 (这里设为 4px，你可以根据需要调小) */
    gap: 10px;

    :deep(.el-button) {
      /* 2. 必须强制清除 Element Plus 默认给第二个按钮加的 12px 左外边距 */
      margin-left: 0 !important;
    }
  }
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
    position: relative; /* 必须为父级开启定位 */
    overflow: hidden; /* 保证内部遮罩不超出圆角 */
  }

  .file-info {
    flex: 1;
    min-width: 0;

    .file-name {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 4px;

      word-break: break-all; /* 强制长英文/数字在字符间断行，防止撑开容器 */

      /* 实现多行文本溢出显示省略号 (标准 WebKit 方法) */
      display: -webkit-box;
      -webkit-line-clamp: 2; /* 限制最多显示 2 行 */
      -webkit-box-orient: vertical;
      overflow: hidden;
      white-space: normal;
    }

    .file-meta {
      font-size: 11px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap; /* 空间不足自动换行 */
      gap: 4px 12px; /* 纵向间距4px，横向间距12px */

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
  :deep(.el-descriptions__body table) {
    table-layout: fixed;
    width: 100%;
  }

  .file-actions-dialog {
    margin-top: 24px;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .long-text-wrapper {
    /* 1. 允许换行 */
    word-break: break-all; /* 强制长数字/英文在字符间断行 */
    white-space: normal; /* 确保正常换行 */
  }

  /* 针对详情弹窗内的所有 alert 进行字体调整 */
  :deep(.el-alert) {
    --el-alert-title-font-size: 13px; /* 标题字体大小 */
    --el-alert-description-font-size: 13px; /* 描述字体大小 */
    --el-alert-title-with-description-font-size: 15px;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 20px 10px;
  border-top: 1px solid #ebeef5;
  width: 100%;
  box-sizing: border-box;

  /* 如果还是微量溢出，允许横向滚动而不是被裁切 */
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px 4px; /* 设置行间距和列间距 */

    /* 让 sizes 和 jumper 在拥挤时能整块移动 */
    .el-pagination__sizes,
    .el-pagination__jump {
      margin: 0;
    }
  }
}

/* 预览入口遮罩层 */
.preview-mask {
  position: absolute;
  bottom: 0; /* 底部对齐 */
  left: 0;
  right: 0;
  height: 30%; /* 覆盖图标下方约 1/3 */
  background: rgba(128, 128, 128, 0.4); /* 默认半透明灰色 */
  color: white;
  font-size: 11px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(2px); /* 磨砂效果，让 UI 显得更高档 */
  user-select: none;
  z-index: 10;
}

/* 悬浮变蓝色 */
.preview-mask:hover {
  background: rgba(64, 158, 255, 0.9); /* Element Plus 品牌蓝 */
  height: 40%; /* 悬浮时略微升高，增强交互感 */
}

.preview-body {
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f8f9fb;
  border-radius: 8px;
  overflow: hidden;
}

.img-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.preview-content-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 3. 视频标签适配逻辑 */
.preview-video {
  /* 核心：确保视频不会超出容器边界 */
  max-width: 100%;
  max-height: 100%;

  /* 自动适配：保持原始比例，且完整显示在容器内 */
  object-fit: contain;

  /* 移除浏览器默认边框/外轮廓 */
  outline: none;

  /* 确保控制栏有足够空间显示 */
  display: block;
}

.archive-preview {
  width: 100%;
  background: #fff;
  padding: 15px;
  border-radius: 4px;

  .archive-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 10px;
    font-weight: bold;
    color: #606266;
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 14px;

  .node-label {
    margin-left: 8px;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .node-size {
    font-size: 12px;
    color: #909399;
    margin-left: 10px;
  }
}

.preview-iframe {
  width: 100%;
  height: 75vh;
  border: none;
  background: white;
}

.preview-footer-info {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .warning-text {
    font-size: 12px;
    color: #f56c6c;
    font-style: italic;
  }
}

.clickable-link {
  color: #409eff;
  text-decoration: none;
  font-weight: bold; /* 加粗，增强引导性 */
}

.clickable-link:hover {
  text-decoration: underline;
}

.status-alert {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 12px;
  border: 1px solid transparent;
}

/* 私密文件样式 - 绿色系 */
.status-alert.private {
  background-color: #f0f9eb;
  color: #67c23a;
  border-color: #e1f3d8;
}

/* 公开文件样式 - 橙色系 */
.status-alert.public {
  background-color: #fdf6ec;
  color: #e6a23c;
  border-color: #faecd8;
}

/* 错误状态 - 红色系 */
.status-alert.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border-color: #fde2e2;
}

.qr-wrapper {
  background: #fff;
  padding: 5px;
  display: inline-block;
}

/* 深度选择器修改 Dialog 样式，让预览更沉浸 */
:deep(.preview-dialog) {
  .el-dialog__body {
    padding: 10px 20px;
  }
}
</style>
