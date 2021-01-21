package pulsar;

import pulsar.clients.PulsarAggregator;
import pulsar.clients.PulsarEventsGenerator;
import pulsar.clients.PulsarStreamProcessor;


public class RunClient {
    public static void main(String[] args)  {
        if ("event-generator".equals(args[0])) {
            PulsarEventsGenerator pulsarEventsGenerator = new PulsarEventsGenerator();
            pulsarEventsGenerator.generateEvents();
        }
        else if ("stream-processor".equals(args[0])) {
            PulsarStreamProcessor pulsarStreamProcessor = new PulsarStreamProcessor();
            pulsarStreamProcessor.transformRawEvent();
        }
        else if ("stream-aggregator".equals(args[0])) {
            PulsarAggregator pulsarAggregator = new PulsarAggregator();
            pulsarAggregator.aggregateAndWriteDataToDB();
        }
    }
}
