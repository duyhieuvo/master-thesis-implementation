package util.eventsource;

import java.util.Map;

//Interface to publish event
//Event generator of each ESP platform must implement this interface to use the CSVSourceEvent to read the CSV and publish events
public interface EventsPublisher {
    void publishEvents(Map<String,String> event);
}
