package com.metatrope.jact.queue.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SqsMessageQueueTest {
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"));
    
    private static final String QUEUE_NAME = "test-queue.fifo";

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME, "--attributes", "FifoQueue=true");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRoundTrip() {
        SqsMessageQueue sqsQ = new SqsMessageQueue(localStack.getAccessKey(), localStack.getSecretKey(), localStack.getRegion(), 
                localStack.getEndpointOverride(Service.SQS), QUEUE_NAME);
        sqsQ.offer(MessageFactory.tell("sender", "receiver", "string"));
        Envelope<String> env = (Envelope<String>) sqsQ.take();
        assertEquals("sender", env.getSenderId());
        assertEquals("receiver", env.getReceiverId());
        assertEquals("string", env.getPayload());
    }
}

