FROM centos:7
FROM store/oracle/jdk:11

WORKDIR    /app

COPY       target/rtk-contest-1.0-SNAPSHOT.jar ./
#EXPOSE     80 9010 1044
EXPOSE     80

CMD        java \
            -Xms1g \
            -Xmx1.8g \
            -XX:+AlwaysPreTouch \
#            -Xlog:gc* \
            -XX:MaxGCPauseMillis=10 \
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