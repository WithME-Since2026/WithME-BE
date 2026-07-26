FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]