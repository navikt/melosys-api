FROM ghcr.io/navikt/baseimages/temurin:17
LABEL maintainer="Team Melosys"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1536m"

COPY docker-init-scripts/*.sh /init-scripts/
COPY /app/target/melosys-sb-execution.jar "/app/app.jar"
