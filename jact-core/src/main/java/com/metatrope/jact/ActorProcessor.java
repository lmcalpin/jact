package com.metatrope.jact;

import com.metatrope.jact.exceptions.ActorException;
import com.metatrope.jact.exceptions.ActorKilledException;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;
import com.metatrope.jact.message.MessageType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ActorProcessor<T, R> {
    private static final Logger logger = LogManager.getLogger(ActorProcessor.class);

    private final ActorSystem actorSystem;
    private final ExecutorService executorService;
    private final Actor<T, R> actor;
    private Future<?> runningProcess;

    public ActorProcessor(ActorSystem actorSystem, Actor<T, R> actor) {
        this.actorSystem = actorSystem;
        this.actor = actor;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Actor<T, R> getActor() {
        return actor;
    }

    /**
     * Called by the <code>ActorScheduler</code> to request that it process the
     * first message in its <code>Mailbox</code> queue. The <code>Actor</code>
     * dequeues the message at the front of the queue and processes it.
     * 
     * @param envelope
     * @return
     */
    public void processMessage() {
        if (isBusy()) {
            logger.debug("Actor {} is busy", actor.getName());
        } else {
            Envelope<T> envelope = actor.getMailbox().take();
            if (envelope != null) {
                logger.trace("{} == PROCESSING == {}", actorSystem.getName(), envelope);
                runningProcess = executorService.submit(() -> processMessage(envelope));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processMessage(Envelope<T> envelope) {
        try {
            R reply = ActorContext.withContext(actorSystem, envelope, () -> {
                try {
                    if (envelope.getMessageType() == MessageType.POISONPILL) {
                        actor.stop();
                        throw new ActorKilledException();
                    }
                    if (actor.isSystemActor()) {
                        // system actors work with the Envelopes, so we wrap the Envelope in an
                        // Envelope, cuz I hear they like Envelopes
                        return (R) actor.processMessage((Envelope<T>) MessageFactory.tell("", "", envelope));
                    } else {
                        return actor.processMessage(envelope);
                    }
                } catch (Exception e) {
                    logger.error("Uncaught exception processing a message", e);
                    throw new ActorException(e);
                }
            });
            if (envelope.isAsk()) {
                actorSystem.getLocalDispatcher().dispatch(MessageFactory.reply(envelope, reply));
            }
        } catch (ActorKilledException e) {
            logger.debug("Actor {} was shut down", envelope.getReceiverId());
        } catch (Exception e) {
            logger.error("Uncaught exception processing a message", e);
        }
    }

    boolean isBusy() {
        return runningProcess != null && !runningProcess.isDone();
    }
}
