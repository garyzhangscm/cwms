FROM openjdk:8-jdk-alpine
RUN apk update && apk upgrade && apk add netcat-openbsd
RUN mkdir -p /usr/local/workorderserver
ADD @project.build.finalName@.jar /usr/local/workorderserver/
ADD run.sh run.sh
# below is the command to change the line feed. It is only needed when
# build the image from windows as windows and linux has different line feed
RUN sed -i 's/\r$//' run.sh  && \
        chmod +x run.sh
CMD ./run.sh
