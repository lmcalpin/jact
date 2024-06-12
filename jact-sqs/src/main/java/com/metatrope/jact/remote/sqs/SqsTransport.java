package com.metatrope.jact.remote.sqs;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.queue.RequestResponseClientTransport;
import com.metatrope.jact.queue.sqs.SqsConnectionProperties;
import com.metatrope.jact.queue.sqs.SqsMessageQueue;
import com.metatrope.jact.remote.Transport;
import com.metatrope.jact.remote.TransportFactory;

public class SqsTransport extends RequestResponseClientTransport implements Transport {
    public static class SqsTransportFactory implements TransportFactory<SqsTransport> {
        ActorSystem remoteActorSystem;
        SqsConnectionProperties sqsConnectionParameters;
        String queueNameTemplate;
        
        public SqsTransportFactory(ActorSystem remoteActorSystem, SqsConnectionProperties connectionParameters, String queueNameTemplate) {
            this.remoteActorSystem = remoteActorSystem;
            this.sqsConnectionParameters = connectionParameters;
            this.queueNameTemplate = queueNameTemplate;
        }

        @Override
        public SqsTransport create(ActorSystem localActorSystem) {
            return new SqsTransport(localActorSystem, remoteActorSystem, sqsConnectionParameters, queueNameTemplate);
        }
    }
    
    public SqsTransport(ActorSystem localActorSystem, ActorSystem remoteActorSystem, SqsConnectionProperties connectionParameters, String queueNameTemplate) {
        super(createSqsMessageQueue(remoteActorSystem, connectionParameters, queueNameTemplate), createSqsMessageQueue(localActorSystem, connectionParameters, queueNameTemplate));
    }
    
    private static SqsMessageQueue createSqsMessageQueue(ActorSystem actorSystem, SqsConnectionProperties sqsConnectionParameters, String queueNameTemplate) {
        String queueName = String.format(queueNameTemplate, actorSystem.getName()); 
        return new SqsMessageQueue(sqsConnectionParameters, queueName);
    }
}
