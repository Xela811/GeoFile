# GeoFile 免登录系统 API对接文档

## 架构说明

GeoFile采用**免登录验证码机制**，通过验证码+JWT Token的方式实现文件传输验证，无需传统登录系统。

## 核心流程

1. **获取图形验证码** → 用户输入验证码 → 验证验证码 → 获取下载Token
2. **下载文件** → 传递Token → 后端验证Token → 允许下载

## API接口文档

### 1. 获取图形验证码

**接口地址**: `GET /api/verification/captcha`

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/verification/captcha"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaKey": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "image": "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
    "expireMinutes": 5
  }
}
```

**前端使用**:
```javascript
// 1. 获取验证码图片
const res = await fetch('/api/verification/captcha')
const { captchaKey, image } = await res.json()

// 2. 显示图片给用户
imgElement.src = image

// 3. 用户输入验证码后，验证
const verifyRes = await fetch('/api/verification/verify', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: `captchaKey=${captchaKey}&code=${userInputCode}`
})
const { data } = await verifyRes.json()
// data: true/false
```

### 2. 验证图形验证码

**接口地址**: `POST /api/verification/verify`

**请求参数**:
- `captchaKey`: 图形验证码Key
- `code`: 用户输入的验证码

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/verification/verify" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "captchaKey=a1b2c3d4&code=AB12"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 3. 获取下载验证码Token（最简洁方式）

**接口地址**: `GET /api/verification/token/download`

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/verification/token/download"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb2RlIjoiMDExMjMiLCJ0eXBlIjoidmVyaWZpY2F0aW9uIiwiaXNzIjoiZ2VvZmlsZS1zZWNyZXQta2V5IiwiaWF0IjoxNzMyNTI2MzAwLCJleHAiOjE3MzI1MjY1MDB9.abc123def456"
}
```

**前端使用**:
```javascript
// 1. 获取下载Token
const res = await fetch('/api/verification/token/download')
const { data: token } = await res.json()

// 2. 将Token作为Authorization header传递
const fileRes = await fetch(`/api/file/download/${fileId}`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})

// 3. 获取文件
const blob = await fileRes.blob()
const url = window.URL.createObjectURL(blob)
window.open(url)
```

### 4. 直接下载文件（带Token验证）

**接口地址**: `GET /api/file/download/{fileId}`

**请求头**:
- `Authorization`: `Bearer ${token}`

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/download/123" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 完整使用流程示例

### 方式一：验证码 + Token（推荐）

```javascript
// 步骤1: 获取下载Token
async function getDownloadToken() {
  const res = await fetch('/api/verification/token/download')
  const { data: token } = await res.json()
  return token
}

// 步骤2: 下载文件
async function downloadFile(fileId) {
  const token = await getDownloadToken()

  const response = await fetch(`/api/file/download/${fileId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })

  if (!response.ok) {
    throw new Error('下载失败')
  }

  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `file-${fileId}.zip`
  a.click()
  window.URL.revokeObjectURL(url)
}

// 使用
try {
  await downloadFile(123)
  ElMessage.success('下载成功')
} catch (error) {
  ElMessage.error('下载失败: ' + error.message)
}
```

### 方式二：图形验证码验证后下载

```javascript
// 步骤1: 获取图形验证码
async function getCaptcha() {
  const res = await fetch('/api/verification/captcha')
  return await res.json()
}

// 步骤2: 验证验证码
async function verifyCaptcha(captchaKey, code) {
  const formData = new URLSearchParams()
  formData.append('captchaKey', captchaKey)
  formData.append('code', code)

  const res = await fetch('/api/verification/verify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: formData
  })

  const result = await res.json()
  return result.data
}

