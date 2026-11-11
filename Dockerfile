# SZU Ops 后端镜像（仓库根级，供 CloudBase Run GitHub 构建使用）
# 构建上下文为仓库根目录；server/ 下的 Dockerfile 供本地或其他平台使用
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY server/settings-aliyun.xml /root/.m2/settings.xml
COPY server/pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY server/src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/campus-ops-server-1.0.0.jar app.jar
# CloudBase Run 服务端口为 80，应用通过 ${PORT} 读取
ENV PORT=80
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]
