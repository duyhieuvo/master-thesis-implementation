package pulsar.clients;

import org.apache.pulsar.client.api.MessageId;

import java.io.IOException;

//Helper class to convert between Pulsar MessageId and Long value
public class MessageIdUtil {
    public static MessageId longToMessageId(long l) throws IOException {
        byte[] result = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= Byte.SIZE;
        }
        return MessageId.fromByteArray(result);
    }

    public static long messageIdToLong(MessageId messageId) {
        long result = 0;
        byte[] b = messageId.toByteArray();
        for (int i = 0; i < Long.BYTES; i++) {
            result <<= Byte.SIZE;
            result |= (b[i] & 0xFF);
        }
        return result;
    }
}
