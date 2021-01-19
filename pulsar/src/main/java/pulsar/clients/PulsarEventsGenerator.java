package pulsar.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.transaction.Transaction;

import pulsar.configuration.Configuration;
import util.eventsource.CSVSourceEvent;
import util.eventsource.EventsPublisher;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PulsarEventsGenerator implements EventsPublisher {
    private PulsarClient client;
    private PulsarAdmin adminClient;
    private Producer<String> producerEvent, producerRowNumber;
    private Consumer<String> consumer;
    private Reader<String> reader;
    private ObjectMapper objectMapper;

    public PulsarEventsGenerator() {
        client = PulsarClientsCreator.createClient();
        adminClient = PulsarClientsCreator.createAdmin();
        producerEvent = PulsarClientsCreator.createProducerEvent(client);
        producerRowNumber = PulsarClientsCreator.createProducerReadingPosition(client);
        objectMapper = new ObjectMapper();
        consumer = PulsarClientsCreator.createConsumer(client);

    }
    public int getLastPublishedEvent(){
        int lastPublishedEvent = 0;
        MessageId messageId = null;
        try {
            messageId = adminClient.topics().getLastMessageId(Configuration.PULSAR_SOURCE_READER_TOPIC);
            reader = PulsarClientsCreator.createReader(client,messageId);
            Message<String> message = reader.readNext(10,TimeUnit.SECONDS);
            if(message!=null){
                lastPublishedEvent = Integer.parseInt(message.getValue());
            }
            System.out.println("Last checkpoint row: " + lastPublishedEvent);
        } catch (PulsarAdminException e) {
            e.printStackTrace();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        return lastPublishedEvent;
    }


    @Override
    public void publishEvents(Map<String, String> event) {
        try {
            System.out.println(event);
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
            System.out.println(txn);

            //Send the event
            producerEvent.newMessage(txn)
                    .key(customerId)
                    .value(eventJson)
                    .sendAsync();


            System.out.println("Publish event: " + eventJson);
            //Send the row number in source CSV file
            producerRowNumber.newMessage(txn)
                    .value(event.get("id"))
                    .sendAsync();
            System.out.println("Publish reading position: " + event.get("id"));
            //Commit the transaction
            txn.commit().get();

            long start = 0;
            float elapsed = 0;
            int wait = 5;
            start = System.currentTimeMillis();
            while(true){
                elapsed= (System.currentTimeMillis()-start)/1000F;
                if(elapsed>wait){
                    break;
                }
            }

        } catch (IllegalArgumentException e){
                e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
//        catch (PulsarClientException e) {
//            e.printStackTrace();
//        }
    }

    public void generateEvents(){
        //get the id of the last published transaction to resume from that point onward
        int lastPublishedEvent = getLastPublishedEvent();
        CSVSourceEvent.generateEventFromCSV(Configuration.PATH_TO_CSV,this,lastPublishedEvent+1);
    }
}
