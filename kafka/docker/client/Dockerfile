FROM openjdk:11

ADD /build/kafkaClient-jar-with-dependencies.jar /opt/kafkaClient.jar

ENTRYPOINT ["java", "-jar", "/opt/kafkaClient.jar"]