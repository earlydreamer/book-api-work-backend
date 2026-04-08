# Stage 1: Build
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

# 캐시 활용을 위해 의존성 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 의존성 다운로드
RUN ./gradlew --no-daemon dependencies

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew --no-daemon bootJar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안을 위해 실행 전용 유저 생성 권장 (여기서는 기본 루트로 진행하되 필요시 조정 가능)
# RUN addgroup -S spring && adduser -S spring -G spring
# USER spring:spring

# 빌드된 JAR 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 실행 시 환경 변수 주입 가능하도록 설정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

EXPOSE 8080
