FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:7e54b85b5d9bb53b33d17584d6007b2cf27acdcf7d55da1cfc0570c4c347e821
LABEL maintainer="Team Melosys"
WORKDIR /app

COPY /app/target/melosys-sb-execution.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.language=no -Duser.country=NO -Duser.timezone=Europe/Oslo"
CMD ["-jar", "/app/app.jar"]
