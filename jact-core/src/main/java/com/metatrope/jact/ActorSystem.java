package com.metatrope.jact;

import com.metatrope.jact.actors.ActorFactory;
import com.metatrope.jact.config.Config;
import com.metatrope.jact.config.DefaultConfig;
import com.metatrope.jact.dispatcher.LocalDispatcher;
import com.metatrope.jact.exceptions.ActorAlreadyRegisteredException;
import com.metatrope.jact.exceptions.ActorNotFoundException;
import com.metatrope.jact.message.MessageFactory;
import com.metatrope.jact.remote.ClientMessageDispatcher;
import com.metatrope.jact.remote.JoinMessage;
import com.metatrope.jact.remote.RemotingDispatcher;
import com.metatrope.jact.remote.ServerConfig;
import com.metatrope.jact.remote.Transport;
import com.metatrope.jact.remote.TransportFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages all the local Actors.
 * 
 * TODO: enable communication between ActorSystems to allow remote actors.
 */
public class ActorSystem implements Closeable {
    private final static Logger logger = LogManager.getLogger(ActorSystem.class);

    private final String friendlyName;
    private final LocalDispatcher localDispatcher;
    private final ExecutorService actorProcessingExecutorService;
    private Map<String, Actor<?, ?>> actorMap = new ConcurrentHashMap<>();
    private AtomicBoolean stopped = new AtomicBoolean();
    private Config config;
    private RemotingDispatcher remotingDispatcher;

    public ActorSystem() {
        this(null, new DefaultConfig());
    }

    public ActorSystem(String friendlyName) {
        this(friendlyName, new DefaultConfig());
    }

    public ActorSystem(Config config) {
        this(null, config);
    }

    public ActorSystem(String friendlyName, Config config) {
        String name = UUID.randomUUID().toString();
        this.friendlyName = friendlyName != null ? friendlyName : name;
        this.config = config;
        this.localDispatcher = new LocalDispatcher(this);
        this.actorProcessingExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        this.actorProcessingExecutorService.submit(this::processingLoop);
        ServerConfig remotingConfig = config.getRemotingConfiguration();
        this.remotingDispatcher = new RemotingDispatcher(this, localDispatcher, remotingConfig);
    }

    public String getName() {
        return friendlyName;
    }

    @SuppressWarnings("unchecked")
    public <T, R> LocalActorRef<T, R> locateLocal(String actorName) {
        Preconditions.checkNotNull(actorName);
        Actor<T, R> actor = (Actor<T, R>) actorMap.get(actorName);
        if (actor != null)
            return actor.self();
        throw new ActorNotFoundException(actorName);
    }

    /**
     * Returns an ActorRef for an Actor, given its name in the local ActorSystem.
     * This method does not throw an Exception if we can't locate the Actor.
     * 
     * @param <T>
     * @param <R>
     * @param actorName
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T, R> LocalActorRef<T, R> tryLocateLocal(String actorName) {
        Actor<T, R> actor = (Actor<T, R>) actorMap.get(actorName);
        if (actor != null)
            return actor.self();
        return null;
    }

    public <T, R> RemoteActorRef<T, R> locateRemote(RemoteActorSystem remoteActorSystem, String actorName) {
        return new RemoteActorRef<T, R>(this, remoteActorSystem, actorName);
    }

    public <T, R> LocalActorRef<T, R> register(Actor<T, R> actor) {
        return register(null, actor);
    }

    @SuppressWarnings("unchecked")
    public <T, R> LocalActorRef<T, R> register(String name, Actor<T, R> actor) {
        if (name == null) {
            name = UUID.randomUUID().toString();
        }
        Actor<T, R> ref = (Actor<T, R>) actorMap.get(name);
        if (ref != null) {
            throw new ActorAlreadyRegisteredException(String.format("Actor `%s` is already registered", ref));
        }
        if (actorMap.values().contains(actor)) {
            throw new ActorAlreadyRegisteredException(String.format("Actor `%s` is already registered", ref));
        }
        actor.start(this, name);
        this.actorMap.put(name, actor);
        return actor.self();
    }

    public <T, R> LocalActorRef<T, R> register(ActorBehavior<T, R> actorBehavior) {
        return register(null, actorBehavior);
    }

    public <T> LocalActorRef<T, Void> register(Consumer<T> actorBehavior) {
        return register(null, actorBehavior);
    }

    public <T, R> LocalActorRef<T, R> register(String name, ActorBehavior<T, R> actorBehavior) {
        return register(name, ActorFactory.create(actorBehavior));
    }

    public <T> LocalActorRef<T, Void> register(String name, Consumer<T> actorBehavior) {
        return register(name, ActorFactory.create(actorBehavior));
    }

    public RemoteActorSystem connect(TransportFactory<?> transportFactory) throws InterruptedException, ExecutionException, TimeoutException {
        return connect(transportFactory.create(this));
    }

    public RemoteActorSystem connect(Transport transport) throws InterruptedException, ExecutionException, TimeoutException {
        ClientMessageDispatcher clientDispatcher = new ClientMessageDispatcher(this, transport);
        Future<String> reply = clientDispatcher.dispatch(MessageFactory.system(getName(), "$join", new JoinMessage(this, transport)));
        this.remotingDispatcher.registerTransportClient(clientDispatcher);
        String remoteActorSystemName = reply.get();
        RemoteActorSystem remoteActorSystem = new RemoteActorSystem(remoteActorSystemName, clientDispatcher);
        return remoteActorSystem;
    }

    public void stop(ActorRef<?, ?> ref) {
        localDispatcher.dispatch(MessageFactory.poison(ref));
    }

    void unregister(Actor<?, ?> actor) {
        this.actorMap.remove(actor.getName());
    }

    @Override
    public void close() {
        stopped.set(true);
        localDispatcher.close();
        remotingDispatcher.close();
        actorProcessingExecutorService.shutdownNow();
    }

    LocalDispatcher getLocalDispatcher() {
        return localDispatcher;
    }

    Config getConfig() {
        return config;
    }

    private void processingLoop() {
        while (!stopped.get()) {
            actorMap.values().stream().filter(a -> a.isRunning() && !a.isBusy() && a.hasMail()).forEach(a -> a.getActorProcessor().processMessage());
        }
    }
}
