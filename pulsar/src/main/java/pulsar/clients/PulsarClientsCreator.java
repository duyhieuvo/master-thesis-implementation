package pulsar.clients;

import org.apache.pulsar.client.api.*;
import pulsar.configuration.Configuration;

import java.util.concurrent.TimeUnit;

public class PulsarClientsCreator {

    //Pulsar client creator
    public static PulsarClient createClient(){
        PulsarClient client = null;
        try {
            client = PulsarClient.builder()
                    .serviceUrl(Configuration.PULSAR_SERVICE_URL)
                    .enableTransaction(true)
                    .build();
        } catch(PulsarClientException e) {
            e.printStackTrace();
        }
        return client;
    }

    //Pulsar consumer creator, the consumer name is taken from environment variable
    //Each Pulsar consumer can only consume from topics specified when initializing
    //Therefore, topic is specified as a parameter here
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
                    .receiverQueueSize(Configuration.CONSUMER_QUEUE_SIZE)
                    .subscribe();

        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return consumer;
    }

    //Pulsar producer creator, the producer name is taken from environment variable
    //Each Pulsar producer can only produce to a topic specified when initializing
    //Therefore, topic is specified as a parameter here
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

    //Pulsar reader creator. MessageId of the starting position to be consume in the topic must be specify
    //Messages after the specified messageId will be read by this reader
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
}
