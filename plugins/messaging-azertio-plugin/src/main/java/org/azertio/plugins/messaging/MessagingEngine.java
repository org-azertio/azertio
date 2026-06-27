package org.azertio.plugins.messaging;

public interface MessagingEngine {

    void subscribe(String destination);

    void publish(String destination, String body);

    void publish(String destination, String key, String body);

    /** Blocks until a message arrives on the destination or the timeout expires. Returns the message body. */
    String pollNext(String destination, int timeoutSeconds);

    void close();

}