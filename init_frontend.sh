#!/bin/bash

# 前端项目初始化 - 创建Vue 3 + TypeScript项目

set -e

echo "================================"
echo "  前端项目初始化"
echo "================================"

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "错误: Node.js未安装"
    exit 1
fi

# 检查npm
if ! command -v npm &> /dev/null; then
    echo "错误: npm未安装"
    exit 1
fi

# 使用npm创建项目
echo "1. 使用Vue CLI创建项目..."
npm create vue@latest frontend -- --typescript --router --pinia

cd frontend

# 安装ElementPlus
echo "2. 安装ElementPlus..."
npm install element-plus
npm install @element-plus/icons-vue

# 安装其他依赖
echo "3. 安装其他依赖..."
npm install axios pinia vue-router echarts
npm install -D sass

# 创建项目目录结构
mkdir -p src/api
mkdir -p src/components
mkdir -p src/layouts
mkdir -p src/router
mkdir -p src/stores
mkdir -p src/utils
mkdir -p src/views
mkdir -p src/assets/images
mkdir -p src/assets/styles

echo "✓ 前端项目创建完成"

# 创建.gitignore
cat > .gitignore <<'EOF'
# Logs
logs
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
lerna-debug.log*

node_modules
dist
dist-ssr
*.local

# Editor directories and files
.vscode/*
!.vscode/extensions.json
.idea
.DS_Store
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?

# Environment variables
.env
.env.local
.env.*.local
EOF

echo "✓ .gitignore 创建完成"

cd ..

echo ""
echo "================================"
echo "  前端项目初始化完成！"
echo "================================"
echo ""
echo "下一步操作："
echo "1. 进入前端目录: cd frontend"
echo "2. 安装依赖: npm install"
echo "3. 启动开发服务器: npm run dev"
echo ""
echo "创建的目录结构："
echo "  - src/api/          API接口封装"
echo "  - src/components/   公共组件"
echo "  - src/layouts/      布局组件"
echo "  - src/router/       路由配置"
echo "  - src/stores/       状态管理"
echo "  - src/utils/        工具函数"
echo "  - src/views/        页面组件"
echo ""
echo "详细文档请查看: frontend/README.md"
