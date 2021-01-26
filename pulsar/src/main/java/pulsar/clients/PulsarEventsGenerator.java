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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PulsarEventsGenerator implements EventsPublisher {
    private PulsarClient client;
    private PulsarAdmin adminClient;
    private Producer<String> producerEvent,  producerReadingPosition;
    private Reader<String> reader;
    private ObjectMapper objectMapper;
    private int counter;

    public PulsarEventsGenerator() {
        client = PulsarClientsCreator.createClient();
        adminClient = PulsarClientsCreator.createAdminClient();
        producerEvent = PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SINK_TOPIC);
        producerReadingPosition = PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SOURCE_TOPIC);
        objectMapper = new ObjectMapper();
        counter = 0;
    }
    public int getLastPublishedEvent(){
        int lastPublishedEvent = 0;
        MessageId messageId = null;
        try {
            messageId = adminClient.topics().getLastMessageId(Configuration.PULSAR_SOURCE_TOPIC);
            reader = PulsarClientsCreator.createReader(client,Configuration.PULSAR_SOURCE_TOPIC,messageId);
            Message<String> message = reader.readNext(10,TimeUnit.SECONDS);
            if(message!=null){
                lastPublishedEvent = Integer.parseInt(message.getValue());
            }
            reader.close();
            System.out.println("Last checkpoint row: " + lastPublishedEvent);
        } catch (PulsarAdminException e) {
            e.printStackTrace();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
            //The transaction feature is not stable yet. Enabling transaction on Pulsar client causes disconnection from Pulsar broker
            Transaction txn = client
                    .newTransaction()
                    .withTransactionTimeout(5, TimeUnit.MINUTES)
                    .build()
                    .get();
            System.out.println(txn);

            //Send the event
            producerEvent.newMessage(txn)
//            producerEvent.newMessage()
                    .key(customerId)
                    .value(eventJson)
                    .send();
            System.out.println("Publish event: " + eventJson);

            bytemanHook(counter);

            //Send the row number in source CSV file
            producerReadingPosition.newMessage(txn)
//            producerReadingPosition.newMessage()
                    .value(event.get("id"))
                    .send();
            System.out.println("Publish reading position: " + event.get("id"));
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
