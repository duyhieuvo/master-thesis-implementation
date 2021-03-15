package kafka.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.configuration.Configuration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.relationalDB.CurrentBalanceDAO;
import util.relationalDB.entity.CurrentBalance;
import util.relationalDB.entity.CurrentReadingPosition;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class KafkaAggregator {
    private KafkaConsumer<String,String> consumer;
    private ObjectMapper objectMapper;
    private CurrentBalanceDAO currentBalanceDAO;
    private Map<String,Float> customersBalances;
    private int counter; //The counter variable for Byteman failure injection
    static final Logger LOGGER = LoggerFactory.getLogger(KafkaAggregator.class);


    public KafkaAggregator(){
        consumer = KafkaClientsCreator.createConsumer();
        objectMapper = new ObjectMapper();
        currentBalanceDAO = new CurrentBalanceDAO();
        counter = 0;


        //Get the list of assigned partitions from environment variable
        List<Integer> assignedPartitions = Arrays.asList(Configuration.ASSIGNED_PARTITION.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());

        //Get current balances of customers on the assigned partitions from the database
        customersBalances = currentBalanceDAO.getCurrentBalance(assignedPartitions);

        //Assign the Kafka consumer to the assigned partitions of the topic
        Collection<TopicPartition> topicPartitions = new ArrayList<>();
        for(Integer assignedPartition : assignedPartitions){
            topicPartitions.add(new TopicPartition(Configuration.KAFKA_SOURCE_TOPIC,assignedPartition));
        }
        consumer.assign(topicPartitions);

        //Get current reading position on each partition from the database and set the consumer to that reading position
        Map<Integer,Long> currentReadingPositions = currentBalanceDAO.getCurrentReadingPosition(assignedPartitions);
        for(Map.Entry<Integer,Long> currentReadingPosition : currentReadingPositions.entrySet()){
            consumer.seek(new TopicPartition(Configuration.KAFKA_SOURCE_TOPIC,currentReadingPosition.getKey()),currentReadingPosition.getValue());
        }
    }



    public void aggregateAndWriteDataToDB(){
        float newTransactionValue;
        float newBalanceValue;
        String customerId;
        while(true){
            Map<String,CurrentBalance> committedBalanceList = new HashMap<>();
            Map<Integer,CurrentReadingPosition> currentReadingPositionList;

            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(10));
            if(consumerRecords.isEmpty()){
                continue;
            }
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(consumerRecord.value());
                    customerId = jsonNode.get("customer").asText();
                    newTransactionValue = Float.valueOf(jsonNode.get("value").asText());
                    newBalanceValue = newTransactionValue + customersBalances.getOrDefault(customerId,0.0f);

                    //Update the local copy of the current balance
                    customersBalances.put(customerId,newBalanceValue);

                    //Update the current balance to send to the database
                    committedBalanceList.put(customerId,new CurrentBalance(customerId,newBalanceValue,consumerRecord.partition()));
                    counter++;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            //Update the current reading position on the Kafka source topic to send to the database
            currentReadingPositionList = getOffsetToCommitOnSourceTopic(consumerRecords);

            //Send the current snapshot of the balance and current reading position of the processed batch of records to database
            currentBalanceDAO.updateListCustomerBalance(committedBalanceList,currentReadingPositionList,counter);

            LOGGER.info("Published: " + committedBalanceList);
        }

    }

    //Helper method to retrieve offset numbers of the last messages from each partition in the newly pull batch of records.
    public Map<Integer,CurrentReadingPosition> getOffsetToCommitOnSourceTopic(ConsumerRecords records){
        Map<Integer,CurrentReadingPosition> offsetsToCommit = new HashMap<>();
        Set<TopicPartition> topicPartitionSet = records.partitions();
        for (TopicPartition partition : topicPartitionSet) {
            List<ConsumerRecord> partitionedRecords = records.records(partition);
            long offset = partitionedRecords.get(partitionedRecords.size() - 1).offset();
            offsetsToCommit.put(partition.partition(), new CurrentReadingPosition(partition.partition(),offset+1));
        }
        return offsetsToCommit;
    }
}
