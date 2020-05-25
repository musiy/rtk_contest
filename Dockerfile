FROM openjdk:11.0.7-slim-buster

WORKDIR    /app
RUN        apt-get update && apt-get install -y iputils-ping telnet procps

COPY       target/rtk-contest-1.0-SNAPSHOT.jar ./
EXPOSE     80 9010 1044
#EXPOSE     80
CMD        java \
            -Xms1g \
            -Xmx2g \
#            -XX:+AlwaysPreTouch \
#            -Xlog:gc* \
            -XX:MaxGCPauseMillis=10 \
#            # отладка
#             -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044 \
#             -XX:+PrintFlagsFinal \
            -XX:+UseContainerSupport \
            -XX:ActiveProcessorCount=4 \
            -Dio.grpc.netty.shaded.io.netty.availableProcessors=4 \
#            -Dcom.sun.management.jmxremote \
#            -Dcom.sun.management.jmxremote.port=9010 \
#            -Dcom.sun.management.jmxremote.rmi.port=9010 \
#            -Dcom.sun.management.jmxremote.local.only=false \
#            -Dcom.sun.management.jmxremote.authenticate=false \
#            -Dcom.sun.management.jmxremote.ssl=false \
            -jar rtk-contest-1.0-SNAPSHOT.jar

#CMD        tail -f /dev/null