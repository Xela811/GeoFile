import { defineStore } from 'pinia'
  import { ref } from 'vue'
  import { deleteFile as apiDeleteFile, type FileVO } from '@/api/file'
  import { ElMessage } from 'element-plus'

  export const useFileStore = defineStore('file', () => {
    const currentFile = ref<FileVO | null>(null)
    const isDeleting = ref(false)

    // 删除文件
    const deleteFile = async (id: string) => {
      isDeleting.value = true
      try {
        await apiDeleteFile(id)
        ElMessage.success('删除成功')
        return true
      } catch (error) {
        ElMessage.error('删除失败')
        return false
      } finally {
        isDeleting.value = false
      }
    }

    // 设置当前文件
    const setCurrentFile = (file: FileVO | null) => {
      currentFile.value = file
    }

    return {
      currentFile,
      isDeleting,
      deleteFile,
      setCurrentFile
    }
  })