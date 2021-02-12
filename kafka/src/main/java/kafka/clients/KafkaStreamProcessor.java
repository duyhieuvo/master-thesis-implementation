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


//This stream processor uses the old approach to fence off old instances when partition rebalance occurs
//In this approach, a new producer instance must be created for each partition assigned to this consumer
@Deprecated
public class KafkaStreamProcessor {
    private Map<Integer,KafkaProducer<String,String>> producers;
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private ProducerRecord<String,String> producerRecord;
    private int counter; //The counter variable for Byteman failure injection

    public KafkaStreamProcessor(){
        producers = new HashMap<>();
        consumer = KafkaClientsCreator.createConsumer();
        objectMapper = new ObjectMapper();
        counter = 0;

        //Subscribe consumer to the Kafka source topic "raw-event"
        //Add the ConsumerRebalanceListener to the consumer to react to when partition is assigned or revoked from the consumer
        //On partition assignment: create a new Kafka producer with the transaction ID from the assigned partition to fence off old instance from producing duplicated results for this partition
        //On partition revoked: delete the corresponding Kafka producer for this paritition
        //More detail about the Listener: https://kafka.apache.org/26/javadoc/org/apache/kafka/clients/consumer/ConsumerRebalanceListener.html
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
            //this stream processor instance and the failover instance are temporarily in split-brain state when they both think they own the same set of messages and produce new outputs for these records
            //without the fencing mechanism, the exactly-once semantics will be violated
            bytemanHookPartitionRevoked(counter);


            //Loop through all the producer instances and start transaction for each producer
            for(Map.Entry<Integer,KafkaProducer<String,String>> producer : producers.entrySet()){
                try{
                    producer.getValue().beginTransaction();
                }catch(ProducerFencedException | OutOfOrderSequenceException e) {
                    e.printStackTrace();
                    System.out.println("Closing the producer");
                    //In case of ProducerFencedException, there is already a newer producer instance with the same transaction id
                    //Close this producer instance
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
                        //Publish the transformed event with the corresponding producer of the input partition where the raw event came from
                        RecordMetadata recordMetadata=producers.get(consumerRecord.partition()).send(producerRecord).get();
                        System.out.println("Publish event: "+ producerRecord.value());
                    }
                    else{
                        //In case of encountering ProducerFencedException or partitionRevoked event, the producer of the revoked partition is closed and delete
                        //Any subsequent raw events from the same revoked partition will be dropped
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
                    //In case of ProducerFencedException, there is already a newer producer instance with the same transaction id
                    //Close this producer instance
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

            //Add the Byteman hook here to simulate the application crash during the transaction
            bytemanHook(counter);

            //Update the offset on the source topic raw-event to pull new records the next time poll method is invoke
            //This is done in the same transaction
            for(Map.Entry<TopicPartition, OffsetAndMetadata> offsetToCommit:getOffsetToCommitOnSourceTopic(consumerRecords).entrySet()){
                Map<TopicPartition, OffsetAndMetadata> mapOffsetToCommit = new HashMap<>();
                mapOffsetToCommit.put(offsetToCommit.getKey(),offsetToCommit.getValue());
                if(producers.containsKey(offsetToCommit.getKey().partition())){
                    try{
                        producers.get(offsetToCommit.getKey().partition()).sendOffsetsToTransaction(mapOffsetToCommit,consumer.groupMetadata().groupId());
                    }catch(ProducerFencedException e){
                        e.printStackTrace();
                        System.out.println("Closing the producer");
                        //In case of ProducerFencedException, there is already a newer producer instance with the same transaction id
                        //Close this producer instance
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
                    //In case of ProducerFencedException, there is already a newer producer instance with the same transaction id
                    //Close this producer instance
                    producer.getValue().close();
                    producers.remove(producer.getKey());
                }catch (KafkaException e) {
                    producer.getValue().abortTransaction();
                    e.printStackTrace();
                }
            }
        }
    }

    //Helper method to retrieve offset numbers of the last messages from each partition in the newly pull batch of records.
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
    public void bytemanHookPartitionRevoked(int counter){
        return;
    }
}
