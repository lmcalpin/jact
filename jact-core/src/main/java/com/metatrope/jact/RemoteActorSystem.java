package com.metatrope.jact;

import com.metatrope.jact.remote.ClientMessageDispatcher;

public class RemoteActorSystem implements AutoCloseable {
    private String name;
    private ClientMessageDispatcher transportClient;

    RemoteActorSystem(String name, ClientMessageDispatcher transportClient) {
        this.name = name;
        this.transportClient = transportClient;
    }

    public String getName() {
        return name;
    }

    ClientMessageDispatcher getTransportClient() {
        return transportClient;
    }

    @Override
    public void close() {
        transportClient.close();
    }
}
