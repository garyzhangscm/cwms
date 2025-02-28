
# FROM openjdk:20-ea-13-jdk-slim
FROM openjdk:20-ea-17-jdk-slim-buster
# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/service

ADD target/APIGateway-v1.58.jar /usr/local/service/


ENTRYPOINT ["java","-jar","/usr/local/service/APIGateway-v1.58.jar"]

