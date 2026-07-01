FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

COPY backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx450m", "-XX:MaxMetaspaceSize=128m", "-XX:MaxDirectMemorySize=64m", "-Xss256k", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
