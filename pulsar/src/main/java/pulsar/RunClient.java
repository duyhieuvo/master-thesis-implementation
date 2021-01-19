package pulsar;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.transaction.Transaction;
import pulsar.clients.PulsarClientsCreator;
import pulsar.clients.PulsarEventsGenerator;
import pulsar.configuration.Configuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RunClient {
    public static void main(String[] args) throws PulsarClientException, ExecutionException, InterruptedException {
//        try {
//            PulsarAdmin admin = PulsarAdmin.builder()
//                                           .serviceHttpUrl("http://localhost:18080")
//                                           .build();
//            MessageId messageId = admin.topics().getLastMessageId("persistent://public/default/reading-position");
//            System.out.println(messageId);
//            admin.close();
//            PulsarClient client = PulsarClientsCreator.createClient();
//            Reader reader = PulsarClientsCreator.createReader(client,MessageId.earliest);
//            while(true) {
//                Message<String> msg = reader.readNext();
//                System.out.printf("Received a message: Key: %s Payload: %s\n", msg.getKey(), msg.getValue());
//            }
//        } catch (PulsarClientException e) {
//            e.printStackTrace();
//        } catch (PulsarAdminException e) {
//            e.printStackTrace();
//        }
//        if ("event-generator".equals(args[0])) {
//            PulsarEventsGenerator pulsarEventsGenerator = new PulsarEventsGenerator();
//            pulsarEventsGenerator.generateEvents();
//        }
        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl(Configuration.PULSAR_URL)
                .statsInterval(0, TimeUnit.SECONDS)
                .enableTransaction(true)
                .build();
        System.out.println("Created Pulsar client");
        String sourceTopic = "persistent://public/default/source-topic";
        String sinkTopic = "persistent://public/default/sink-topic";

        Producer<String> sourceProducer = pulsarClient
                .newProducer(Schema.STRING)
                .topic(sourceTopic)
                .create();
        sourceProducer.newMessage().value("hello pulsar transaction").sendAsync();
        Consumer<String> sourceConsumer = pulsarClient
                .newConsumer(Schema.STRING)
                .topic(sourceTopic)
                .subscriptionName("test")
                .subscriptionType(SubscriptionType.Shared)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscribe();

        Producer<String> sinkProducer = pulsarClient
                .newProducer(Schema.STRING)
                .topic(sinkTopic)
                .sendTimeout(0, TimeUnit.SECONDS)
                .create();

        Transaction txn = pulsarClient
                .newTransaction()
                .withTransactionTimeout(5, TimeUnit.MINUTES)
                .build()
                .get();

    // source message acknowledgement and sink message produce belong to one transaction,
    // they are combined into an atomic operation.
        Message<String> message = sourceConsumer.receive();
        sourceConsumer.acknowledgeAsync(message.getMessageId(),txn);
        sinkProducer.newMessage(txn).value("sink data").sendAsync();

        txn.commit().get();

        txn = pulsarClient
                .newTransaction()
                .withTransactionTimeout(5, TimeUnit.MINUTES)
                .build()
                .get();

        sinkProducer.newMessage(txn).value("sink data 1").sendAsync();
        txn.commit().get();
        System.out.println("Finish");

    }
}
