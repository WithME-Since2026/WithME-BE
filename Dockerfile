FROM eclipse-temurin:21-jre-alpine

# 컨테이너 healthcheck에서 사용한다.
RUN apk add --no-cache curl

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
