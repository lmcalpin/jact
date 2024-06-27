package com.metatrope.jact.config;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.queue.BlockingMessageQueue;
import com.metatrope.jact.queue.MessageQueue;
import com.metatrope.jact.remote.ServerConfig;

import java.util.LinkedList;
import java.util.Queue;

public interface Config {
    default MessageQueue getLocalDispatcherQueue() {
        return new BlockingMessageQueue();
        
    }
    default <T> Queue<Envelope<T>> getMailboxQueue(String name) {
        return new LinkedList<Envelope<T>>();
    }
    public ServerConfig getRemotingConfiguration();
}
