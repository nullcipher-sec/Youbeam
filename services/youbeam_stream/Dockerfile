# ==========================================
# ETAPA 1: Compilación (Builder)
# ==========================================
FROM golang:1.27-alpine AS builder

# Instalar certificados y git (útil para dependencias)
RUN apk add --no-cache git ca-certificates

WORKDIR /app

# Copiar archivos de dependencias primero (para aprovechar la caché de Docker)
COPY go.mod go.sum ./
RUN go mod download

# Copiar el resto del código fuente
COPY *.go ./

# Compilar el binario (el punto al final le dice que compile todos los archivos de la carpeta)
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /youbeam-stream .

# ==========================================
# ETAPA 2: Imagen Final (Producción)
# ==========================================
FROM alpine:3.20

# Instalar certificados raíz (necesarios para conexiones HTTPS seguras)
RUN apk add --no-cache ca-certificates

WORKDIR /app

# Copiar solo el binario compilado desde la etapa anterior
COPY --from=builder /youbeam-stream /usr/local/bin/youbeam-stream

# Exponer los puertos:
# 8081/TCP para el endpoint HTTP /offer (señalización interna)
# 8443/UDP para la transmisión de video/audio WebRTC
EXPOSE 8081/tcp
EXPOSE 8443/udp

# Comando de ejecución
CMD ["youbeam-stream"]