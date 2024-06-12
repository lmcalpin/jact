package com.metatrope.jact.remote;

import com.metatrope.jact.ActorSystem;

import java.util.Map;
import java.util.Objects;

public class JoinMessage {
    private String clientActorSystem;
    private Map<String, String> connectionParameters;
    
    protected JoinMessage() {} 
 
    public JoinMessage(ActorSystem actorSystem, Transport transport) {
        this.clientActorSystem = actorSystem.getName();
        this.connectionParameters = transport.getConnectionProperties();
    }

    public String getClientActorSystem() {
        return clientActorSystem;
    }

    public Map<String, String> getConnectionParameters() {
        return connectionParameters;
    }
    
    @Override
    public String toString() {
        return "JoinMessage{" +
                "clientActorSystem='" + clientActorSystem + '\'' +
                ", connectionParameters=" + connectionParameters +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientActorSystem, connectionParameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JoinMessage other = (JoinMessage) obj;
        return Objects.equals(clientActorSystem, other.clientActorSystem) && Objects.equals(connectionParameters, other.connectionParameters);
    }
}
