FROM navikt/java:8
LABEL maintainer="Team Melosys"

# NAIS requirements
ENV LC_ALL="no_NB.UTF-8"
ENV LANG="no_NB.UTF-8"
ENV TZ="Europe/Oslo"

# Tell the JVM to be aware of Docker memory limits
ENV DEFAULT_JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

COPY /build/jar/target/melosys.jar "/app/app.jar"

WORKDIR /app

EXPOSE 8080

# Initial command on docker container start
CMD java -jar $DEFAULT_JAVA_OPTS $JAVA_OPTS /app/app.jar