# GeoFile 前端项目

## 技术栈
- Vue 3
- TypeScript
- ElementPlus
- Axios
- Pinia
- Vue Router
- ECharts

## 快速开始

### 1. 项目初始化

```bash
# 使用Vue CLI创建项目
npm create vue@latest
# 选择 TypeScript、Pinia、Vue Router

# 安装依赖
cd frontend
npm install

# 安装ElementPlus
npm install element-plus
npm install @element-plus/icons-vue

# 安装其他依赖
npm install axios pinia vue-router
npm install -D sass
```

### 2. 项目结构

```
frontend/
├── public/
├── src/
│   ├── api/                 # API接口
│   │   ├── file.ts
│   │   ├── upload.ts
│   │   ├── download.ts
│   │   └── geo.ts
│   ├── assets/              # 静态资源
│   ├── components/          # 公共组件
│   │   ├── UploadFile.vue
│   │   ├── FileList.vue
│   │   ├── PreviewDialog.vue
│   │   └── GeoLocation.vue
│   ├── layouts/             # 布局组件
│   ├── router/              # 路由配置
│   ├── stores/              # 状态管理
│   ├── utils/               # 工具函数
│   ├── views/               # 页面组件
│   │   ├── Login.vue
│   │   ├── Upload.vue
│   │   ├── FileList.vue
│   │   ├── FileDetail.vue
│   │   ├── Preview.vue
│   │   └── NotFound.vue
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
└── vite.config.ts
```

### 3. 基础配置

#### main.ts
```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

#### vite.config.ts
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 4. API接口封装

#### api/file.ts
```typescript
import request from '@/utils/request'

export interface FileVO {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  originalName: string
  uploadTime: string
  expireTime: string
  downloadCount: number
  status: number
  locationLat?: number
  locationLng?: number
  locationRadius?: number
}

// 获取文件列表
export function getFileList(params: any) {
  return request.get('/files', { params })
}

// 获取文件详情
export function getFileDetail(id: number) {
  return request.get(`/files/${id}`)
}

// 上传文件
export function uploadFile(formData: FormData) {
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除文件
export function deleteFile(id: number) {
  return request.delete(`/files/${id}`)
}
```

#### api/geo.ts - 地理位置API
```typescript
import request from '@/utils/request'

// 获取用户位置
export function getUserLocation() {
  return request.get('/geo/location')
}

// 搜索附近的文件
export function searchNearbyFiles(lat: number, lng: number, radius: number) {
  return request.get('/geo/search', {
    params: { lat, lng, radius }
  })
}
```

#### utils/request.ts - Axios封装
```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default service
```

### 5. 路由配置

#### router/index.ts
```typescript
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Upload',
        component: () => import('@/views/Upload.vue')
      },
      {
        path: 'files',
        name: 'FileList',
        component: () => import('@/views/FileList.vue')
      },
      {
        path: 'files/:id',
        name: 'FileDetail',
        component: () => import('@/views/FileDetail.vue')
      },
      {
        path: 'preview/:id',
        name: 'Preview',
        component: () => import('@/views/Preview.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
```

### 6. 页面组件示例

#### views/Upload.vue - 文件上传页面
```vue
<template>
  <div class="upload-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
        </div>
      </template>

      <el-upload
        class="upload-demo"
        drag
        action="/api/files/upload"
        :on-success="handleSuccess"
        :before-upload="beforeUpload"
        :file-list="fileList"
        multiple
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 jpg/png/pdf/zip/rar/txt 文件，且不超过 100MB
          </div>
        </template>
      </el-upload>

      <div class="location-section">
        <el-divider />
        <h4>文件位置设置</h4>
        <el-form :model="form" label-width="100px">
          <el-form-item label="所在区域">
            <el-input v-model="form.region" placeholder="请选择区域" />
          </el-form-item>
          <el-form-item label="搜索半径">
            <el-slider v-model="form.radius" :min="100" :max="5000" :step="100" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const fileList = ref<any[]>([])
const form = ref({
  region: '',
  radius: 1000
})

const beforeUpload = (file: File) => {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('上传文件大小不能超过 100MB!')
    return false
  }
  return true
}

const handleSuccess = (response: any) => {
  ElMessage.success('上传成功')
}
</script>

<style scoped>
.upload-page {
  padding: 20px;
}
.upload-demo {
  margin-bottom: 30px;
}
</style>
```

#### views/FileDetail.vue - 文件详情
```vue
<template>
  <div class="file-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack">返回</el-button>
          <span>文件详情</span>
        </div>
      </template>

      <div v-if="fileInfo" class="file-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件名">{{ fileInfo.fileName }}</el-descriptions-item>
          <el-descriptions-item label="原始文件名">{{ fileInfo.originalName }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(fileInfo.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ fileInfo.uploadTime }}</el-descriptions-item>
          <el-descriptions-item label="有效截止">{{ fileInfo.expireTime }}</el-descriptions-item>
          <el-descriptions-item label="下载次数">{{ fileInfo.downloadCount }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button type="primary" @click="handleDownload">下载文件</el-button>
          <el-button type="warning" @click="handlePreview">在线预览</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFileDetail } from '@/api/file'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const fileInfo = ref<any>(null)

onMounted(async () => {
  try {
    const id = route.params.id
    const res = await getFileDetail(Number(id))
    fileInfo.value = res.data
  } catch (error) {
    ElMessage.error('获取文件信息失败')
  }
})

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const goBack = () => {
  router.back()
}

const handleDownload = () => {
  ElMessage.info('下载功能开发中...')
}

const handlePreview = () => {
  router.push(`/preview/${route.params.id}`)
}
</script>

<style scoped>
.file-detail {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
```

#### components/GeoLocation.vue - 地理位置组件
```vue
<template>
  <div class="geo-location">
    <el-button
      type="primary"
      :loading="loading"
      @click="getLocation"
    >
      获取位置信息
    </el-button>

    <div v-if="location" class="location-info">
      <el-tag v-if="location.status === 'success'" type="success">
        定位成功: {{ location.city }}
      </el-tag>
      <el-tag v-else type="danger">
        {{ location.message }}
      </el-tag>

      <el-descriptions :column="2" style="margin-top: 15px;">
        <el-descriptions-item label="纬度">{{ location.lat }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ location.lng }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ location.region }}</el-descriptions-item>
        <el-descriptions-item label="定位方式">{{ location.type }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getUserLocation } from '@/api/geo'

interface LocationInfo {
  status: 'success' | 'error'
  message?: string
  lat: number
  lng: number
  city?: string
  region?: string
  type: string
}

const loading = ref(false)
const location = ref<LocationInfo | null>(null)

const getLocation = async () => {
  loading.value = true
  try {
    const res = await getUserLocation()
    location.value = res.data
  } catch (error) {
    location.value = {
      status: 'error',
      message: '定位失败',
      lat: 0,
      lng: 0,
      type: 'ERROR'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.geo-location {
  padding: 20px;
}
.location-info {
  margin-top: 20px;
}
</style>
```

### 7. 运行项目

```bash
# 开发环境
npm run dev

# 生产环境构建
npm run build

# 预览生产构建
npm run preview
```

### 8. 常用npm命令

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .vue,.js,.jsx,.cjs,.mjs --fix --ignore-path .gitignore"
  }
}
```

## 部署

### 1. 构建生产版本
```bash
npm run build
```

### 2. 部署到Nginx
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/geofile/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```
