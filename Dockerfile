FROM eclipse-temurin:22-alpine

COPY target/assignment-0.0.1.jar app.jar
EXPOSE 5050

ENV KAFKA_HOST=kafka:9092
ENV REDIS_HOST=redis
ENV MYSQL_URL=jdbc:mysql://host.docker.internal:2000/ipl
ENV MYSQL_USERNAME=root
ENV MYSQL_PASSWORD=root

ENTRYPOINT ["java", "-jar", \
    "-Dspring.kafka.bootstrap-servers=${KAFKA_HOST}", \
    "-Dspring.data.redis.host=${REDIS_HOST}", \
    "-Dspring.datasource.url=${MYSQL_URL}", \
    "-Dspring.datasource.username=${MYSQL_USERNAME}", \
    "-Dspring.datasource.password=${MYSQL_PASSWORD}", \
    "/app.jar"]