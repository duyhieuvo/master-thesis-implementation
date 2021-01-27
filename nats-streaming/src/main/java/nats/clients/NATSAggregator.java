package nats.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;
import nats.configuration.Configuration;
import util.relationalDB.CurrentBalanceDAO;
import util.relationalDB.entity.CurrentBalance;
import util.relationalDB.entity.CurrentReadingPosition;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NATSAggregator {
    private StreamingConnection natsClient;
    private ObjectMapper objectMapper;
    private CurrentBalanceDAO currentBalanceDAO;
    private Map<String,Float> customersBalances;

    //The new balance values and current reading position to commit to the database
    Map<String, CurrentBalance> committedBalanceList;
    Map<Integer, CurrentReadingPosition> committedReadingPositionList;

    private int counter;
    private long lastProcessedMessage;

    private final Object lock = new Object();

    public NATSAggregator(){
        natsClient = NATSClientsCreator.createStreamingConnection();
        objectMapper = new ObjectMapper();
        counter = 0;
        lastProcessedMessage = -1L;
        currentBalanceDAO = new CurrentBalanceDAO();

//        committedBalanceList = new HashMap<>();
//        committedReadingPositionList = new HashMap<>();

        //Since NATS channel cannot be partitioned, to reuse the same DAO as Kafka and Pulsar,
        //the partition number of the entity is assigned value -1.
        // Therefore, the subscriber is assigned partition -1 to get current balances of all customers with events on the channel
        List<Integer> assignedPartitions = Arrays.asList(-1);

        //Get current balance from the database
        customersBalances = currentBalanceDAO.getCurrentBalance(assignedPartitions);

        //Get current reading position on channel and set the subscriber to that reading position
        Map<Integer,Long> currentReadingPositions = currentBalanceDAO.getCurrentReadingPosition(assignedPartitions);
        Long currentReadingPosition = Configuration.START_POSITION;
        if(currentReadingPositions.containsKey(-1)){
            currentReadingPosition = currentReadingPositions.get(-1);
        }

        //Create a subscription on the "transformed-event" channel to read transformed event and aggregate the current balance
        Subscription aggregatorSubscription = NATSClientsCreator.subscribeToChannel(natsClient, Configuration.SOURCE_CHANNEL_NAME,aggregateAndPublishBalance(),currentReadingPosition);
    }

    //Message handler of subscription on "transformed-event" to aggregate and publish snapshot of the current balance to the database
    public MessageHandler aggregateAndPublishBalance(){
        return new MessageHandler() {
            @Override
            public void onMessage(Message message) {
                //Client cannot be ensured whether the server receive the last acknowledgement since there is no direct connection between client and server
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
                System.out.println("Received event: " + new String(message.getData()));

                Map<String, CurrentBalance> committedBalanceList = new HashMap<>();
                Map<Integer, CurrentReadingPosition> committedReadingPositionList = new HashMap<>();

                float newTransactionValue;
                float newBalanceValue;
                String customerId;
                try {
                    JsonNode jsonNode = objectMapper.readTree(new String(message.getData()));
                    customerId = jsonNode.get("customer").asText();
                    newTransactionValue = Float.valueOf(jsonNode.get("value").asText());
                    newBalanceValue = newTransactionValue + customersBalances.getOrDefault(customerId, 0.0f);

                    //Update the local copy of the current balance
                    customersBalances.put(customerId,newBalanceValue);
//                    synchronized (lock) {

                    //Update the current balance to send to the database
                    committedBalanceList.put(customerId, new CurrentBalance(customerId, newBalanceValue, -1));
                    committedReadingPositionList.put(-1, new CurrentReadingPosition(-1, message.getSequence() + 1));
                    counter++;
//                    }

                    //Send the current snapshot of the balance and current reading position of the processed batch of records to database
                    currentBalanceDAO.updateListCustomerBalance(committedBalanceList,committedReadingPositionList,counter);

                    //Store the sequence number of the last processed message to drop it if the server resend it
                    lastProcessedMessage = message.getSequence();

                    //Acknowledge successful consumption of the message with the server to receive the next message
                    message.ack();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }
    //Commit result periodically in another thread
    //At the moment, commit result every time a message is processed seems to have low impact on the throughput
    //Therefore, this method is not used.
//    public void writeDataToDB(){
//        while(true){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            synchronized (lock) {
//                if(!committedReadingPositionList.isEmpty() && !committedReadingPositionList.isEmpty()){
//                    currentBalanceDAO.updateListCustomerBalance(committedBalanceList,committedReadingPositionList,counter);
//                    committedBalanceList = new HashMap<>();
//                    committedReadingPositionList = new HashMap<>();
//                }
//            }
//        }
//    }
}
