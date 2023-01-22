
FROM openjdk:20-ea-13-jdk-slim
# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/service

ARG VERSION
ADD target/zuulsvr-${VERSION}.jar /usr/local/service/

ENV env_version=${VERSION}

# ENTRYPOINT ["java","-jar","/usr/local/service/zuulsvr-$env_version.jar"]
ENTRYPOINT java -jar /usr/local/service/zuulsvr-$env_version.jar

