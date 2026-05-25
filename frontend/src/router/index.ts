import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: {
      title: 'GeoFile - 基于地理位置的文件共享工具',
      desc: 'GeoFile 是一款跨端临时文件分享工具。无需登录，支持基于地理位置（1km内）的公开文件免密闪传、私密取件码安全提取。'
    }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/UploadView.vue'),
    meta: {
      title: '上传文件 - GeoFile 临时传输通道',
      desc: '安全、不限速地上传您的临时文件。支持公开地理位置分享与私密取件码分享，文件到期全自动物理销毁。'
    }
  },
  {
    path: '/s/:code',
    name: 'PrivateShare',
    component: () => import('@/views/HomeView.vue'),
    props: true,
    meta: {
      title: '私密文件提取通道 - GeoFile',
      desc: '您收到了一份 GeoFile 私密文件分享。系统已自动填充取件码，请允许位置权限以进行安全校验并提取文件。'
    }
  },
  // 新增：公开批次分享路径 (b 代表 batch)
  {
    path: '/b/:token',
    name: 'PublicShare',
    component: () => import('@/views/HomeView.vue'),
    props: true,
    meta: {
      title: '公开文件批次分享 - GeoFile',
      desc: '您收到了一份 GeoFile 公开批次文件分享链接，进站即可直接查看文件列表。下载前请确保地理位置符合要求。'
    }
  },
  {
    path: '/download-redirect',
    name: 'DownloadRedirect',
    component: () => import('@/views/DownloadRedirectView.vue'),
  },
  {
    path: '/error',
    name: 'ErrorPage',
    component: () => import('@/views/ErrorView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  if (to.meta.title) document.title = to.meta.title as string
  if (to.meta.desc) {
    const descMeta = document.querySelector('meta[name="description"]')
    if (descMeta) descMeta.setAttribute('content', to.meta.desc as string)
  }
})

export default router
