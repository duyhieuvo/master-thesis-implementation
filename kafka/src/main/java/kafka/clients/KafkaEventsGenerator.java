package kafka.clients;

import kafka.configuration.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import util.eventsource.CSVSourceEvent;
import util.eventsource.EventsPublisher;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class KafkaEventsGenerator implements EventsPublisher {
    private KafkaProducer<String,String> producer;
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private ProducerRecord<String,String> record, currentReadingPosition;
    private int counter; //The counter variable for Byteman failure injection

    public KafkaEventsGenerator(){
        producer = KafkaClientsCreator.createProducer();
        consumer = KafkaClientsCreator.createConsumer();
        objectMapper = new ObjectMapper();
        counter = 0;
        consumer.subscribe(Collections.singletonList(Configuration.KAFKA_SOURCE_TOPIC)); //Subscribe to "reading-position" topic to obtain the last processed line in the CSV source file
    }

    //Get the last processed line in the CSV source file from the "reading-position" Kafka topic
    public int getLastPublishedEvent(){
        int lastPublishedEvent = 0;
        ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(15));
        if(consumerRecords.isEmpty()){
            return 0;
        }
        for (ConsumerRecord<String, String> record : consumerRecords) {
            lastPublishedEvent = Integer.parseInt(record.value());
        }
        return lastPublishedEvent;
    }

    @Override
    public void publishEvents(Map<String, String> event) {
        try {
            //Get the customer ID to use as the key for records
            String customerId = event.get("customer");
            if(customerId==null){ //If there is no customer ID, throw exception and skip this event
                throw new IllegalArgumentException("Customer ID is missing");
            }

            //Convert the event to Json string
            String eventJson = objectMapper.writeValueAsString(event);

            //Create Kafka record with the event
            record = new ProducerRecord<String, String>(Configuration.KAFKA_SINK_TOPIC,customerId, eventJson);
            //Create Kafka record with the current line number in the CSV file
            currentReadingPosition = new ProducerRecord<String,String>(Configuration.KAFKA_SOURCE_TOPIC,event.get("id"));

            //Begin the transaction to atomically publish event and commit the corresponding reading position on the source file
            producer.beginTransaction();
            producer.send(record).get(); //Publish the actual event
            RecordMetadata recordMetadata = producer.send(currentReadingPosition).get(); //Publish the line number on CSV file
            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
            offsetsToCommit.put(new TopicPartition(recordMetadata.topic(),recordMetadata.partition()),new OffsetAndMetadata(recordMetadata.offset()));

            //Add the Byteman hook here to simulate the application crash during the transaction
            bytemanHook(counter);

            //Update the reading position of the Kafka consumer to the offset of the published line number in the same transaction
            //so that it can retrieve exactly the Kafka record with the corresponding reading position from the source file when restarting
            producer.sendOffsetsToTransaction(offsetsToCommit, consumer.groupMetadata().groupId());

            //Commit the transaction
            producer.commitTransaction();
            counter++;
            System.out.println("Published event: " + eventJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } catch(ProducerFencedException | OutOfOrderSequenceException e) {
            producer.close();
        } catch(KafkaException e) {
            producer.abortTransaction();
        }
    }

    public void generateEvents(){
        //get the id of the last published transaction to resume from that point onward
        int lastPublishedEvent = getLastPublishedEvent();
        CSVSourceEvent.generateEventFromCSV(Configuration.PATH_TO_CSV,this,lastPublishedEvent+1);
    }

    public void bytemanHook(int counter){
        return;
    }
}
