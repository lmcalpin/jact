package com.metatrope.jact.queue;

import com.metatrope.jact.message.Envelope;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Wraps a BlockingQueue to conform to various jact interfaces.
 */
public class BlockingMessageQueue implements MessageQueue, QueueReceiver {
    private BlockingQueue<Envelope<?>> queue = new LinkedBlockingQueue<>();

    @Override
    public void offer(Envelope<?> envelope) {
        queue.offer(envelope);
    }

    @Override
    public Envelope<?> take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void close() throws Exception {

    }
}
