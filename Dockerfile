FROM navikt/java:17
LABEL maintainer="Team Melosys"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx2048m"

COPY docker-init-scripts/*.sh /init-scripts/
COPY /app/target/melosys.jar "/app/app.jar"
