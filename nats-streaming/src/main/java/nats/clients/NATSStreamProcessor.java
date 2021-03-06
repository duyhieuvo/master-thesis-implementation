package nats.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;
import nats.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class NATSStreamProcessor {
    private StreamingConnection natsClient;
    private ObjectMapper objectMapper;
    private int counter;
    private long lastProcessedMessage;
    static final Logger LOGGER = LoggerFactory.getLogger(NATSStreamProcessor.class);


    public NATSStreamProcessor(){
        natsClient = NATSClientsCreator.createStreamingConnection();
        objectMapper = new ObjectMapper();
        counter = 0;
        lastProcessedMessage = -1L;

        //Create a subscription on the "raw-event" channel with the message handler to transform and publish new transformed events
        Subscription streamProcessorSubscription = NATSClientsCreator.subscribeToChannel(natsClient,Configuration.SOURCE_CHANNEL_NAME,transformRawEvent(),Configuration.START_POSITION);
    }


    public MessageHandler transformRawEvent(){
        return new MessageHandler() {
            @Override
            public void onMessage(Message message) {
                //Client cannot be ensured whether the server receive the last acknowledgement since there is no direct TCP connection between client and server
                //In case acknowledgment is lost, server will resend the last acknowledged message
                //In this case, client must maintain the sequence ID of last processed message and discard it and acknowledge again with the server to receive new message
                if(message.getSequence()<=lastProcessedMessage){
                    try {
                        message.ack();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                float value;
                String type;
                String customerId;
                JsonNode jsonNode = null;
                try{

                    //Transform raw event
                    jsonNode = objectMapper.readTree(new String(message.getData()));
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
                    String transformedRecordString= objectMapper.writeValueAsString(transformedRecord);

                    //Publish the transformed event with retry logic
                    PublishingUtils.publishWithRetry(natsClient, Configuration.SINK_CHANNEL_NAME,transformedRecordString.getBytes(),5);
                    LOGGER.info("Published the transformed event: " + transformedRecordString);

                    //Add the Byteman hook here to simulate the application crash
                    bytemanHook(counter);

                    //Acknowledge successful consumption of the message with the server
                    message.ack();
                    LOGGER.info("Acknowledge the consumption of message with server.");

                    //Store the sequence number of the last processed message to drop it if the server resend it
                    lastProcessedMessage = message.getSequence();
                    counter++;

                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }
    public void bytemanHook(int counter){
        return;
    }
}
