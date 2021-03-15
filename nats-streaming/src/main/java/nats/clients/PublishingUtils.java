package nats.clients;

import io.nats.streaming.StreamingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class PublishingUtils {
    static final Logger LOGGER = LoggerFactory.getLogger(PublishingUtils.class);
    //Util function to publish message and retry in case of failure up to a specified max retries
    public static void publishWithRetry(StreamingConnection con, String channel, byte[] data, int maxRetries ) throws TimeoutException {
        int count = 0;
        while(true) {
            try{
                con.publish(channel,data);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                if (++count == maxRetries) {
                    LOGGER.error("Retries are exhausted");
                    throw e;
                }
            }
        }
    }
}
