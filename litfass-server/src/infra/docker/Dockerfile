FROM openjdk:11-jre-slim

RUN mkdir /var/litfass

WORKDIR /var/litfass

ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE $PORT

ADD ./app.jar app.jar

ENTRYPOINT java $JAVA_OPTS -jar -ea app.jar
