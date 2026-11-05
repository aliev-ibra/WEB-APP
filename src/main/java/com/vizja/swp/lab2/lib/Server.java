package com.vizja.swp.lab2.lib;

import com.vizja.swp.lab2.lib.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService executorService;
    private ServerSocket serverSocket;

    public void start(int port) {
        if (isRunning.get()) {
            log.warn("Server is already running");
            return;
        }

        isRunning.set(true);
        executorService = Executors.newFixedThreadPool(3);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        try (var server = new ServerSocket(port)) {
            this.serverSocket = server;
            log.info("Server started on port {}", port);

            while (isRunning.get()) {
                var clientSocket = server.accept();
                log.info("Client connected: {}", clientSocket.getInetAddress().getHostAddress());
                handleClient(clientSocket);
            }

        } catch (IOException exception) {
            if (isRunning.get()) log.error("Unexpected server error", exception);
        }
        finally {close();}
    }

    private void handleClient(Socket client) {
        executorService.execute(() -> {
            try (client;
                 var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 var out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {

                String requestLine = in.readLine();
                if (requestLine == null || requestLine.isBlank()) return;

                log.info("Request Line: {}", requestLine);
                var request = HttpRequest.parse(requestLine, in);
                var response = FrontController.handle(request);

                out.write(response.toString());
                out.flush();

            } catch (IOException e) {
                log.error("Error handling client", e);
            }
        });
    }


    @Override
    public void close() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Shutting down server...");

            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log.warn("Error closing server socket", e);
            }

            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        log.warn("Forcing executor shutdown...");
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    log.warn("Shutdown interrupted", e);
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            log.info("Server stopped cleanly.");
        }
    }

}
