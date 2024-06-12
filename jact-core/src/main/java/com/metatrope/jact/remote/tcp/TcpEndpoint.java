package com.metatrope.jact.remote.tcp;

import com.metatrope.jact.ActorPath;
import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.exceptions.ActorException;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.queue.BlockingMessageQueue;
import com.metatrope.jact.remote.ServerEndpoint;
import com.metatrope.jact.remote.tcp.TcpConnection.ServerSocketConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TcpEndpoint implements ServerEndpoint {
    private static final Logger logger = LogManager.getLogger(TcpEndpoint.class);

    private Thread serverLoopThread;
    private Thread connectionProcessingThread;
    private ServerSocket serverSocket;
    private AtomicBoolean running = new AtomicBoolean(true);
    private ActorSystem actorSystem;
    private final int port;

    private List<TcpConnection> connections = new ArrayList<>();
    private ConcurrentMap<String, TcpConnection> knownClients = new MapMaker().weakValues().makeMap();

    BlockingMessageQueue serverQueue = new BlockingMessageQueue();
    
    public TcpEndpoint(int port) {
        this.port = port;
    }

    @Override
    public void start(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.serverLoopThread = Thread.startVirtualThread(this::serverLoop);
        this.connectionProcessingThread = Thread.startVirtualThread(this::connectionProcessingLoop);
    }
    
    public int getPort() {
        return port;
    }

    private void serverLoop() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                this.serverSocket = serverSocket;
                while (running.get()) {
                    Socket socket = serverSocket.accept();
                    logger.info("accepted a connection from " + socket);
                    TcpConnection clientConnection = new ServerSocketConnection(socket);
                    clientConnection.start();
                    connections.add(clientConnection);
                }
            }
        } catch (SocketException e) {
            // shut down
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Shutting down serverLoop");
    }

    private void connectionProcessingLoop() {
        while (running.get()) {
            connections.removeIf(c -> c.getSocket().isClosed());
            List<TcpConnection> connectionCopy = Lists.newArrayList(connections);
            for (TcpConnection connection : connectionCopy) {
                Envelope<?> message = connection.take();
                if (message != null) {
                    logger.debug("{} -- received from connected client {}", actorSystem.getName(), message);
                    if (message.getSenderId().indexOf('/') > 0) {
                        String sourceActorSystem = message.getSenderId().split("/")[0];
                        knownClients.putIfAbsent(sourceActorSystem, connection);
                    }
                    serverQueue.offer(message);
                }
            }
        }
        System.out.println("Shutting down connectionProcessingLoop");
    }

    @Override
    public Envelope<?> take() {
        return serverQueue.take();
    }

    @Override
    public <T> void send(Envelope<T> envelope) {
        String recipientID = envelope.getReceiverId();
        ActorPath actorPath = new ActorPath(actorSystem, recipientID);
        String targetActorSystem = actorPath.getActorSystemName();
        TcpConnection connection = knownClients.get(targetActorSystem);
        if (connection == null) {
            throw new ActorException("attempting to send a message to an unknown client: " + targetActorSystem + " for " + recipientID);
        }
        connection.offer(envelope);
    }

    @Override
    public void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
                for (TcpConnection connection : connections) {
                    connection.getSocket().close();
                }
            }
            running.set(false);
            serverLoopThread.join(1000);
            connectionProcessingThread.join(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
