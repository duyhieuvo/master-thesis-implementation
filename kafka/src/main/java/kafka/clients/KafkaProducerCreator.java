package kafka.clients;

import kafka.configuration.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class KafkaProducerCreator {
    public static KafkaProducer<String,String> createProducer(){
        Properties props = new Properties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, Configuration.PRODUCER_ID);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Configuration.KAFKA_URL);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,Configuration.KEY_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,Configuration.VALUE_SERIALIZER);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,Configuration.TRANSACTION_ID);

        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(props);
        producer.initTransactions();
        return producer;
    }
}
