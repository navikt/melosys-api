FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
LABEL maintainer="Team Melosys"
WORKDIR /app

COPY /app/target/melosys-sb-execution.jar app.jar

# Gjelder bare utenfor NAIS. I NAIS overstyres den av JAVA_TOOL_OPTIONS i nais/nais.yml; endre begge.
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.language=no -Duser.country=NO -Duser.timezone=Europe/Oslo"
CMD ["-jar", "/app/app.jar"]
