FROM navikt/java:8
LABEL maintainer="Team Melosys"

COPY /build/target/melosys.jar "/app/app.jar"
