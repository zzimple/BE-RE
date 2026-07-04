# 모든 서비스 모듈이 공유하는 멀티스테이지 빌드 템플릿.
# 사용 예: docker build -f infra/Dockerfile --build-arg SERVICE_MODULE=gateway .
FROM eclipse-temurin:17-jdk AS builder
ARG SERVICE_MODULE
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY ${SERVICE_MODULE} ./${SERVICE_MODULE}

RUN ./gradlew :${SERVICE_MODULE}:bootJar --no-daemon

FROM eclipse-temurin:17-jre
ARG SERVICE_MODULE
WORKDIR /app
COPY --from=builder /workspace/${SERVICE_MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
