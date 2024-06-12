package com.metatrope.jact.queue.sqs;

public class SqsMessageQueueFactory {
    private SqsConnectionProperties sqsConnectionParameters;
    private String queueNameTemplate;

    public SqsMessageQueueFactory(SqsConnectionProperties sqsConnectionParameters, String queueNameTemplate) {
        this.sqsConnectionParameters = sqsConnectionParameters;
        this.queueNameTemplate = queueNameTemplate;
    }
    
    public SqsMessageQueue createSqsMessageQueue(String actorSystemName) {
        String queueName = String.format(queueNameTemplate, actorSystemName); 
        return new SqsMessageQueue(sqsConnectionParameters, queueName);
    }
}
