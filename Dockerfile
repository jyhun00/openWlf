# Multi-stage build for optimized image size
FROM gradle:9.2.1-jdk17 AS build

WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Copy all modules
COPY core-module ./core-module
COPY data-module ./data-module
COPY batch-module ./batch-module
COPY api-module ./api-module

# Build the application
RUN ./gradlew :api-module:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/api-module/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/filter/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
