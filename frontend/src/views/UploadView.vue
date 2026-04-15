<template>
  <div class="upload-page">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome"> 返回首页 </el-button>
    </div>

    <!-- 标题区域 -->
    <div class="page-header">
      <h1>文件上传</h1>
      <p class="subtitle">支持拖拽上传，大文件自动切换分片上传模式</p>
    </div>

    <!-- 可复用上传组件 -->
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      "
    >
      <h2 style="margin: 0">文件列表</h2>
      <el-button type="primary" :icon="Back" @click="showLimitDialog"> 设置下载限制 </el-button>
    </div>

    <FileUpload
      ref="uploadRef"
      title="上传文件"
      :multiple="true"
      :limit="10"
      :max-downloads="downloadLimitConfig.maxDownloads"
      :valid-minutes="downloadLimitConfig.validMinutes"
      :need-code="downloadLimitConfig.needCode"
      tip="支持 JPG、PNG、PDF、DOC、DOCX、XLS、XLSX 等格式，单个文件不超过 100MB"
      @success="handleUploadSuccess"
      @upload-success="handleFileUploadSuccess"
      @error="handleUploadError"
      @require-limit-config="handleRequireLimitConfig"
    >
      <template #success="{ files }">
        <el-alert
          v-if="files.length > 0"
          title="上传成功"
          type="success"
          :closable="false"
          style="margin-top: 20px"
        >
          <template #default>
            <div v-for="file in files" :key="file.uid" class="success-item">
              <el-icon><SuccessFilled /></el-icon>
              <span>{{ file.name }}</span>
            </div>
          </template>
        </el-alert>
      </template>
    </FileUpload>

    <!-- 私有批次：取件码与文件（本地持久化，可刷新 / 删单文件 / 删整批） -->
    <el-card v-if="pickupBatches.length > 0" class="pickup-batches-card" shadow="never">
      <template #header>
        <div class="pickup-batches-header">
          <span>我的取件码（本机记录）</span>
          <el-button type="primary" link :icon="Refresh" @click="refreshAllPickupBatches">
            全部刷新
          </el-button>
        </div>
      </template>
      <p class="pickup-batches-hint">
        仅当您在本浏览器以「私有」模式上传时记录；取件码与后端 Redis 一致过期。删除部分文件后点「刷新」可同步列表；「删除整批」将移除该令牌下全部文件并失效取件码。
      </p>
      <div v-for="batch in pickupBatches" :key="batch.code" class="pickup-batch-block">
        <div class="pickup-batch-toolbar">
          <div class="pickup-code-row">
            <span class="label">取件码</span>
            <el-tag type="warning" size="large" effect="dark">{{ batch.code }}</el-tag>
            <el-button type="primary" link :icon="DocumentCopy" @click="copyPickupCode(batch.code)">
              复制
            </el-button>
          </div>
          <div class="pickup-meta">
            <span v-if="batch.validMinutes > 0" class="meta-item">
              约 {{ formatBatchExpire(batch) }} 失效（按上传时所设有效期估算）
            </span>
            <span v-else class="meta-item">未限制时长（仍以服务端为准）</span>
            <span v-if="batch.lastSyncedAt" class="meta-item subtle">
              最近同步：{{ formatSyncedAt(batch.lastSyncedAt) }}
            </span>
          </div>
          <div class="pickup-actions">
            <el-button
              size="small"
              :icon="Refresh"
              :loading="batch.refreshing"
              @click="refreshPickupBatch(batch)"
            >
              刷新列表
            </el-button>
            <el-button size="small" type="danger" plain :icon="Delete" @click="confirmDeleteEntireBatch(batch)">
              删除整批
            </el-button>
          </div>
        </div>
        <el-alert v-if="batch.syncError" type="error" :closable="false" class="batch-alert">
          {{ batch.syncError }}
        </el-alert>
        <el-table v-if="batch.files.length > 0" :data="batch.files" size="small" stripe class="pickup-file-table">
          <el-table-column prop="fileName" label="文件名" min-width="160" show-overflow-tooltip />
          <el-table-column label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="uploadTime" label="上传时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                size="small"
                @click="deleteSingleInBatch(batch, row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无文件或正在加载" :image-size="64" />
      </div>
    </el-card>

    <!-- 下载限制配置对话框 -->
    <el-dialog
      v-model="showDownloadLimitDialog"
      title="下载限制配置"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="120px">
        <el-form-item label="下载次数限制">
          <el-radio-group v-model="downloadLimitConfig.maxDownloads">
            <el-radio :value="1">1次</el-radio>
            <el-radio :value="5">5次</el-radio>
            <el-radio :value="10">10次</el-radio>
            <el-radio :value="50">50次</el-radio>
            <el-radio :value="100">100次</el-radio>
          </el-radio-group>
          <el-input-number
            v-model="downloadLimitConfig.maxDownloads"
            :min="1"
            :max="9999"
            style="margin-left: 12px; width: 150px"
          />
        </el-form-item>

        <el-form-item label="有效时长">
          <el-radio-group v-model="downloadLimitConfig.validMinutes">
            <el-radio :value="5">5分钟</el-radio>
            <el-radio :value="10">10分钟</el-radio>
            <el-radio :value="30">30分钟</el-radio>
            <el-radio :value="60">1小时</el-radio>
            <el-radio :value="1440">1天</el-radio>
          </el-radio-group>
          <el-input-number
            v-model="downloadLimitConfig.validMinutes"
            :min="1"
            :max="525600"
            style="margin-left: 12px; width: 150px"
          />
          <span style="margin-left: 12px; color: #909399; font-size: 12px">分钟</span>
        </el-form-item>

        <el-form-item label="分享模式">
          <el-radio-group v-model="downloadLimitConfig.needCode">
            <el-radio :label="true">私有（需取件码）</el-radio>
            <el-radio :label="false">公开（所有人可见）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="配置说明">
          <el-alert :title="getLimitDescription()" type="info" :closable="false" show-icon />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDownloadLimitDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmDownloadLimit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快捷操作 -->
    <!--<div class="quick-actions">
      <el-card>
        <template #header>
          <span>快捷操作</span>
        </template>
        <div class="action-buttons">
          <el-button type="primary" :icon="Upload" @click="triggerUpload">
            <el-icon><Upload /></el-icon>
            立即上传
          </el-button>
          <el-button @click="handleClear" :icon="Delete">
            <el-icon><Delete /></el-icon>
            清空列表
          </el-button>
        </div>
      </el-card>
    </div>-->
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Back, SuccessFilled, Refresh, DocumentCopy, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import { reconcileMyUploadedFiles } from '@/services/reconcileService'

