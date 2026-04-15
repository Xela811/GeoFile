export const MY_UPLOADED_FILES_KEY = 'myUploadedFiles'

export type MyUploadedFilesMap = Record<string, string>

export function readMyUploadedFiles(): MyUploadedFilesMap {
  try {
    const stored = localStorage.getItem(MY_UPLOADED_FILES_KEY)
    const parsed = stored ? (JSON.parse(stored) as unknown) : {}
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    return parsed as MyUploadedFilesMap
  } catch {
    return {}
  }
}

export function writeMyUploadedFiles(map: MyUploadedFilesMap) {
  localStorage.setItem(MY_UPLOADED_FILES_KEY, JSON.stringify(map))
}

/**
 * 调用后端 /api/file/reconcile 对账：仅保留后端仍“有效”的文件 id（status 1/3 且未逻辑删除）。
 * 返回被清理掉的 fileId 列表。
 */
export async function reconcileMyUploadedFiles(): Promise<number[]> {
  const myFiles = readMyUploadedFiles()
  const ids = Object.keys(myFiles)
    .map((k) => Number(k))
    .filter((n) => Number.isFinite(n))

  if (ids.length === 0) return []

  try {
    const res = await fetch('/api/file/reconcile', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(ids),
    })

    const json = (await res.json()) as {
      code: number
      data?: number[]
      message?: string
    }

    if (json.code !== 200 || !Array.isArray(json.data)) {
      return []
    }

    const validSet = new Set(json.data.map((x) => Number(x)).filter((n) => Number.isFinite(n)))
    const removed: number[] = []
    for (const id of ids) {
      if (!validSet.has(id)) {
        delete myFiles[String(id)]
        removed.push(id)
      }
    }
    if (removed.length > 0) {
      writeMyUploadedFiles(myFiles)
    }
    return removed
  } catch {
    return []
  }
}

