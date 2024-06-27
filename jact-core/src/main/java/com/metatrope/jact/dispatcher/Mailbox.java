package com.metatrope.jact.dispatcher;

import com.metatrope.jact.message.Envelope;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Encapsulates a queue that holds messages being sent to a specific <code>Actor</code> instance registered
 * to an <code>ActorSystem</code>.
 */
public class Mailbox<T> {
    private Queue<Envelope<T>> queue = new LinkedList<>();

    Mailbox() {
    }

    public boolean deliver(Envelope<T> message) {
        return queue.offer(message);
    }

    public Envelope<T> take() {
        return queue.poll();
    }

    public boolean hasMail() {
        return !queue.isEmpty();
    }
}
