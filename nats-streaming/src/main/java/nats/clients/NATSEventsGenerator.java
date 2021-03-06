package nats.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.streaming.*;
import nats.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.eventsource.CSVSourceEvent;
import util.eventsource.EventsPublisher;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class NATSEventsGenerator implements EventsPublisher {
    private StreamingConnection natsClient;
    private ObjectMapper objectMapper;
    private int counter;
    private volatile int lastPublishedMessage;
    static final Logger LOGGER = LoggerFactory.getLogger(NATSEventsGenerator.class);


    public NATSEventsGenerator(){
        natsClient = NATSClientsCreator.createStreamingConnection();
        objectMapper = new ObjectMapper();
        counter = 0;
        lastPublishedMessage= 0;

        //Create a subscription on the "reading-position" channel to get the line number of the last published event in the source CSV file
        Subscription subscriptionForLastPublisedMessage= NATSClientsCreator.subscribeToChannel(natsClient, Configuration.SOURCE_CHANNEL_NAME,getLastPublishedMessage(),Configuration.START_POSITION);

        //Pause the current thread for 5 seconds to try to retrieve the position of last published reading position on the source file since the message handler of the subscription is executed in a separated thread
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Unsubscribe to the reading position topic after getting the last published reading position
        try {
            subscriptionForLastPublisedMessage.unsubscribe();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Message handler to get the last published reading position to use it to resume the event generator
    public MessageHandler getLastPublishedMessage(){
        return new MessageHandler() {
            @Override
            public void onMessage(Message message) {
                String positionInSourceFile= new String(message.getData());
                lastPublishedMessage = Integer.parseInt(positionInSourceFile);
            }
        };
    }



    @Override
    public void publishEvents(Map<String, String> event) {
        try{
            //Get the customer ID to use as the key for records
            String customerId = event.get("customer");
            if(customerId==null){
                throw new IllegalArgumentException("Customer ID is missing");
            }

            String eventJson = objectMapper.writeValueAsString(event);
            String currentReadingPosition = event.get("id");

            //Publish the event with retry logic
            PublishingUtils.publishWithRetry(natsClient,Configuration.SINK_CHANNEL_NAME,eventJson.getBytes(),5);
            LOGGER.info("Published event: " + eventJson);

            //Add the Byteman hook here to simulate the application crash
            bytemanHook(counter);

            //Publish the current reading position with retry logic to the "reading_position" channel
            PublishingUtils.publishWithRetry(natsClient,Configuration.SOURCE_CHANNEL_NAME,currentReadingPosition.getBytes(),5);
            LOGGER.info("Published reading position: " + currentReadingPosition);
            counter++;

            boolean isLastMessage = Integer.parseInt(event.get("id"))==1000;
            if(isLastMessage){
                LOGGER.info("All 1000 events have been published. Stop the event generator");
                System.exit(0);
            }


        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void generateEvents(){
        CSVSourceEvent.generateEventFromCSV(Configuration.PATH_TO_CSV,this,lastPublishedMessage+1);
    }

    public void bytemanHook(int counter){
        return;
    }

}
