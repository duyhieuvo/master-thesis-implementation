package pulsar.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.transaction.Transaction;

import pulsar.configuration.Configuration;
import util.eventsource.CSVSourceEvent;
import util.eventsource.EventsPublisher;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PulsarEventsGenerator implements EventsPublisher {
    private PulsarClient client;
    private Producer<String> producerEvent,  producerReadingPosition;
    private Consumer<String> consumerReadingPosition;
    private ObjectMapper objectMapper;
    private int counter; //The counter variable for Byteman failure injection
    private MessageId lastPublishedReadingPosition;

    public PulsarEventsGenerator() {
        client = PulsarClientsCreator.createClient();
        producerEvent = PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SINK_TOPIC);
        producerReadingPosition = PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SOURCE_TOPIC);
        consumerReadingPosition = PulsarClientsCreator.createConsumer(client,Configuration.PULSAR_SOURCE_TOPIC);
        objectMapper = new ObjectMapper();
        counter = 0;
    }

    //Get the last processed line in the CSV source file from the "reading-position" Pulsar topic
    public int getLastPublishedEvent(){
        int lastPublishedEvent = 0;
        try {
            Message<String> message= consumerReadingPosition.receive(10,TimeUnit.SECONDS);
            if(message!=null){
                lastPublishedEvent = Integer.parseInt(message.getValue());
                lastPublishedReadingPosition = message.getMessageId();
            }
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        return lastPublishedEvent;
    }


    @Override
    public void publishEvents(Map<String, String> event) {
        try {
            //Get the customer ID to use as the key for records
            String customerId = event.get("customer");
            if(customerId==null) {
                throw new IllegalArgumentException("Customer ID is missing");
            }
            String eventJson = objectMapper.writeValueAsString(event);

            //Create the transaction to publish event along with the corresponding row number in the source CSV file
            Transaction txn = client
                    .newTransaction()
                    .withTransactionTimeout(5, TimeUnit.MINUTES)
                    .build()
                    .get();

            //Send the event
            producerEvent.newMessage(txn)
                    .key(customerId)
                    .value(eventJson)
                    .send();
            System.out.println("Publish event: " + eventJson);

            //Add the Byteman hook here to simulate the application crash during the transaction
            bytemanHook(counter);

            //Send the row number in source CSV file and get the returned message ID when the send operation is successful
            MessageId messageId = producerReadingPosition.newMessage(txn)
                    .value(event.get("id"))
                    .send();
            System.out.println("Publish reading position: " + event.get("id"));

            //Acknowledge message in the reading position up to the message before the newly published reading position
            //When the instance is restarted, its consumer will receive the first unacknowledged message on "reading-position" topic from Pulsar
            //which is the newly published reading position
            if(lastPublishedReadingPosition!=null){
                consumerReadingPosition.acknowledgeCumulativeAsync(lastPublishedReadingPosition,txn);
                lastPublishedReadingPosition= messageId;
            }else{
                lastPublishedReadingPosition= messageId;
            }
            //Commit the transaction
            txn.commit().get();
            counter++;
        } catch (IllegalArgumentException e){
                e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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
