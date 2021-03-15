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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class KafkaStreamProcessorNew {
    private KafkaProducer<String,String> producer;
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private ProducerRecord<String,String> producerRecord;
    private int counter; //The counter variable for Byteman failure injection
    static final Logger LOGGER = LoggerFactory.getLogger(KafkaStreamProcessorNew.class);

    public KafkaStreamProcessorNew(){
        producer = KafkaClientsCreator.createProducer();
        consumer = KafkaClientsCreator.createConsumer();
        consumer.subscribe(Collections.singletonList(Configuration.KAFKA_SOURCE_TOPIC), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    LOGGER.info("Partition revoked: " + topicPartition.partition());
                }

            }
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                for(TopicPartition topicPartition : collection){
                    LOGGER.info("Partition assigned: " + topicPartition.partition());
                }
            }
        });
        objectMapper = new ObjectMapper();
        counter = 0;

    }

    public void transformRawEvent(){
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


            try{
                producer.beginTransaction();
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                     producerRecord = processConsumedRecord(consumerRecord);
                    RecordMetadata recordMetadata=producer.send(producerRecord).get();
                    LOGGER.info("Published event: "+ producerRecord.value());
                    counter++;
                }
                //Add the Byteman hook here to simulate the application crash during the transaction
                bytemanHook(counter);


                //Update the offset on the source topic raw-event to pull new records the next time poll method is invoke
                //This is done in the same transaction
                producer.sendOffsetsToTransaction(getOffsetToCommitOnSourceTopic(consumerRecords),consumer.groupMetadata());


                //Commit the transactions
                producer.commitTransaction();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (OutOfOrderSequenceException  e) { //This exception is throw when Kafka producer executes send() method and Kafka broker detects out-of-order messages caused by producer retrying sending a message
//                e.printStackTrace();
                LOGGER.error("Unrecoverable exception is encountered, stop the application",e);
                producer.close();
                System.exit(-1);
            } catch (ProducerFencedException  e) { //This exception is throw by producer methods: beginTransaction(), sendOffsetsToTransaction(), commitTransaction() when a new instance of producer with the same transactional id as this producer is registered with the Kafka broker
//                e.printStackTrace();
                LOGGER.error("Unrecoverable exception is encountered, stop the application",e);
                producer.close();
                System.exit(-1);
            }  catch (CommitFailedException e) { //This exception is thrown when sendOffsetsToTransaction() is executed with obsolete metadata of the consumer group
//                e.printStackTrace();
                LOGGER.error("Commit failed",e);
                producer.abortTransaction();
            }  catch (KafkaException e) {
                e.printStackTrace();
            } catch (ExecutionException e) { //This exception is thrown when the send() method returns an error
                e.printStackTrace();
                if(e.getCause() != null && e.getCause() instanceof ProducerFencedException){ //This nested exception is thrown when a new instance of producer with the same transactional id as this producer is registered with the Kafka broker
                    LOGGER.error("Unrecoverable exception is encountered, stop the application",e.getCause());
                    producer.close();
                    System.exit(-1);
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

    //Process consumer record
    public ProducerRecord<String,String> processConsumedRecord(ConsumerRecord<String,String> record) {
        JsonNode jsonNode = null;
        ProducerRecord<String, String> producerRecord = null;
        try {
            jsonNode = objectMapper.readTree(record.value());
            float value = Float.valueOf(jsonNode.get("value").asText());
            String type = jsonNode.get("type").asText();
            String customerId = jsonNode.get("customer").asText();
            if (type.equals("WITHDRAW")) {
                value = value * (-1);
            }
            ObjectNode transformedRecord = objectMapper.createObjectNode();
            transformedRecord.put("id", jsonNode.get("id").asText());
            transformedRecord.put("customer", customerId);
            transformedRecord.put("value", value);
            producerRecord = new ProducerRecord<String, String>(Configuration.KAFKA_SINK_TOPIC, customerId, objectMapper.writeValueAsString(transformedRecord));

        }catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return producerRecord;
    }

    public void bytemanHook(int counter){
        return;
    }
    public void bytemanHookPartitionRevoked(int counter){
        return;
    }
}
