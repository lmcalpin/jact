package com.metatrope.jact.remote.tcp;

import com.metatrope.jact.remote.Transport;

import java.net.ConnectException;
import java.net.Socket;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TcpTransport extends TcpConnection implements Transport {
    private static final Logger logger = LogManager.getLogger(TcpTransport.class);

    private final String host;
    private final int port;

    public TcpTransport(String host, int port) {
        this.host = host;
        this.port = port;
        start();
    }

    @Override
    public Socket createSocket() {
        try {
            if (host == null)
                return null;
            while (true) {
                try {
                    Socket socket = new Socket(host, port);
                    logger.debug("{} Connected to {}:{}", this.hashCode(), host, port);
                    return socket;
                } catch (ConnectException e) {
                    // retry
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public Map<String, String> getConnectionProperties() {
        return null;
    }
}
