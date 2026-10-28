# Etapa 1: Compilar con Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Entorno con Python y gdown para sincronizar Google Drive
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache python3 py3-pip bash curl && \
    pip install --no-cache-dir gdown --break-system-packages

COPY --from=builder /app/target/dian-study-bot-1.0.0.jar app.jar
COPY start.sh .
RUN chmod +x start.sh

CMD ["./start.sh"]
