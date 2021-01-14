package kafka.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.configuration.Configuration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
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

    public KafkaAggregator(){
        consumer = KafkaConsumerCreator.createConsumer();
        objectMapper = new ObjectMapper();
        currentBalanceDAO = new CurrentBalanceDAO();

        //Get the list of assigned partitions
        List<Integer> assignedPartitions = Arrays.asList(Configuration.ASSIGNED_PARTITION.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());

        //Get current balance from the database
        customersBalances = currentBalanceDAO.getCurrentBalance(assignedPartitions);

        //Assign the consumer to the defined partitions of the topic
        Collection<TopicPartition> topicPartitions = new ArrayList<>();
        for(Integer assignedPartition : assignedPartitions){
            topicPartitions.add(new TopicPartition(Configuration.KAFKA_SOURCE_TOPIC,assignedPartition));
        }
        consumer.assign(topicPartitions);

        //Get current reading position on each partition and set the consumer to that reading position
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
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(consumerRecord.value());
                    customerId = jsonNode.get("customer").asText();
//                    System.out.println(jsonNode);
                    newTransactionValue = Float.valueOf(jsonNode.get("value").asText());
                    newBalanceValue = newTransactionValue + customersBalances.getOrDefault(customerId,0.0f);
                    //Update the local copy of the current balance
                    customersBalances.put(customerId,newBalanceValue);
                    //Update the current balance to send to the database
                    committedBalanceList.put(customerId,new CurrentBalance(customerId,newBalanceValue,consumerRecord.partition()));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            currentReadingPositionList = getOffsetToCommitOnSourceTopic(consumerRecords);
            for(Map.Entry<Integer,CurrentReadingPosition> read: currentReadingPositionList.entrySet()){
                System.out.println(read.getKey() + read.getValue().getCurrentReadingPosition());
            }
            currentBalanceDAO.updateListCustomerBalance(committedBalanceList,currentReadingPositionList);

        }

    }

    public Map<Integer,CurrentReadingPosition> getOffsetToCommitOnSourceTopic(ConsumerRecords records){
        Map<Integer,CurrentReadingPosition> offsetsToCommit = new HashMap<>();
        Set<TopicPartition> topicPartitionSet = records.partitions();
        for (TopicPartition partition : topicPartitionSet) {
            List<ConsumerRecord> partitionedRecords = records.records(partition);
            for(ConsumerRecord record:partitionedRecords){
                System.out.println("Offset " + record.offset() + record.value());
            }
            long offset = partitionedRecords.get(partitionedRecords.size() - 1).offset();
            offsetsToCommit.put(partition.partition(), new CurrentReadingPosition(partition.partition(),offset+1));
        }
        return offsetsToCommit;
    }
}
