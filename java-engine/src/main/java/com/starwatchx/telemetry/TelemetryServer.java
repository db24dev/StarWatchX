package com.starwatchx.telemetry;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket server responsible for broadcasting telemetry packets.
 * The Next.js dashboard connects to ws://localhost:PORT by default.
 * Update NEXT_PUBLIC_TELEMETRY_URL if you forward the port.
 */
public class TelemetryServer implements AutoCloseable {

    private final int port;
    private final Gson gson = new Gson();
    private BroadcastServer server;
    private volatile boolean running;

    public TelemetryServer(int port) {
        this.port = port;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        server = new BroadcastServer(new InetSocketAddress(port));
        server.start();
        running = true;
        System.out.println("[Telemetry] WebSocket server listening on port " + port);
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        try {
            server.stop();
        } catch (InterruptedException e) {
            System.err.println("[Telemetry] Error stopping server: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void broadcastTelemetry(TelemetryPacket packet) {
        if (!running || packet == null) {
            return;
        }
        String payload = gson.toJson(packet);
        System.out.println("[Telemetry] Sending packet: " + payload);
        server.broadcast(payload);
    }

    public void broadcastBatch(List<TelemetryPacket> packets) {
        if (packets == null) {
            return;
        }
        for (TelemetryPacket packet : packets) {
            broadcastTelemetry(packet);
        }
    }

    @Override
    public void close() {
        stop();
    }

    private static class BroadcastServer extends WebSocketServer {
        private final CopyOnWriteArraySet<WebSocket> connections = new CopyOnWriteArraySet<>();

        BroadcastServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            connections.add(conn);
            System.out.println("[Telemetry] Client connected: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            connections.remove(conn);
            System.out.println("[Telemetry] Client disconnected: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // No inbound messages expected; ignore or log
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("[Telemetry] WebSocket error: " + ex.getMessage());
        }

        @Override
        public void onStart() {
            setConnectionLostTimeout(30);
        }

        @Override
        public void broadcast(String payload) {
            for (WebSocket conn : connections) {
                conn.send(payload);
            }
        }
    }
}

