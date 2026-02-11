#!/bin/bash

# GeoFile 项目初始化脚本

echo "================================"
echo "  GeoFile 项目初始化脚本"
echo "================================"

# 检查Java环境
echo "1. 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: Java未安装，请先安装JDK 17+"
    exit 1
fi
java -version
echo "✓ Java环境检查通过"

# 检查Maven环境
echo "2. 检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装，请先安装Maven 3.8+"
    exit 1
fi
mvn -version
echo "✓ Maven环境检查通过"

# 检查Node.js环境
echo "3. 检查Node.js环境..."
if ! command -v node &> /dev/null; then
    echo "错误: Node.js未安装，请先安装Node.js 18+"
    exit 1
fi
node -v
npm -v
echo "✓ Node.js环境检查通过"

# 检查MySQL环境
echo "4. 检查MySQL环境..."
if ! command -v mysql &> /dev/null; then
    echo "警告: MySQL命令行工具未安装，请确保MySQL服务已启动"
else
    mysql --version
    echo "✓ MySQL环境检查通过"
fi

# 检查Redis环境
echo "5. 检查Redis环境..."
if ! command -v redis-cli &> /dev/null; then
    echo "警告: Redis CLI未安装，请确保Redis服务已启动"
else
    redis-cli --version
    echo "✓ Redis环境检查通过"
fi

# 创建数据库
echo "6. 创建MySQL数据库..."
mysql -u root -p <<EOF
CREATE DATABASE IF NOT EXISTS geofile DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW CREATE DATABASE geofile;
EOF

# 导入数据库表结构
echo "7. 导入数据库表结构..."
if [ -f "backend/sql/schema.sql" ]; then
    mysql -u root -p geofile < backend/sql/schema.sql
    echo "✓ 数据库表结构导入成功"
else
    echo "⚠ 数据库表结构文件不存在: backend/sql/schema.sql"
fi

# 创建目录结构
echo "8. 创建项目目录结构..."
mkdir -p backend/src/main/java/com/geofile/{controller,service/impl,mapper,entity,dto,vo,config,security,util,exception}
mkdir -p backend/src/main/resources/{mapper,static,templates}
mkdir -p frontend/src/{api,components,layouts,router,stores,utils,views,assets}
mkdir -p logs

echo "================================"
echo "  初始化完成！"
echo "================================"
echo ""
echo "下一步操作："
echo "1. 进入后端目录: cd backend"
echo "2. 使用Spring Initializr创建Spring Boot项目: https://start.spring.io/"
echo "3. 进入前端目录: cd frontend"
echo "4. 运行: npm install"
echo "5. 启动后端: cd backend && mvn spring-boot:run"
echo "6. 启动前端: cd frontend && npm run dev"
echo ""
echo "详细文档请查看: PROJECT_DESIGN.md"
