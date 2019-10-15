#!/bin/sh
echo "********************************************************"
echo "Waiting for the configuration server to start on port $CONFIGSERVER_PORT"
echo "********************************************************"
# while ! 'nc -z configserver $CONFIGSERVER_PORT '; do sleep 3; done
# The original bash file will only test if the ip and port is open. During testing
# I found that even the configuration server is up, it is possible that the
# url to get the actual configuration may not be ready yet. So here we will need
# to test the actual configuration file instead of the IP:PORT
#
# until $(curl --output /dev/null --silent --head --fail http://192.168.99.100:8888/layoutservice/default); do
#    printf '.'
#    sleep 5
#done
while ! nc -z configserver $CONFIGSERVER_PORT; do sleep 3; done
echo ">>>>>>>>>>>> Configuration Server has started"


echo "********************************************************"
echo "Starting Zuul Server with Configuration Service :  $CONFIGSERVER_URI";
echo "SPRING_PROFILE :  $SPRING_PROFILE";
echo "********************************************************"
java -Dspring.cloud.config.uri=$CONFIGSERVER_URI -Dspring.profiles.active=$SPRING_PROFILE -jar /usr/local/zuulserver/@project.build.finalName@.jar
