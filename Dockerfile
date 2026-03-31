FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace

COPY backend/ backend/
COPY --from=frontend-build /workspace/frontend/dist /workspace/frontend/dist

RUN cd backend \
    && mvn -q -DskipTests package \
    && cp "$(ls target/*.jar | grep -v '\.original$' | head -n 1)" /workspace/app.jar

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app

COPY --from=backend-build /workspace/app.jar /app/app.jar

EXPOSE 8082
USER app

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
