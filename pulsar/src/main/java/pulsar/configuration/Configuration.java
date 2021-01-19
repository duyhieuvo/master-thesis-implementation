package pulsar.configuration;

import util.EnvironmentVariableParser;

public interface Configuration {
    //General Pulsar configurations
    String PULSAR_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_URL","pulsar://localhost:16650,localhost:26650");

    //Pulsar consumer configurations
    String PULSAR_SOURCE_TOPIC = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SOURCE_TOPIC","persistent://public/default/test-topic");
    String CONSUMER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("CONSUMER_NAME","consumer-1");
    String SUBSCRIPTION_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_NAME","default-subscription");
    String SUBSCRIPTION_TYPE = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_TYPE","Exclusive");
    String SUBSCRIPTION_INITIAL_POSITION = (String) EnvironmentVariableParser.getEnvironmentVariables("SUBSCRIPTION_INITIAL_POSITION","Earliest");

    //Pulsar reader configuration
    String PULSAR_SOURCE_READER_TOPIC = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SOURCE_READER_TOPIC","persistent://public/default/reading-position");
    String READER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("READER_NAME","reader-1");

    //Pulsar producer configurations
    String PULSAR_SINK_TOPIC= (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_SINK_TOPIC","persistent://public/default/test-topic");
    String PRODUCER_NAME = (String) EnvironmentVariableParser.getEnvironmentVariables("PRODUCER_NAME","producer-1");

    //Pulsar admin configuratiosn
    String PULSAR_ADMIN_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("PULSAR_ADMIN_URL","http://localhost:8080");

    //Event generator configurations
    String PATH_TO_CSV = (String) EnvironmentVariableParser.getEnvironmentVariables("PATH_TO_CSV","C:\\Users\\dvo\\Thesis\\New\\Thesis\\Code\\implementation\\test_events.csv");
}
