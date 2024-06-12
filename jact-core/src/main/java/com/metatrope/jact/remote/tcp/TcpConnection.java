package com.metatrope.jact.remote.tcp;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.Serdes;
import com.metatrope.jact.queue.BlockingMessageQueue;
import com.metatrope.jact.queue.MessageQueue;
import com.metatrope.jact.queue.NonBlockingMessageQueue;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TcpConnection implements MessageQueue {
    private static final Logger logger = LogManager.getLogger(TcpConnection.class);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final BlockingMessageQueue outQueue = new BlockingMessageQueue();
    private final BlockingMessageQueue inQueue = new BlockingMessageQueue();
    private Socket socket;
    private Thread readThread;
    private Thread writeThread;
    protected boolean isServer = false;

    private final static Serdes SERDES = new Serdes();

    public TcpConnection() {
    }

    public static class ServerSocketConnection extends TcpConnection {
        private final Socket socket;

        public ServerSocketConnection(Socket socket) {
            this.socket = socket;
            this.isServer = true;
        }

        @Override
        public Socket createSocket() {
            logger.info("{} Server socket {} ", hashCode(), socket);
            return this.socket;
        }

    }

    public abstract Socket createSocket();

    public synchronized Socket getSocket() {
        if (socket == null) {
            socket = createSocket();
        }
        return socket;
    }

    public void start() {
        this.socket = getSocket();
        this.readThread = Thread.ofVirtual().start(this::readThread);
        this.writeThread = Thread.ofVirtual().start(this::writeThread);
    }

    public void readThread() {
        try {
            logger.trace("{} {} reading {} ", hashCode(), isServer, socket);
            Socket socket = getSocket();
            if (socket != null) {
                InputStream input = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(input);
                while (running.get()) {
                    String messageBody = ois.readUTF();
                    logger.info("{} {} receiving {}", hashCode(), isServer, messageBody);
                    inQueue.offer(SERDES.deserialize(messageBody));
                }
            }
        } catch (SocketException | EOFException e) {
            logger.trace("{} {} lost connection {} ", hashCode(), isServer, socket);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeThread() {
        try {
            logger.trace("{} {} writing {} ", hashCode(), isServer, socket);
            Socket socket = getSocket();
            if (socket != null) {
                OutputStream output = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(output);
                while (running.get()) {
                    Envelope<?> envelope = outQueue.take();
                    logger.info("{} {} sending {}", hashCode(), isServer, SERDES.serialize(envelope));
                    oos.writeUTF(SERDES.serialize(envelope));
                    oos.flush();
                }
            }
        } catch (SocketException e) {
            logger.trace("{} {} lost connection {} ", hashCode(), isServer, socket);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void offer(Envelope<?> envelope) {
        outQueue.offer(envelope);
    }

    @Override
    public Envelope<?> take() {
        return inQueue.take();
    }

    @Override
    public void close() {
        this.running.set(false);
        try {
            if (this.socket != null) {
                this.socket.close();
            }
            this.readThread.join(1000);
            this.writeThread.join(1000);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}