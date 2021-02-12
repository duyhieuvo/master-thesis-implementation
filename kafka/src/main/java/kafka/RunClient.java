package kafka;

import kafka.clients.KafkaAggregator;
import kafka.clients.KafkaEventsGenerator;
import kafka.clients.KafkaStreamProcessor;
import kafka.clients.KafkaStreamProcessorNew;

public class RunClient {
    public static void main(String[] args){
        if ("event-generator".equals(args[0])) {
            KafkaEventsGenerator kafkaEventsGenerator = new KafkaEventsGenerator();
            kafkaEventsGenerator.generateEvents();
        }
        else if ("stream-processor".equals(args[0])){
            KafkaStreamProcessorNew kafkaStreamProcessor = new KafkaStreamProcessorNew();
            kafkaStreamProcessor.transformRawEvent();
        }
        else if("stream-aggregator".equals(args[0])){
            KafkaAggregator kafkaAggregator = new KafkaAggregator();
            kafkaAggregator.aggregateAndWriteDataToDB();
        }
    }
}
