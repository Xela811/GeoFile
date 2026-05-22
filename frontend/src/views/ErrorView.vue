<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <header class="top-nav">
      <div class="logo">GeoFile</div>
      <nav class="nav-menu">
        <router-link to="/">首页</router-link>
        <router-link to="/upload">上传文件</router-link>
      </nav>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 标题区域 -->
      <div class="page-header">
        <h1>GeoFile - 附近文件</h1>
        <p class="subtitle">基于地理位置的文件共享服务</p>
      </div>
      <div class="error-container">
        <el-result icon="error" title="下载受限" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const errorMessage = computed(() => {
  // 在 createWebHistory 模式下，这里能直接拿到重定向过来的 query
  return route.query.msg || '发生未知错误，无法完成下载'
})
</script>

<style scoped lang="scss">
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80vh;
}
.home-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.top-nav {
  background: white;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  .logo {
    font-size: 24px;
    font-weight: bold;
    color: #409eff;
  }

  .nav-menu {
    display: flex;
    gap: 20px;

    a {
      color: #606266;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover,
      &.active {
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }
}

.main-content {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;

  h1 {
    font-size: 36px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 10px 0;
  }

  .subtitle {
    font-size: 16px;
    color: #909399;
    margin: 0;
  }
}
</style>
