package com.krisleonard.newrelic.project.server;

import com.krisleonard.newrelic.project.service.NumbersService;
import com.krisleonard.newrelic.project.service.impl.NumbersServiceRAFImpl;
import com.krisleonard.newrelic.project.util.ThreadUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The Socket Server for the New Relic project. This class starts the ServerSocket and accepts connections on a
 * specified port. All sockets are handed off to a thread pool for processing input. A status of the processed
 * input from the sockets will be logged to the console every ten seconds.
 */
public class SocketServer {

    /**
     * The default status timer delay in seconds
     */
    private static final int STATUS_TIMER_DELAY = 10;

    /**
     * The server socket
     */
    private ServerSocket serverSocket = null;

    /**
     * Indicator of if the server is stopped
     */
    private boolean stopped = false;

    /**
     * The thread pool executor
     */
    private ThreadPoolExecutor threadPoolExecutor = null;

    /**
     * The status timer executor
     */
    private ScheduledExecutorService statusTimerExecutor = null;

    /**
     * The numbers service for handling all input numbers from sockets
     */
    private NumbersService numbersService = null;

    /**
     * The lock used for concurrency
     */
    private Lock socketServerLock = new ReentrantLock();

    /**
     * Start the socket server and listen on the input port for messages. All messages are processed by a thread pool
     * that is the size of the input client pool size.
     *
     * @param port The port to accept socket connections on
     * @param clientPoolSize The client socket pool size. Any client socket connections beyond the pool size will be
     *                       place in the work queue.
     * @param clientConnectionWorkQueueSize The size of the work client socket connection work queue
     */
    public void startServer(int port, int clientPoolSize, int clientConnectionWorkQueueSize) {
        System.out.println("Starting server");

        // Create thread pool executor
        threadPoolExecutor = ThreadUtil.createDaemonExecutor(clientPoolSize,
                clientConnectionWorkQueueSize, "NewRelic Project Socket Server");

        try {
            // Create the number service. Currently using the version of the NumbersService that uses
            // a Random Access File for keeping track of input numbers
            numbersService = new NumbersServiceRAFImpl();

            // Create the server socket
            serverSocket = new ServerSocket(port);

            // Set up a thread to print out the status every STATUS_TIMER_DELAY seconds
            statusTimerExecutor = Executors.newSingleThreadScheduledExecutor();
            statusTimerExecutor.scheduleAtFixedRate(() -> System.out.println(numbersService.getStatus()),
                    STATUS_TIMER_DELAY, STATUS_TIMER_DELAY, TimeUnit.SECONDS);

            // Loop until the server is stopped
            while(!stopping()) {
                // Pass any server socket connections on to a client worker
                ClientWorker clientWorker = new ClientWorker(numbersService, this, serverSocket.accept());
                threadPoolExecutor.execute(clientWorker);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            shutdown();
        }
    }

    /**
     * Check if the server is stopped or not
     *
     * @return If the server is stopped or not
     */
    public boolean stopping() {
        socketServerLock.lock();
        try {
            return stopped;
        } finally {
            socketServerLock.unlock();
        }
    }

    /**
     * Shutdown the socket server. This method closes the server socket and thread pool executors.
     */
    public void shutdown() {
        socketServerLock.lock();
        try {
            // Set stopped to true
            stopped = true;

            System.out.println("Shutting down server");

            // Stop the thread pool executor
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
                try {
                    if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) { // TODO use constant
                        threadPoolExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPoolExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Stop the status time executor
            if (statusTimerExecutor != null) {
                statusTimerExecutor.shutdown();
                try {
                    if (!statusTimerExecutor.awaitTermination(30, TimeUnit.SECONDS)) { // TODO use constant
                        statusTimerExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    statusTimerExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Close the socket server
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO throw error
                }
            }
        } finally {
            socketServerLock.unlock();
        }
        System.out.println("Server stopped");

    }
}
