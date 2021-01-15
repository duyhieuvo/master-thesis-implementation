package kafka.clients;

import kafka.configuration.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.kafka.clients.consumer.*;
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
    private Map<Integer,KafkaProducer<String,String>> producers;
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private ProducerRecord<String,String> producerRecord;

    public KafkaStreamProcessor(){
        producers = new HashMap<>();
        consumer = KafkaConsumerCreator.createConsumer();
        objectMapper = new ObjectMapper();

        consumer.subscribe(Collections.singletonList(Configuration.KAFKA_SOURCE_TOPIC), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    System.out.println("Partition revoked: " + topicPartition.partition());
                    producers.remove(topicPartition.partition());
                }

            }
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    System.out.println("Partition assigned: " + topicPartition.partition());
                    producers.put(topicPartition.partition(), KafkaProducerCreator.createProducer("partition-" + topicPartition.partition()));
                }
            }
        });
    }

    public void transformRawEvent(){
//        int i = 0;
        float value;
        String type;
        String customerId;
        while(true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(10));
            if(consumerRecords.isEmpty()){
                continue;
            }
            for(Map.Entry<Integer,KafkaProducer<String,String>> producer : producers.entrySet()){
                try{
                    producer.getValue().beginTransaction();
                }catch(ProducerFencedException | OutOfOrderSequenceException e) {
                    producer.getValue().close();
                    e.printStackTrace();
                }catch (KafkaException e) {
                    producer.getValue().abortTransaction();
                    e.printStackTrace();
                }
            }
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                JsonNode jsonNode = null;
                try {
                    jsonNode = objectMapper.readTree(consumerRecord.value());
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
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                Future<RecordMetadata> recordMetadata=producers.get(consumerRecord.partition()).send(producerRecord);
                System.out.println("Publish event: "+ producerRecord.value());
                long start = 0;
                float elapsed = 0;
                int wait = 1;
                start = System.currentTimeMillis();
                while(true){
                    elapsed= (System.currentTimeMillis()-start)/1000F;
                    if(elapsed>wait){
                        break;
                    }
                }

//              //Enforce sending records one by one instead of in batch to see the effect of read_committed to discard any record of unfinished transaction
//              //Otherwise, the application may be stopped before the batch is sent and therefore event the consumer with read_uncommitted cannot see these records
//              recordMetadata.get();
//              // Simulate application crashes during a transaction
//              i++;
//              System.out.println(i);
//              if(i==120){
//                   System.exit(1);
//              }
            }
            for(Map.Entry<TopicPartition, OffsetAndMetadata> offsetToCommit:getOffsetToCommitOnSourceTopic(consumerRecords).entrySet()){
                Map<TopicPartition, OffsetAndMetadata> mapOffsetToCommit = new HashMap<>();
                mapOffsetToCommit.put(offsetToCommit.getKey(),offsetToCommit.getValue());
                producers.get(offsetToCommit.getKey().partition()).sendOffsetsToTransaction(mapOffsetToCommit,consumer.groupMetadata().groupId());
            }
            for(Map.Entry<Integer,KafkaProducer<String,String>> producer : producers.entrySet()){
                try{
                    producer.getValue().commitTransaction();
                }catch(ProducerFencedException | OutOfOrderSequenceException e) {
                    producer.getValue().close();
                    e.printStackTrace();
                }catch (KafkaException e) {
                    producer.getValue().abortTransaction();
                    e.printStackTrace();
                }
            }
        }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
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
