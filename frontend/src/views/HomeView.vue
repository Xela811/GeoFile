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

      <el-card class="location-card" v-loading="locationLoading">
        <template #header>
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: bold;"><span class="radar-dot"></span> 📍 共享定位中心</span>
            <el-button
              type="primary"
              :icon="Location"
              :loading="locationLoading"
              @click="handleGetCurrentLocation"
            >
              {{ locationInfo ? '刷新地理位置' : '扫瞄当前位置' }}
            </el-button>
          </div>
        </template>

        <div v-if="locationInfo" class="location-info-geek" style="padding: 5px 0; text-align: center;">
          <div style="display: inline-flex; align-items: center; gap: 12px; font-size: 14px; flex-wrap: wrap; justify-content: center;">
            <span style="font-weight: bold; color: #409eff;">
              {{ locationInfo.city || locationInfo.province || '未知城市' }}
            </span>
            
            <span style="color: #dcdfe6;">|</span>
            
            <span style="color: #606266;">
              E {{ locationInfo.lng.toFixed(2) }}° / N {{ locationInfo.lat.toFixed(2) }}°
            </span>
            
            <el-tag :type="useFixedCoordinates ? 'warning' : 'success'" size="small" effect="plain" style="height: 20px; line-height: 18px;">
              {{ useFixedCoordinates ? '模拟' : 'GPS' }}
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
              <el-button
                type="primary"
                size="small"
                :icon="Download"
                :disabled="isBtnDisabled(file)"
                @click.stop="handleQuickDownload(file)"
              >
                下载文件
              </el-button>
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
            <template v-if="isImage(activeFile?.fileType || '')">
              <div class="img-wrapper">
                <img :src="previewUrl" @load="previewLoading = false" class="preview-content-img" />
              </div>
            </template>

            <template v-else-if="isDocOrText(activeFile?.fileType || '')">
              <iframe
                :src="previewUrl"
                class="preview-iframe"
                @load="previewLoading = false"
              ></iframe>
            </template>

            <template v-else-if="isVideo(activeFile?.fileType || '')">
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

            <template v-else-if="isAudio(activeFile?.fileType || '')">
              <div class="audio-wrapper">
                <audio :src="previewUrl" controls autoplay @canplay="previewLoading = false">
                  您的浏览器不支持音频播放
                </audio>
              </div>
            </template>

            <template v-else-if="isArchive(activeFile?.fileType || '')">
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

            <template v-else-if="isInstaller(activeFile?.fileType || '')">
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
                    formatFileSize(activeFile?.fileSize ?? 0)
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
                      ((selectedFile.maxDownloads ?? 0) - (selectedFile.downloadCount || 0)) +
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
              :disabled="isFileExpiredRealtime || isMaxedOut || selectedFile?.distanceExceeded"
            >
              <el-icon><Download /></el-icon>
              {{
                isFileExpiredRealtime
                  ? '文件已过期'
                  : isMaxedOut
                    ? '下载次数已满'
                    : selectedFile?.distanceExceeded
                      ? '超出距离限制(>1km)'
                      : '下载文件'
              }}
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
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from 'vue'
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
const isCompLoading = ref(false)

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
  //return `${window.location.origin}/api/file/download/${selectedFile.value.id}?token=${selectedFile.value.downloadToken}`
  // 保持和 copyFileLink 决策树逻辑完全一致
  const isPublic = !currentFileIsPrivate.value
  const hasLocation =
    selectedFile.value.locationLat !== null && selectedFile.value.locationLat !== undefined

  if (isPublic && hasLocation) {
    // 公开带位置的文件：生成的二维码指向前端中转页
    return `${window.location.origin}/download-redirect?fileId=${selectedFile.value.id}&token=${selectedFile.value.downloadToken}`
  } else {
    // 私密文件或普通文件：生成的二维码直接指向后端下载流
    return `${window.location.origin}/api/file/download/${selectedFile.value.id}?token=${selectedFile.value.downloadToken}`
  }
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

    // 2. 降级高德：不再调 getDetailedAddress 详细接口了，除非想保留粗略城市。
    // 如果想要无开销的体验，可以直接通过高德的普通 IP/低精度粗略定位，或者直接从本地推算
    // 为了不报错，我们直接模拟一个干净的粗略结构，或者只调高德只拿城市名，不展示街道
    let coarseAddress = { province: '', city: '定位成功', district: '', township: '', formattedAddress: '' }
    try {
       // 如果你实在想留着城市名，可以调，但界面不要展示具体街道了
       const detailed = await locationService.getDetailedAddress(lat, lng)
       coarseAddress.city = detailed.city || detailed.province
    } catch(e) {
       coarseAddress.city = "未知区域"
    }

    // 3. 合并位置信息
    locationInfo.value = {
      lat,
      lng,
      city: coarseAddress.city,
      useFixedCoords: useFixedCoordinates.value,
      updateTime: new Date().toISOString(),
    } as any

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

