FROM openjdk:11.0.7-slim-buster

WORKDIR    /app
RUN        apt-get update && apt-get install -y iputils-ping telnet
COPY       target/rtk-contest-1.0-SNAPSHOT.jar ./
EXPOSE     80
# -XX:+PrintCompilation
CMD        java -jar rtk-contest-1.0-SNAPSHOT.jar