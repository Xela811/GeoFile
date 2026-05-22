/**
 * 高德地图 API 服务
 *
 * 使用说明：
 * 1. 访问 https://console.amap.com/dev/key/app 注册并获取 API Key
 * 2. 将 Key 配置到后端 application.yml 的 amap.key
 * 3. 前端通过后端 API 调用高德地图服务
 *
 * API 文档：
 * - 高德 Web 服务 API: https://lbs.amap.com/api/webservice/guide/api
 * - POI 搜索: https://lbs.amap.com/api/webservice/guide/api/search
 * - 逆地理编码: https://lbs.amap.com/api/webservice/guide/api/regeo
 * - 地理编码: https://lbs.amap.com/api/webservice/guide/api/geocode
 */

/**
 * 地址信息
 */
export interface AddressInfo {
  addressComponent: AddressComponent
  formattedAddress: string
  coordinate: Coordinate
}

/**
 * 地址组件信息
 */
export interface AddressComponent {
  province: string
  city: string
  district: string
  township: string
  streetNumber: string
  location: number
}

/**
 * 坐标信息
 */
export interface Coordinate {
  lng: number
  lat: number
}

/**
 * POI 搜索结果
 */
export interface POISearchResult {
  count: number
  pois: POIResult[]
}

/**
 * POI 信息
 */
export interface POIResult {
  name: string
  type: string
  address: string
  lng: number
  lat: number
  tel?: string
  thumbPhoto?: string
}

/**
 * 地址补全建议
 */
export interface AddressSuggestion {
  address: string
  lng: number
  lat: number
}

/**
 * 高德地图 API 服务类
 */
class AmapService {
  /**
   * 高德地图 Web 服务 API 基础地址
   */
  private baseUrl = '/api/amap'

  /**
   * 使用经纬度逆地理编码（获取地址信息）
   *
   * 功能：将经纬度坐标转换为详细的地址信息
   * API：https://restapi.amap.com/v3/geocode/regeo
   *
   * 使用场景：
   * - 用户点击地图获取当前位置
   * - 根据坐标获取省市街道信息
   * - 显示精确的地理位置
   *
   * @param lat 纬度
   * @param lng 经度
   * @returns 地址信息
   */
  async geocodeFromLocation(lat: number, lng: number): Promise<AddressInfo> {
    try {
      const params = new URLSearchParams({
        lat: lat.toString(),
        lng: lng.toString(),
      })

      const response = await fetch(`${this.baseUrl}/geocode-from-location?${params}`)
      const result = await response.json()

      if (result.code === 200) {
        return result.data
      } else {
        throw new Error(result.message || '逆地理编码失败')
      }
    } catch (error) {
      console.error('逆地理编码失败:', error)
      throw error
    }
  }

  /**
   * 使用地址进行地理编码（获取经纬度）
   *
   * 功能：将地址字符串转换为经纬度坐标
   * API：https://restapi.amap.com/v3/geocode/geo
   *
   * 使用场景：
   * - 用户输入地址搜索位置
   * - 地址补全功能
   * - 保存用户自定义位置
   *
   * @param address 地址字符串
   * @param city 城市（可选）
   * @returns 坐标信息
   */
  async geocodeFromAddress(address: string, city?: string): Promise<Coordinate> {
    try {
      const params = new URLSearchParams({
        address: address,
      })

      if (city) {
        params.append('city', city)
      }

      const response = await fetch(`${this.baseUrl}/geocode-from-address?${params}`)
      const result = await response.json()

      if (result.code === 200) {
        return result.data
      } else {
        throw new Error(result.message || '地理编码失败')
      }
    } catch (error) {
      console.error('地理编码失败:', error)
      throw error
    }
  }

