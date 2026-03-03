import request from '@/utils/request'

  export interface FileListParams {
    page?: number
    pageSize?: number
    keyword?: string
    fileType?: string
    status?: number
  }

  export interface NearbyFilesParams {
    lat: number
    lng: number
    radius?: number
  }

  export interface FileVO {
    id: string
    fileName: string
    fileType: string
    fileSize: number
    uploadTime: string
    location?: string
    downloadCount: number
    expireTime: string
    status: number
  }

  export interface FileDetailVO extends FileVO {
    content?: string
  }

  export interface NearbyFileVO extends FileVO {
    distance?: number
  }

  export function getFileList(params?: FileListParams) {
    return request.get<{ list: FileVO[], total: number }>('/file/list', { params })
  }

  export function getFileDetail(id: string) {
    return request.get<FileDetailVO>(`/file/detail/${id}`)
  }

  export function deleteFile(id: string) {
    return request.delete(`/file/${id}`)
  }

  export function uploadFile(formData: FormData) {
    return request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }

  export interface UploadInfo {
    uploadId: string
    fileName: string
    fileSize: number
    chunkSize: number
    totalChunks: number
    uploadedChunks: number
    completed: boolean
  }

  export interface UploadProgress {
    uploadId: string
    currentChunk: number
    totalChunks: number
    progress: number
    status: string
    fileName: string
    fileSize: number
    uploadedBytes: number
  }

  export function initChunkUpload(params: {
    fileName: string
    fileSize: number
    chunkSize?: number
    chunkIndex?: number
  }) {
    return request.post<UploadInfo>('/file/upload/init', null, { params })
  }

  export function uploadChunk(formData: FormData) {
    return request.post<UploadProgress>('/file/upload/chunk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }

  export function mergeChunks(params: {
    fileName: string
    totalChunks: number
    fileHash?: string
  }) {
    return request.post<FileVO>('/file/upload/merge', null, { params })
  }

  export function searchNearbyFiles(params: NearbyFilesParams) {
    return request.get<NearbyFileVO[]>('/file/nearby', { params })
  }