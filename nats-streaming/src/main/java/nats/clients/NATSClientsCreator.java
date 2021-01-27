package nats.clients;

import io.nats.streaming.*;
import nats.configuration.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NATSClientsCreator {

    //Creator of NATS streaming connection.
    //Once a streaming connection created, it can be used to publish messages or multiple subscriptions can be created on top of it
    public static StreamingConnection createStreamingConnection() {
        StreamingConnection con = null;
        Options options = new Options.Builder()
                .clientId(Configuration.CLIENT_NAME)
                .clusterId(Configuration.CLUSTER_ID)
                .natsUrl(Configuration.NATS_URL)
                .build();
        StreamingConnectionFactory cf = new StreamingConnectionFactory(options);
        try {
            con = cf.createConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return con;
    }

    //Create subscription on the streaming connection. The subscription can be durable or non-durable based on the the environment variable DURABLE_ENABLE
    //A message handler must be given to asynchronously receive and process whenever a message is pushed to the client
    //Start position of the subscrition must also be specified: 0: from the first message on the channel, -1: from the last message on the channel (include also the last message), other long positive value: from the message with the matched sequence id on the channel
    //Users can also specify start position based on time with NATS Streaming. Nevertheless, for demo implementation, this option is neglected for simplicity
    public static Subscription subscribeToChannel(StreamingConnection streamingConnection, String channelName, MessageHandler messageHandler, Long start_position){
        Subscription subscription = null;
        try{
            if(Configuration.DURABLE_ENABLE){
                subscription = streamingConnection.subscribe(channelName,messageHandler,createDurableSubscriptionOptions(start_position));
            }else{
                subscription = streamingConnection.subscribe(channelName,messageHandler,createSubscriptionOptions(start_position));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return subscription;
    }



    public static SubscriptionOptions createSubscriptionOptions(Long start_position){
        SubscriptionOptions options = null;
        if(start_position.equals(-1L)){
            options = new SubscriptionOptions.Builder().startWithLastReceived().ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else if(start_position.equals(0L)){
            options = new SubscriptionOptions.Builder().deliverAllAvailable().ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else if(start_position > 0){
            options =  new SubscriptionOptions.Builder().startAtSequence(start_position).ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else {
            throw new IllegalArgumentException("Start position is invalid. Valid options: 0: from the first message on the channel, -1: from the end of the channel, other positive value: from the message with the matched sequence id on the channel");
        }
        return options;
    }

    public static SubscriptionOptions createDurableSubscriptionOptions(Long start_position){
        SubscriptionOptions options = null;
        if(start_position.equals(-1L)){
            options = new SubscriptionOptions.Builder().durableName(Configuration.DURABLE_NAME).startWithLastReceived().ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else if(start_position.equals(0L)){
            options = new SubscriptionOptions.Builder().durableName(Configuration.DURABLE_NAME).deliverAllAvailable().ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else if(start_position > 0){
            options =  new SubscriptionOptions.Builder().durableName(Configuration.DURABLE_NAME).startAtSequence(start_position).ackWait(Duration.ofSeconds(60)).manualAcks().maxInFlight(Configuration.MAX_IN_FLIGHT).build();
        }else {
            throw new IllegalArgumentException("Start position is invalid. Valid options: 0: from the first message on the channel, -1: from the end of the channel, other positive value: from the message with the matched sequence id on the channel");
        }
        return options;
    }
}
