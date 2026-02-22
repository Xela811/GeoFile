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

  export function searchNearbyFiles(params: NearbyFilesParams) {
    return request.get<NearbyFileVO[]>('/file/nearby', { params })
  }