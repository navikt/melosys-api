FROM docker.adeo.no:5000/jetty:9.4.6-jre8-alpine

ARG app_name
ARG version

# Kubernetes requirements from NAIS
ENV LC_ALL="no_NB.UTF-8"
ENV LANG="no_NB.UTF-8"
ENV TZ="Europe/Oslo"

# Tell the JVM to be aware of Docker memory limits
ENV DEFAULT_JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# Copy webapp into jetty folder
COPY $app_name-$version.war "webapps/root.war

# Expose default http jetty port
EXPOSE 8080

# Initial command on docker container start
ENTRYPOINT ["java", "-jar $DEFAULT_JAVA_OPTS $JAVA_OPTS", "/usr/local/jetty/start.jar"]