FROM maven:3.8-openjdk-11-slim as backend
WORKDIR /src
COPY . .
RUN ["mvn", "-DskipTests", "clean", "package"]

FROM node:16-alpine3.11 as frontend
COPY ./frontend /src
WORKDIR /src
RUN ["npm", "install"]
RUN ["npm", "run", "build"]

FROM openjdk:11.0.12-jre-slim
EXPOSE 80 443 8080
WORKDIR /opt/service-spy
COPY --from=backend /src/target/service-spy*-fat.jar service-spy.jar
COPY --from=frontend /src/build /opt/webroot
COPY ./conf/docker-application.yml application.yml
ENTRYPOINT ["java", "-jar", "service-spy.jar"]