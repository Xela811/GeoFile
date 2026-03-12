<template>
  <div class="location-page">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome"> 返回首页 </el-button>
    </div>

    <!-- 标题区域 -->
    <div class="page-header">
      <h1>附近文件</h1>
      <p class="subtitle">
        基于HTML5 Geolocation API的地理位置搜索服务
        <el-tag
          :type="useFixedCoordinates ? 'warning' : 'success'"
          size="small"
          style="margin-left: 12px"
        >
          {{ useFixedCoordinates ? '开发测试模式' : '生产模式' }}
        </el-tag>
      </p>
      <div class="mode-switch">
        <el-switch
          v-model="useFixedCoordinates"
          active-text="使用固定坐标"
          inactive-text="使用实际GPS"
          active-color="#e6a23c"
          inactive-color="#67c23a"
        />
      </div>
    </div>

    <!-- 位置信息卡片 -->
    <el-card class="location-card" v-loading="locationLoading">
      <template #header>
        <div class="card-header">
          <span>当前位置信息</span>
          <el-button
            type="primary"
            :icon="Location"
            :loading="locationLoading"
            @click="handleGetCurrentLocation"
          >
            获取当前位置
          </el-button>
        </div>
      </template>

      <div v-if="locationInfo" class="location-info">
        <!-- 坐标信息 -->
        <div class="coordinate-section">
          <h3>📍 位置坐标</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="纬度">{{
              locationInfo.lat.toFixed(6)
            }}</el-descriptions-item>
            <el-descriptions-item label="经度">{{
              locationInfo.lng.toFixed(6)
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 详细地址信息 -->
        <div class="address-section">
          <h3>🏠 详细地址</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="省份">{{ locationInfo.province }}</el-descriptions-item>
            <el-descriptions-item label="城市">{{ locationInfo.city }}</el-descriptions-item>
            <el-descriptions-item label="区县">{{ locationInfo.district }}</el-descriptions-item>
            <el-descriptions-item label="街道">{{ locationInfo.township }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="!locationInfo.formattedAddress"
            title="地址信息不完整"
            type="warning"
            :closable="false"
            style="margin-top: 12px"
          >
            地址信息可能获取失败，请重试
          </el-alert>
        </div>

        <!-- 搜索半径 -->
        <div class="radius-section">
          <h3>🔍 搜索范围</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="搜索半径"
              >{{ locationInfo.radius }}米</el-descriptions-item
            >
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

      <el-empty v-else description="请点击获取当前位置按钮" />
    </el-card>

    <!-- 附近文件搜索 -->
    <el-card class="search-card" v-loading="filesLoading">
      <template #header>
        <div class="card-header">
          <span>附近文件 ({{ nearbyFiles.length }})</span>
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
            <el-button type="primary" :icon="Search" :loading="filesLoading" @click="handleSearch">
              开始搜索
            </el-button>
          </el-form-item>
        </el-form>
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

      <el-empty v-else-if="locationInfo?.lat && locationInfo?.lng" description="附近没有找到文件" />
      <el-empty v-else description="请先获取位置信息" />
    </el-card>

    <!-- 文件详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="文件详情" width="600px">
      <div v-if="selectedFile" class="file-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件名">{{ selectedFile.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ selectedFile.fileType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{
            formatFileSize(selectedFile.fileSize)
          }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{
            selectedFile.uploadTime
          }}</el-descriptions-item>
          <el-descriptions-item label="有效期至">{{
            selectedFile.expireTime
          }}</el-descriptions-item>
          <el-descriptions-item label="下载次数">{{
            selectedFile.downloadCount || 0
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="file-actions-dialog">
          <el-button type="primary" size="large" @click="downloadFile">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
          <el-button size="large" @click="copyFileLink">
            <el-icon><Link /></el-icon>
            复制链接
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Back,
  Location,
  Search,
  Download,
  Link,
  Picture,
  Document,
  VideoCamera,
  Headset,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import locationService, { LocationInfo, NearbyFile } from '@/services/locationService'
import { watch } from 'vue'

const router = useRouter()

// 位置信息
const locationInfo = ref<LocationInfo | null>(null)
const locationLoading = ref(false)

// 开发测试配置
const useFixedCoordinates = ref(false)
const FIXED_LAT = 38.914
const FIXED_LNG = 121.614

// 附近文件
const nearbyFiles = ref<NearbyFile[]>([])
const filesLoading = ref(false)
const searchRadius = ref(1000)

// 文件详情
const detailDialogVisible = ref(false)
const selectedFile = ref<NearbyFile | null>(null)

// 监听模式切换
watch(useFixedCoordinates, (newVal) => {
  if (newVal) {
    ElMessage.warning('已切换到开发测试模式：使用固定坐标')
  } else {
    ElMessage.success('已切换到生产模式：使用实际GPS坐标')
  }
})

// 初始化
onMounted(() => {
  // 检查是否已经有位置信息（可以从localStorage读取）
  const savedLocation = localStorage.getItem('userLocation')
  if (savedLocation) {
    try {
      locationInfo.value = JSON.parse(savedLocation)
      // 自动搜索附近文件
      handleSearch()
    } catch (error) {
      console.error('解析位置信息失败', error)
    }
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

    // 提示用户当前使用的模式
    if (useFixedCoordinates.value) {
      ElMessage.info('开发测试模式：使用固定坐标')
    } else {
      ElMessage.info('生产模式：使用实际GPS坐标')
    }

    // 1. 获取经纬度坐标（使用固定的或实际的）
    const { lat, lng } = await getCoordinates()

    // 2. 获取详细地址信息
    ElMessage.info('正在解析地址信息...')

    console.log('使用的坐标:', { lat, lng })

    const detailedAddress = await locationService.getDetailedAddress(lat, lng)

    // 3. 合并位置信息
    locationInfo.value = {
      ...basicLocation,
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

  try {
    filesLoading.value = true
    ElMessage.info('正在搜索附近文件...')

    const result = await locationService.getNearbyFiles(
      locationInfo.value.lat,
      locationInfo.value.lng,
      searchRadius.value,
    )

    nearbyFiles.value = result.files
    ElMessage.success(`找到 ${result.count} 个附近文件`)
  } catch (error: any) {
    ElMessage.error(error.message || '搜索附近文件失败')
    console.error('搜索附近文件失败', error)
  } finally {
    filesLoading.value = false
  }
}

// 查看文件详情
const viewFileDetail = (file: NearbyFile) => {
  selectedFile.value = file
  detailDialogVisible.value = true
}

// 下载文件
const downloadFile = () => {
  if (selectedFile.value) {
    ElMessage.info('下载功能开发中...')
    // TODO: 实现文件下载功能
    console.log('下载文件:', selectedFile.value)
  }
}

// 复制文件链接
const copyFileLink = () => {
  if (selectedFile.value) {
    const link = `${window.location.origin}/file/${selectedFile.value.id}`
    navigator.clipboard
      .writeText(link)
      .then(() => {
        ElMessage.success('链接已复制到剪贴板')
      })
      .catch(() => {
        ElMessage.error('复制失败')
      })
  }
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
  if (fileType.includes('pdf')) return 'Document'
  if (fileType.includes('image')) return 'Picture'
  if (fileType.includes('video')) return 'VideoCamera'
  if (fileType.includes('audio')) return 'Headset'
  return 'Document'
}

// 返回首页
const goHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.location-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  margin-bottom: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;

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

  .mode-switch {
    margin-top: 10px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-card {
  margin-top: 20px;
}

.search-params {
  margin-bottom: 20px;
}

// 位置信息卡片样式
.location-info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.coordinate-section,
.address-section,
.radius-section {
  h3 {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 12px;
    padding-left: 12px;
    border-left: 4px solid #409eff;
  }
}

.formatted-address {
  h3 {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 12px;
    padding-left: 12px;
    border-left: 4px solid #67c23a;
  }
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
}

.file-icon {
  margin-right: 16px;
  color: #409eff;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.file-meta {
  font-size: 12px;
  color: #909399;

  .divider {
    margin: 0 8px;
  }
}

.file-actions {
  margin-left: 16px;
}

.file-actions-dialog {
  margin-top: 30px;
  text-align: center;
  padding: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
