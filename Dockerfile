FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY course-schedule-server-*.jar app.jar

VOLUME /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
