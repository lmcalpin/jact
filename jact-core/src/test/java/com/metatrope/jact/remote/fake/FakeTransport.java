package com.metatrope.jact.remote.fake;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.queue.BlockingMessageQueue;
import com.metatrope.jact.queue.RequestResponseClientTransport;
import com.metatrope.jact.remote.Transport;
import com.metatrope.jact.remote.TransportFactory;

public class FakeTransport extends RequestResponseClientTransport implements Transport {
    private String clientID;
    
    public static class FakeTransportFactory implements TransportFactory<FakeTransport> {
        FakeEndpoint endpoint;
        
        public FakeTransportFactory(FakeEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public FakeTransport create(ActorSystem actorSystem) {
            return new FakeTransport(actorSystem, endpoint);
        }
    }
    
    public FakeTransport(ActorSystem actorSystem, FakeEndpoint endpoint) {
        super(endpoint.serverQueue, new BlockingMessageQueue());
        this.clientID = actorSystem.getName();
        endpoint.registerReplyQueue(clientID, getReplyQueue());
        
    }
}
