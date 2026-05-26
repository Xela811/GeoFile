<template>
  <div class="home-page">
    <header class="top-nav">
      <div class="logo">GeoFile</div>
      <nav class="nav-menu">
        <router-link to="/">首页</router-link>
        <router-link to="/upload">上传文件</router-link>
      </nav>
    </header>

    <main class="main-content">
      <div class="page-header">
        <h1>GeoFile - 附近文件</h1>
        <p class="subtitle">基于地理位置的文件共享服务</p>
      </div>

      <div class="redirect-container">
        <el-result
          v-if="loading"
          icon="info"
          title="安全校验中"
          sub-title="正在为您校验地理下载资质，请确保浏览器已允许位置权限..."
        >
          <template #extra>
            <div class="loading-spinner"></div>
          </template>
        </el-result>

        <el-result
          v-else-if="downloadSuccess"
          icon="success"
          title="下载成功"
          sub-title="位置校验通过，文件已成功唤起系统下载！"
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </template>
        </el-result>

        <el-result v-else icon="error" title="下载受限" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()

const loading = ref(true)
const downloadSuccess = ref(false) // 新增一个成功标志变量
const errorMessage = ref('发生未知错误，无法完成下载')

const fileId = route.query.fileId
const token = route.query.token

onMounted(() => {
  if (!fileId || !token) {
    loading.value = false
    errorMessage.value = '下载凭证或文件参数残缺，解析失败'
    return
  }

  // 1. 唤起浏览器定位
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        // 定位获取成功，把经纬度送给后端拦截检验
        triggerBackendDownload(position.coords.latitude, position.coords.longitude)
      },
      (err) => {
        loading.value = false
        errorMessage.value = `获取地理位置失败 [Code: ${err.code}]: ${err.message}`
        ElMessage.error(`定位失败: ${err.message}`)
      },
      {
        enableHighAccuracy: false, // ✨ 改为 false，降低对物理 GPS 硬件的依赖，走网络模糊定位
        timeout: 15000, // ✨ 开发阶段放宽到 15 秒
      },
    )
  } else {
    loading.value = false
    errorMessage.value = '您的浏览器不支持环境 GPS 定位，无法完成安全校验'
  }
})

// 2. 携带经纬度请求后端直链
const triggerBackendDownload = async (lat: number, lng: number) => {
  try {
    const downloadUrl = `/api/file/download/${fileId}?token=${token}&lat=${lat}&lng=${lng}`
    /*const response = await fetch(downloadUrl, {
      headers: {
        // 显式告诉后端：我是异步请求，我只要数据或文件流，不要给我返回 HTML 网页！
        'X-Requested-With': 'XMLHttpRequest',
      },
    })

    if (!response.ok) {
      // 捕获后端抛出的 DownloadException 错误 JSON
      const errData = await response.json().catch(() => null)
      loading.value = false
      errorMessage.value = errData?.message || '超出分享地理范围，已被服务器拒绝下载'
      return
    }

    // 校验通过，后端响应了文件的二进制实体
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)

    // ✨ 2. 升级文件名提取（全面兼容 filename= 或 filename*=）
    let finalFileName = 'geofile_shared_file'
    const contentDisposition = response.headers.get('content-disposition')
    if (contentDisposition) {
      // 优先匹配标准的 filename*=UTF-8''，其次匹配通用的 filename="xxx"
      const match =
        contentDisposition.match(/filename\*=UTF-8''(.+)/) ||
        contentDisposition.match(/filename="?([^";]+)"?/)
      if (match && match[1]) {
        finalFileName = decodeURIComponent(match[1])
      }
    }

    // 触发盲点原生下载
    const a = document.createElement('a')
    a.href = url
    a.download = finalFileName
    document.body.appendChild(a)
    a.click()

    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)*/

    // 废弃掉在内存里拉取完整 blob 的沉重逻辑，直接利用隐藏的 a 标签指向后端的真实下载 URL
    const a = document.createElement('a')
    a.href = downloadUrl
    a.setAttribute('download', '') // 强行声明下载属性
    a.style.display = 'none'
    document.body.appendChild(a)
    
    a.click() // 夸克会识别为用户纯粹的下载意愿，直接拉起原生下载器
    
    document.body.removeChild(a)

    // 下载完毕，引导回首页
    downloadSuccess.value = true // 先确立成功标志
    loading.value = false
    ElMessage.success('位置校验通过，文件已开始下载！')
  } catch (e) {
    loading.value = false
    errorMessage.value = '网络交互失败，或服务器文件流冲刷崩溃'
  }
}
</script>

<style scoped lang="scss">
// 完美复刻 ErrorView 的页面骨架与菜单表现
.redirect-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80vh;
}
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

      &:hover {
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

// 专属的加载动画
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 10px auto;
}
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
