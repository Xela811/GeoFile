# 1. 修改为支持 Java 21 的轻量级运行时镜像
FROM eclipse-temurin:21-jre-alpine

# 2. 设置容器内的时区为东八区（上海）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 3. 复制你的 GeoFile 项目 Jar 包
COPY target/geofile-backend.jar app.jar

EXPOSE 8080

# 4. 配置 JVM 参数并启动
# 提示：Java 21 默认开启了更现代的垃圾回收器，针对 256m-512m 的小内存场景，默认配置或搭配下面的参数就很够用
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]