// 步骤3: 下载文件
async function downloadWithVerification(fileId) {
  // 获取验证码
  const captchaRes = await getCaptcha()
  const { captchaKey, image } = captchaRes.data

  // 显示验证码给用户
  showCaptchaDialog(image)

  // 等待用户输入验证码
  const userCode = await waitForUserCode()

  // 验证验证码
  const isValid = await verifyCaptcha(captchaKey, userCode)

  if (!isValid) {
    ElMessage.error('验证码错误')
    return
  }

  // 验证成功，直接下载
  const tokenRes = await fetch('/api/verification/token/download')
  const { data: token } = await tokenRes.json()

  const fileRes = await fetch(`/api/file/download/${fileId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })

  const blob = await fileRes.blob()
  const url = window.URL.createObjectURL(blob)
  window.open(url)
}
```

## Spring Security 配置说明

### 安全规则

1. **公开接口**（无需验证）:
   - `/api/verification/**` - 验证码接口
   - `/api/verification/captcha` - 图形验证码
   - `/api/verification/token/download` - 获取下载Token
   - `/api/file/list` - 文件列表
   - `/api/file/nearby` - 附近文件
   - `/api/file/download/**` - 文件下载
   - `/ws/**` - WebSocket

2. **需要验证的接口**（通过验证码Token验证）:
   - 其他所有 `/api/**` 接口

### 拦截器配置

**VerificationCodeInterceptor**:
- 拦截所有 `/api/**` 接口
- 排除验证码和WebSocket接口
- 验证请求头中的JWT Token
- 验证Token是否过期
- 验证Token中的验证码是否正确

**FileUploadInterceptor**:
- 拦截文件上传接口
- 只允许POST方法
- 只允许multipart/form-data格式

## JWT Token说明

### Token结构

```json
{
  "code": "011233",  // 验证码
  "type": "verification",  // 类型
  "iss": "geofile-secret-key",  // 签发者
  "iat": 1732526300,  // 签发时间
  "exp": 1732526600  // 过期时间
}
```

### Token有效期

- 验证码有效期：5分钟
- Token有效期：5分钟

### 安全机制

1. **一次性验证码**: 验证成功后立即从Redis删除
2. **过期机制**: Token过期后无法验证
3. **Redis存储**: 验证码存储在Redis中，确保安全性
4. **HTTPS**: 生产环境建议使用HTTPS传输

## 常见问题

### Q1: Token验证失败怎么办？
**A**: 检查以下几点：
- Token是否过期（5分钟有效期）
- Token是否正确传递（Authorization header）
- Redis是否正常工作
- JWT Secret是否匹配

### Q2: 图形验证码不显示？
**A**:
- 检查Redis是否正常
- 检查验证码生成代码
- 查看浏览器控制台错误信息

### Q3: 下载文件被拦截？
**A**:
- 检查是否传递了Authorization header
- Token是否正确
- Token是否过期

### Q4: 如何提高安全性？
**A**:
1. 生产环境使用HTTPS
2. 增加验证码复杂度
3. 限制下载频率
4. 添加IP限制
5. 定期更换JWT Secret

## 前端集成示例

### Vue 3 + Axios

```javascript
// src/api/verification.js
import request from './request'

/**
 * 获取下载Token
 */
export function getDownloadToken() {
  return request.get('/verification/token/download')
}

/**
 * 验证验证码
 */
export function verifyCaptcha(captchaKey, code) {
  const formData = new URLSearchParams()
  formData.append('captchaKey', captchaKey)
  formData.append('code', code)
  return request.post('/verification/verify', formData)
}
```

```javascript
// src/utils/download.js
import { getDownloadToken } from '@/api/verification'

/**
 * 下载文件（带Token）
 */
export async function downloadFile(fileId) {
  try {
    // 获取Token
    const { data: token } = await getDownloadToken()

    // 下载文件
    const response = await fetch(`/file/download/${fileId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `file-${fileId}`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败: ' + error.message)
  }
}
```

## 总结

GeoFile的免登录系统通过以下方式实现简洁的文件传输：

1. **无需注册/登录**：用户直接通过验证码验证
2. **Token机制**：JWT Token实现无状态验证
3. **Redis缓存**：确保验证码安全性和时效性
4. **简洁流程**：获取Token → 下载文件，步骤简单
5. **安全性**：一次性验证码、过期机制、HTTPS建议

这种设计既保证了安全性，又提供了极佳的用户体验。
