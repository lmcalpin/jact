package com.metatrope.jact.actors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.exceptions.ActorNotFoundException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class SimpleActorTest {
    class Ping {

    }

    @Test
    @Timeout(value = 1, unit = SECONDS)
    public void testSender() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            system.register("ping", new SimpleActor() {
                // returns either a Boolean (if message == 'receiver1') or a String (if message refers to a nonexistent ActorRef)
                @Override
                public Object onMessage(Object message) throws InterruptedException, ExecutionException, TimeoutException {
                    try {
                        Future<Object> rec = system.locateLocal((String) message).ask(new Ping());
                        return rec.get();
                    } catch (ActorNotFoundException e) {
                        return "not found: " + e.getMessage();
                    }
                }
            });
            system.register("receiver1", new SimpleActor() {
                @Override
                public Object onMessage(Object message) {
                    System.out.println(message);
                    return message instanceof Ping;
                }
            });
            Future<Object> rec = system.locateLocal("ping").ask("receiver1");
            assertEquals(true, (Boolean) rec.get());
            rec = system.locateLocal("ping").ask("receiver2");
            assertEquals("not found: receiver2", (String) rec.get());
        }
    }
}
