package com.metatrope.jact;

public class ActorPath {
    private ActorSystem localActorSystem;
    private String actorPath;
    private String actorSystemName;
    private String localRefName;

    public ActorPath(ActorSystem actorSystem, String actorPath) {
        this.localActorSystem = actorSystem;
        this.actorPath = actorPath;
        parse();
    }

    public String getActorPath() {
        return actorPath;
    }

    public String getActorSystemName() {
        return actorSystemName;
    }

    public String getLocalRefName() {
        return localRefName;
    }

    private void parse() {
        if (actorPath.contains("/")) {
            String[] receiverIdParts = actorPath.split("/");
            actorSystemName = receiverIdParts[0];
            localRefName = receiverIdParts[1];
        } else {
            actorSystemName = localActorSystem.getName();
            localRefName = actorPath;
        }
    }
}
