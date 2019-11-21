package com.krisleonard.newrelic.project.service.impl;

import com.krisleonard.newrelic.project.service.NumbersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A service class for adding numbers to a numbers log file and keeping track of the number of
 * unique numbers added and the number of duplicates.
 */
public class NumbersServiceRAFImpl implements NumbersService {

    /**
     * The set of received duplicate numbers.
     * TODO If a large number of duplicates are received then the code should be changed just to keep a count of dupes
     * TODO as this set will grow too large.
     */
    private HashSet<String> duplicateNumbers = new HashSet<>();

    /**
     * The current count of newly received unique numbers since the last status report
     */
    private int receivedUniqueCount = 0;

    /**
     * The total unique number count
     */
    private int totalUniqueCount = 0;

    /**
     * The current count of newly received duplicate numbers since the last status report
     */
    private int receivedNewDupes = 0;

    /**
     * The lock used to prevent concurrency issues when adding numbers and getting the status
     */
    private final Lock numbersLock = new ReentrantLock();

    /**
     * Default constructor that deletes the numbers log file if it exists and recreates it.
     *
     * @throws IOException When there is an IO issue with the numbers log file
     */
    public NumbersServiceRAFImpl() throws IOException {
        Path numbersLogPath = Paths.get(NUMBERS_LOG_FILE_NAME);
        boolean deletedFile = Files.deleteIfExists(numbersLogPath);
        if(!deletedFile) {
            // Do nothing. Log that the file didn't exist
        }

        // Create the file
        Files.createFile(numbersLogPath);

        System.out.println("Numbers log file path: " + numbersLogPath.toFile().getAbsolutePath());
    }

    /**
     * Checks to make sure the input string has nine characters and then
     * converts the input string to an int. It strips any leading zeros from the input string before converting
     * the string to an int.
     *
     * @param numberString The string to convert to a number
     * @return The input string as an int
     * @throws IllegalArgumentException
     */
    public static int convertToInteger(final String numberString) throws IllegalArgumentException {
        if(numberString.length() != NUMBER_CHARACTER_COUNT) {
            throw new IllegalArgumentException("Invalid length on input number: " + numberString);
        }

        // Strip the leading zeros off the string
        String leadingZeroStrippedNumber =
                LEADING_ZEROS_PATTERN.matcher(numberString).replaceFirst("");


        // Convert the string to an int
        return StringUtils.isBlank(leadingZeroStrippedNumber) ? 0 : Integer.parseInt(leadingZeroStrippedNumber);
    }

    /**
     * {@inheritDoc}
     */
    public void addNumber(final String numberString) throws IOException {
        numbersLock.lock();
        // Convert the input number string to an int
        int number = convertToInteger(numberString);
        try {
            // Check if the existing dupes set already has the number string so we can skip file IO
            if (duplicateNumbers.contains(numberString)) {
                receivedNewDupes++;
                return;
            }

            // TODO one change could be to break out the reading and writing in to seperate methods
            // Open a read/write random access file FileChannel for the numbers log
            try (FileChannel numberLogFileChannel =
                         (FileChannel.open(Paths.get(NUMBERS_LOG_FILE_NAME),
                                 StandardOpenOption.READ, StandardOpenOption.WRITE))) {
                // Read number string plus system line separator character bytes from the file at the number position
                ByteBuffer numberByteBuffer = ByteBuffer.allocate(numberString.length()
                        + System.lineSeparator().length());

                // Set the position to number times line size
                numberLogFileChannel.position(number * TOTAL_LINE_CHARACTER_COUNT);

                // Read line
                int numberBytesRead = 0;
                do {
                    numberBytesRead = numberLogFileChannel.read(numberByteBuffer);
                } while (numberBytesRead != -1 && numberByteBuffer.hasRemaining());

                // Turn read bytes into a string and trim
                String readString = StringUtils.trim(new String(numberByteBuffer.array(), StandardCharsets.UTF_8));

                // If any bytes are read the number exists already
                if (numberString.equals(readString)) {
                    // Update duplicate numbers hash set
                    duplicateNumbers.add(numberString);
                    receivedNewDupes++;
                    return;
                }

                // Reset the buffer
                numberByteBuffer.clear();

                // Add the number and new line to the buffer
                numberByteBuffer.put((numberString + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

                // flip byte buffer for output to file
                numberByteBuffer.flip();
                // Write number to file at same position as number
                numberLogFileChannel.position(number * TOTAL_LINE_CHARACTER_COUNT);
                while (numberByteBuffer.hasRemaining()) {
                    numberLogFileChannel.write(numberByteBuffer);
                }

                // Update counters
                totalUniqueCount++;
                receivedUniqueCount++;
            }
        } finally {
            numbersLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        numbersLock.lock();
        try {

            String status = String.format(STATUS_STRING, receivedUniqueCount, receivedNewDupes, totalUniqueCount);
            receivedUniqueCount = 0;
            receivedNewDupes = 0;
            return status;
        } finally {
            numbersLock.unlock();
        }
    }
}
