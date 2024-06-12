package com.metatrope.jact.remote;

import com.metatrope.jact.config.Config;
import com.metatrope.jact.config.DefaultConfig;
import com.metatrope.jact.remote.tcp.TcpEndpoint;
import com.metatrope.jact.remote.tcp.TcpTransport;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;

public class TcpActorSystemTest extends RemoteActorSystemTestBase {
    int port = 4444;
    Map<String, TcpEndpoint> configs = new HashMap<>();
    
    @AfterEach
    public void after() {
        for (TcpEndpoint endpoint : configs.values()) {
            endpoint.close();
        }
    }

    @Override
    protected TransportFactory<?> getTransportFactory(String serverId) {
        TcpEndpoint endpoint = getTcpEndpoint(serverId);
        TcpTransport fakeRemoteActorSystem = new TcpTransport("127.0.0.1", endpoint.getPort());
        return actorSystem -> { 
            System.out.println("Connecting " + actorSystem.getName() + " to " + serverId + " over tcp on port " + endpoint.getPort());
            return fakeRemoteActorSystem;
        };
    }

    @Override
    protected Config getServerConfig(String serverId) {
        TcpEndpoint endpoint = getTcpEndpoint(serverId);
        Config config = new DefaultConfig() {

            @Override
            public ServerConfig getRemotingConfiguration() {
                return new ServerConfig() {
                    @Override
                    public ServerEndpoint[] getEndpoints() {
                        return new ServerEndpoint[] { endpoint };
                    }
                };
            }

        };
        return config;
    }

    private TcpEndpoint getTcpEndpoint(String serverId) {
        configs.putIfAbsent(serverId, new TcpEndpoint(port++));
        TcpEndpoint endpoint = configs.get(serverId);
        return endpoint;
    }
}
