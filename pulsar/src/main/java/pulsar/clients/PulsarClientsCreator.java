package pulsar.clients;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.*;
import pulsar.configuration.Configuration;

import java.util.concurrent.TimeUnit;

public class PulsarClientsCreator {

    public static PulsarClient createClient(){
        PulsarClient client = null;
        try {
            client = PulsarClient.builder()
                    .serviceUrl(Configuration.PULSAR_SERVICE_URL)
                    .enableTransaction(true) //create enable transaction causes disconnection from Pulsar: possible bug
                    .build();
        } catch(PulsarClientException e) {
            e.printStackTrace();
        }
        return client;
    }


    public static Consumer<String> createConsumer(PulsarClient client, String topic){
        Consumer<String> consumer = null;
        try{

            consumer =  client.newConsumer(Schema.STRING)
                    .topic(topic)
                    .consumerName(Configuration.CONSUMER_NAME)
                    .subscriptionName(Configuration.SUBSCRIPTION_NAME)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.valueOf(Configuration.SUBSCRIPTION_INITIAL_POSITION))
                    .subscriptionType(SubscriptionType.valueOf(Configuration.SUBSCRIPTION_TYPE))
                    .acknowledgmentGroupTime(0,TimeUnit.SECONDS)
                    .subscribe();

        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return consumer;
    }


    public static Producer<String> createProducer(PulsarClient client, String topic){
        Producer<String> producer = null;
        try {
            producer =  client.newProducer(Schema.STRING)
                    .topic(topic)
                    .producerName(Configuration.PRODUCER_NAME)
                    .sendTimeout(0, TimeUnit.SECONDS)
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        return producer;
    }
//
//    public static Producer<String> createProducer(PulsarClient client, String topic, String partition) throws PulsarClientException {
//        Producer<String>  producer =  client.newProducer(Schema.STRING)
//                .topic(topic)
//                .producerName(Configuration.PRODUCER_NAME + partition)
//                .sendTimeout(0, TimeUnit.SECONDS)
//                .create();
//        return producer;
//    }



    public static Reader<String> createReader(PulsarClient client, String topic, MessageId messageId){
        Reader<String> reader = null;
        try {
            reader = client.newReader(Schema.STRING)
                    .topic(topic)
                    .readerName(Configuration.READER_NAME)
                    .startMessageId(messageId)
                    .startMessageIdInclusive()
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return reader;
    }

    public static Reader<String> createReader(PulsarClient client, String topic, String readerName, MessageId messageId){
        Reader<String> reader = null;
        try {
            reader = client.newReader(Schema.STRING)
                    .topic(topic)
                    .readerName(readerName)
                    .startMessageId(messageId)
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return reader;
    }

    public static PulsarAdmin createAdminClient(){
        PulsarAdmin admin = null;
        try {
            admin = PulsarAdmin.builder()
                               .serviceHttpUrl(Configuration.PULSAR_WEB_SERVICE_URL)
                               .build();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return admin;
    }
}
