package com.krisleonard.newrelic.project.service.impl;

import com.krisleonard.newrelic.project.service.NumbersService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.krisleonard.newrelic.project.service.NumbersService.*;
import static org.junit.Assert.assertEquals;

/**
 * The Unit test class for NumbersServiceRAFImpl
 */
public class NumbersServiceRAFImplTest {

    /**
     * The numbers service
     */
    private NumbersService numbersService = null;

    @Before
    public void before() {
        try {
            // Create the numbers service
            numbersService = new NumbersServiceRAFImpl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic happy path test for NumberServiceRAFImpl
     *
     * @throws IOException
     */
    @Test
    public void testHappyPathFunctionality() throws IOException {
        // Check initial value
        assertEquals(String.format(STATUS_STRING, 0, 0, 0), numbersService.getStatus());

        // Add all zeros
        numbersService.addNumber("000000000");
        assertEquals(String.format(STATUS_STRING, 1, 0, 1), numbersService.getStatus());

        // Check after adding a number
        numbersService.addNumber("123456789");
        assertEquals(String.format(STATUS_STRING, 1, 0, 2), numbersService.getStatus());

        // Check immediately after asking for status and not adding a number
        assertEquals(String.format(STATUS_STRING, 0, 0, 2), numbersService.getStatus());

        // Check after adding a duplicate
        numbersService.addNumber("123456789");
        assertEquals(String.format(STATUS_STRING, 0, 1, 2), numbersService.getStatus());
    }

    /**
     * Verify the number log is updated.
     *
     * @throws IOException
     */
    @Test
    public void testNumberLogUpdated() throws IOException {
        int number = 5;
        String numberString = "000000005";
        numbersService.addNumber(numberString);

        int numberTwo = 6;
        String numberStringTwo = "000000006";
        numbersService.addNumber(numberStringTwo);

        // TODO if numberService had a read method could call that here instead of duplicating this code
        try (FileChannel numberLogFileChannel =
                     (FileChannel.open(Paths.get(NUMBERS_LOG_FILE_NAME),
                             StandardOpenOption.READ, StandardOpenOption.WRITE))) {

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
            assertEquals(numberString, readString);

            numberByteBuffer.clear();

            numberLogFileChannel.position(numberTwo * TOTAL_LINE_CHARACTER_COUNT);
            numberBytesRead = 0;
            do {
                numberBytesRead = numberLogFileChannel.read(numberByteBuffer);
            } while (numberBytesRead != -1 && numberByteBuffer.hasRemaining());
            readString = StringUtils.trim(new String(numberByteBuffer.array(), StandardCharsets.UTF_8));
            assertEquals(numberStringTwo, readString);
        }
    }
}
