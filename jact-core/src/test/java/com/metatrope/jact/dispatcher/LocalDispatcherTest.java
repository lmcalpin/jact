package com.metatrope.jact.dispatcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.message.MessageFactory;

import org.junit.jupiter.api.Test;

public class LocalDispatcherTest {
    @Test
    public void testReply() {
        LocalDispatcher dispatcher = new LocalDispatcher(new ActorSystem());
        Mailbox<String, ?> mailbox = dispatcher.register("recipient");
        assertFalse(mailbox.hasMail());
        dispatcher.dispatch(MessageFactory.tell("sender", "recipient", "hello"));
        dispatcher.distributeEnvelopes();
        assertTrue(mailbox.hasMail());
        dispatcher.close();
    }
}
