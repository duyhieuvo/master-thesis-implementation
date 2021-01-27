package nats.configuration;

import util.EnvironmentVariableParser;

public interface Configuration {
    //General NATS configurations
    String CLIENT_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("CLIENT_NAME","default-client");
    String CLUSTER_ID = (String) EnvironmentVariableParser.getEnvironmentVariables("CLUSTER_ID","default-cluster");
    String NATS_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("NATS_URL","nats://localhost:4222");

    //NATS publisher configurations
    String SINK_CHANNEL_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("SINK_CHANNEL_NAME","default-sink-channel");


    //NATS subscriber configurations
    String SOURCE_CHANNEL_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("SOURCE_CHANNEL_NAME","default-source-channel");
    Boolean DURABLE_ENABLE = (Boolean) EnvironmentVariableParser.getEnvironmentVariables("DURABLE_ENABLE",false);
    String DURABLE_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("DURABLE_NAME","default-durable");
    //Specify the starting position to read messages on the channel: 0: from the first message on the channel, -1: from the last message on the channel (include also the last message), other long positive value: from the message with the matched sequence id on the channel
    Long START_POSITION = (Long) EnvironmentVariableParser.getEnvironmentVariables("START_POSITION",0L);
    //Specify maximum number of unacknowledged messages the server are allowed to push to the client.
    //To ensure that message is processed in order, this value should be set to 1. More detail: https://docs.nats.io/developing-with-nats-streaming/acks#max-in-flight
    Integer MAX_IN_FLIGHT = (Integer) EnvironmentVariableParser.getEnvironmentVariables("MAX_IN_FLIGHT",1);



    //Event generator configurations
    String PATH_TO_CSV = (String) EnvironmentVariableParser.getEnvironmentVariables("PATH_TO_CSV","C:\\Users\\dvo\\Thesis\\New\\Thesis\\Code\\implementation\\test_events.csv");

}
