package kafka;

import kafka.clients.KafkaAggregator;
import kafka.clients.KafkaEventsGenerator;
import kafka.clients.KafkaStreamProcessor;

public class RunClient {
    public static void main(String[] args){
        if ("event-generator".equals(args[0])) {
            KafkaEventsGenerator kafkaEventsGenerator = new KafkaEventsGenerator();
            kafkaEventsGenerator.generateEvents();
        }
        else if ("stream-processor".equals(args[0])){
            KafkaStreamProcessor kafkaStreamProcessor = new KafkaStreamProcessor();
            kafkaStreamProcessor.transformRawEvent();
        }
        else if("stream-aggregator".equals(args[0])){
            KafkaAggregator kafkaAggregator = new KafkaAggregator();
            kafkaAggregator.aggregateAndWriteDataToDB();
        }
    }
}
