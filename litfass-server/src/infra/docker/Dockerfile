FROM openjdk:8-jre-slim

RUN mkdir /var/litfass

VOLUME /var/litfass/conf
VOLUME /var/litfass/ext

WORKDIR /var/litfass

ENV PORT=8080 \
    JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:InitialRAMFraction=2 -XX:MinRAMFraction=2 -XX:MaxRAMFraction=2 -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE $PORT

ADD ./app.jar app.jar
ADD ./lib lib
ADD ./application.conf conf/application.conf
ADD ./logback.xml conf/logback.xml

ENTRYPOINT java $JAVA_OPTS -server -ea -classpath "app.jar:lib/*:conf/*:ext/*" lit.fass.litfass.server.AppKt -Dlogback.configurationFile=conf/logback.xml -config=conf/application.conf

HEALTHCHECK --interval=3m --timeout=10s CMD curl -f http://localhost:$PORT/health || exit 1
