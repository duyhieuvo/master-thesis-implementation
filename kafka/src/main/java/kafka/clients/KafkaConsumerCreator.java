package kafka.clients;

import kafka.configuration.Configuration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

public class KafkaConsumerCreator {

    public static KafkaConsumer<String,String> createConsumer(){
        Properties props = new Properties();
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, Configuration.CONSUMER_ID);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Configuration.KAFKA_URL);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,Configuration.KEY_DESERIALIZER);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,Configuration.VALUE_DESERIALIZER);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, Configuration.GROUP_ID);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Configuration.MAX_POLL_RECORDS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, Configuration.OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,"read_committed");

        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(props);
        return consumer;
    }
}
