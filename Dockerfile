
# Build stage
FROM public.ecr.aws/docker/library/maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Restrict Maven compiler memory to prevent server OOM during build
ENV MAVEN_OPTS="-Xmx512m -XX:+UseSerialGC"

# Resolve only the dependencies required by the actual build and retain them
# independently of Docker layer invalidation.
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

# Run stage
FROM public.ecr.aws/docker/library/amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Application configurations
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
