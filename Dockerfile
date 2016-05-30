FROM java:8
EXPOSE 80 81
COPY target/service-spy*.jar service-spy.jar

ENTRYPOINT ["java", "-jar", "service-spy.jar", "--proxy.port=81", "--server.port=80"]
