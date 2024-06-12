package com.metatrope.jact.remote;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.dispatcher.ReplyDispatcher;

public class ClientMessageDispatcher extends ReplyDispatcher<Transport> {
    private final ActorSystem actorSystem;

    public ClientMessageDispatcher(ActorSystem actorSystem, Transport transport) {
        super(transport);
        this.actorSystem = actorSystem;
    }

    @Override
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public void close() {
    }
}