// 记录上一次真正发起请求时的筛选条件（用于检测用户是否更改了输入）
const lastSearchKeyword = ref('')
const lastSearchFileType = ref('')

// 搜索附近文件（会退出「取件码私有列表」视图）
const handleSearch = async (mode = 'keep') => {
  //if (isExtractListView.value && mode !== 'force' && mode !== 'public') return
  // 核心拦截机制微调：如果当前处于公开分享链接展示模式，直接切入前端本地筛选，安全且速度极快
  if (isExtractListView.value && mode !== 'force' && mode !== 'public') {
    console.log('当前处于分享列表视图，执行纯前端多条件筛选...')
    applyExtractPage() // 触发上面改造好的过滤分页函数
    return // 结束执行，不去打扰后端接口
  }
  
  if (!locationInfo.value?.lat || !locationInfo.value?.lng) {
    ElMessage.warning('请先获取位置信息')
    return
  }

  const currentKeyword = searchKeyword.value?.trim() || ''
  const currentFileType = searchFileType.value || ''

  if (currentKeyword !== lastSearchKeyword.value || currentFileType !== lastSearchFileType.value) {
    console.log('检测到筛选条件发生变更，强行重置当前页码为 1')
    paginationInfo.value.pageNum = 1
    // 同步更新旧值记录
    lastSearchKeyword.value = currentKeyword
    lastSearchFileType.value = currentFileType
  }

  if (mode === 'public') {
    paginationInfo.value.pageNum = 1
    activeExtractCode.value = '' // 只有明确说要搜附近时，才清空码
    extractCode.value = ''

    lastSearchKeyword.value = ''
    lastSearchFileType.value = ''
    // 如果当前处于公开分享或取件码的列表视图，切换回附近文件模式
    if (isExtractListView.value) {
      isExtractListView.value = false
      extractedFilesFull.value = []
    }
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
      if (activeExtractCode.value && result.files.length === 0) {
        nearbyFiles.value = []
        paginationInfo.value.total = 0
        ElMessage.warning('该提取码下的文件已全部被删除或不存在')
        return // 直接跳出，不显示绿色的成功提示
      }

      nearbyFiles.value = result.files

      // 如果后端没有返回 total，则使用当前返回的数组长度作为兜底
      let finalTotal = result.total !== undefined ? result.total : result.files.length

      // 智能拦截：当用户选择了某种筛选条件（比如切换为"其他"），而我们恰好在第 1 页
      // 后端返回的列表长度小于一页的大小（说明后续根本没数据了），但后端返回的总数却依然大过列表长度（说明后端 Count 没带上筛选条件漏洞）
      if ((searchFileType.value || searchKeyword.value) && 
          paginationInfo.value.pageNum === 1 && 
          finalTotal > result.files.length && 
          result.files.length < paginationInfo.value.pageSize) {
        console.log(`检测到后端未适配"${searchFileType.value}"等条件的总数统计漏洞（原始总数：${finalTotal}），前端强制校准为真实筛选条数: ${result.files.length}`)
        finalTotal = result.files.length
      }

      // 更新分页信息
      if (finalTotal !== undefined) {
        paginationInfo.value.total = finalTotal
        paginationInfo.value.totalPages = Math.ceil(finalTotal / paginationInfo.value.pageSize)
        paginationInfo.value.hasPrevious = paginationInfo.value.pageNum > 1
        paginationInfo.value.hasNext =
          paginationInfo.value.pageNum < paginationInfo.value.totalPages
      } else {
        paginationInfo.value.total = result.files.length
        paginationInfo.value.totalPages = 1
        paginationInfo.value.hasPrevious = false
        paginationInfo.value.hasNext = false
      }
      if (!isExtractListView.value) {
  //  核心修正：只有在明确是主动发起搜索、切换模式时（mode === 'public'）或者处于提取码初始化时，才弹出总数提示
  // 翻页时（mode === 'keep'）保持静默，不触发 ElMessage 轰炸用户
  if (mode === 'public' || paginationInfo.value.pageNum === 1) {
    // 核心修正：msg 提示的总数应当是 finalTotal（也就是后端统计出的真实总文件数），而不是当前页的 files.length
    const msg = activeExtractCode.value
      ? `已成功提取私有文件，共 ${finalTotal} 个`
      : `搜索成功，共找到附近的 ${finalTotal} 个文件`
      
    ElMessage.success(msg)
  }
}
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
    // 捕获后端 throw 的 IllegalArgumentException 的 message
    const errorMsg = error.response?.data?.message || error.message || '搜索附近文件失败'
    ElMessage.error(errorMsg)

    nearbyFiles.value = []
    paginationInfo.value.total = 0

    if (errorMsg.includes('过期') || errorMsg.includes('不存在')) {
      activeExtractCode.value = ''
    }
  } finally {
    // 确保无论成功或失败都会关闭加载状态
    if (filesLoading.value === true) {
      filesLoading.value = false
    }
  }
}

