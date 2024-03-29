/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.krisleonard.newrelic.project;

import com.krisleonard.newrelic.project.server.SocketServer;
import com.krisleonard.newrelic.project.service.impl.NumbersServiceRAFImpl;
import com.krisleonard.newrelic.project.util.ThreadUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static com.krisleonard.newrelic.project.service.NumbersService.NUMBERS_LOG_FILE_NAME;
import static com.krisleonard.newrelic.project.service.NumbersService.TOTAL_LINE_CHARACTER_COUNT;
import static org.junit.Assert.*;

public class IntegrationTest {

    /**
     * Random number
     */
    private static Random rand = new Random();

    /**
     * Generate a random number
     *
     * @return A random number
     */
    private static String generateRandomNumberString() {
        int randomInteger = rand.nextInt(100000000);
        return String.format("%09d", randomInteger);
    }

    @Test
    public void testAppIntegrationSmall() throws InterruptedException {
        SocketServer socketServer = new SocketServer();
        Thread appThread = new Thread(() ->
                socketServer.startServer(4000, 5, 200));
        appThread.start();

        try {
            // Map of all unique numbers created
            ConcurrentHashMap<String, String> numbers = new ConcurrentHashMap<>();

            // List of client threads
            List<Thread> threads = new ArrayList<>();

            // Create several client threads
            IntStream.range(0, 5).forEach(value -> {
                Runnable runnable = () -> {
                    System.out.println("running thread");
                    try (Socket socket = new Socket("localhost", 4000)) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        String number = generateRandomNumberString();
                        numbers.put(number, number);
                        out.println(number);
                        out.flush();
                        number = generateRandomNumberString();
                        numbers.put(number, number);
                        out.println(number);
                        out.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                };

                threads.add(new Thread(runnable));
            });

            // Start the client threads
            for (Thread thread : threads) {
                thread.start();
            }

            Thread.sleep(2 * 1000);
            // Wait for client threads to finish
            threads.stream().forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            // Check log file for numbers clients sent
            // TODO duplicate reading code
            numbers.keySet().stream().forEach(numberString -> {
                System.out.println(numberString);
                try (FileChannel numberLogFileChannel =
                             (FileChannel.open(Paths.get(NUMBERS_LOG_FILE_NAME),
                                     StandardOpenOption.READ, StandardOpenOption.WRITE))) {

                    ByteBuffer numberByteBuffer = ByteBuffer.allocate(numberString.length()
                            + System.lineSeparator().length());

                    // Set the position to number times line size
                    numberLogFileChannel.position(
                            NumbersServiceRAFImpl.convertToInteger(numberString) * TOTAL_LINE_CHARACTER_COUNT);

                    // Read line
                    int numberBytesRead = 0;
                    do {
                        numberBytesRead = numberLogFileChannel.read(numberByteBuffer);
                    } while (numberBytesRead != -1 && numberByteBuffer.hasRemaining());

                    // Turn read bytes into a string and trim
                    String readString = StringUtils.trim(new String(numberByteBuffer.array(), StandardCharsets.UTF_8));
                    assertEquals(numberString, readString);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

        } finally {
            // Shutdown everything
            socketServer.shutdown();
            appThread.join();
        }
    }
}
