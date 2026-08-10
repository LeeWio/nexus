
# Build stage
FROM public.ecr.aws/docker/library/maven:3.9.15-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Restrict Maven compiler memory to prevent server OOM during build
ENV MAVEN_OPTS="-Xmx512m -XX:+UseSerialGC"

# Resolve only the dependencies required by the actual build and retain them
# independently of Docker layer invalidation.
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

# Optional JDK 21 verification target for the complete backend test suite.
FROM build AS test
RUN --mount=type=cache,target=/root/.m2 \
    mvn test

# Run stage
FROM public.ecr.aws/docker/library/amazoncorretto:21-alpine AS runner
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Application configurations
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=2m --retries=6 \
    CMD wget --quiet --spider --tries=1 --timeout=5 http://127.0.0.1:8080/readyz || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


# ========================================================
# 🌟 Stage 3: runner-prebuilt (专为 GitHub Actions 宿主机编译打造的极速精简运行层)
# ========================================================
FROM public.ecr.aws/docker/library/amazoncorretto:21-alpine AS runner-prebuilt

WORKDIR /app

# 直接从 GitHub 宿主机上 COPY 编译完的 JAR 产物
COPY target/*.jar app.jar

# 应用程序运行配置
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=2m --retries=6 \
    CMD wget --quiet --spider --tries=1 --timeout=5 http://127.0.0.1:8080/readyz || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
