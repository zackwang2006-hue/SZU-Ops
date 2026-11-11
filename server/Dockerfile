# SZU Ops 后端镜像：云端多阶段构建（CloudBase Run）
# 阶段一：Maven 构建（阿里云镜像加速依赖下载）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY settings-aliyun.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B package -DskipTests

# 阶段二：精简运行时
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/campus-ops-server-1.0.0.jar app.jar
# CloudBase Run 通过 PORT 注入监听端口
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
