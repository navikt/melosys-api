FROM gcr.io/distroless/java17-debian12:nonroot
LABEL maintainer="Team Melosys"
WORKDIR /app

COPY /app/target/melosys-sb-execution.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.language=no -Duser.country=NO -Duser.timezone=Europe/Oslo"
CMD ["/app/app.jar"]
