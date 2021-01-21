package pulsar.configuration;

import util.EnvironmentVariableParser;

public interface Configuration {
    //General Pulsar configurations
    String PULSAR_SERVICE_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SERVICE_URL","pulsar://localhost:6650");
    String PULSAR_WEB_SERVICE_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_WEB_SERVICE_URL","http://localhost:8080");

    //Pulsar consumer configurations
    String CONSUMER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("CONSUMER_NAME","consumer-1");
    String SUBSCRIPTION_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_NAME","default-subscription");
    String SUBSCRIPTION_TYPE = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_TYPE","Exclusive");
    String SUBSCRIPTION_INITIAL_POSITION = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_INITIAL_POSITION","Earliest");

    //Pulsar reader configuration
    String READER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("READER_NAME","reader-1");
    // In case of partitioned topics to create multiple reader instances
    String ASSIGNED_PARTITION = (String) EnvironmentVariableParser.getEnvironmentVariables("ASSIGNED_PARTITION","0");

    //Pulsar producer configurations
    String PRODUCER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("PRODUCER_NAME","producer-1");

    //Pulsar topics
    String PULSAR_SOURCE_TOPIC = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SOURCE_TOPIC","persistent://public/default/source-topic");
    String PULSAR_SINK_TOPIC= (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SINK_TOPIC","persistent://public/default/sink-topic");



    //Event generator configurations
    String PATH_TO_CSV = (String) EnvironmentVariableParser.getEnvironmentVariables("PATH_TO_CSV","C:\\Users\\dvo\\Thesis\\New\\Thesis\\Code\\implementation\\test_events.csv");
}
