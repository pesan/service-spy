FROM java:8
EXPOSE 8080 8081
COPY target/service-spy*.jar service-spy.jar

ENTRYPOINT ["java", "-jar", "service-spy.jar", "--proxy.port=8080", "--server.port=8081"]
