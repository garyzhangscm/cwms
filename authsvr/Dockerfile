
# FROM openjdk:20-ea-13-jdk-slim
FROM openjdk:20-ea-17-jdk-slim-buster
# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/service

ADD target/authenticationserver-v1.63.jar /usr/local/service/

ENTRYPOINT ["java","-jar","/usr/local/service/authenticationserver-v1.63.jar"]
