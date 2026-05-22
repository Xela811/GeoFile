import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/files',
    name: 'FileList',
    component: () => import('@/views/FileList.vue'),
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/UploadView.vue'),
  },
  {
    path: '/file/:id',
    name: 'FileDetail',
    component: () => import('@/views/FileDetail.vue'),
  },
  {
    path: '/s/:code',
    name: 'PrivateShare',
    component: () => import('@/views/HomeView.vue'),
    props: true,
  },
  // 新增：公开批次分享路径 (b 代表 batch)
  {
    path: '/b/:token',
    name: 'PublicShare',
    component: () => import('@/views/HomeView.vue'),
    props: true,
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

export default router
