package com.metatrope.jact.queue.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.ActorRef;
import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.RemoteActorSystem;
import com.metatrope.jact.config.Config;
import com.metatrope.jact.config.DefaultConfig;
import com.metatrope.jact.remote.ServerConfig;
import com.metatrope.jact.remote.ServerEndpoint;
import com.metatrope.jact.remote.sqs.SqsEndpoint;
import com.metatrope.jact.remote.sqs.SqsTransport.SqsTransportFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SimpleSqsActorTest {
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"));

    private static final String QUEUE_NAME_TEMPLATE = "test-queue-%s.fifo";

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", String.format(QUEUE_NAME_TEMPLATE, "local"), "--attributes", "FifoQueue=true");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", String.format(QUEUE_NAME_TEMPLATE, "remote"), "--attributes", "FifoQueue=true");
    }
    
    @Test
    public void testRemoteTellSender() throws Exception {
        SqsConnectionProperties connectionProperties = new SqsConnectionProperties(localStack.getAccessKey(), localStack.getSecretKey(), localStack.getRegion(), localStack.getEndpointOverride(Service.SQS));

        Map<String, Integer> counts = new HashMap<>();
        ActorSystem local = new ActorSystem("local");
        ActorSystem remote = new ActorSystem("remote", getServerConfig(connectionProperties));
        remote.<String,Integer>register("count", (String str) -> {
            if (counts.get(str) == null) {
                counts.put(str, 0);
            }
            Integer count = counts.get(str);
            count++;
            counts.put(str, count);
            return count;
        });
        RemoteActorSystem remoteActorSystem = local.connect(new SqsTransportFactory(remote, connectionProperties, QUEUE_NAME_TEMPLATE));
        ActorRef<String,Integer> countRef = local.locateRemote(remoteActorSystem, "count");
        countRef.tell("hello");
        Future<Integer> reply = countRef.ask("hello");
        assertEquals(2, reply.get().intValue());
        reply = countRef.ask("bye");
        assertEquals(1, reply.get().intValue());
        remote.close();
        local.close();
    }

    private Config getServerConfig(SqsConnectionProperties connProps) {
        Config config = new DefaultConfig() {

            @Override
            public ServerConfig getRemotingConfiguration() {
                return new ServerConfig() {
                    @Override
                    public ServerEndpoint[] getEndpoints() {
                        return new ServerEndpoint[] { new SqsEndpoint(new SqsMessageQueueFactory(connProps, QUEUE_NAME_TEMPLATE)) };
                    }
                };
            }

        };
        return config;
    }
}
