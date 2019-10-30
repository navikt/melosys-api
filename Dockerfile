FROM navikt/java:8
LABEL maintainer="Team Melosys"

ENV JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC -Xms512m -Xmx2048m"

COPY /build/target/melosys.jar "/app/app.jar"
