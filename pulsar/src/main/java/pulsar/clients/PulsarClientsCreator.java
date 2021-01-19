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
                    .serviceUrl(Configuration.PULSAR_URL)
                    .enableTransaction(true)
                    .build();
        } catch(PulsarClientException e) {
            e.printStackTrace();
        }
        return client;
    }

    public static Consumer<String> createConsumer(PulsarClient client){
        Consumer<String> consumer = null;
        try{

            consumer =  client.newConsumer(Schema.STRING)
                    .topic(Configuration.PULSAR_SOURCE_TOPIC)
                    .consumerName(Configuration.CONSUMER_NAME)
                    .subscriptionName(Configuration.SUBSCRIPTION_NAME)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.valueOf(Configuration.SUBSCRIPTION_INITIAL_POSITION))
                    .subscriptionType(SubscriptionType.valueOf(Configuration.SUBSCRIPTION_TYPE))
                    .subscribe();

        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return consumer;
    }

    public static Producer<String> createProducerEvent(PulsarClient client){
        Producer<String> producer = null;
        try {
            producer =  client.newProducer(Schema.STRING)
                    .topic(Configuration.PULSAR_SINK_TOPIC)
                    .producerName(Configuration.PRODUCER_NAME)
                    .sendTimeout(0, TimeUnit.SECONDS)
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        return producer;
    }

    public static Producer<String> createProducerReadingPosition(PulsarClient client){
        Producer<String> producer = null;
        try {
            producer =  client.newProducer(Schema.STRING)
                    .topic(Configuration.PULSAR_SOURCE_READER_TOPIC)
                    .producerName(Configuration.PRODUCER_NAME)
                    .sendTimeout(0, TimeUnit.SECONDS)
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        return producer;
    }

    public static Reader<String> createReader(PulsarClient client, MessageId messageId){
        Reader<String> reader = null;
        try {
            reader = client.newReader(Schema.STRING)
                    .topic(Configuration.PULSAR_SOURCE_READER_TOPIC)
                    .readerName(Configuration.READER_NAME)
                    .startMessageId(messageId)
                    .startMessageIdInclusive()
                    .create();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return reader;
    }

    public static PulsarAdmin createAdmin(){
        PulsarAdmin admin = null;
        try {
            admin = PulsarAdmin.builder()
                               .serviceHttpUrl(Configuration.PULSAR_ADMIN_URL)
                               .build();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return admin;
    }
}
