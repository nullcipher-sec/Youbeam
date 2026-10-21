# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /usr/src/app

# Copy pom.xml first so the dependency layer is cached while pom.xml doesn't change
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Extract the Spring Boot jar into layers (dependencies change less than application code)
RUN java -Djarmode=tools -jar target/youbeam-core-*.jar extract --layers --launcher --destination extracted

# Runtime stage: JRE only
FROM eclipse-temurin:21-jre-alpine

# curl for HEALTHCHECK, latest security patches
RUN apk upgrade --no-cache && apk add --no-cache curl

# Non-root user for security
RUN addgroup -S appuser \
    && adduser -S -G appuser -h /app -s /sbin/nologin appuser

WORKDIR /app

# Copy jar layers from least to most frequently changed
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/application/ ./

EXPOSE 8080

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containers, overridable at runtime with JAVA_OPTS
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
