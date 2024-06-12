package com.metatrope.jact.remote;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.queue.QueueReceiver;

/**
 * Implemented by a server ActorSystem that is open to connections from clients using a matching Transport.
 */
public interface ServerEndpoint extends QueueReceiver, AutoCloseable {
    /**
     * Start listening for connections from clients.
     * 
     * @param actorSystem
     *            the actor system this ServerEndpoint is attached to.
     */
    public void start(ActorSystem actorSystem);

    /**
     * Retrieve a message that has been sent to this server.
     */
    @Override
    Envelope<?> take();

    /**
     * Send a message (usually a reply) to the client.
     * 
     * @param <T>
     * @param envelope
     */
    public <T> void send(Envelope<T> envelope);

    @Override
    public void close();
}
