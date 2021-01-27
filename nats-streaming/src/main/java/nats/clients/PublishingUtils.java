package nats.clients;

import io.nats.streaming.StreamingConnection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class PublishingUtils {
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
                    System.out.println("Retries are exhausted");
                    throw e;
                }
            }
        }
    }
}
