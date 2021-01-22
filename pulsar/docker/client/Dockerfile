FROM openjdk:11

ADD /build/pulsarClient-jar-with-dependencies.jar /opt/pulsarClient.jar
ADD /build/byteman.jar /byteman/byteman.jar

ADD /byteman-rules/byteman_eventsgenerator.btm /byteman/byteman_eventsgenerator.btm
ADD /byteman-rules/byteman_streamprocessor.btm /byteman/byteman_streamprocessor.btm
ADD /byteman-rules/byteman_streamaggregator.btm /byteman/byteman_streamaggregator.btm


ENTRYPOINT ["java", "-jar", "/opt/pulsarClient.jar"]