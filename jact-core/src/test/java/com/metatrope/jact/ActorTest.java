package com.metatrope.jact;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.metatrope.jact.actors.VoidActor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Basic tests for Actor features.
 */
public class ActorTest {
    @Test
    public void testInvalidName() throws Exception {
        Actor<?, ?> actor = new VoidActor();
        assertThrows(IllegalArgumentException.class, () -> {
            actor.start(null, "$specialcharacters_are!!!forbidden");
        });
    }

    @Test
    public void testTell() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            CountDownLatch cdl = new CountDownLatch(1);
            String[] lastReceivedMessage = new String[] { "nothing yet" };
            system.register("hello", new Actor<String, Void>() {
                @Override
                public Void onMessage(String message) {
                    lastReceivedMessage[0] = message.toString();
                    cdl.countDown();
                    return null;
                }
            });
            system.locateLocal("hello").tell("world");
            cdl.await();
            assertEquals("world", lastReceivedMessage[0]);
        }
    }

    @Test
    public void testTellLambda() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            CountDownLatch cdl = new CountDownLatch(1);
            String[] lastReceivedMessage = new String[] { "nothing yet" };
            system.register("hello", s -> {
                lastReceivedMessage[0] = s.toString();
                cdl.countDown();
                return null;
            });
            system.locateLocal("hello").tell("world");
            cdl.await();
            assertEquals("world", lastReceivedMessage[0]);
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testBehave() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            List<String> received = new ArrayList<>();
            CountDownLatch cdl = new CountDownLatch(4);
            system.register("hello", new Actor<String, Void>() {
                @Override
                public Void onMessage(String message) {
                    received.add(message);
                    cdl.countDown();
                    become(new ActorBehavior<String, Void>() {
                        @Override
                        public Void onMessage(String message) {
                            received.add("XYZ");
                            cdl.countDown();
                            unbecome();
                            return null;
                        }
                    });
                    return null;
                }
            });
            system.locateLocal("hello").tell("1");
            system.locateLocal("hello").tell("2");
            system.locateLocal("hello").tell("3");
            system.locateLocal("hello").tell("4");
            cdl.await();
            assertEquals("1", received.get(0));
            assertEquals("XYZ", received.get(1));
            assertEquals("3", received.get(2));
            assertEquals("XYZ", received.get(3));
        }
    }

    @Test
    public void testSelf() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            CountDownLatch cdl = new CountDownLatch(1);
            system.register("hello", new Actor<String, Void>() {
                @Override
                public Void onMessage(String message) {
                    if (message.equals("Ping")) {
                        self().tell("Pong");
                    } else if (message.equals("Pong")) {
                        cdl.countDown();
                    } else {
                        fail("unexpected message received: " + message);
                    }
                    return null;
                }
            });
            system.locateLocal("hello").tell("Ping");
            cdl.await();
        }
    }

    /**
     * Test that we can find an ActorRef for ourselves when using a lambda
     * 
     * @throws Exception
     */
    @Test
    public void testSelfUsingThreadLocalContext() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            CountDownLatch cdl = new CountDownLatch(1);
            system.register("abcdefg", message -> {
                if (message.equals("start")) {
                    ActorContext.self().tell(ActorContext.self().getName());
                } else if (message.equals("abcdefg")) {
                    // we expect to receive a message with our name
                    cdl.countDown();
                } else {
                    fail("unexpected message received: " + message);
                }
                return null;
            });
            system.locateLocal("abcdefg").tell("start");
            cdl.await();
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testSender() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            CountDownLatch cdl = new CountDownLatch(1);
            system.register("sender", new Actor<String, Void>() {
                @Override
                public Void onMessage(String message) {
                    if (message.equals("hi")) {
                        System.out.println("sending hi!");
                        system.locateLocal("receiver").tell(message);
                    } else if (message.equals("reply")) {
                        cdl.countDown();
                    } else {
                        fail("unexpected message received: " + message);
                    }
                    return null;
                }
            });
            system.register("receiver", new Actor<String, Void>() {
                @SuppressWarnings("unchecked")
                @Override
                public Void onMessage(String message) {
                    if (message.equals("hi")) {
                        System.out.println("sending reply!");
                        ((ActorRef<String, Void>) sender()).tell("reply");
                    } else {
                        fail("unexpected message received: " + message);
                    }
                    return null;
                }
            });
            System.out.println("sending hi!");
            system.locateLocal("sender").tell("hi");
            cdl.await();
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testAsk() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            ActorRef<String, String> ref = system.register("echo", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    return "received: " + message;
                }
            });
            Future<String> replyF = ref.ask("hello");
            String reply = replyF.get();
            assertEquals("received: hello", reply);
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testForwardAskNoSender() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            ActorRef<String, String> receiver = system.register("echo", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    return "sender was: " + ActorContext.sender();
                }
            });
            ActorRef<String, String> ref = system.register("fwd", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    Future<String> reply = receiver.forward();
                    try {
                        return "forwarded and received: " + reply.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            Future<String> replyF = ref.ask("hi");
            String reply = replyF.get();
            assertEquals("forwarded and received: sender was: null", reply);
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testForwardAskWithSender() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            ActorRef<String, String> receiver = system.register("echo", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    return "sender was: " + ActorContext.sender().getName();
                }
            });
            ActorRef<String, String> ref = system.register("fwd", new Actor<String, String>() {
                @Override
                public String onMessage(String message) throws Exception {
                    Future<String> reply = receiver.forward();
                    return "forwarded and received: " + reply.get();
                }
            });
            ActorRef<String, String> sender = system.register("sender", new Actor<String, String>() {
                @Override
                public String onMessage(String message) throws Exception {
                    Future<String> replyF = ref.ask("hi");
                    return replyF.get();
                }
            });
            Future<String> replyF = sender.ask("hi");
            String reply = replyF.get();
            assertEquals("forwarded and received: sender was: sender", reply);
        }
    }

    @Test
    public void testStop() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            system.register("hello", new Actor<String, Void>() {
                @Override
                public Void onMessage(String message) {
                    return null;
                }
            });
            ActorRef<String, Void> ref = system.locateLocal("hello");
            ref.tell("world");
            system.stop(ref);
            ref.tell("world");
        }
    }

}
