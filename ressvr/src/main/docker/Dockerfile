#### ROM openjdk:12-oraclelinux7
FROM openjdk:12-jdk-alpine
RUN apk update && apk upgrade && apk add netcat-openbsd && apk add ttf-dejavu
## Setup support for UTF-8
#### ENV MUSL_LOCALE_DEPS cmake make musl-dev gcc gettext-dev libintl
#### ENV MUSL_LOCPATH /usr/share/i18n/locales/musl
#### RUN apk add --no-cache \
####     $MUSL_LOCALE_DEPS \
####     && wget https://gitlab.com/rilian-la-te/musl-locales/-/archive/master/musl-locales-master.zip \
####     && unzip musl-locales-master.zip \
####       && cd musl-locales-master \
####       && cmake -DLOCALE_PROFILE=OFF -D CMAKE_INSTALL_PREFIX:PATH=/usr . && make && make install \
####       && cd .. && rm -r musl-locales-master

# Install language pack
# RUN apk --no-cache add ca-certificates wget && \
RUN apk --no-cache add ca-certificates wget && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-2.25-r0.apk && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-bin-2.25-r0.apk && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.25-r0/glibc-i18n-2.25-r0.apk && \
    apk add glibc-bin-2.25-r0.apk glibc-i18n-2.25-r0.apk glibc-2.25-r0.apk && \
    sleep 3
# RUN mkdir -p /usr/local/glibc
# ADD glibc-2.25-r0.apk /usr/local/glibc/
# ADD glibc-i18n-2.25-r0.apk /usr/local/glibc/
# ADD glibc-bin-2.25-r0.apk /usr/local/glibc/
# RUN apk add /usr/local/glibc/glibc-bin-2.25-r0.apk /usr/local/glibc/glibc-i18n-2.25-r0.apk /usr/local/glibc/glibc-2.25-r0.apk
# Iterate through all locale and install it
# Note that locale -a is not available in alpine linux, use `/usr/glibc-compat/bin/locale -a` instead
# RUN cat locale.md | xargs -i /usr/glibc-compat/bin/localedef -i {} -f UTF-8 {}.UTF-8
RUN /usr/glibc-compat/bin/localedef -i zh_CN -f UTF-8 zh_CN.UTF-8
RUN /usr/glibc-compat/bin/localedef -i en_US -f UTF-8 en_US.UTF-8
RUN /usr/glibc-compat/bin/localedef -i zh_TW -f UTF-8 zh_TW.UTF-8
# Set the lang, you can also specify it as as environment variable through docker-compose.yml
ENV LANG=zh_CN.UTF-8 \
    LANGUAGE=zh_CN.UTF-8
# Setup font for Chinese
# RUN apk add font-adobe-100dpi
# RUN mkdir -p /usr/share/fonts/win/
# ADD SimSun.ttf /usr/share/fonts/win/
# RUN chmod 777 /usr/share/fonts/win/SimSun.ttf
# Deploy the code
RUN mkdir -p /usr/local/ressvr
RUN mkdir -p /usr/local/reports
RUN mkdir -p /usr/local/reports/templates
RUN mkdir -p /usr/local/reports/templates_temp
ADD @project.build.finalName@.jar /usr/local/ressvr/
ADD run.sh run.sh
# below is the command to change the line feed. It is only needed when
# build the image from windows as windows and linux has different line feed
RUN sed -i 's/\r$//' run.sh  && \
        chmod +x run.sh
CMD ./run.sh