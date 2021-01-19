package kafka.configuration;

import util.EnvironmentVariableParser;

public interface Configuration {
    //General Kafka configurations
    String KAFKA_URL = (String) EnvironmentVariableParser.getEnvironmentVariables("KAFKA_URL","localhost:19092");

    //Kafka consumer configurations
    String CONSUMER_ID = (String) EnvironmentVariableParser.getEnvironmentVariables("CONSUMER_ID","consumer-1");
    String GROUP_ID = (String) EnvironmentVariableParser.getEnvironmentVariables("GROUP_ID","default-consumer-group");
    String KAFKA_SOURCE_TOPIC = (String) EnvironmentVariableParser.getEnvironmentVariables("KAFKA_SOURCE_TOPIC","test-topic1");
    String OFFSET_RESET = (String) EnvironmentVariableParser.getEnvironmentVariables("OFFSET_RESET","earliest");
    int MAX_POLL_RECORDS = (Integer) EnvironmentVariableParser.getEnvironmentVariables("MAX_POLL_RECORDS",100);
    String KEY_DESERIALIZER = (String) EnvironmentVariableParser.getEnvironmentVariables("KEY_DESERIALIZER","org.apache.kafka.common.serialization.StringDeserializer");
    String VALUE_DESERIALIZER = (String) EnvironmentVariableParser.getEnvironmentVariables("VALUE_DESERIALIZER","org.apache.kafka.common.serialization.StringDeserializer");
    // In case manual assignment of partitions is used
    String ASSIGNED_PARTITION = (String) EnvironmentVariableParser.getEnvironmentVariables("ASSIGNED_PARTITION","0");


    //Kafka producer configurations
    String TRANSACTION_ID = (String) EnvironmentVariableParser.getEnvironmentVariables("TRANSACTION_ID","default-transaction-id");
    String PRODUCER_ID = (String) EnvironmentVariableParser.getEnvironmentVariables("PRODUCER_ID","producer-1");
    String KAFKA_SINK_TOPIC = (String) EnvironmentVariableParser.getEnvironmentVariables("KAFKA_SINK_TOPIC","test-topic");
    String KEY_SERIALIZER = (String) EnvironmentVariableParser.getEnvironmentVariables("KEY_SERIALIZER","org.apache.kafka.common.serialization.StringSerializer");
    String VALUE_SERIALIZER = (String) EnvironmentVariableParser.getEnvironmentVariables("VALUE_SERIALIZER","org.apache.kafka.common.serialization.StringSerializer");


    //Event generator configurations
    String PATH_TO_CSV = (String) EnvironmentVariableParser.getEnvironmentVariables("PATH_TO_CSV","C:\\Users\\dvo\\Thesis\\New\\Thesis\\Code\\implementation\\test_events.csv");



}
