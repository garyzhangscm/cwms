# FROM openjdk:8-jdk-alpine
# FROM openjdk:13-jdk-alpine
FROM openjdk:20-ea-13-jdk-slim
RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
RUN mkdir -p /usr/local/service
RUN echo $JAVA_HOME

ADD @project.build.finalName@.jar /usr/local/service/

ENTRYPOINT ["java",   "-jar", "/usr/local/service/@project.build.finalName@.jar"]