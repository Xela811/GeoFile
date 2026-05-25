<template>
  <div class="upload-page">
    <div class="top-bar">
      <el-button type="primary" link :icon="Back" @click="goHome" :disabled="isCompLoading"> 返回首页 </el-button>
    </div>

    <!-- 标题区域 -->
    <div class="page-header">
      <h1>文件上传</h1>
      <p class="subtitle">支持拖拽上传，重复文件自动检测秒传</p>
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
      <el-button type="primary" :icon="Back" @click="showLimitDialog" :disabled="isCompLoading"> 设置下载限制 </el-button>
    </div>

    <FileUpload
    v-model:loading="isCompLoading"
      ref="uploadRef"
      title="上传文件"
      :multiple="true"
      :limit="50"
      :max-downloads="downloadLimitConfig.maxDownloads"
      :valid-minutes="downloadLimitConfig.validMinutes"
      :need-code="downloadLimitConfig.needCode"
      tip="支持 JPG、PNG、PDF、DOC、DOCX、XLS、XLSX 等格式，单个文件不超过 3GB，单次上传文件数量上限50"
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
        仅当您在本浏览器以「私有」模式上传时记录。删除部分文件后点「刷新」可同步列表；「删除整批」将移除该记录下全部文件并失效取件码。
      </p>
      
      <el-collapse class="custom-batch-collapse">
        <el-collapse-item 
          v-for="batch in pickupBatches" 
          :key="batch.code" 
          :name="batch.code"
          class="pickup-batch-block"
        >
          <template #title>
            <div class="pickup-batch-header-trigger" @click.stop>
              <el-icon class="batch-folder-icon"><FolderOpened /></el-icon>
              
              <div class="pickup-code-row">
                <span class="label">取件码</span>
                <el-tag type="warning" size="large" effect="dark">{{ batch.code }}</el-tag>
                <el-button type="primary" link :icon="DocumentCopy" @click.stop="copyPickupCode(batch.code)">
                  复制
                </el-button>
                <el-button
                  type="primary"
                  link
                  :icon="Share"
                  @click.stop="openShare(batch.code)"
                >
                  分享
                </el-button>
              </div>

              <div class="mobile-first-file-info">
                <span class="first-file-name" v-if="batch.files && batch.files.length > 0">
                  {{ batch.files?.[0]?.fileName }}
                </span>
                <el-tag size="small" type="info" class="file-count-tag" v-if="batch.files">
                  等共 {{ batch.files.length }} 个文件
                </el-tag>
              </div>

              <div class="pickup-meta">
                <span v-if="batch.validMinutes > 0" class="meta-item">
                  约 {{ formatBatchExpire(batch) }} 失效
                </span>
                <span v-else class="meta-item">未限制时长</span>
              </div>
            </div>
          </template>
          <template #icon="{ isActive }">
    <span class="click-trigger-text">
      {{ isActive ? '收起' : '展开' }}
    </span>
  </template>
          
          

          <div class="pickup-batch-content-body">
            <div class="pickup-batch-toolbar">
              <div class="pickup-meta" style="margin-top: 4px;">
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
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :icon="Delete"
                  @click="confirmDeleteEntireBatch(batch)"
                >
                  删除整批
                </el-button>
              </div>
            </div>
            
            <el-alert v-if="batch.syncError" type="error" :closable="false" class="batch-alert">
              {{ batch.syncError }}
            </el-alert>
            
            <el-table
              v-if="batch.files.length > 0"
              :key="`${batch.code}-${batch.files.length}`"
              :data="batch.files"
              size="small"
              stripe
              class="pickup-file-table"
            >
              <el-table-column prop="fileName" label="文件名" min-width="120" show-overflow-tooltip />
              <el-table-column label="大小" width="80">
                <template #default="{ row }">
                  {{ formatFileSize(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="170" class-name="mobile-hide" label-class-name="mobile-hide" />
              <el-table-column label="操作" width="60" align="right">
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
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-card v-if="publicBatches.length > 0" class="pickup-batches-card public-batches-card" shadow="never">
      <template #header>
        <div class="pickup-batches-header">
          <span style="color: #67c23a; font-weight: bold;">
            <el-icon style="vertical-align: middle; margin-right: 4px;"><Share /></el-icon>
            我的公开分享（附近1km可见）
          </span>
        </div>
      </template>
      <p class="pickup-batches-hint">
        仅记录您在本浏览器上传的「公开」文件。这些文件在GeoFile首页上对1km内所有人可见。
        删除整批将使该批次文件彻底移除。
      </p>

      <el-collapse class="custom-batch-collapse public-collapse">
        <el-collapse-item 
          v-for="batch in publicBatches" 
          :key="batch.uploadToken" 
          :name="batch.uploadToken"
          class="pickup-batch-block"
        >
          <template #title>
            <div class="pickup-batch-header-trigger" @click.stop>
              <el-icon class="batch-folder-icon" style="color: #67c23a;"><FolderOpened /></el-icon>
              
              <div class="pickup-code-row">
                <span class="label">区域公开文件</span>
                
                <el-button
                  type="primary"
                  link
                  :icon="Share"
                  @click.stop="openPublicShare(batch.uploadToken)"
                >
                  分享
                </el-button>
              </div>

              <div class="mobile-first-file-info">
                <span class="first-file-name" v-if="batch.files && batch.files.length > 0">
                  {{ batch.files?.[0]?.fileName }}
                </span>
                <el-tag size="small" type="info" class="file-count-tag" v-if="batch.files">
                  等共 {{ batch.files.length }} 个文件
                </el-tag>
              </div>

              <div class="pickup-meta">
                <!--<span class="meta-item">
                  {{ formatSyncedAt(batch.createdAt) }}
                </span>-->
                <span v-if="batch.validMinutes > 0" class="meta-item">
                  约 {{ formatBatchExpire(batch) }} 失效
                </span>
                <span v-else class="meta-item">未限制时长</span>
              </div>
            </div>
          </template>
          <template #icon="{ isActive }">
    <span class="click-trigger-text">
      {{ isActive ? '收起' : '展开' }}
    </span>
  </template>

          <div class="pickup-batch-content-body">
            <div class="pickup-batch-toolbar">
              <div class="pickup-meta" style="margin-top: 4px;">
                <span v-if="batch.lastSyncedAt" class="meta-item subtle">
                  最近操作：{{ formatSyncedAt(batch.lastSyncedAt) }}
                </span>
              </div>
              
              <div class="pickup-actions">
                <el-button
                  size="small"
                  :icon="Refresh"
                  :loading="batch.refreshing"
                  @click="refreshPublicBatch(batch)"
                >
                  刷新列表
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :icon="Delete"
                  @click="confirmDeleteEntirePublicBatch(batch)"
                >
                  删除整批文件并下架
                </el-button>
              </div>
            </div>

            <el-table :data="batch.files" size="small" stripe class="pickup-file-table">
              <el-table-column prop="fileName" label="文件名" min-width="120" show-overflow-tooltip />
              <el-table-column label="大小" width="80">
                <template #default="{ row }">
                  {{ formatFileSize(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="170" class-name="mobile-hide" label-class-name="mobile-hide" />
              <el-table-column label="操作" width="60" align="right">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    link
                    size="small"
                    
                    @click="deleteSingleInPublicBatch(batch, row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 下载限制配置对话框 -->
    <el-dialog
      v-model="showDownloadLimitDialog"
      title="下载限制配置"
      width="90%"
      style="max-width: 500px"
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

    <el-dialog v-model="shareVisible" :title="shareTitle" width="320px" center append-to-body>
      <div style="display: flex; flex-direction: column; align-items: center">
        <qrcode-vue :value="fullShareUrl" :size="200" level="H" render-as="svg" />

        <p style="margin: 15px 0 5px; font-size: 14px; color: #606266">
          {{shareHint}}
        </p>

        <el-input v-model="fullShareUrl" readonly size="small" style="margin-top: 10px">
          <template #append>
            <el-button @click="copyLink">复制链接</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Back, SuccessFilled, Refresh, DocumentCopy, Delete, FolderOpened, Share } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import { reconcileMyUploadedFiles } from '@/services/reconcileService'
import QrcodeVue from 'qrcode.vue'

const isCompLoading = ref(false); // 接收子组件的上传状态

// --- 分享相关逻辑 ---
const shareVisible = ref(false)
const currentShareId = ref('') // 当前点击分享的取件码

const shareType = ref<'private' | 'public'>('private') // 标识分享类型
// 生成动态链接
const fullShareUrl = computed(() => {
  // 生成格式如：http://yourdomain.com/#/s/TOKQE
  // 注意：这里使用了你之前提到的 /s/ 路由格式
  //return `${window.location.origin}/#/s/${currentShareCode.value}`
  if (!currentShareId.value) return ''
  const base = window.location.origin
  // 私有使用 /s/，公开使用 /b/
  const path = shareType.value === 'private' ? '/s/' : '/b/'
  return `${base}${path}${currentShareId.value}`
})

const shareTitle = computed(() => shareType.value === 'private' ? '分享私有文件' : '分享公开批次')
const shareHint = computed(() => 
  shareType.value === 'private' 
    ? '扫码或发送链接，输入取件码即可下载' 
    : '扫码或发送链接，对方在1km内即可查看并下载'
)

// 打开分享弹窗
const openShare = (code: string) => {
  shareType.value = 'private'
  currentShareId.value = code
  shareVisible.value = true
}

const openPublicShare = (token: string) => {
  shareType.value = 'public'
  currentShareId.value = token
  shareVisible.value = true
}

// 复制链接
const copyLink = () => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(fullShareUrl.value)
    ElMessage.success('分享链接已复制到剪贴板')
  }
}

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

// 新增公开批次相关的常量与变量 ---
const PUBLIC_BATCHES_STORAGE_KEY = 'geofile_public_upload_batches'
const publicBatches = ref<PickupBatchDisplay[]>([]) // 复用私有批次的接口定义

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
const handleFileUploadSuccess = (payload: { code?: number; data?: unknown; message?: string }, total?: number) => {
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
  } else if (
    raw &&
    typeof raw === 'object' &&
    raw !== null &&
    'id' in raw &&
    'uploadToken' in raw
  ) {
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
    //ElMessage.success('文件上传成功并已保存操作令牌')
  } else {
    console.warn('上传文件数据不完整:', payload)
  }

  // --- 修改后的逻辑分流 ---
if (items.length > 0) {
  const first = items[0]

  if (downloadLimitConfig.value.needCode) {
    // 1. 私有模式逻辑：必须有 downloadCode 和 uploadToken
    if (first?.downloadCode && first?.uploadToken) {
      upsertPickupBatchFromUpload(first.downloadCode, first.uploadToken, items, total)
    } else {
      console.warn('私有上传缺少取件码或令牌', first)
    }
  } else {
    // 2. 公开模式逻辑：只要有 uploadToken 即可入库
    if (first?.uploadToken) {
      upsertPublicBatchFromUpload(first.uploadToken, items, total ?? 0)
    } else {
      console.warn('公开上传缺少令牌', first)
    }
  }
}
}

function storedBatchesOnly(list: PickupBatchDisplay[]): PickupBatchStored[] {
  return list.map(({ code, uploadToken, validMinutes, createdAt, files }) => ({
    code,
    uploadToken,
    validMinutes,
    createdAt,
    files: files.map(f => ({ id: f.id, fileName: f.fileName, fileSize: f.fileSize, uploadTime: f.uploadTime }))
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

// 新增公开批次的持久化逻辑 ---
function savePublicBatchesToStorage() {
  try {
    localStorage.setItem(
      PUBLIC_BATCHES_STORAGE_KEY,
      JSON.stringify(storedBatchesOnly(publicBatches.value)),
    )
  } catch (e) {
    console.error('保存公开批次失败:', e)
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
      files: (b as any).files || [],
      lastSyncedAt: null,
      syncError: null,
      refreshing: false,
    }))
  } catch (e) {
    console.error('读取取件批次失败:', e)
    return []
  }
}

function loadPublicBatchesFromStorage(): PickupBatchDisplay[] {
  try {
    const s = localStorage.getItem(PUBLIC_BATCHES_STORAGE_KEY)
    if (!s) return []
    const parsed = JSON.parse(s) as PickupBatchStored[]
    if (!Array.isArray(parsed)) return []
    return parsed.map((b) => ({
      ...b,
      files: (b as any).files || [],
      lastSyncedAt: null,
      syncError: null,
      refreshing: false,
    }))
  } catch (e) {
    console.error('读取公开批次失败:', e)
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

// 在 UploadView.vue 中定义一个闭包变量
let uploadNoticeTimer: any = null;
let pendingFilesCount = 0;
let processedTaskCount = 0; // 记录已处理的文件坑位

function upsertPickupBatchFromUpload(code: any, uploadToken: any, items: any, totalInBatch: any) {
  const dataItems = Array.isArray(items) ? items : [items];
  processedTaskCount += dataItems.length;
  const validMinutes = downloadLimitConfig.value.validMinutes;
  let addedCount = 0;
  // 1. 核心逻辑：找到或创建 Batch
  let existing = pickupBatches.value.find((b) => b.code === code);
  
  // 1. 统一转换数据格式 (提到最前面，确保全局可用)
  const newMappedFiles = dataItems.map(x => mapVoToRow(x));

  if (existing) {
    // 累加模式：将新来的 items 合并到现有 files 中，去重
    //const newMappedFiles = items.map(x => mapVoToRow(x));
    newMappedFiles.forEach(nf => {
        if (!existing!.files.find(f => f.id === nf.id)) {
            existing!.files.push(nf);
            addedCount++;
        }
    });
    existing.lastSyncedAt = Date.now();
  } else {
    // 新建模式
    existing = {
      code,
      uploadToken,
      validMinutes,
      createdAt: Date.now(),
      files: newMappedFiles,
      lastSyncedAt: Date.now(),
      syncError: null,
      refreshing: false,
    };
    addedCount = newMappedFiles.length; // 新建批次，全量计入
    pickupBatches.value.unshift(existing);
  }

  // 2. 统计本次任务累积的新增文件数
  //pendingFilesCount += items.length;
// 2. 只有真正新增了文件，才累加进防抖统计量
if (addedCount > 0) {
    pendingFilesCount += addedCount;
    // 3. 【防抖核心】修复气泡多次弹出
  if (uploadNoticeTimer) clearTimeout(uploadNoticeTimer);
  
  uploadNoticeTimer = setTimeout(() => {
    // 只有在 500ms 内没有新的 upsert 请求时，才弹窗一次
    if (processedTaskCount >= totalInBatch) {
    ElMessage.success({
      message: `取件码 ${code}：本次成功上传 ${pendingFilesCount} 个文件，已保存在下方`,
      duration: 5000,
      showClose: true
    });
   // 重置统计量
   pendingFilesCount = 0;
    uploadNoticeTimer = null; 
    processedTaskCount = 0;
  }
    
  }, 500); // 500ms 的缓冲时间足够跳出循环
  }
  

  savePickupBatchesToStorage();
}

// 新增公开批次的维护函数 ---
function upsertPublicBatchFromUpload(uploadToken: string, items: any[], totalInBatch: number) {
  const dataItems = Array.isArray(items) ? items : [items]
  const newMappedFiles = dataItems.map(x => mapVoToRow(x))
  
  // 公开模式下没有 downloadCode，我们用 Token 前缀作为标识
  const displayCode = uploadToken.substring(0, 8).toUpperCase()
  
  let existing = publicBatches.value.find((b) => b.uploadToken === uploadToken)
  
  if (existing) {
    newMappedFiles.forEach(nf => {
      if (!existing!.files.find(f => f.id === nf.id)) {
        existing!.files.push(nf)
      }
    })
    existing.lastSyncedAt = Date.now()
  } else {
    existing = {
      code: displayCode, // 仅展示用
      uploadToken,
      validMinutes: downloadLimitConfig.value.validMinutes,
      createdAt: Date.now(),
      files: newMappedFiles,
      lastSyncedAt: Date.now(),
      syncError: null,
      refreshing: false,
    }
    publicBatches.value.unshift(existing)
  }
  savePublicBatchesToStorage()
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
      //removePickupBatch(batch.code, batch)
      //if (json.message) ElMessage.warning(json.message)
      if (json.message && (json.message.includes('过期') || json.message.includes('不存在'))) {
        // 慎重删除，或者让用户手动点删除
      removePickupBatch(batch.code, batch) 
      }
      // --- 新增：刷新成功后也要存一次盘 ---
      savePickupBatchesToStorage()
    }
  } catch {
    batch.syncError = '网络错误，请稍后重试'
  } finally {
    batch.refreshing = false
  }
}

// 新增：刷新单个公开批次（主要靠后端校验 Token 下是否还有文件）
async function refreshPublicBatch(batch: PickupBatchDisplay) {
  batch.refreshing = true;
  try {
    // 建议后端提供一个 /api/file/list-by-token 接口
    // 如果没有，可以复用你的搜索接口，但这里我们通过判断文件是否还在
    const res = await fetch(`/api/file/list-by-token?uploadToken=${encodeURIComponent(batch.uploadToken)}`)
    const json = await res.json()
    
    if (json.code === 200) {
      if (!json.data || json.data.length === 0) {
        // 后端说没文件了，前端立即移除该批次
        publicBatches.value = publicBatches.value.filter(b => b.uploadToken !== batch.uploadToken)
        savePublicBatchesToStorage()
      } else {
        batch.files = json.data.map((x: any) => mapVoToRow(x))
        savePublicBatchesToStorage()
      }
    }
  } catch (e) {
    console.error("刷新公开批次失败", e)
  } finally {
    batch.refreshing = false;
  }
}

async function refreshAllPickupBatches() {
  /*for (const b of pickupBatches.value) {
    await refreshPickupBatch(b)
  }*/
  // 创建一个浅拷贝副本进行遍历，确保每一个批次都能被处理到
  const batchesToRefresh = [...pickupBatches.value]
  for (const b of batchesToRefresh) {
    await refreshPickupBatch(b)
  }
  // 2. 刷新公开
  const publicToRefresh = [...publicBatches.value]
  for (const b of publicToRefresh) {
    await refreshPublicBatch(b)
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
    await ElMessageBox.confirm(
      '确定删除该文件？删除后他人将无法通过取件码下载此文件。',
      '删除文件',
      {
        type: 'warning',
      },
    )
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
      /*removeUploadTokensForFileIds([fileId])
      ElMessage.success('已删除')
      await refreshPickupBatch(batch)*/
// 1. 手动从本地数组中剔除该文件（不要完全依赖刷新，先做前端响应式剔除）
const index = batch.files.findIndex(f => f.id === fileId);
      if (index !== -1) {
        batch.files.splice(index, 1);
      }

      removeUploadTokensForFileIds([fileId])
      ElMessage.success('已删除')

      // 2. 如果文件删完了，直接调用删除整批的本地逻辑，避免滞留
      if (batch.files.length === 0) {
        removePickupBatch(batch.code, batch);
      } else {
        // 3. 如果还有文件，再刷新
        await refreshPickupBatch(batch);
      }
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
    const json = (await res.json()) as {
      code: number
      message?: string
      data?: { deletedCount?: number }
    }
    if (json.code === 200) {
      const n = json.data?.deletedCount
      ElMessage.success(typeof n === 'number' ? `已删除 ${n} 个文件` : '整批已删除')
      batch.files = [];
      removePickupBatch(batch.code, batch)
    } else {
      ElMessage.error(json.message || '删除失败')
      await refreshPickupBatch(batch)
    }
  } catch {
    ElMessage.error('删除请求失败')
  }
}

// 公开批次的单文件删除
async function deleteSingleInPublicBatch(batch: PickupBatchDisplay, fileId: number) {
  try {
    await ElMessageBox.confirm('确定删除该公开文件？删除后将不再显示在附近公开文件列表中。', '提示', { type: 'warning' })
    const res = await fetch(`/api/file/delete/${fileId}?uploadToken=${encodeURIComponent(batch.uploadToken)}`, { method: 'DELETE' })
    const json = await res.json()
    if (json.code === 200) {
      batch.files = batch.files.filter(f => f.id !== fileId)
      if (batch.files.length === 0) {
        publicBatches.value = publicBatches.value.filter(b => b.uploadToken !== batch.uploadToken)
      }
      savePublicBatchesToStorage()
      ElMessage.success('删除成功')
    }
  } catch (e) { /* 取消或失败处理 */ }
}

// 公开批次的整批删除
async function confirmDeleteEntirePublicBatch(batch: PickupBatchDisplay) {
  try {
    await ElMessageBox.confirm('确定删除该批次所有公开文件？', '警告', { type: 'error' })
    const res = await fetch(`/api/file/batch-by-upload-token?uploadToken=${encodeURIComponent(batch.uploadToken)}`, { method: 'DELETE' })
    const json = await res.json()
    if (json.code === 200) {
      publicBatches.value = publicBatches.value.filter(b => b.uploadToken !== batch.uploadToken)
      savePublicBatchesToStorage()
      ElMessage.success('整批文件已下架')
    }
  } catch (e) { /* 取消或失败处理 */ }
}

async function reconcilePublicBatches() {
  if (publicBatches.value.length === 0) return;

  const validBatches: PickupBatchDisplay[] = [];
  let changed = false;

  for (const batch of publicBatches.value) {
    try {
      // 利用你现有的获取详情接口，验证该 Token 是否还存活
      const res = await fetch(`/api/file/list-by-token?uploadToken=${encodeURIComponent(batch.uploadToken)}`);
      const json = await res.json();

      // 如果后端返回 200 且有数据，保留；否则标记为需要删除
      if (json.code === 200 && Array.isArray(json.data) && json.data.length > 0) {
        // 顺便更新一下最新的文件状态（比如下载次数）
        batch.files = json.data.map((x: any) => mapVoToRow(x));
        validBatches.push(batch);
      } else {
        changed = true;
        console.log(`[对账] 公开批次已失效或下架: ${batch.uploadToken}`);
      }
    } catch (e) {
      // 网络错误时保留，防止误删
      validBatches.push(batch);
    }
  }

  if (changed) {
    publicBatches.value = validBatches;
    savePublicBatchesToStorage();
  }
}

onMounted(() => {
  pickupBatches.value = loadPickupBatchesFromStorage()
  publicBatches.value = loadPublicBatchesFromStorage()
  void refreshAllPickupBatches()
  pickupPollTimer = setInterval(() => {
    void refreshAllPickupBatches()
  }, 120_000)

  void reconcilePublicBatches()
  // 对账清理 myUploadedFiles（后端定时任务导致的过期/下架不会自动反映到本地缓存）
  void reconcileMyUploadedFiles()
  reconcileTimer = setInterval(() => {
    void reconcileMyUploadedFiles()
    void reconcilePublicBatches()
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

.public-batches-card {
  margin-top: 20px;
  border-left: 4px solid #67c23a; // 绿色左边框表示公开
  background-color: #f0f9eb; // 浅绿色背景

  :deep(.el-collapse-item__header) {
    background-color: #f0f9eb !important;
  }
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

// 🌟【新增】清除 Element 默认边框，使其融入你的原生块设计
.custom-batch-collapse {
  border-top: none !important;
  border-bottom: none !important;
  background: transparent !important;

  :deep(.el-collapse-item__header) {
    border-bottom: none !important;
    height: auto !important;
    line-height: inherit !important;
    padding: 12px 0;
    cursor: pointer;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none !important;
    background-color: transparent !important;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 8px;
  }
}

// 【新增】专门处理一长行折叠头的 Flex 布局控制
.pickup-batch-header-trigger {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
  padding-right: 8px;
  overflow: hidden;
}

// 【新增】小图标美化
.batch-folder-icon {
  font-size: 16px;
  color: #e6a23c;
  flex-shrink: 0;
}

// 【新增】移动端/折叠状态下第一个文件名显示的裁切规范
.mobile-first-file-info {
  display: flex;
  align-items: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px; 
  gap: 6px;

  .first-file-name {
    font-size: 13px;
    font-weight: 500;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .file-count-tag {
    flex-shrink: 0;
  }
}

.pickup-batch-block {
  //padding: 16px 0;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
    //padding-bottom: 0;
  }
}

.pickup-batch-toolbar {
  margin-bottom: 12px;

  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pickup-code-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  //margin-bottom: 8px;

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

// 【新增】手机响应式断点适配：大幅节省屏幕垂直与水平空间
@media (max-width: 768px) {
  .pickup-batch-content-body {
    padding: 4px 4px 12px 4px; // 手机端让出左边距
  }
  
  .mobile-first-file-info {
    max-width: 180px; // 手机端屏幕窄，进一步限制首个文件名的宽度防止挤爆
  }
  
  .pickup-meta {
    font-size: 11px; // 元数据缩小
  }
  
  // 配合上方 el-table-column，在手机端强制隐藏“上传时间”这一列
  :deep(.mobile-hide) {
    display: none !important;
  }

  .pickup-file-table {
    :deep(.is-right) {
      text-align: left !important; // 强行靠左，紧贴前面的“大小”列
      
      .cell {
        text-align: left !important; // 确保内部的容器也靠左对齐
        padding-left: 4px !important; // 稍微给 4px 的左内边距，防止跟大小的数字粘在一起
      }
    }
  }
}

.click-trigger-text {
  font-size: 13px;
  font-weight: bold;
  color: #409eff;       /* 暗示可以点击 */
  white-space: nowrap;  /* 强制文字不换行 */
  
  /* 核心修改：上下 padding 缩减到 4px（解决撑开间距），左右保持 12px 方便手机点击 */
  padding: 4px 12px !important;
  
  /* 强行让它在 Flex 轴线上完美居中，不破坏整体的排版平衡 */
  display: inline-flex;
  align-items: center;
  /* 提高层级，强制拦截触控 */
  position: relative;
  z-index: 100 !important; 
}

</style>
