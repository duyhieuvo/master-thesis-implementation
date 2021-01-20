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

public class PulsarStreamProcessor {
    private PulsarClient client;
    private Map<Integer, Producer<String>> producers;
    private Consumer<String> consumer;
    private ObjectMapper objectMapper;
    private int counter;

    public PulsarStreamProcessor(){
        client = PulsarClientsCreator.createClient();
        producers = new HashMap<>();
        objectMapper = new ObjectMapper();
        counter = 0;
        consumer = PulsarClientsCreator.createConsumer(client, Configuration.PULSAR_SOURCE_TOPIC, new ConsumerEventListener() {
            @Override
            public void becameActive(Consumer<?> consumer, int partition) {
                System.out.println("Partition assigned: " + partition);
                int count = 0;
                int maxTries = 10;
                while(true){
                    try {
                        System.out.println("Attempt: "+ count);
                        producers.put(partition, PulsarClientsCreator.createProducer(client,Configuration.PULSAR_SINK_TOPIC,"partition-"+partition));
                        System.out.println("Create producer: " + Configuration.PRODUCER_NAME + "partition-" + partition);
                        break;
                    } catch (PulsarClientException e) {
                        long start = System.currentTimeMillis();;
                        int wait = 5;
                        while(true){
                            if(((System.currentTimeMillis()-start)/1000F)>wait){
                                break;
                            }
                        }
                        if (++count == maxTries){
                            e.printStackTrace();
                            break;
                        }
                    }
                }

            }

            @Override
            public void becameInactive(Consumer<?> consumer, int partition) {
                System.out.println("Partition revoked: " + partition);
                if(producers.containsKey(partition)) {
                    try {
                        producers.get(partition).close();
                    } catch (PulsarClientException e) {
                        e.printStackTrace();
                    }
                    producers.remove(partition);
                }
            }
        });
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
                System.out.println("Received: " + message.getValue());
                String sourcePartitionTopic = message.getTopicName();
                int sourcePartition = Integer.parseInt(sourcePartitionTopic.substring(sourcePartitionTopic.length()-1));
                System.out.print("source partition " + sourcePartition + " " + sourcePartitionTopic);
                if(!producers.containsKey(sourcePartition)){
                    System.out.println("The record belongs to a revoked partition => skip");
                    continue;
                }
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


                //Publish the transformed event to output topic
                if(producers.containsKey(sourcePartition)){
                    producers.get(sourcePartition).newMessage()
                            .key(customerId)
                            .value(objectMapper.writeValueAsString(transformedRecord))
                            .send();
                    System.out.println("Publish transformed event: " + transformedRecord);
                }

                //Acknowledge the consumption of message on input topic
                if(producers.containsKey(sourcePartition)){
                    consumer.acknowledge(message);
                    System.out.println("Acknowledge input event");
                }
            } catch (PulsarClientException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
