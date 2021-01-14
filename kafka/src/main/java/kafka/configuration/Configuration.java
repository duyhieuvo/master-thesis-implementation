package kafka.configuration;

public interface Configuration {
    //General Kafka configurations
    String KAFKA_URL = (String) getEnvironmentVariables("KAFKA_URL","localhost:19092");

    //Kafka consumer configurations
    String CONSUMER_ID = (String) getEnvironmentVariables("CONSUMER_ID","consumer-1");
    String GROUP_ID = (String) getEnvironmentVariables("GROUP_ID","default-consumer-group");
    String KAFKA_SOURCE_TOPIC = (String) getEnvironmentVariables("KAFKA_SOURCE_TOPIC","test-topic1");
    String OFFSET_RESET = (String) getEnvironmentVariables("OFFSET_RESET","earliest");
    int MAX_POLL_RECORDS = (Integer) getEnvironmentVariables("MAX_POLL_RECORDS",100);
    String KEY_DESERIALIZER = (String) getEnvironmentVariables("KEY_DESERIALIZER","org.apache.kafka.common.serialization.StringDeserializer");
    String VALUE_DESERIALIZER = (String) getEnvironmentVariables("VALUE_DESERIALIZER","org.apache.kafka.common.serialization.StringDeserializer");
    // In case manual assignment of partitions is used
    String ASSIGNED_PARTITION = (String) getEnvironmentVariables("ASSIGNED_PARTITION","0");


    //Kafka producer configurations
    String TRANSACTION_ID = (String) getEnvironmentVariables("TRANSACTION_ID","default-transaction-id");
    String PRODUCER_ID = (String) getEnvironmentVariables("PRODUCER_ID","producer-1");
    String KAFKA_SINK_TOPIC = (String) getEnvironmentVariables("KAFKA_SINK_TOPIC","test-topic");
    String KEY_SERIALIZER = (String) getEnvironmentVariables("KEY_SERIALIZER","org.apache.kafka.common.serialization.StringSerializer");
    String VALUE_SERIALIZER = (String) getEnvironmentVariables("VALUE_SERIALIZER","org.apache.kafka.common.serialization.StringSerializer");


    //Event generator configurations
    String PATH_TO_CSV = (String) getEnvironmentVariables("PATH_TO_CSV","C:\\Users\\dvo\\Thesis\\New\\Thesis\\Code\\implementation\\test_events.csv");


    static Object getEnvironmentVariables(String envName, Object defaultValue) {
        Object result = null;
        String env = System.getenv(envName);
        if(env == null){
            result = defaultValue;
        }else{
            if(defaultValue instanceof Boolean){
                result = Boolean.valueOf(env);
            }else if(defaultValue instanceof Integer){
                result = Integer.valueOf(env);
            }else if(defaultValue instanceof Double){
                result = Double.valueOf(env);
            }else if(defaultValue instanceof Float){
                result = Float.valueOf(env);
            }else{
                result = env;
            }
        }
        return result;
    }



}
