package com.metatrope.jact.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EnvelopeTest {
    @Test
    public void testIsAsk() {
        Envelope<String> ask = MessageFactory.ask("sender", "receiver", "");
        assertTrue(ask.isAsk());
        Envelope<String> forward = MessageFactory.forward("newreceiver", ask);
        assertTrue(forward.isAsk());
        assertEquals("sender", forward.getSenderId());
    }

    @Test
    public void testType() {
        Envelope<String> tell1 = MessageFactory.tell("sender", "receiver", "string");
        assertEquals(String.class, tell1.getPayloadType());
        Envelope<Integer> tell2 = MessageFactory.tell("sender", "receiver", Integer.valueOf(42));
        assertEquals(Integer.class, tell2.getPayloadType());
    }
}
