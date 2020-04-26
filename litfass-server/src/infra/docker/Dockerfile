FROM openjdk:11-jre-slim

RUN mkdir /var/litfass

VOLUME /var/litfass/ext

WORKDIR /var/litfass

ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError --add-opens java.base/java.lang=ALL-UNNAMED"

EXPOSE $PORT

ADD ./app.jar app.jar
ADD ./lib lib

ENTRYPOINT java $JAVA_OPTS -server -ea -classpath "app.jar:lib/*:ext/*" lit.fass.server.ServerApplicationKt
