
# FROM openjdk:20-ea-13-jdk-slim
FROM openjdk:20-ea-13-oracle
# RUN apk update && apk upgrade && apk add libfreetype6

# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/reports
RUN mkdir -p /usr/local/reports/templates
RUN mkdir -p /usr/local/reports/templates_temp

RUN mkdir -p /usr/local/service

ADD target/resourceserver-v1.26.jar /usr/local/service/

ENTRYPOINT ["java",   "-jar", "/usr/local/service/resourceserver-v1.26.jar", "-Dfile.encoding=UTF-8", "-Dloader.path=/usr/local/reports/templates/", "org.springframework.boot.loader.PropertiesLauncher"]
