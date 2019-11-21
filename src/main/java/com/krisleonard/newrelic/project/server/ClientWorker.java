package com.krisleonard.newrelic.project.server;

import com.krisleonard.newrelic.project.service.NumbersService;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * The client worker thread for handling socket input.
 */
public class ClientWorker implements Runnable {

    /**
     * The message to terminate the server
     */
    private static final String SERVER_TERMINATE_MESSAGE = "terminate";

    /**
     * The socket to read data from
     */
    private Socket socket = null;

    /**
     * The socket server
     */
    private SocketServer socketServer = null;

    /**
     * The numbers service
     */
    private NumbersService numbersService = null;

    /**
     * Default constructor
     *
     * @param numbersService The numbers service
     * @param server The socket server
     * @param socket The socket to read data from
     */
    public ClientWorker(NumbersService numbersService, SocketServer server, Socket socket) {
        this.socketServer = server;
        this.socket = socket;
        this.numbersService = numbersService;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        // While the socket server is running
        if(!socketServer.stopping()) {
            // Read data from the socket
            try (Scanner scanner = new Scanner(socket.getInputStream())) {
                while (scanner.hasNextLine())
                {
                    String socketData = scanner.nextLine();
                    if (socketData.length() == 0)
                    {
                        break; // blank line terminates input
                    }

                    if(SERVER_TERMINATE_MESSAGE.equalsIgnoreCase(socketData)) {
                        socketServer.shutdown();
                    }

                    try {
                        // Add the number to the file
                        numbersService.addNumber(socketData);
                    } catch (IllegalArgumentException ex) {
                        // If the number is invalid shutdown the socket
                        socket.close();
                    } catch (IOException ex) {
                        // If there is an IO issue terminate the server
                        System.out.println("IO error while adding number. Shutting down server");
                        socket.close();
                        socketServer.shutdown();
                    }
                }
            } catch (IOException e) {
                // If there is a socket reading IO exception print to the console and end the threat
                e.printStackTrace();
            }
        }
    }
}
