FROM java:8
EXPOSE 80 443 8080
COPY target/service-spy*.jar service-spy.jar

ENTRYPOINT ["java", "-jar", "service-spy.jar", "--proxy.servers.http.port=80", "--proxy.servers.https.port=443", "--server.port=8080"]
