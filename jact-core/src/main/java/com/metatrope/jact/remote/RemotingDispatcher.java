package com.metatrope.jact.remote;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.LocalActorRef;
import com.metatrope.jact.SystemActor;
import com.metatrope.jact.dispatcher.LocalDispatcher;
import com.metatrope.jact.exceptions.ActorException;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;
import com.metatrope.jact.message.MessageType;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemotingDispatcher implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(RemotingDispatcher.class);

    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ActorSystem actorSystem;
    private final LocalDispatcher dispatcher;
    private ServerEndpoint[] endpoints;
    private final ExecutorService executorService;
    private final ConcurrentSkipListSet<String> connectedClients;

    public RemotingDispatcher(ActorSystem actorSystem, LocalDispatcher dispatcher, ServerConfig config) {
        this.actorSystem = actorSystem;
        this.dispatcher = dispatcher;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.connectedClients = new ConcurrentSkipListSet<>();
        if (config != null) {
            this.endpoints = config.getEndpoints();
            for (ServerEndpoint endpoint : this.endpoints) {
                registerRemotingEndpoint(endpoint);
            }
        }
        actorSystem.register("$join", createJoinActor());
    }

    private SystemActor<JoinMessage, String> createJoinActor() {
        return new SystemActor<>() {
            @Override
            public String onMessage(Envelope<JoinMessage> message) throws Exception {
                JoinMessage joiningActorSystem = message.getPayload();
                logger.debug("{} -- client connection request from {} received", actorSystem.getName(), joiningActorSystem);
                connectedClients.add(joiningActorSystem.getClientActorSystem());
                return actorSystem.getName();
            }
        };
    }

    /**
     * Process messages that this ActorSystem, while acting as a server, has
     * received from a remote ActorSystem.
     * 
     * @param <T>
     * @param <R>
     * @param endpoint
     */
    @SuppressWarnings("unchecked")
    public <T, R> void registerRemotingEndpoint(ServerEndpoint endpoint) {
        if (stopped.get())
            return;
        executorService.submit(() -> {
            try {
                endpoint.start(actorSystem);
                while (!stopped.get()) {
                    Envelope<T> envelope = (Envelope<T>) endpoint.take();
                    if (envelope != null) {
                        if (envelope.getMessageType() == MessageType.SYSTEM) {
                            logger.debug("{} == CLIENT {} CONNECTED", actorSystem.getName(), envelope.getPayload().toString());
                            endpoint.send(MessageFactory.reply(envelope, this.actorSystem.getName()));
                        } else {
                            logger.debug("{} == RECEIVED FROM CONNECTED CLIENT == `{}`", actorSystem.getName(), envelope);
                            String receiverId = asLocalActorId(envelope.getReceiverId());
                            Envelope<T> relayedEnvelope = MessageFactory.relay(findOrCreateRemotingProxyActor(endpoint, envelope.getSenderId()), receiverId, envelope);
                            Future<R> futureReply = dispatcher.dispatch(relayedEnvelope);
                            if (futureReply != null) {
                                executorService.submit(() -> {
                                    try {
                                        R replyPayload = futureReply.get();
                                        endpoint.send(MessageFactory.reply(envelope, replyPayload));
                                    } catch (Exception e) {
                                        logger.error("Error relaying a reply back to the calling ActorSystem's Reply Queue", e);
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{} == Error in remoting endpoint {}", actorSystem.getName(), endpoint, e);
            } finally {
                endpoint.close();
            }
        });
    }

    <T> String findOrCreateRemotingProxyActor(ServerEndpoint endpoint, String clientActorSystemActorName) {
        String name = String.format("$endpoint_%d_relay_%s", endpoint.hashCode(), clientActorSystemActorName);
        LocalActorRef<Envelope<T>, Void> ref = this.actorSystem.tryLocateLocal(name);
        if (ref == null) {
            ref = this.actorSystem.register(name, new SystemActor<T, Void>() {
                @Override
                public Void onMessage(Envelope<T> env) throws Exception {
                    logger.debug("{} == SENDING TO CONNECTED CLIENT {} == `{}`", actorSystem.getName(), clientActorSystemActorName, env);
                    Envelope<T> forwardedEnvelope = MessageFactory.forward(clientActorSystemActorName, env);
                    endpoint.send(forwardedEnvelope);
                    return null;
                }
            });
        }
        return ref.getName();
    }

    /**
     * Process messages that this ActorSystem, while acting as a client, has
     * received from a remote ActorSystem, such as replies to messages that were
     * sent to the server, or messages the server sent back to the sender()
     * reference in the ActorContext.
     * 
     * @param <T>
     * @param <R>
     * @param endpoint
     */
    public void registerTransportClient(ClientMessageDispatcher transportClient) {
        if (stopped.get())
            return;
        executorService.submit(() -> {
            while (!stopped.get()) {
                Envelope<?> envelope = transportClient.take();
                if (envelope != null) {
                    String receiverId = asLocalActorId(envelope.getReceiverId());
                    logger.debug("{} == FORWARD TO LOCAL ACTOR {} == `{}`", actorSystem.getName(), envelope.getReceiverId(), envelope);
                    envelope = MessageFactory.forward(receiverId, envelope);
                    dispatcher.dispatch(envelope);
                }
            }
            transportClient.close();
        });
    }

    private String asLocalActorId(String receiverId) {
        String intendedActorSystemReceiver = null;
        if (receiverId.contains("/")) {
            String[] receiverIdParts = receiverId.split("/");
            intendedActorSystemReceiver = receiverIdParts[0];
            receiverId = receiverIdParts[1];
        }
        if (intendedActorSystemReceiver != null && !intendedActorSystemReceiver.equals(actorSystem.getName())) {
            logger.error("{} == Received a message for another actorsystem {}", actorSystem.getName(), intendedActorSystemReceiver);
            throw new ActorException(String.format("Received a message for another actorsystem %s", intendedActorSystemReceiver));
        }
        return receiverId;
    }
    
    @Override
    public void close() {
        stopped.set(true);
        executorService.shutdownNow();
    }
}
