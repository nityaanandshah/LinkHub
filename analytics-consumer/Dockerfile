# ─── Stage 1: Build ───
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy parent POM and all module POMs for dependency caching
COPY pom.xml ./pom.xml
COPY analytics-consumer/pom.xml ./analytics-consumer/pom.xml
COPY backend/pom.xml ./backend/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl analytics-consumer -am -B

# Copy source code
COPY analytics-consumer/src ./analytics-consumer/src

# Build the application (skip tests for Docker build)
RUN mvn clean package -pl analytics-consumer -am -DskipTests -B

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S linkhub && adduser -S linkhub -G linkhub

# Copy JAR from build stage
COPY --from=build /app/analytics-consumer/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs /data/geoip && chown -R linkhub:linkhub /app /data/geoip

USER linkhub

EXPOSE 8081

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
