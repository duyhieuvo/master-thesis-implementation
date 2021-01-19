FROM openjdk:11

ADD /build/kafkaClient-jar-with-dependencies.jar /opt/kafkaClient.jar
ADD /build/byteman.jar /byteman/byteman.jar

ADD /byteman-rules/byteman_eventsgenerator.btm /byteman/byteman_eventsgenerator.btm
ADD /byteman-rules/byteman_streamprocessor.btm /byteman/byteman_streamprocessor.btm
ADD /byteman-rules/byteman_streamaggregator.btm /byteman/byteman_streamaggregator.btm
ADD /byteman-rules/byteman_fencedproducer.btm /byteman/byteman_fencedproducer.btm


ENTRYPOINT ["java", "-jar", "/opt/kafkaClient.jar"]