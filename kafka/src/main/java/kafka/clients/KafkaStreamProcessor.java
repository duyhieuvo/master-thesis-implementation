package kafka.clients;

import kafka.configuration.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaStreamProcessor {
    private KafkaProducer<String,String> producer;
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private ProducerRecord<String,String> producerRecord;

    public KafkaStreamProcessor(){
        producer = KafkaProducerCreator.createProducer();
        consumer = KafkaConsumerCreator.createConsumer();
        objectMapper = new ObjectMapper();

        consumer.subscribe(Collections.singletonList(Configuration.KAFKA_SOURCE_TOPIC));
    }

    public void transformRawEvent(){
//        int i = 0;
        float value;
        String type;
        String customerId;
        while(true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(10));
            try {
                producer.beginTransaction();
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    JsonNode jsonNode = objectMapper.readTree(consumerRecord.value());
                    value = Float.valueOf(jsonNode.get("value").asText());
                    type = jsonNode.get("type").asText();
                    customerId = jsonNode.get("customer").asText();
                    if (type.equals("WITHDRAW")) {
                        value = value * (-1);
                    }
                    ObjectNode transformedRecord = objectMapper.createObjectNode();
                    transformedRecord.put("id", jsonNode.get("id").asText());
                    transformedRecord.put("customer",customerId);
                    transformedRecord.put("value", value);
                    producerRecord = new ProducerRecord<String, String>(Configuration.KAFKA_SINK_TOPIC, customerId, objectMapper.writeValueAsString(transformedRecord));
                    Future<RecordMetadata> recordMetadata=producer.send(producerRecord);

//                    //Enforce sending records one by one instead of in batch to see the effect of read_committed to discard any record of unfinished transaction
//                    //Otherwise, the application may be stopped before the batch is sent and therefore event the consumer with read_uncommitted cannot see these records
//                    recordMetadata.get();
//                    // Simulate application crashes during a transaction
//                    i++;
//                    System.out.println(i);
//                    if(i==120){
//                        System.exit(1);
//                    }
                }
                producer.sendOffsetsToTransaction(getOffsetToCommitOnSourceTopic(consumerRecords), consumer.groupMetadata().groupId());
                producer.commitTransaction();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (ProducerFencedException | OutOfOrderSequenceException e) {
                producer.close();
            } catch (KafkaException e) {
                producer.abortTransaction();
            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
        }
    }

    public Map<TopicPartition, OffsetAndMetadata> getOffsetToCommitOnSourceTopic(ConsumerRecords records){
        Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
        Set<TopicPartition> topicPartitionSet = records.partitions();
        for (TopicPartition partition : topicPartitionSet) {
            List<ConsumerRecord> partitionedRecords = records.records(partition);
            long offset = partitionedRecords.get(partitionedRecords.size() - 1).offset();
            offsetsToCommit.put(partition, new OffsetAndMetadata(offset + 1));
        }
        return offsetsToCommit;
    }
}
