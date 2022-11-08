FROM maven:3.8-openjdk-17-slim as backend
WORKDIR /src
COPY . .
RUN ["mvn", "-DskipTests", "clean", "package"]

FROM node:18.11-alpine3.16 as frontend
COPY ./frontend /src
WORKDIR /src
RUN ["npm", "install"]
RUN ["npm", "run", "build"]

FROM openjdk:17.0-slim
EXPOSE 80 443 8080
WORKDIR /opt/service-spy
COPY --from=backend /src/service-spy-service/target/service-spy*-fat.jar service-spy.jar
COPY --from=frontend /src/build webroot
COPY ./conf/docker-application.yml application.yml
ENTRYPOINT ["java", "-jar", "service-spy.jar"]