const PICKUP_BATCHES_STORAGE_KEY = 'geofile_private_pickup_batches'

interface ExtractFileRow {
  id: number
  fileName: string
  fileSize: number
  uploadTime?: string
  fileType?: string
}

interface PickupBatchStored {
  code: string
  uploadToken: string
  validMinutes: number
  createdAt: number
}

interface PickupBatchDisplay extends PickupBatchStored {
  files: ExtractFileRow[]
  lastSyncedAt: number | null
  syncError: string | null
  refreshing: boolean
}

const router = useRouter()
const uploadRef = ref<InstanceType<typeof FileUpload>>()

const pickupBatches = ref<PickupBatchDisplay[]>([])
let pickupPollTimer: ReturnType<typeof setInterval> | null = null
let reconcileTimer: ReturnType<typeof setInterval> | null = null

// 下载限制配置
const showDownloadLimitDialog = ref(false)
const downloadLimitConfig = ref({
  maxDownloads: 1,
  validMinutes: 30, // 默认30分钟
  needCode: true, // 默认为私有模式，需要验证码
})

// 返回首页
const goHome = () => {
  router.push('/')
}

// 显示下载限制配置对话框
const showLimitDialog = () => {
  showDownloadLimitDialog.value = true
}

// 确认下载限制配置
const confirmDownloadLimit = () => {
  const maxDesc = `最多下载 ${downloadLimitConfig.value.maxDownloads} 次`
  const minutesDesc = `${downloadLimitConfig.value.validMinutes} 分钟`

  ElMessage.success(`下载限制已设置: ${maxDesc}, ${minutesDesc}`)
  showDownloadLimitDialog.value = false
}

