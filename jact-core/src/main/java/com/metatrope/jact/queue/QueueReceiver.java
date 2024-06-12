package com.metatrope.jact.queue;

import com.metatrope.jact.message.Envelope;

public interface QueueReceiver {
    Envelope<?> take();
}