/** 取件码模式：根据全量列表与当前页做前端分页，写入 nearbyFiles */
const applyExtractPage = () => {
  // 1. 核心改动：在分页之前，先根据前端搜索框输入的 keyword 和 fileType 对全量数据进行过滤
  let filteredList = [...extractedFilesFull.value]

  // 按文件名模糊检索 (忽略大小写)
  if (searchKeyword.value && searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    filteredList = filteredList.filter(file => 
      file.fileName && file.fileName.toLowerCase().includes(keyword)
    )
  }

  // 按文件类型精确过滤
  if (searchFileType.value) {
    filteredList = filteredList.filter(file => {
      if (!file.fileType) return false
      
      const rawType = file.fileType.toLowerCase().trim() // 后端返回的后缀名，如 png, mp4
      let mappedType = '' // 前端大类标签

      //  直接调用你写好的函数和 Set 进行归类
      if (isImage(rawType)) {
        mappedType = 'image'        // 匹配图片大类
      } else if (isVideo(rawType)) {
        mappedType = 'video'        // 匹配视频大类
      } else if (isAudio(rawType)) {
        mappedType = 'audio'        // 匹配音频大类
      } else if (isDocOrText(rawType)) {
        mappedType = 'text'         // 匹配文档/文本大类
      } else if (ARCHIVE_EXTS.has(rawType)) {
        mappedType = 'archive'      // 匹配压缩包大类
      } else {
        mappedType = 'other'        // 其他
      }

      // 比较映射后的大类是否和搜索框选中的 `searchFileType.value` 一致
      // 兼容性兜底：如果搜索框本身选的就是具体后缀 'png'，也允许全等通过
      return mappedType === searchFileType.value || rawType === searchFileType.value.toLowerCase()
    })
  }

  //const total = extractedFilesFull.value.length
  const total = filteredList.length
  paginationInfo.value.total = total
  const pageSize = paginationInfo.value.pageSize
  let pageNum = paginationInfo.value.pageNum
  const maxPage = Math.max(1, Math.ceil(total / pageSize) || 1)
  if (pageNum > maxPage) {
    pageNum = maxPage
    paginationInfo.value.pageNum = pageNum
  }
  const start = (pageNum - 1) * pageSize
  //nearbyFiles.value = extractedFilesFull.value.slice(start, start + pageSize)
  nearbyFiles.value = filteredList.slice(start, start + pageSize)
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

// HomeView.vue 中新增一个清理取件码本地存储的辅助函数
const syncPickupBatchesAfterDelete = (fileId: number, uploadToken: string) => {
  const PICKUP_BATCHES_STORAGE_KEY = 'geofile_private_pickup_batches' // 请确保这个 Key 与 UploadView 中一致
  const savedBatchesStr = localStorage.getItem(PICKUP_BATCHES_STORAGE_KEY)

  if (!savedBatchesStr) return

  try {
    let localBatches = JSON.parse(savedBatchesStr)
    let changed = false

    localBatches = localBatches.filter((batch: any) => {
      if (batch.uploadToken === uploadToken) {
        // 如果这个批次只剩这一个文件了，直接把整个批次壳子删掉
        // 注意：因为你的 UploadView 存储时可能没存 files，这里要兼容处理
        if (!batch.files || batch.files.length <= 1) {
          changed = true
          return false // 移除批次
        }
        // 如果还有多个，则过滤掉当前这一个
        batch.files = batch.files.filter((f: any) => f.id !== fileId)
        changed = true
      }
      return true
    })

    if (changed) {
      localStorage.setItem(PICKUP_BATCHES_STORAGE_KEY, JSON.stringify(localBatches))
      console.log('同步清理本地取件码记录成功')
    }
  } catch (e) {
    console.error('同步取件码本地存储失败', e)
  }
}

// 全量同步函数
const syncAllLocalBatchesAfterDelete = (fileId: number, uploadToken: string) => {
  // 定义所有需要同步的 Key
  const STORAGE_KEYS = {
    PRIVATE: 'geofile_private_pickup_batches',
    PUBLIC: 'geofile_public_upload_batches',
  }

  Object.values(STORAGE_KEYS).forEach((key) => {
    const savedStr = localStorage.getItem(key)
    if (!savedStr) return

    try {
      let localBatches = JSON.parse(savedStr)
      let changed = false

      localBatches = localBatches.filter((batch: any) => {
        if (batch.uploadToken === uploadToken) {
          // 如果该批次只有一个文件（即当前被删的文件），则直接移除整个批次
          if (!batch.files || batch.files.length <= 1) {
            changed = true
            return false
          }
          // 否则，从文件列表中过滤掉该文件
          const originalLen = batch.files.length
          batch.files = batch.files.filter((f: any) => f.id !== fileId)
          if (batch.files.length !== originalLen) changed = true
        }
        return true
      })

      if (changed) {
        localStorage.setItem(key, JSON.stringify(localBatches))
        console.log(`已同步清理本地存储 [${key}]`)
      }
    } catch (e) {
      console.error(`同步清理 ${key} 失败`, e)
    }
  })
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

      // 新增：同步清理取件码批次记录
      // 传入当前删除的文件 ID 和它所属的 uploadToken
      syncAllLocalBatchesAfterDelete(file.id, uploadToken)

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
const viewFileDetail = async (file: NearbyFile) => {
  console.log('当前点击的文件详情数据:', file)
  //selectedFile.value = file
  //showDetailDialog.value = true
  // 1. 先展示旧数据，让弹窗秒开，提升用户体验
  selectedFile.value = { ...file }
  showDetailDialog.value = true

  try {
    // 2. 立即请求后端获取最新状态
    const response = await fetch(`/api/file/detail/${file.id}${getPosParams()}`)
    const resData = await response.json()

    if (resData.code === 200) {
      // 3. 用后端最新数据覆盖旧数据
      selectedFile.value = resData.data

      // 4. 同步更新列表中的那一行，保证背景列表也是准的
      const index = nearbyFiles.value.findIndex((f) => f.id === file.id)
      if (index !== -1) {
        nearbyFiles.value[index] = resData.data
      }
      // 如果发现超距了，顺便给个气泡提示
      if (resData.data.distanceExceeded) {
        ElMessage.error('检测到您当前距离此文件过远（已超1km），该文件将无法下载')
      }
      console.log('详情已同步最新下载次数:', resData.data.downloadCount)
    }
  } catch (error) {
    console.error('同步文件详情失败:', error)
  }
}

// 快速下载逻辑
const handleQuickDownload = (file: any) => {
  if (file.distanceExceeded) {
    ElMessage.error('由于距离超过1km，无法进行快速下载')
    return
  }

  if (isBtnDisabled(file)) {
    ElMessage.error('文件已失效或达到下载上限')
    return
  }

  selectedFile.value = file // 必须先给 selectedFile 赋值，因为 downloadFile 依赖它
  nextTick(() => {
    downloadFile() // 直接调用你代码中已有的 downloadFile 方法
  })
}

// 统一判定函数，确保列表按钮和详情按钮逻辑一致
const isBtnDisabled = (file: any) => {
  // 1. 判断后端状态拦截 (status=0已删, status=2过期)
  if (file.status === 0 || file.status === 2) return true

  // 新增：判断是否超出 1km 距离限制
  if (file.distanceExceeded) return true

  // 2. 判断下载次数是否达到上限
  // 对应源码中的 isMaxedOut 逻辑
  const isMaxedOutLocal = file.maxDownloads > 0 && (file.downloadCount || 0) >= file.maxDownloads

  // 3. 判断是否实时过期
  // 对应源码中的 isFileExpiredRealtime 逻辑
  const isExpiredLocal = file.expireTime && new Date(file.expireTime) < new Date()

  return isMaxedOutLocal || isExpiredLocal
}

// 辅助函数：拼接当前位置参数
const getPosParams = () => {
  const lat = locationInfo.value?.lat
  const lng = locationInfo.value?.lng
  return lat && lng ? `?lat=${lat}&lng=${lng}` : ''
}

// 下载文件
const downloadFile = async () => {
  
  // 1. 基础校验
  if (!selectedFile.value) {
    ElMessage.warning('请先选择一个文件')
    return
  }

  // --- 新增：下载前最后一次同步校验 ---
  try {
    const syncRes = await fetch(`/api/file/detail/${selectedFile.value.id}${getPosParams()}`)
    const syncData = await syncRes.json()
    if (syncData.code === 200) {
      selectedFile.value = syncData.data

      // 拦截超距
      if (selectedFile.value!.distanceExceeded) {
        ElMessage.error('当前距离已超出1km限制，已被服务器拒绝下载')
        return
      }

      // 如果同步后发现已经满了，直接拦截，不走下载流
      if (
        (selectedFile.value!.maxDownloads ?? 0) > 0 &&
        (selectedFile.value!.downloadCount ?? 0) >= (selectedFile.value!.maxDownloads ?? 0)
      ) {
        ElMessage.error('该文件刚刚已达到下载次数上限')
        return
      }
    }
  } catch (e) {
    console.warn('下载前同步失败，将尝试直接下载', e)
  }
  // --- 同步结束 ---

  // 2. 直接从当前选中的文件对象中获取 token
  const token = selectedFile.value!.downloadToken

  if (!token) {
    ElMessage.error('该文件缺少下载凭证，请刷新列表重试')
    return
  }

  try {
    ElMessage.success('开始下载文件...')

    // ====================  修复核心：安全拼接参数，杜绝多问号和丢 Token Bug ====================
    // 1. 提取基础参数
    const fileId = selectedFile.value!.id
    const token = selectedFile.value!.downloadToken!

    // 2. 使用原生的 URLSearchParams 动态组装参数，这样绝对不会漏掉或错乱
    const params = new URLSearchParams()
    params.append('token', token)

    // 3. 如果能获取到位置参数，安全地追加进去
    if (typeof getPosParams === 'function') {
      // 解析你 getPosParams() 返回的内容，防止它内部带了 '?' 导致冲突
      const posStr = getPosParams().replace('?', '') // 把可能存在的问号去掉
      if (posStr) {
        const posParams = new URLSearchParams(posStr)
        if (posParams.has('lat')) params.append('lat', posParams.get('lat')!)
        if (posParams.has('lng')) params.append('lng', posParams.get('lng')!)
      }
    }

    console.log('实际请求的下载 URL 完整参数为:', params.toString())

    // 3. 使用 fetch 获取文件，这样可以捕获错误
    const response = await fetch(`/api/file/download/${fileId}?${params.toString()}`)

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
    a.download = selectedFile.value!.fileName || 'file'
    document.body.appendChild(a)
    a.click()

    // 6. 清理
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('下载成功')

    // 不要手动 += 1，而是直接请求后端拿最权威的数据
    const finalRes = await fetch(`/api/file/detail/${selectedFile.value!.id}`)
    const finalData = await finalRes.json()
    if (finalData.code === 200) {
      selectedFile.value = finalData.data
    }

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

  let url = ''

  // 1. 公开文件判定
  const isPublic = !currentFileIsPrivate.value

  // 2.  全字段兼容性防御
  // 看看后端到底是返回的 locationLat、locationLng 还是小写的 lat、lng 或者是字符形式
  const fileObj = selectedFile.value as any

  // 打印完整的对象结构，让你在控制台一眼看到地理位置字段叫什么名字！
  console.log('【Debug 核心文件对象实体】:', fileObj)

  const hasLocation =
    (fileObj.locationLat !== null &&
      fileObj.locationLat !== undefined &&
      fileObj.locationLat !== '') ||
    (fileObj.lat !== null && fileObj.lat !== undefined && fileObj.lat !== '') ||
    (fileObj.latitude !== null && fileObj.latitude !== undefined && fileObj.latitude !== '')

  console.log('【Debug 分流决策修正版】', { isPublic, hasLocation })

  //  核心分流决策树
  if (isPublic && hasLocation) {
    // 【分流 A】：如果是公开且有位置的文件  走前端中转验证页面
    url = `${window.location.origin}/download-redirect?fileId=${selectedFile.value.id}&token=${selectedFile.value.downloadToken}`
  } else {
    // 【分流 B】：如果是私密文件或普通无位置文件 ️ 直接提供后端的直连盲抓流链接
    url = `${window.location.origin}/api/file/download/${selectedFile.value.id}?token=${selectedFile.value.downloadToken}`
  }

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

  const path = window.location.hash || window.location.pathname
  const urlParams = new URLSearchParams(window.location.hash.split('?')[1])
  const isSharing = path.includes('/s/') || path.includes('/b/') || urlParams.has('uploadToken')

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
        if (isSharing) {
          console.log('检测到分享链接，跳过常规附近搜索')
          return // 直接结束，不弹提示，也不执行搜索
        }
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
  if (route.path !== '/' && !route.path.startsWith('/s/') && !route.path.startsWith('/b/')) {
    return
  }

  // 1. 尝试从 Vue Router 对象获取 (常规做法)
  let codeFromUrl = route.params.code || route.query.code

  // 解析公开令牌 (Token)
  let tokenFromUrl = route.params.token || route.query.token

  // 2. 如果 Router 没拿到 (Hash 模式下的冷启动 bug)，直接解析原生 Hash 字符串
  if (!codeFromUrl && !tokenFromUrl) {
    const hashOrUrl = window.location.href
    if (hashOrUrl.includes('/s/')) {
      codeFromUrl = hashOrUrl.split('/s/')[1]?.split('?')[0]
    } else if (hashOrUrl.includes('/b/')) {
      tokenFromUrl = hashOrUrl.split('/b/')[1]?.split('?')[0]
    }
  }

  console.log('--- 提取码解析结果 ---')
  console.log('原生 Hash:', window.location.hash)
  console.log('解析到的 Code:', codeFromUrl)

  if (codeFromUrl) {
    const finalCode = Array.isArray(codeFromUrl) ? codeFromUrl[0] : codeFromUrl
    // 自动填充输入框
    extractCode.value = finalCode || ''

    // 触发后端接口请求
    setTimeout(() => {
      handleExtractByCode()
    }, 500)
  } else if (tokenFromUrl) {
    // 公开分享不需要填充输入框，直接调用根据 Token 加载的逻辑
    setTimeout(() => {
      const finalToken = Array.isArray(tokenFromUrl) ? tokenFromUrl[0] : tokenFromUrl
      handleLoadPublicBatchByToken(finalToken || '')
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
  { immediate: true }
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

/**
 * 通过公开分享令牌加载文件批次
 * 逻辑：直接调用后端 list-by-token 接口，并进入列表展示模式
 */
const handleLoadPublicBatchByToken = async (token: string) => {
  if (!token) return

  isCompLoading.value = true // 全局加载状态
  try {
    // 1. 设置状态：进入“提取列表视图”模式
    // 这样界面会自动切换到表格展示，而不是地图/卡片流
    //isExtractListView.value = true

    // 2. 清空之前的取件码状态，确保不冲突
    activeExtractCode.value = ''
    extractCode.value = ''

    // 3. 重置分页信息
    paginationInfo.value.pageNum = 1

    // 4. 获取数据
    // 注意：这里不直接复用 handleSearch，因为 handleSearch 依赖地理坐标或提取码
    // 公开分享链接通常允许用户直接看到这批文件（即你新增的后端接口）
    const res = await fetch(`/api/file/list-by-token?uploadToken=${encodeURIComponent(token)}`)
    const json = await res.json()

    if (json.code === 200 && Array.isArray(json.data) && json.data.length > 0) {
      // 只有真正拿到文件了，才切换到提取列表视图
      isExtractListView.value = true

      // 5. 填充全量列表并进行前端分页处理
      // 这里复用你系统中处理“提取结果”的逻辑
      extractedFilesFull.value = json.data
      paginationInfo.value.total = json.data.length

      // 调用你的分页切片函数
      applyExtractPage()

      ElMessage.success('已加载分享的公开文件')
    } else {
      //ElMessage.error(json.message || '分享链接已失效')
      //isExtractListView.value = false // 失败则退出列表模式
      // 修改点：如果 code 不对，或者数组是空的，都视为失效
      const errorMsg =
        json.data?.length === 0
          ? '该分享批次不包含任何文件或已失效'
          : json.message || '分享链接已失效'
      ElMessage.error(errorMsg)

      // 关键：强制设为常规模式并触发定位/搜索
      exitExtractModeAndSearch()
    }
  } catch (error) {
    console.error('加载公开批次出错:', error)
    ElMessage.error('网络错误，请稍后再试')
    //isExtractListView.value = false
    exitExtractModeAndSearch()
  } finally {
    isCompLoading.value = false
  }
}

// 辅助函数：退出提取模式并恢复常规搜索
const exitExtractModeAndSearch = () => {
  isExtractListView.value = false
  extractedFilesFull.value = []
  // 触发一次常规的定位和搜索，让页面不至于空白
  // @ts-ignore
  if (typeof initLocation === 'function') {
    // @ts-ignore
    initLocation()
  } else {
    handleSearch('public')
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
  if ((file.maxDownloads ?? 0) > 0 && (file.downloadCount ?? 0) >= (file.maxDownloads ?? 0)) {
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
  return selectedFile.value?.isPrivate === 1
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
  margin: 8px 0 12px 0;

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
    letter-spacing: 0.5px;
  }
}

.mode-switch {
  margin-bottom: 10px;
}

.location-card,
.files-card {
  margin-bottom: 10px;

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
  .file-actions {
    flex-direction: column;
    align-items: stretch; /* 按钮宽度撑满 */
    width: 100px; /* 给定固定宽度防止挤压 */
  }
  .file-actions .el-button {
    margin-left: 0 !important; /* 强制清除 Element Plus 按钮默认的左边距 */
    width: 100%;
  }
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
  margin-top: 10px;
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
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: nowrap;
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