// 获取限制说明
const getLimitDescription = () => {
  const maxDesc = `最多下载 ${downloadLimitConfig.value.maxDownloads} 次，超过后无法下载`

  const minutes = downloadLimitConfig.value.validMinutes

  let timeDesc
  if (minutes === 0) {
    timeDesc = '文件永久有效'
  } else {
    timeDesc = `文件有效期 ${minutes} 分钟`
  }

  return `${maxDesc}，${timeDesc}`
}

// 上传成功（保存上传令牌）
const handleUploadSuccess = (files: UploadUserFile[]) => {
  console.log('上传成功:', files)
  ElMessage.success(`成功上传 ${files.length} 个文件`)
}

// 上传成功回调（包含 uploadToken）；与 FileUpload 对齐：可为 Result{ data }、FileVO[] 或单个 FileVO
const handleFileUploadSuccess = (payload: {
  code?: number
  data?: unknown
  message?: string
}) => {
  console.log('收到上传文件数据:', payload)

  const raw = payload && payload.data !== undefined ? payload.data : payload
  let items: {
    id?: number
    uploadToken?: string
    downloadCode?: string
    fileName?: string
    fileSize?: number
    uploadTime?: string
    fileType?: string
  }[] = []
  if (Array.isArray(raw)) {
    items = raw
  } else if (raw && typeof raw === 'object' && raw !== null && 'id' in raw && 'uploadToken' in raw) {
    items = [raw as (typeof items)[0]]
  }
  let saved = 0
  items.forEach((file) => {
    if (file.id != null && file.uploadToken) {
      saveUploadToken(Number(file.id), file.uploadToken)
      saved++
    }
  })
  if (saved > 0) {
    ElMessage.success('文件上传成功并已保存操作令牌')
  } else {
    console.warn('上传文件数据不完整:', payload)
  }

  if (downloadLimitConfig.value.needCode && items.length > 0) {
    const first = items[0]
    if (first?.downloadCode && first?.uploadToken) {
      upsertPickupBatchFromUpload(first.downloadCode, first.uploadToken, items)
    }
  }
}

function storedBatchesOnly(list: PickupBatchDisplay[]): PickupBatchStored[] {
  return list.map(({ code, uploadToken, validMinutes, createdAt }) => ({
    code,
    uploadToken,
    validMinutes,
    createdAt,
  }))
}

function savePickupBatchesToStorage() {
  try {
    localStorage.setItem(
      PICKUP_BATCHES_STORAGE_KEY,
      JSON.stringify(storedBatchesOnly(pickupBatches.value)),
    )
  } catch (e) {
    console.error('保存取件批次失败:', e)
  }
}

function loadPickupBatchesFromStorage(): PickupBatchDisplay[] {
  try {
    const s = localStorage.getItem(PICKUP_BATCHES_STORAGE_KEY)
    if (!s) return []
    const parsed = JSON.parse(s) as PickupBatchStored[]
    if (!Array.isArray(parsed)) return []
    return parsed.map((b) => ({
      ...b,
      files: [],
      lastSyncedAt: null,
      syncError: null,
      refreshing: false,
    }))
  } catch (e) {
    console.error('读取取件批次失败:', e)
    return []
  }
}

function mapVoToRow(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  raw: any,
): ExtractFileRow {
  return {
    id: Number(raw.id),
    fileName: raw.fileName ?? raw.originalName ?? '未命名',
    fileSize: Number(raw.fileSize ?? 0),
    uploadTime: raw.uploadTime,
    fileType: raw.fileType,
  }
}

