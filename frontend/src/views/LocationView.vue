<template>
  <div class="location-container">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome"> 返回首页 </el-button>
    </div>
    <el-card>
      <template #header>
        <h2>位置服务</h2>
      </template>

      <!-- 定位按钮 -->
      <div class="location-control">
        <el-button :type="location.type" :loading="isLocating" @click="getLocation">
          <el-icon><Location /></el-icon>
          {{ location.text }}
        </el-button>
        <el-button @click="refreshFiles">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 附近文件列表 -->
      <div class="location-files">
        <div v-if="nearbyFiles.length > 0">
          <div
            v-for="file in nearbyFiles"
            :key="file.id"
            class="file-item"
            @click="handleFileClick(file)"
          >
            <el-icon><Document /></el-icon>
            <div class="file-info">
              <div class="file-name">{{ file.fileName }}</div>
              <div class="file-meta">
                <span>{{ file.fileType }}</span>
                <span>{{ file.fileSize }}</span>
                <span>{{ file.distance }} km</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无附近文件" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Location, Refresh, Document, Back } from '@element-plus/icons-vue'
import { searchNearbyFiles, type NearbyFileVO } from '@/api/file'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router' // 2. 引入路由

const router = useRouter() // 初始化路由

// 3. 跳转函数
const goHome = () => {
  router.push('/') // 对应你路由配置中 HomeView.vue 的路径
}

const isLocating = ref(false)
const location = ref({ type: 'info', text: '点击定位' })
const nearbyFiles = ref<NearbyFileVO[]>([])

onMounted(() => {
  refreshFiles()
})

const getLocation = async () => {
  isLocating.value = true
  try {
    // 获取当前位置
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          location.value = {
            type: 'success',
            text: `定位成功: ${position.coords.latitude.toFixed(4)},
  ${position.coords.longitude.toFixed(4)}`,
          }
          await refreshFiles()
        },
        (error) => {
          ElMessage.error('定位失败: ' + error.message)
          location.value = { type: 'error', text: '定位失败' }
        },
      )
    } else {
      ElMessage.warning('浏览器不支持地理位置功能')
    }
  } finally {
    isLocating.value = false
  }
}

const refreshFiles = async () => {
  try {
    const res = await searchNearbyFiles({
      lat: 39.9042,
      lng: 116.4074,
      radius: 1000,
    })
    nearbyFiles.value = res as unknown as NearbyFileVO[]
  } catch {
    ElMessage.error('加载附近文件失败')
  }
}

const handleFileClick = (file: NearbyFileVO) => {
  window.open(`/api/file/download/${file.id}`, '_blank')
}
</script>

<style scoped lang="scss">
.location-container {
  max-width: 800px;
  margin: 0 auto;
}

.location-control {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.location-files {
  .file-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: #ecf5ff;
      transform: translateX(4px);
    }

    .file-info {
      flex: 1;

      .file-name {
        font-weight: 500;
        margin-bottom: 8px;
      }

      .file-meta {
        display: flex;
        gap: 12px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style>
