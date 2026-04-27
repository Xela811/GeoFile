/**
 * 地理位置服务
 * 使用HTML5 Geolocation API
 */

import { fa } from 'element-plus/es/locales.mjs'

export interface LocationInfo {
  userId: string
  lat: number
  lng: number
  radius: number
  region: string
  city: string
  province: string
  district: string
  township: string
  formattedAddress: string
  updateTime: string
}

export interface NearbyFile {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  uploadTime: string
  expireTime: string
  locationLat?: number
  locationLng?: number
  distance?: number // 距离(米)
  uploadToken?: string // 上传令牌
  downloadToken?: string // 下载令牌
  downloadCount?: number // 下载次数
  maxDownloads?: number // 下载次数上限（0表示不限制）
}

export interface LocationResponse {
  location?: LocationInfo
  files: NearbyFile[]
  count: number
  total?: number
  pageNum?: number
  pageSize?: number
  totalPages?: number
  hasPrevious?: boolean
  hasNext?: boolean
}

class LocationService {
  private baseUrl = '/api/location'

  /**
   * 获取当前用户位置
   * 使用浏览器原生Geolocation API
   */
  async getCurrentLocation(): Promise<LocationInfo> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('浏览器不支持Geolocation API'))
        return
      }

      // 检查当前环境是否支持Geolocation API
      if (!window.isSecureContext) {
        reject(new Error('Geolocation API 仅在 HTTPS 或 localhost 环境下可用'))
        return
      }

      console.log('开始获取地理位置...')

      navigator.geolocation.getCurrentPosition(
        async (position) => {
          try {
            console.log('位置获取成功:', position.coords)
            const lat = position.coords.latitude
            const lng = position.coords.longitude

            // 调用后端API保存位置信息
            const response = await fetch(`${this.baseUrl}/current`, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
              },
              body: JSON.stringify({
                lat,
                lng,
                radius: 1000, // 默认搜索半径1000米
                region: '未知区域',
                city: '未知城市',
                province: '未知省份',
              }),
            })

            console.log('后端响应状态:', response.status)
            const result = await response.json()
            console.log('后端响应数据:', result)

            if (result.code === 200) {
              resolve(result.data)
            } else {
              reject(new Error(result.message || '获取位置失败'))
            }
          } catch (error) {
            console.error('获取位置时出错:', error)
            reject(error)
          }
        },
        (error) => {
          console.error('地理位置获取失败:', error)
          let errorMessage = '获取位置失败'

          switch (error.code) {
            case error.PERMISSION_DENIED:
              errorMessage = '用户拒绝了位置请求，请在浏览器设置中允许位置权限'
              break
            case error.POSITION_UNAVAILABLE:
              errorMessage = '位置信息不可用，请检查设备GPS或网络位置服务是否开启'
              break
            case error.TIMEOUT:
              errorMessage = '获取位置超时，请重试'
              break
            default:
              errorMessage = `获取位置失败: ${error.message}`
          }

          reject(new Error(errorMessage))
        },
        {
          enableHighAccuracy: false, // 启用高精度定位
          timeout: 10000, // 超时时间10秒
          maximumAge: 0, // 不使用缓存的位置
        },
      )
    })
  }

  /**
   * 获取附近文件
   */
  async getNearbyFiles(
    lat: number,
    lng: number,
    radius: number = 1000,
    excludeFileId?: number,
    keyword?: string,
    fileType?: string,
    pageNum: number = 1,
    pageSize: number = 10,
    sortBy?: string,
    sortOrder?: string,
    extractCode?: string,
  ): Promise<LocationResponse> {
    try {
      const params = new URLSearchParams({
        lat: lat.toString(),
        lng: lng.toString(),
        radius: radius.toString(),
        pageNum: pageNum.toString(),
        pageSize: pageSize.toString(),
      })

      if (excludeFileId) {
        params.append('excludeFileId', excludeFileId.toString())
      }

      if (keyword) {
        params.append('keyword', keyword)
      }

      if (fileType) {
        params.append('fileType', fileType)
      }

      if (sortBy) {
        params.append('sortBy', sortBy)
      }

      if (sortOrder) {
        params.append('sortOrder', sortOrder)
      }

      if (extractCode) {
        params.append('extractCode', extractCode)
      }

      console.log(
        `正在搜索文件: mode=${extractCode ? '提取码' : '附近'}, lat=${lat}, lng=${lng}, radius=${radius}, keyword=${keyword}, fileType=${fileType}, extractCode=${extractCode}`,
      )

      // 添加超时控制
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 15000) // 15秒超时

      const response = await fetch(`/api/file/nearby?${params.toString()}`, {
        signal: controller.signal,
      })

      clearTimeout(timeoutId)

      console.log(`API响应状态: ${response.status}`)

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log(`API响应数据:`, result)

      if (result.code === 200) {
        return result.data
      } else {
        throw new Error(result.message || '获取附近文件失败')
      }
    } catch (error: any) {
      console.error('获取附近文件失败:', error)

      // 处理超时错误
      if (error.name === 'AbortError') {
        throw new Error('请求超时，请检查网络连接或稍后重试')
      }

      throw error
    }
  }

  /**
   * 计算两点之间的距离（米）
   * 使用Haversine公式
   */
  calculateDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const EARTH_RADIUS = 6371000 // 地球半径(米)

    const latDistance = Math.toRadians(lat2 - lat1)
    const lngDistance = Math.toRadians(lng2 - lng1)

    const a =
      Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
      Math.cos(Math.toRadians(lat1)) *
        Math.cos(Math.toRadians(lat2)) *
        Math.sin(lngDistance / 2) *
        Math.sin(lngDistance / 2)

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return EARTH_RADIUS * c
  }

  /**
   * 格式化距离显示
   */
  formatDistance(distance: number): string {
    if (distance < 1000) {
      return `${Math.round(distance)}米`
    } else {
      return `${(distance / 1000).toFixed(2)}公里`
    }
  }

  /**
   * 使用经纬度获取详细地址信息
   *
   * 说明：
   * 调用后端高德地图 API，将经纬度转换为详细的省市区街道信息
   *
   * @param lat 纬度
   * @param lng 经度
   * @returns 详细地址信息
   */
  async getDetailedAddress(
    lat: number,
    lng: number,
  ): Promise<{
    province: string
    city: string
    district: string
    township: string
    formattedAddress: string
  }> {
    try {
      const params = new URLSearchParams({
        lat: lat.toString(),
        lng: lng.toString(),
      })

      const response = await fetch(`/api/amap/geocode-from-location?${params}`)
      const result = await response.json()

      if (result.code === 200) {
        return {
          province: result.data.province,
          city: result.data.city,
          district: result.data.district,
          township: result.data.township,
          formattedAddress: result.data.formattedAddress,
        }
      } else {
        throw new Error(result.message || '获取详细地址失败')
      }
    } catch (error) {
      console.error('获取详细地址失败:', error)
      throw error
    }
  }

  /**
   * 格式化位置显示
   *
   * @param addressInfo 地址信息
   * @returns 格式化的位置字符串
   */
  formatAddress(addressInfo: {
    province: string
    city: string
    district: string
    township: string
  }): string {
    const { province, city, district, township } = addressInfo
    if (township) {
      return `${province}${city}${district}${township}`
    }
    return `${province}${city}${district}`
  }
}

export default new LocationService()
