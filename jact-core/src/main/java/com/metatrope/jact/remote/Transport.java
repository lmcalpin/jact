package com.metatrope.jact.remote;

import com.metatrope.jact.queue.MessageQueue;

import java.util.Map;

/**
 * A Transport is a pair of queues, one for sending messages to a server, and another
 * for receiving replies.  When registering a Transport with a LocalActorSystem, the
 * actor system will first send a system message to the RemoteActorSystem with 
 * any information (if any) to access the reply queue so it can respond or send messages 
 * to the client.
 */
public interface Transport extends MessageQueue {
    Map<String, String> getConnectionProperties();
}
