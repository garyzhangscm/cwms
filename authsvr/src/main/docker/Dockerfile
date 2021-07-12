FROM openjdk:8-jdk-alpine
RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add curl
RUN mkdir -p /usr/local/service
RUN echo $JAVA_HOME
RUN cd /tmp/ && \
# This is the docker file copied from <Spring Microservices in Action> by John Carnell. curl in openjdk:8-jdk-alpine seems has bugs
# which causes the download fail and the jce_policy-8.zip is actually a 404 html response. So the unzip will fail and report a 'short read' error
#    curl -k -LO "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip" -H 'Cookie: oraclelicense=accept-securebackup-cookie' && \
    curl -L -b "oraclelicense=a" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip -O && \
    unzip jce_policy-8.zip && \
    rm jce_policy-8.zip && \
    yes |cp -v /tmp/UnlimitedJCEPolicyJDK8/*.jar /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/
ADD @project.build.finalName@.jar /usr/local/service/
ADD run.sh run.sh
# below is the command to change the line feed. It is only needed when
# build the image from windows as windows and linux has different line feed
#RUN sed -i 's/\r$//' run.sh  && \
#        chmod +x run.sh
#CMD ./run.sh
ENTRYPOINT ["java",   "-jar", "/usr/local/service/@project.build.finalName@.jar"]