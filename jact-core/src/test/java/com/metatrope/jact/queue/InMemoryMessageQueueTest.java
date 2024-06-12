package com.metatrope.jact.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;

import org.junit.jupiter.api.Test;

public class InMemoryMessageQueueTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testRoundTrip() {
        BlockingMessageQueue q = new BlockingMessageQueue();
        q.offer(MessageFactory.tell("sender", "receiver", "string"));
        Envelope<String> env = (Envelope<String>) q.take();
        assertEquals("sender", env.getSenderId());
        assertEquals("receiver", env.getReceiverId());
        assertEquals("string", env.getPayload());
    }
}