  /**
   * POI 搜索（搜索附近的兴趣点）
   *
   * 功能：在指定位置搜索特定类型的地点
   * API：https://restapi.amap.com/v3/place/text
   *
   * 使用场景：
   * - 搜索附近的餐厅
   * - 搜索地铁站
   * - 搜索附近的医院
   * - 搜索特定类型的商家
   *
   * 参数说明：
   * - keywords: 搜索关键词（如"餐厅"、"加油站"）
   * - city: 搜索城市（如"北京"、"上海"）
   * - location: 中心点经纬度
   * - type: POI 类型（可选）
   * - radius: 搜索半径（可选）
   *
   * @param keywords 搜索关键词
   * @param city 搜索城市
   * @param location 中心点经纬度（格式："经度,纬度"）
   * @param type POI 类型（可选）
   * @param radius 搜索半径（可选，默认5000米）
   * @returns POI 搜索结果
   */
  async searchPOI(
    keywords: string,
    location: string,
    city?: string,
    type?: string,
    radius: number = 5000,
  ): Promise<POISearchResult> {
    try {
      const params = new URLSearchParams({
        keywords,
        location,
        radius: radius.toString(),
      })

      if (city) {
        params.append('city', city)
      }

      if (type) {
        params.append('type', type)
      }

      const response = await fetch(`${this.baseUrl}/search-poi?${params}`)
      const result = await response.json()

      if (result.code === 200) {
        return result.data
      } else {
        throw new Error(result.message || 'POI 搜索失败')
      }
    } catch (error) {
      console.error('POI 搜索失败:', error)
      throw error
    }
  }

  /**
   * 根据输入框自动补全地址
   *
   * 功能：用户输入地址时提供自动补全建议
   * API：https://restapi.amap.com/v3/assistant/inputtip
   *
   * 使用场景：
   * - 地址搜索框自动补全
   * - 地址输入建议
   * - 提高用户体验
   *
   * @param keyWord 输入的关键词
   * @returns 地址补全建议列表
   */
  async autoComplete(keyWord: string): Promise<AddressSuggestion[]> {
    try {
      const params = new URLSearchParams({
        keywords: keyWord,
      })

      const response = await fetch(`${this.baseUrl}/auto-complete?${params}`)
      const result = await response.json()

      if (result.code === 200) {
        return result.data
      } else {
        console.warn('地址自动补全失败:', result.message)
        return []
      }
    } catch (error) {
      console.error('地址自动补全失败:', error)
      return []
    }
  }

  /**
   * 获取周边的地铁站点
   *
   * @param location 中心点经纬度
   * @param radius 搜索半径
   * @returns 地铁站列表
   */
  async searchSubwayStations(location: string, radius: number = 5000): Promise<POIResult[]> {
    const result = await this.searchPOI('地铁站', location, undefined, '080201', radius)
    return result.pois
  }

  /**
   * 获取周边的医院
   *
   * @param location 中心点经纬度
   * @param radius 搜索半径
   * @returns 医院列表
   */
  async searchHospitals(location: string, radius: number = 5000): Promise<POIResult[]> {
    const result = await this.searchPOI('医院', location, undefined, '060000', radius)
    return result.pois
  }

  /**
   * 获取周边的餐厅
   *
   * @param location 中心点经纬度
   * @param radius 搜索半径
   * @returns 餐厅列表
   */
  async searchRestaurants(location: string, radius: number = 5000): Promise<POIResult[]> {
    const result = await this.searchPOI('餐厅', location, undefined, '030000', radius)
    return result.pois
  }

  /**
   * 格式化地址显示
   *
   * @param addressInfo 地址信息
   * @returns 格式化的地址字符串
   */
  formatAddress(addressInfo: AddressInfo): string {
    return addressInfo.formattedAddress
  }

  /**
   * 获取详细地址组件
   *
   * @param addressInfo 地址信息
   * @returns 详细地址组件
   */
  getDetailedAddress(addressInfo: AddressInfo): string {
    const { province, city, district, township, streetNumber } = addressInfo.addressComponent
    return `${province}${city}${district}${township}${streetNumber}`
  }
}

export default new AmapService()
