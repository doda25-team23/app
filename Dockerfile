# ===========================
# Stage 1: Build the application
# ===========================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app

# Build args for GitHub Packages authentication
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# Create Maven settings.xml with GitHub Packages credentials
RUN mkdir -p /root/.m2 && \
    echo '<settings><servers><server><id>github</id><username>'${GITHUB_ACTOR}'</username><password>'${GITHUB_TOKEN}'</password></server></servers></settings>' > /root/.m2/settings.xml

# Copy pom.xml and download dependencies (improves caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests

# Remove credentials from build stage
RUN rm -f /root/.m2/settings.xml


# ===========================
# Stage 2: Runtime Image
# ===========================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# ===== F6: Flexible configuration =====
# Default app port
ENV APP_PORT=8080

# Default backend host (model-service)
ENV MODEL_HOST=http://model-service:8081

# Expose dynamic port
EXPOSE ${APP_PORT}

# Run Spring Boot with dynamic port + model host
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${APP_PORT} --model.host=${MODEL_HOST}"]
