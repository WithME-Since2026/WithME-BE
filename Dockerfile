FROM eclipse-temurin:21-jre-alpine

# 컨테이너 healthcheck에서 사용한다.
RUN apk add --no-cache curl

# 애플리케이션 취약점이나 컨테이너 탈출 시 권한 범위를 줄이기 위해 비루트로 실행한다.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

# bootJar 산출물만 대상으로 한다. build.gradle 에서 plain jar 생성을 꺼두었으므로
# 이 glob 은 항상 정확히 하나의 파일에 매칭된다.
ARG JAR_FILE=build/libs/*.jar
COPY --chown=app:app ${JAR_FILE} app.jar

USER app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# exec 로 java 를 PID 1 로 만든다. sh 가 PID 1 로 남으면 SIGTERM 이 JVM 에 전달되지 않아
# 종료 시 shutdown hook 과 graceful shutdown 이 동작하지 않는다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
