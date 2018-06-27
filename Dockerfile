FROM maven:3.5-jdk-8-slim
WORKDIR /src
COPY . .
RUN mvn clean package

FROM openjdk:8-jre-alpine3.7
EXPOSE 80 443 8080
WORKDIR /opt/service-spy
COPY --from=0 /src/target/service-spy*.jar service-spy.jar
ENTRYPOINT ["java", "-jar", "service-spy.jar", "--proxy.servers.http.port=80", "--proxy.servers.https.port=443", "--server.port=8080"]
