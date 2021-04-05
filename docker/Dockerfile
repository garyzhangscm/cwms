FROM openjdk:12-jdk-alpine
RUN apk update && apk upgrade && apk add netcat-openbsd && apk add ttf-dejavu
RUN apk --no-cache add ca-certificates wget && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-2.25-r0.apk && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-bin-2.25-r0.apk && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-i18n-2.25-r0.apk && \
    apk add glibc-bin-2.25-r0.apk glibc-i18n-2.25-r0.apk glibc-2.25-r0.apk && \
RUN /usr/glibc-compat/bin/localedef -i zh_CN -f UTF-8 zh_CN.UTF-8
RUN /usr/glibc-compat/bin/localedef -i en_US -f UTF-8 en_US.UTF-8
RUN /usr/glibc-compat/bin/localedef -i zh_TW -f UTF-8 zh_TW.UTF-8
