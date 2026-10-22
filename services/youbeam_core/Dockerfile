# Etapa de compilación
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /usr/src/app

# Se copia primero el pom.xml para cachear la capa de dependencias mientras no cambie
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Extrae el jar de Spring Boot por capas (las dependencias cambian menos que el código)
RUN java -Djarmode=tools -jar target/youbeam-core-*.jar extract --layers --launcher --destination extracted

# Etapa de ejecución: solo JRE
FROM eclipse-temurin:21-jre-alpine

# curl para el HEALTHCHECK, ffmpeg (con libvpx y libopus) para transcodificar las subidas
# y últimos parches de seguridad
RUN apk upgrade --no-cache && apk add --no-cache curl ffmpeg

# Usuario sin privilegios de root por seguridad
RUN addgroup -S appuser \
    && adduser -S -G appuser -h /app -s /sbin/nologin appuser

WORKDIR /app

# Copia las capas del jar de la que menos cambia a la que más
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /usr/src/app/extracted/application/ ./

EXPOSE 8080

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Opciones de la JVM para contenedores, sobrescribibles en ejecución con JAVA_OPTS
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
