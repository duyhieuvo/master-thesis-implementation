package util.eventsource;

import java.util.Map;

public interface EventsPublisher {
    void publishEvents(Map<String,String> event);
}
