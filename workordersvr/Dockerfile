
# FROM openjdk:20-ea-13-jdk-slim
FROM openjdk:20-rc-oraclelinux7

ADD Starfield_Root_Certificate_Authority_G2.crt /usr/local/
ADD Starfield_Secure_Certificate_Authority_G2.crt /usr/local/
ADD www.mysilotrackcloud.com.crt /usr/local/

RUN cat /usr/local/Starfield_Root_Certificate_Authority_G2.crt >> /etc/pki/tls/certs/ca-bundle.crt
RUN cat /usr/local/Starfield_Secure_Certificate_Authority_G2.crt >> /etc/pki/tls/certs/ca-bundle.crt
RUN cat /usr/local/www.mysilotrackcloud.com.crt >> /etc/pki/tls/certs/ca-bundle.crt

# FROM openjdk:20-rc
# RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
# RUN  apk update && apk upgrade && apk add ca-certificates

# MAINTAINER Gary Zhang <gzhang1999@gmail.com>

RUN mkdir -p /usr/local/service

ADD target/workorderserver-v1.48.jar /usr/local/service/

ENTRYPOINT ["java","-jar","/usr/local/service/workorderserver-v1.48.jar"]