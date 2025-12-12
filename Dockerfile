# --- Stage 1: Build ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# 1. Copy only Maven wrapper + pom first for caching dependencies
COPY pom.xml mvnw* ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw && ./mvnw dependency:go-offline

# 2. Cache buster (optional)
ARG CODE_VERSION=1
RUN echo "Building version: ${CODE_VERSION}"

# 3. Copy the source code
COPY src/ src/

# 4. Build application (no clean → faster)
RUN ./mvnw package -DskipTests

# --- Stage 2: Run ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the final jar from build stage
COPY --from=build /workspace/target/relayhook-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
