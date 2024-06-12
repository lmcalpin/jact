package com.metatrope.jact.remote;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.Actor;
import com.metatrope.jact.ActorContext;
import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.RemoteActorSystem;
import com.metatrope.jact.actors.ActorFactory;
import com.metatrope.jact.config.Config;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public abstract class RemoteActorSystemTestBase {
    protected abstract Config getServerConfig(String serverId);
    protected abstract TransportFactory<?> getTransportFactory(String serverId);

    @Test
    @Timeout(value = 5, unit = SECONDS)
    public void testRemoteAskAndReply() throws Exception {
        try (ActorSystem system2 = new ActorSystem("remote", getServerConfig("remote"))) {
            try (ActorSystem system1 = new ActorSystem("local")) {
                // system1 connects to the shared queue that system2 uses to receive remote
                // requests
                RemoteActorSystem remote = system1.connect(getTransportFactory("remote"));
                var ref = system1.register("doubler", new Actor<Integer, Integer>() {
                    @Override
                    public Integer onMessage(Integer message) throws InterruptedException, ExecutionException, TimeoutException {
                        var sum = system1.<List<Integer>, Integer> locateRemote(remote, "adder").ask(Lists.newArrayList(message, message));
                        return sum.get();
                    }
                });
                system2.register("adder", new Actor<List<Integer>, Integer>() {
                    @Override
                    public Integer onMessage(List<Integer> message) {
                        return message.stream().reduce(0, Integer::sum);
                    }
                });

                Future<Integer> replyF = ref.ask(42);
                Integer reply = replyF.get();
                assertEquals(84, reply);
            }
        }
    }

    // system1 sends "test" to alpha's echo actor and expects "test" as a response
    // alpha sends anything to beta's foo_me actor and expects "foo" as a response
    @Test
    @Timeout(value = 5, unit = SECONDS)
    public void testMultipleRemote() throws Exception {
        ActorSystem alpha = new ActorSystem("alpha", getServerConfig("alpha"));
        alpha.register("echo", (String s) -> s);
        ActorSystem beta = new ActorSystem("beta", getServerConfig("beta"));
        beta.register("foome", (String s) -> "foo");
        try (ActorSystem system1 = new ActorSystem("local")) {
            RemoteActorSystem localToAlpha = system1.connect(getTransportFactory("alpha"));
            RemoteActorSystem alphaToBeta = alpha.connect(getTransportFactory("beta"));
            Future<String> promisedReplyFromAlpha = system1.<String, String> locateRemote(localToAlpha, "echo").ask("test");
            String reply = promisedReplyFromAlpha.get();
            assertEquals("test", reply);
            Future<String> promisedReplyFromBeta = alpha.<String, String> locateRemote(alphaToBeta, "foome").ask("test");
            String reply2 = promisedReplyFromBeta.get();
            assertEquals("foo", reply2);
        } finally {
            alpha.close();
            beta.close();
        }
    }

    @Test
    @Timeout(value = 5, unit = SECONDS)
    public void testRemoteTellSender() throws Exception {
        try (ActorSystem system2 = new ActorSystem("remote", getServerConfig("remote"))) {
            try (ActorSystem system1 = new ActorSystem("local")) {
                var cdl = new CountDownLatch(1);
                // system1 connects to the shared queue that system2 uses to be able to
                // send and receive responses from system2's actor
                RemoteActorSystem remote = system1.connect(getTransportFactory("remote"));

                var ref = system1.register("testActor", ActorFactory.create(i -> {
                    if (i instanceof Integer) {
                        System.out.println("#" + i);
                        system1.locateRemote(remote, "adder").tell(Lists.newArrayList(i, i));
                    } else if (i instanceof String) {
                        System.out.println("$" + i);
                        assertEquals("84", (String) i);
                        cdl.countDown();
                    }
                    return null;
                }));
                // send a String message to the sender to shut the test down successfully
                system2.register("adder", ActorFactory.<List<Integer>> create(is -> {
                    Integer sum = is.stream().reduce(0, Integer::sum);
                    ActorContext.sender().tell(String.valueOf(sum));
                }));

                // send an Integer message to the testActor to start the test
                ref.tell(42);
                cdl.await();
            }
        }
    }
}
