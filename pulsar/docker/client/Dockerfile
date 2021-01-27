FROM openjdk:11

#Add jar file of compiled Pulsars client to the Docker image
ADD /build/pulsarClient-jar-with-dependencies.jar /opt/pulsarClient.jar

#Add byteman rules for different failure scenarios
RUN mkdir -p /byteman
ADD /byteman-rules/byteman_eventsgenerator.btm /byteman/byteman_eventsgenerator.btm
ADD /byteman-rules/byteman_streamprocessor.btm /byteman/byteman_streamprocessor.btm
ADD /byteman-rules/byteman_streamaggregator.btm /byteman/byteman_streamaggregator.btm
ADD /byteman-rules/byteman_duplicatedproducer.btm /byteman/byteman_duplicatedproducer.btm

#Download byteman jar file for failure injection
RUN curl -o byteman.zip https://downloads.jboss.org/byteman/4.0.13/byteman-download-4.0.13-bin.zip
RUN (unzip byteman.zip -d byteman-unzip && cp /byteman-unzip/byteman-download-4.0.13/lib/byteman.jar /byteman/byteman.jar)
RUN (rm -f byteman.zip && rm -r byteman-unzip)

ENTRYPOINT ["java", "-jar", "/opt/pulsarClient.jar"]