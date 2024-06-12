package com.metatrope.jact.remote;

import com.metatrope.jact.config.Config;
import com.metatrope.jact.config.DefaultConfig;
import com.metatrope.jact.remote.fake.FakeEndpoint;
import com.metatrope.jact.remote.fake.FakeTransport;

public class FakeActorSystemTest extends RemoteActorSystemTestBase {
    FakeEndpoint fakeEndpoint = new FakeEndpoint();

    @Override
    protected TransportFactory<?> getTransportFactory(String serverId) {
        return actorSystem -> new FakeTransport(actorSystem, fakeEndpoint);
    }

    @Override
    protected Config getServerConfig(String serverId) {
        Config config = new DefaultConfig() {

            @Override
            public ServerConfig getRemotingConfiguration() {
                return new ServerConfig() {
                    @Override
                    public ServerEndpoint[] getEndpoints() {
                        return new ServerEndpoint[] { fakeEndpoint };
                    }
                };
            }

        };
        return config;
    }
}
