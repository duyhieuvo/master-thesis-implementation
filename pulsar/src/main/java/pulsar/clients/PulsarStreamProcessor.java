package pulsar.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pulsar.client.api.*;
import pulsar.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
                message = consumer.receive();
                counter++;
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



                //Publish the transformed event to output topic

                producer.newMessage()
                         .key(customerId)
                         .value(objectMapper.writeValueAsString(transformedRecord))
                         .send();
                System.out.println("Publish transformed event: " + transformedRecord);

                bytemanHook(counter);
                //Acknowledge the consumption of message on input topic
                CompletableFuture<Void> ack = consumer.acknowledgeAsync(message);
                ack.get();
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
}
