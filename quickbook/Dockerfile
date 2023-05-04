
FROM openjdk:20-ea-13-jdk-slim
# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/service

ADD target/quickbook-v1.9.jar /usr/local/service/

ENTRYPOINT ["java","-jar","/usr/local/service/quickbook-v1.9.jar"]