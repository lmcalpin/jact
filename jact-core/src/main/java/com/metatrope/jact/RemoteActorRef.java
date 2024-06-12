package com.metatrope.jact;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;
import com.metatrope.jact.remote.ClientMessageDispatcher;

import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteActorRef<T, R> implements ActorRef<T, R> {
    private static final Logger logger = LogManager.getLogger(RemoteActorRef.class);

    private final ActorSystem localActorSystem;
    private final RemoteActorSystem remoteActorSystem;
    private final ClientMessageDispatcher transport;
    private final String name;

    public RemoteActorRef(ActorSystem localActorSystem, RemoteActorSystem remoteActorSystem, String name) {
        this.localActorSystem = localActorSystem;
        this.remoteActorSystem = remoteActorSystem;
        this.transport = remoteActorSystem.getTransportClient();
        this.name = name;
    }

    private String getRemoteActorSystemName() {
        return remoteActorSystem.getName();
    }

    @Override
    public String getName() {
        return getRemoteActorSystemName() + "/" + name;
    }

    /**
     * Send a message to the remote actor using a fire and forget pattern.
     */
    @Override
    public void tell(T payload) {
        Envelope<T> message = MessageFactory.tell(getSenderActorPath(), this.getName(), payload);
        logger.debug("{} == SENDING TO REMOTE {} == `{}`", localActorSystem.getName(), remoteActorSystem.getName(), message);
        transport.dispatch(message);
    }

    /**
     * Send a message to the remote actor and returns a Future which will eventually encapsulate the remote actor's reply.
     */
    @Override
    public Future<R> ask(T payload) {
        Envelope<T> message = MessageFactory.ask(getSenderActorPath(), this.getName(), payload);
        logger.debug("{} == SENDING TO REMOTE {} == `{}`", localActorSystem.getName(), remoteActorSystem.getName(), message);
        return transport.dispatch(message);
    }

    /**
     * @return the fully qualified path of the current actor, which is the concatenation of the local actor system name and the actor name.
     */
    private String getSenderActorPath() {
        ActorRef<?, ?> ref = ActorContext.self();
        String name = "$";
        if (ref != null) {
            name = ref.getName();
        }
        return localActorSystem.getName() + "/" + name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Future<R> forward() {
        Envelope<T> originalEnvelope = ActorContext.envelope();
        Envelope<T> forwarded = MessageFactory.forward(this.getName(), originalEnvelope);
        logger.debug("{} == SENDING TO REMOTE {} == `{}`", localActorSystem.getName(), remoteActorSystem.getName(), forwarded);
        return (Future<R>) transport.dispatch(forwarded);
    }

}
