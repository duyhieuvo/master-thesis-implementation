package pulsar.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.*;

import pulsar.configuration.Configuration;
import util.relationalDB.CurrentBalanceDAO;
import util.relationalDB.entity.CurrentBalance;
import util.relationalDB.entity.CurrentReadingPosition;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PulsarAggregator {
    private PulsarClient client;
    private Map<Integer, Reader<String>> readers;
    private ObjectMapper objectMapper;
    private CurrentBalanceDAO currentBalanceDAO;
    private Map<String,Float> customersBalances;
    private int counter;

    public PulsarAggregator() throws IOException {
        client = PulsarClientsCreator.createClient();
        readers = new HashMap<>();
        objectMapper = new ObjectMapper();
        counter = 0;
        currentBalanceDAO = new CurrentBalanceDAO();

        //Get the list of assigned partitions
        List<Integer> assignedPartitions = Arrays.asList(Configuration.ASSIGNED_PARTITION.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());

        //Get current balances of customers on the assigned partitions from the database
        customersBalances = currentBalanceDAO.getCurrentBalance(assignedPartitions);

        //Get current reading position on each partition
        Map<Integer,Long> currentReadingPositions = currentBalanceDAO.getCurrentReadingPosition(assignedPartitions);

        //Create reader for each assigned partition and set the reader to the current reading position on the source topic
        for(Integer assignedPartition : assignedPartitions){
            MessageId startPosition;
            if(!currentReadingPositions.containsKey(assignedPartition)){
                startPosition = MessageId.earliest;
            }else{
                startPosition = MessageIdUtil.longToMessageId(currentReadingPositions.get(assignedPartition));
            }
            readers.put(assignedPartition,PulsarClientsCreator.createReader(client,Configuration.PULSAR_SOURCE_TOPIC+"-partition-"+assignedPartition,Configuration.READER_NAME+assignedPartition,startPosition));
        }

    }

    public void aggregateAndWriteDataToDB(){
        float newTransactionValue;
        float newBalanceValue;
        String customerId;
        while(true){
            Map<String, CurrentBalance> committedBalanceList = new HashMap<>();
            Map<Integer, CurrentReadingPosition> currentReadingPositionList = new HashMap<>();
            for(Map.Entry<Integer,Reader<String>>  reader : readers.entrySet()){
                for(int i = 0; i<10;i++){ //Read messages in batch of ten from each partition before commit the snapshot to relational database
                    try {
                        Message<String> msg = reader.getValue().readNext(1, TimeUnit.SECONDS);
                        if (msg==null){
                            break;
                        }
                        System.out.println(msg.getValue());

                        String sourcePartitionTopic = msg.getTopicName();
                        int sourcePartition = Integer.parseInt(sourcePartitionTopic.substring(sourcePartitionTopic.length()-1));
                        JsonNode jsonNode = objectMapper.readTree(msg.getValue());
                        customerId = jsonNode.get("customer").asText();
                        newTransactionValue = Float.valueOf(jsonNode.get("value").asText());
                        newBalanceValue = newTransactionValue + customersBalances.getOrDefault(customerId,0.0f);

                        //Update the local copy of the current balance
                        customersBalances.put(customerId,newBalanceValue);

                        //Update the current balance to send to the database
                        committedBalanceList.put(customerId,new CurrentBalance(customerId,newBalanceValue,sourcePartition));
                        counter++;

                        //Update the reading position on source topic to send to the database
                        currentReadingPositionList.put(sourcePartition,new CurrentReadingPosition(sourcePartition,MessageIdUtil.messageIdToLong(msg.getMessageId())));
                    } catch (PulsarClientException e) {
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        e.printStackTrace();
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

            }
            if(committedBalanceList.isEmpty()&&currentReadingPositionList.isEmpty()){
                continue;
            }

            //Send the current snapshot of the balance and current reading position of the processed batch of records to database
            currentBalanceDAO.updateListCustomerBalance(committedBalanceList,currentReadingPositionList,counter);
            System.out.println("Published: " + committedBalanceList);
        }
    }
}
