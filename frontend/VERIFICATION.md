# 前端验证码集成指南

## 快速开始

### 方法一：直接获取Token下载文件（推荐）

```vue
<script setup>
import { ElMessage } from 'element-plus'

// 下载文件（最简单方式）
async function downloadFile(fileId) {
  try {
    // 1. 获取下载Token
    const res = await fetch('/api/verification/token/download')
    const { data: token } = await res.json()

    // 2. 下载文件
    const fileRes = await fetch(`/api/file/download/${fileId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    // 3. 处理文件下载
    const blob = await fileRes.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `file-${fileId}`
    a.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败: ' + error.message)
  }
}
</script>

<template>
  <el-button @click="downloadFile(123)">下载文件</el-button>
</template>
```

### 方法二：图形验证码（更安全）

```vue
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const captchaKey = ref('')
const captchaImage = ref('')
const userInputCode = ref('')

// 获取图形验证码
async function getCaptcha() {
  const res = await fetch('/api/verification/captcha')
  const result = await res.json()
  captchaKey.value = result.data.captchaKey
  captchaImage.value = result.data.image
}

// 验证验证码
async function verifyCaptcha() {
  const res = await fetch('/api/verification/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `captchaKey=${captchaKey.value}&code=${userInputCode.value}`
  })

  const result = await res.json()
  return result.data
}

// 下载文件（带验证码）
async function downloadWithVerification(fileId) {
  // 1. 获取并显示验证码
  await getCaptcha()
  // 2. 等待用户输入
  // 3. 验证验证码
  const isValid = await verifyCaptcha()
  if (!isValid) {
    ElMessage.error('验证码错误')
    return
  }
  // 4. 下载文件
  await downloadFile(fileId)
}
</script>

<template>
  <el-button @click="downloadWithVerification(123)">下载（需验证码）</el-button>

  <!-- 验证码对话框 -->
  <el-dialog v-model="showCaptcha" title="请输入验证码" width="400px">
    <img :src="captchaImage" alt="验证码" style="width: 100%" />
    <el-input
      v-model="userInputCode"
      placeholder="请输入验证码"
      style="margin-top: 20px"
    />
    <template #footer>
      <el-button @click="getCaptcha">刷新验证码</el-button>
      <el-button type="primary" @click="downloadWithVerification(123)">确定</el-button>
    </template>
  </el-dialog>
</template>
```

## API调用工具类

### src/utils/api.js

```javascript
// API基础URL
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 请求拦截器
const request = {
  async get(url, params = {}) {
    const queryString = new URLSearchParams(params).toString()
    const response = await fetch(`${BASE_URL}${url}?${queryString}`)
    return response.json()
  },

  async post(url, data = {}) {
    const response = await fetch(`${BASE_URL}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    return response.json()
  },

  async download(url, fileId, token) {
    const response = await fetch(`${BASE_URL}${url}/${fileId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    return response.blob()
  }
}

export default request
```

### src/api/verification.js

```javascript
import request from '@/utils/api'

/**
 * 获取下载Token
 */
export function getDownloadToken() {
  return request.get('/api/verification/token/download')
}

/**
 * 验证验证码
 */
export function verifyCaptcha(captchaKey, code) {
  return request.post('/api/verification/verify', {
    captchaKey,
    code
  })
}

/**
 * 获取图形验证码
 */
export function getCaptcha() {
  return request.get('/api/verification/captcha')
}
```

## 完整示例：文件列表 + 下载

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFileList } from '@/api/file'
import { getDownloadToken } from '@/api/verification'

const fileList = ref([])

// 加载文件列表
async function loadFileList() {
  try {
    const res = await getFileList({ page: 1, size: 10 })
    fileList.value = res.data.list
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

// 下载文件（最简单方式）
async function handleDownload(file) {
  try {
    // 获取Token
    const { data: token } = await getDownloadToken()

    // 下载文件
    const response = await fetch(`/api/file/download/${file.id}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.fileName
    a.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败: ' + error.message)
  }
}

onMounted(() => {
  loadFileList()
})
</script>

<template>
  <el-table :data="fileList" stripe>
    <el-table-column prop="fileName" label="文件名" />
    <el-table-column prop="fileType" label="类型" />
    <el-table-column prop="fileSize" label="大小" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button type="primary" size="small" @click="handleDownload(row)">
          下载
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
```

## 配置环境变量

### .env.development

```env
VITE_API_BASE_URL=http://localhost:8080
```

### .env.production

```env
VITE_API_BASE_URL=https://api.geofile.com
```

## 错误处理

```javascript
async function downloadFile(fileId) {
  try {
    const { data: token } = await getDownloadToken()
    const blob = await fetchDownload(fileId, token)
    // 处理下载
  } catch (error) {
    if (error.message.includes('401')) {
      ElMessage.error('验证码已过期，请刷新页面重试')
    } else {
      ElMessage.error('下载失败: ' + error.message)
    }
  }
}
```

## 常见问题

### Q1: Token过期怎么办？
**A**: 刷新页面重新获取Token即可，Token有效期5分钟。

### Q2: 下载失败返回401？
**A**: Token已过期，刷新页面重试。

### Q3: 如何防止重复下载？
**A**:
```javascript
// 可以在下载时添加去重逻辑
const downloading = ref(new Set())

async function handleDownload(file) {
  if (downloading.value.has(file.id)) {
    ElMessage.warning('文件正在下载中')
    return
  }

  downloading.value.add(file.id)
  try {
    await downloadFile(file.id)
  } finally {
    downloading.value.delete(file.id)
  }
}
```

## 安全建议

1. **生产环境必须使用HTTPS**
2. **不要将Token硬编码在代码中**
3. **考虑添加下载频率限制**
4. **定期更新JWT Secret**
5. **启用CORS白名单**

## 参考文档

- 后端API文档: `/home/xela/Projects/GeoFile/API.md`
- Spring Security配置: `com.geofile.config.SecurityConfig`
- JWT工具类: `com.geofile.util.JwtUtil`
- 验证码Service: `com.geofile.service.VerificationCodeService`
