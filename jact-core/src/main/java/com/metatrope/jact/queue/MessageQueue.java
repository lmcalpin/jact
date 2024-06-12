package com.metatrope.jact.queue;

import com.metatrope.jact.message.Envelope;

public interface MessageQueue extends QueueSender, QueueReceiver, AutoCloseable {

    @Override
    void offer(Envelope<?> envelope);

    @Override
    Envelope<?> take();

}
