FROM docker.elastic.co/logstash/logstash:7.8.0
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
ADD pipeline/ /usr/share/logstash/pipeline/
RUN mkdir -p /usr/share/logstash/test-log
USER root
RUN chown root:logstash /usr/share/logstash/pipeline/logstash.conf
RUN chmod go-w /usr/share/logstash/pipeline/logstash.conf
USER logstash
