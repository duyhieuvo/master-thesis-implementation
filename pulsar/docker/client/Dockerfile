FROM openjdk:11

ADD /build/pulsarClient-jar-with-dependencies.jar /opt/pulsarClient.jar

ENTRYPOINT ["java", "-jar", "/opt/pulsarClient.jar"]