# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache bash curl

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package \
    && cp target/*.jar /workspace/app.jar

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S blog && adduser -S blog -G blog \
    && mkdir -p /data/uploads \
    && chown -R blog:blog /data /app

COPY --from=build /workspace/app.jar /app/app.jar

USER blog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
