package kafka.clients;

import kafka.configuration.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
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
    private int counter;

    public KafkaStreamProcessor(){
        producers = new HashMap<>();
        consumer = KafkaClientsCreator.createConsumer();
        objectMapper = new ObjectMapper();
        counter = 0;

        consumer.subscribe(Collections.singletonList(Configuration.KAFKA_SOURCE_TOPIC), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    System.out.println("Partition revoked: " + topicPartition.partition());
                    if(producers.containsKey(topicPartition.partition())){
                        producers.get(topicPartition.partition()).close();
                        producers.remove(topicPartition.partition());
                    }
                }

            }
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    System.out.println("Partition assigned: " + topicPartition.partition());
                    producers.put(topicPartition.partition(), KafkaClientsCreator.createProducer("partition-" + topicPartition.partition()));
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
            //simulate the case the application is paused and the consumer is considered disconnected by Kafka since it does not send new poll request within the time limit
            //in this case, this stream processor still have some records buffered locally from the last poll
            //this stream processor instance and the failover instance are temporarily in split-brain state when they both produce new outputs for these records
            //without the fencing mechanism, the exactly-once semantics will be violated
            bytemanHookZombieInstance();


            //Loop through all the producer instances and start transaction for each producer
            for(Map.Entry<Integer,KafkaProducer<String,String>> producer : producers.entrySet()){
                try{
                    producer.getValue().beginTransaction();
                }catch(ProducerFencedException | OutOfOrderSequenceException e) {
                    e.printStackTrace();
                    System.out.println("Closing the producer");
                    producer.getValue().close();
                    producers.remove(producer.getKey());
                }catch (KafkaException e) {
                    producer.getValue().abortTransaction();
                    e.printStackTrace();
                }
            }

            //Read records from the newly pulled batch, transform them and publish to the transformed-event topic
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
                    if(producers.containsKey(consumerRecord.partition())){
                        RecordMetadata recordMetadata=producers.get(consumerRecord.partition()).send(producerRecord).get();
                        System.out.println("Publish event: "+ producerRecord.value());
                    }
                    else{
                        System.out.println("The record belongs to the a revoked partition => skip");
                    }
                    counter++;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ProducerFencedException | OutOfOrderSequenceException e) {
                    e.printStackTrace();
                    System.out.println("Closing the producer");
                    producers.get(consumerRecord.partition()).close();
                    producers.remove(consumerRecord.partition());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    if(e.getCause() != null && e.getCause() instanceof ProducerFencedException){
                        System.out.println("Closing the producer");
                        producers.get(consumerRecord.partition()).close();
                        producers.remove(consumerRecord.partition());
                    }
                }
            }

            bytemanHook(counter);

            //Update the offset on the source topic raw-event to pull new records the next time poll method is invoke
            for(Map.Entry<TopicPartition, OffsetAndMetadata> offsetToCommit:getOffsetToCommitOnSourceTopic(consumerRecords).entrySet()){
                Map<TopicPartition, OffsetAndMetadata> mapOffsetToCommit = new HashMap<>();
                mapOffsetToCommit.put(offsetToCommit.getKey(),offsetToCommit.getValue());
                if(producers.containsKey(offsetToCommit.getKey().partition())){
                    try{
                        producers.get(offsetToCommit.getKey().partition()).sendOffsetsToTransaction(mapOffsetToCommit,consumer.groupMetadata().groupId());
                    }catch(ProducerFencedException e){
                        e.printStackTrace();
                        System.out.println("Closing the producer");
                        producers.get(offsetToCommit.getKey().partition()).close();
                        producers.remove(offsetToCommit.getKey().partition());
                    }
                }
            }

            //Commit the transactions
            for(Map.Entry<Integer,KafkaProducer<String,String>> producer : producers.entrySet()){
                try{
                    producer.getValue().commitTransaction();
                }catch(ProducerFencedException | OutOfOrderSequenceException e) {
                    e.printStackTrace();
                    System.out.println("Closing the producer");
                    producer.getValue().close();
                    producers.remove(producer.getKey());
                }catch (KafkaException e) {
                    producer.getValue().abortTransaction();
                    e.printStackTrace();
                }
            }
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

    public void bytemanHook(int counter){
        return;
    }
    public void bytemanHookZombieInstance(){
        return;
    }
}
