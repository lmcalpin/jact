package com.metatrope.jact.queue.sqs;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.Serdes;
import com.metatrope.jact.queue.MessageQueue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsMessageQueue implements MessageQueue {
    private static final Logger logger = LogManager.getLogger(SqsMessageQueue.class);

    private final SqsClient sqs;
    private final String queueName;

    private final static Serdes SERDES = new Serdes();

    public SqsMessageQueue(String user, String secret, String region, URI endpoint, String queueName) {
        this(new SqsConnectionProperties(user, secret, region, endpoint), queueName);
    }
    
    public SqsMessageQueue(SqsConnectionProperties connectionProperties, String queueName) {
        this.sqs = SqsClient.builder().endpointOverride(connectionProperties.getEndpoint()).credentialsProvider(() -> AwsBasicCredentials.create(connectionProperties.getUser(), connectionProperties.getSecret())).region(Region.of(connectionProperties.getRegion())).build();
        this.queueName = queueName;
        logger.info("Connected to {}", queueName);
    }
    
    public static SqsMessageQueue createFromProperties() {
        Properties props = loadProperties("/jact-sqs.properties");
        String user = (String) props.get("aws_access_key");
        String secret = (String) props.get("aws_secret");
        String region = (String) props.get("aws_region");
        String endpoint = (String) props.get("sqs_endpoint");
        String queueName = (String) props.get("queue_name");
        try {
            return new SqsMessageQueue(new SqsConnectionProperties(user, secret, region, endpoint), queueName);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties loadProperties(String resource) {
        var is = SqsMessageQueue.class.getResourceAsStream(resource);
        Properties props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return props;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public void offer(Envelope<?> envelope) {
        SendMessageRequest request = SendMessageRequest.builder().messageGroupId(envelope.getReceiverId()).messageBody(SERDES.serialize(envelope)).messageDeduplicationId(envelope.getMessageId()).queueUrl(queueName).build();
        sqs.sendMessage(request);
    }

    @Override
    public Envelope<?> take() {
        try {
            ReceiveMessageResponse res = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueName).waitTimeSeconds(60).build());
            List<Message> messages = res.messages();
            for (Message message : messages) {
                try {
                    Envelope<?> envelope = SERDES.deserialize(message.body());
                    return envelope;
                } catch (Exception e) {
                    logger.error("Unhandled exception deserializing a message {} from {}", message.body(), queueName);
                } finally {
                    sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueName).receiptHandle(message.receiptHandle()).build());
                }
            }
        } catch (AbortedException e) {
            // this is fine, probably...
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        
    }
}
