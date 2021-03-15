FROM navikt/java:15
LABEL maintainer="Team Melosys"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx2048m --enable-preview"

COPY /app/target/melosys.jar "/app/app.jar"
