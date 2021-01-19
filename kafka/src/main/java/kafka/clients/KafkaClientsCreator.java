package kafka.clients;

import kafka.configuration.Configuration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class KafkaClientsCreator {
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

    public static KafkaProducer<String,String> createProducer(String partition){
        Properties props = new Properties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, Configuration.PRODUCER_ID);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Configuration.KAFKA_URL);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,Configuration.KEY_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,Configuration.VALUE_SERIALIZER);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,Configuration.TRANSACTION_ID + partition);

        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(props);
        producer.initTransactions();
        return producer;
    }

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
