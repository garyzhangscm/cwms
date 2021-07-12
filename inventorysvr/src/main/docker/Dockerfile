FROM openjdk:8-jdk-alpine
# RUN echo -e "http://nl.alpinelinux.org/alpine/v3.5/main\nhttp://nl.alpinelinux.org/alpine/v3.5/community" > /etc/apk/repositories
RUN apk update && apk upgrade && apk add netcat-openbsd
RUN mkdir -p /usr/local/service
ADD @project.build.finalName@.jar /usr/local/service/
ADD run.sh run.sh
# below is the command to change the line feed. It is only needed when
# build the image from windows as windows and linux has different line feed
# RUN sed -i 's/\r$//' run.sh  && \
#        chmod +x run.sh
#CMD ./run.sh
ENTRYPOINT ["java",   "-jar", "/usr/local/service/@project.build.finalName@.jar"]
