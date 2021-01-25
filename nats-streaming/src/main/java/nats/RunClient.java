package nats;

import nats.clients.NATSAggregator;
import nats.clients.NATSEventsGenerator;
import nats.clients.NATSStreamProcessor;

public class RunClient {
    public static void main(String[] args){
        if ("event-generator".equals(args[0])) {
            NATSEventsGenerator natsEventsGenerator = new NATSEventsGenerator();
            natsEventsGenerator.generateEvents();
        }
        else if ("stream-processor".equals(args[0])){
            NATSStreamProcessor natsStreamProcessor = new NATSStreamProcessor();
            natsStreamProcessor.transformRawEvent();
        }
        else if("stream-aggregator".equals(args[0])){
            NATSAggregator natsAggregator = new NATSAggregator();
//            natsAggregator.writeDataToDB();
        }
    }
}
