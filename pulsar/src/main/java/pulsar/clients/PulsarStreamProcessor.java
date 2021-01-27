package pulsar.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.transaction.Transaction;
import pulsar.configuration.Configuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PulsarStreamProcessor {
    private PulsarClient client;
    private Producer<String> producer;
    private Consumer<String> consumer;
    private ObjectMapper objectMapper;
    private int counter;

    public PulsarStreamProcessor(){
        client = PulsarClientsCreator.createClient();
        producer = PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SINK_TOPIC);
        objectMapper = new ObjectMapper();
        counter = 0;
        consumer = PulsarClientsCreator.createConsumer(client, Configuration.PULSAR_SOURCE_TOPIC);
    }

    public void transformRawEvent(){
        float value;
        String type;
        String customerId;
        Message<String> message = null;
        JsonNode jsonNode = null;
        while(true){
            try {
                //Read new record from the buffer queue
                message = consumer.receive();

                //Briefly pause the stream processor instance after receiving the full queue of 1000 messages from Pulsar
                //to give the other instance time to start and trigger the rebalance of partitions among two instances.
                // In this case, some messages on the queue of this instance which belongs to the reassigned partition will be redelivered to the other instance.
                // As a result, there are two instances which process the same messages.
                // If Pulsar has no prevention mechanism, duplicated transformed events will be generated.
                bytemanHookPartitionRevoked(counter);

                counter++;
                //Transform the raw event
                jsonNode = objectMapper.readTree(message.getValue());
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

                //Create the transaction to publish transformed event and acknowledge the processed raw event
                Transaction txn = client
                        .newTransaction()
                        .withTransactionTimeout(5, TimeUnit.MINUTES)
                        .build()
                        .get();


                //Publish the transformed event to output topic
                producer.newMessage(txn)
                         .key(customerId)
                         .value(objectMapper.writeValueAsString(transformedRecord))
                         .send();
                System.out.println("Publish transformed event: " + transformedRecord);

                //Add the Byteman hook here to simulate the application crash during the transaction
                bytemanHook(counter);

                //Acknowledge the consumption of message on input topic
                consumer.acknowledgeAsync(message.getMessageId(),txn);
                txn.commit().get();
                System.out.println("Acknowledge input event");

            } catch (PulsarClientException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void bytemanHook(int counter){
        return;
    }
    public void bytemanHookPartitionRevoked(int counter) { return; }
}