function upsertPickupBatchFromUpload(
  code: string,
  uploadToken: string,
  items: {
    id?: number
    fileName?: string
    fileSize?: number
    uploadTime?: string
    fileType?: string
  }[],
) {
  const validMinutes = downloadLimitConfig.value.validMinutes
  const createdAt = Date.now()
  const existing = pickupBatches.value.find((b) => b.code === code)
  if (existing) {
    existing.uploadToken = uploadToken
    existing.validMinutes = validMinutes
    existing.files = items
      .filter((x) => x.id != null)
      .map((x) => mapVoToRow(x))
    existing.lastSyncedAt = Date.now()
    existing.syncError = null
    savePickupBatchesToStorage()
    ElMessage.success(`取件码 ${code} 已更新，可在下方查看与管理`)
    return
  }
  const batch: PickupBatchDisplay = {
    code,
    uploadToken,
    validMinutes,
    createdAt,
    files: items
      .filter((x) => x.id != null)
      .map((x) => mapVoToRow(x)),
    lastSyncedAt: Date.now(),
    syncError: null,
    refreshing: false,
  }
  pickupBatches.value.unshift(batch)
  savePickupBatchesToStorage()
  ElMessage.success(`取件码 ${code} 已生成，已保存在本页下方`)
}

/** 从 localStorage `myUploadedFiles` 中移除指定文件 id 对应的上传令牌 */
function removeUploadTokensForFileIds(fileIds: number[]) {
  if (fileIds.length === 0) return
  try {
    const stored = localStorage.getItem('myUploadedFiles')
    const myFiles: Record<string, string> = stored ? JSON.parse(stored) : {}
    for (const id of fileIds) {
      delete myFiles[String(id)]
    }
    localStorage.setItem('myUploadedFiles', JSON.stringify(myFiles))
  } catch (e) {
    console.error('清除本地上传令牌失败:', e)
  }
}

function removePickupBatch(code: string, batch?: PickupBatchDisplay) {
  if (batch?.files?.length) {
    removeUploadTokensForFileIds(batch.files.map((f) => f.id))
  }
  pickupBatches.value = pickupBatches.value.filter((b) => b.code !== code)
  savePickupBatchesToStorage()
}

async function refreshPickupBatch(batch: PickupBatchDisplay) {
  batch.refreshing = true
  batch.syncError = null
  try {
    const res = await fetch(`/api/file/extract/${encodeURIComponent(batch.code)}`)
    const json = (await res.json()) as {
      code: number
      data?: unknown[]
      message?: string
    }
    if (json.code === 200 && Array.isArray(json.data)) {
      batch.files = json.data.map((row) => mapVoToRow(row))
      batch.lastSyncedAt = Date.now()
      if (json.data.length === 0) {
        batch.syncError = '该取件码下已无可访问文件'
        removePickupBatch(batch.code, batch)
      }
    } else {
      batch.syncError = json.message || '取件码无效或已过期'
      removePickupBatch(batch.code, batch)
      if (json.message) ElMessage.warning(json.message)
    }
  } catch {
    batch.syncError = '网络错误，请稍后重试'
  } finally {
    batch.refreshing = false
  }
}

async function refreshAllPickupBatches() {
  for (const b of pickupBatches.value) {
    await refreshPickupBatch(b)
  }
}

