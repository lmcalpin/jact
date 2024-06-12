package com.metatrope.jact;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.metatrope.jact.actors.VoidActor;
import com.metatrope.jact.exceptions.ActorAlreadyRegisteredException;
import com.metatrope.jact.exceptions.ActorNotFoundException;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ActorSystemTest {
    /**
     * Tests registration of an Actor.
     * 
     * @throws Exception
     */
    @Test
    public void testRegister() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            ActorRef<String, String> ref = system.register("echo", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    return "received:" + message;
                }
            });
            ActorRef<String, String> locatedRef = system.locateLocal("echo");
            assertEquals(ref.getName(), locatedRef.getName());
            assertEquals("echo", locatedRef.getName());
        }
    }

    /**
     * Only once instance of an Actor can be registered per ActorSystem.
     * 
     * @throws Exception
     */
    @Test
    public void testFailOnDuplicateActor() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            VoidActor actor = new VoidActor();
            system.register("echo1", actor);
            assertThrows(ActorAlreadyRegisteredException.class, () -> {
                system.register("echo2", actor);
            });
        }
    }

    /**
     * Only once instance of an Actor can be registered per ActorSystem.
     * 
     * @throws Exception
     */
    @Test
    public void testFailOnDuplicateActorName() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            system.register("echo1", new VoidActor());
            assertThrows(ActorAlreadyRegisteredException.class, () -> {
                system.register("echo1", new VoidActor());
            });
        }
    }

    @Test
    @Timeout(value = 2, unit = SECONDS)
    public void testAsk() throws Exception {
        try (ActorSystem system = new ActorSystem()) {
            system.register("echo", new Actor<String, String>() {
                @Override
                public String onMessage(String message) {
                    return "received: " + message;
                }
            });
            Future<String> replyF = system.<String, String> locateLocal("echo").ask("hello");
            String reply = (String) replyF.get();
            assertEquals("received: hello", reply);
        }
    }

    @Test
    public void testRegisterActorAlreadyRegistered() {
        ActorSystem actorSystem = new ActorSystem();
        var actor1 = new VoidActor();
        LocalActorRef<Void, Void> ref1 = actorSystem.register(actor1);
        assertThrows(ActorAlreadyRegisteredException.class, () -> {
            actorSystem.register(actor1);
        });
    }

    @Test
    public void testLocateActorNotFound() {
        ActorSystem actorSystem = new ActorSystem();
        assertThrows(ActorNotFoundException.class, () -> {
            actorSystem.locateLocal("nonExistentActor"); // This should throw ActorNotFoundException
        });
    }

    @Test
    public void testTryLocateActorNotFound() {
        ActorSystem actorSystem = new ActorSystem();
        LocalActorRef<String, String> ref = actorSystem.tryLocateLocal("nonExistentActor");
        assertNull(ref); // The method should return null instead of throwing an exception
    }

}
