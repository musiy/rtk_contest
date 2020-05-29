FROM centos:7
FROM shipilev/openjdk-shenandoah:8

WORKDIR    /app

COPY       target/rtk-contest-1.0-SNAPSHOT.jar ./
#EXPOSE     80 9010 1044
EXPOSE     80

CMD        java \
            -Xms1800m \
            -Xmx1800m \
            -XX:+AlwaysPreTouch \
            -XX:+UseContainerSupport \
#            -Xlog:gc* \
            -XX:+UseShenandoahGC \
#            -XX:+UseG1GC \
#            -XX:MaxGCPauseMillis=10 \
#            -XX:ConcGCThreads=2 \
#            -XX:+UnlockExperimentalVMOptions \
#            -XX:G1MixedGCLiveThresholdPercent=90 \
#            -XX:InitiatingHeapOccupancyPercent=90 \
#            -XX:G1NewSizePercent=70 \
#            -XX:G1MaxNewSizePercent=70 \
#            -XX:G1MixedGCCountTarget=1 \
            -XX:ActiveProcessorCount=4 \
            -Dio.grpc.netty.shaded.io.netty.availableProcessors=4 \
            -Dcom.sun.management.jmxremote \
            -Dcom.sun.management.jmxremote.port=9010 \
            -Dcom.sun.management.jmxremote.rmi.port=9010 \
            -Dcom.sun.management.jmxremote.local.only=false \
            -Dcom.sun.management.jmxremote.authenticate=false \
            -Dcom.sun.management.jmxremote.ssl=false \
            -jar rtk-contest-1.0-SNAPSHOT.jar

#CMD        tail -f /dev/null