function copyPickupCode(code: string) {
  navigator.clipboard.writeText(code).then(
    () => ElMessage.success('取件码已复制'),
    () => ElMessage.error('复制失败'),
  )
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${Math.round((bytes / Math.pow(k, i)) * 100) / 100} ${sizes[i]}`
}

function formatBatchExpire(batch: PickupBatchDisplay): string {
  if (batch.validMinutes <= 0) return '—'
  const ms = batch.createdAt + batch.validMinutes * 60_000
  const d = new Date(ms)
  return d.toLocaleString()
}

function formatSyncedAt(ts: number): string {
  return new Date(ts).toLocaleString()
}

async function deleteSingleInBatch(batch: PickupBatchDisplay, fileId: number) {
  try {
    await ElMessageBox.confirm('确定删除该文件？删除后他人将无法通过取件码下载此文件。', '删除文件', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const res = await fetch(
      `/api/file/delete/${fileId}?uploadToken=${encodeURIComponent(batch.uploadToken)}`,
      { method: 'DELETE' },
    )
    const json = (await res.json()) as { code: number; message?: string }
    if (json.code === 200) {
      removeUploadTokensForFileIds([fileId])
      ElMessage.success('已删除')
      await refreshPickupBatch(batch)
    } else {
      ElMessage.error(json.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除请求失败')
  }
}

async function confirmDeleteEntireBatch(batch: PickupBatchDisplay) {
  try {
    await ElMessageBox.confirm(
      '将删除该取件码对应的全部文件，取件码将失效。此操作不可恢复。',
      '删除整批',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    const res = await fetch(
      `/api/file/batch-by-upload-token?uploadToken=${encodeURIComponent(batch.uploadToken)}`,
      { method: 'DELETE' },
    )
    const json = (await res.json()) as { code: number; message?: string; data?: { deletedCount?: number } }
    if (json.code === 200) {
      const n = json.data?.deletedCount
      ElMessage.success(typeof n === 'number' ? `已删除 ${n} 个文件` : '整批已删除')
      removePickupBatch(batch.code, batch)
    } else {
      ElMessage.error(json.message || '删除失败')
      await refreshPickupBatch(batch)
    }
  } catch {
    ElMessage.error('删除请求失败')
  }
}

onMounted(() => {
  pickupBatches.value = loadPickupBatchesFromStorage()
  void refreshAllPickupBatches()
  pickupPollTimer = setInterval(() => {
    void refreshAllPickupBatches()
  }, 120_000)

  // 对账清理 myUploadedFiles（后端定时任务导致的过期/下架不会自动反映到本地缓存）
  void reconcileMyUploadedFiles()
  reconcileTimer = setInterval(() => {
    void reconcileMyUploadedFiles()
  }, 5 * 60_000)
})

onUnmounted(() => {
  if (pickupPollTimer) {
    clearInterval(pickupPollTimer)
    pickupPollTimer = null
  }
  if (reconcileTimer) {
    clearInterval(reconcileTimer)
    reconcileTimer = null
  }
})

// 监听需要配置下载限制的事件
const handleRequireLimitConfig = () => {
  showDownloadLimitDialog.value = true
}

// 保存上传令牌
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
    console.error('获取上传文件列表失败:', e)
    return {}
  }
}

// 上传失败
const handleUploadError = (error: Error) => {
  console.error('上传失败:', error)
}

// 立即上传
//const triggerUpload = () => {
//  if (uploadRef.value) {
//    uploadRef.value.startUpload()
//  }
//}

// 清空列表
//const handleClear = () => {
//if (uploadRef.value) {
//uploadRef.value.clear()
//ElMessage.info('已清空文件列表')
//}
//}
</script>

<style scoped lang="scss">
.upload-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  margin-bottom: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;

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
}

.quick-actions {
  margin-top: 20px;

  .action-buttons {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}

.success-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  span {
    color: #67c23a;
  }
}

.pickup-batches-card {
  margin-top: 28px;
}

.pickup-batches-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pickup-batches-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.pickup-batch-block {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.pickup-batch-toolbar {
  margin-bottom: 12px;
}

.pickup-code-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;

  .label {
    font-weight: 500;
    color: #606266;
  }
}

.pickup-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;

  .subtle {
    color: #909399;
  }
}

.pickup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.batch-alert {
  margin-bottom: 10px;
}

.pickup-file-table {
  width: 100%;
}
</style>
