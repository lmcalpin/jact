package com.metatrope.jact.remote;

import com.metatrope.jact.ActorSystem;

public interface TransportFactory<T extends Transport> {
    public T create(ActorSystem actorSystem);
}
