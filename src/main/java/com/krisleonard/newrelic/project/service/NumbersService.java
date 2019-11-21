package com.krisleonard.newrelic.project.service;

import java.io.IOException;
import java.util.regex.Pattern;

public interface NumbersService {

    /**
     * The name of the numbers log file
     */
    static final String NUMBERS_LOG_FILE_NAME = "numbers.log";

    /**
     * The leading zeros regex pattern
     */
    static final Pattern LEADING_ZEROS_PATTERN = Pattern.compile("^0*");

    /**
     * The expected number of characters in a received number
     */
    static final int NUMBER_CHARACTER_COUNT = 9;

    /**
     * The total character count of a line including the new line character
     */
    static final long TOTAL_LINE_CHARACTER_COUNT = NUMBER_CHARACTER_COUNT + System.lineSeparator().length();

    /**
     * The status string
     */
     static final String STATUS_STRING = "Received %d unique numbers, %d duplicates. Unique total: %d";

    /**
     * Adds a number to the number.log file and updates counters. If the number has already been received then
     * the count of newly received duplicates is incremented. If the number has not been received before, then
     * the count of newly received unique numbers and the total count of unique numbers is incremented.
     *
     * @param numberString The number to possibly add to the number log
     * @throws IOException When there is an IO issue with the numbers log file.
     */
    public void addNumber(final String numberString) throws IOException;

    /**
     * Get the status string containing the number of newly received unique numbers,
     * the number of newly received duplicates, and the total count of unique numbers received.
     * This method also resets the received unique and duplicate number counters.
     *
     * @return The status string containing the number of newly received unique numbers,
     */
    public String getStatus();
}
