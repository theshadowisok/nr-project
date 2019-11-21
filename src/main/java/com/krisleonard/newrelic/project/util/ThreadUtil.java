package com.krisleonard.newrelic.project.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for common thread methods
 */
public class ThreadUtil {

    /**
     * Create a daemon thread pool executor.
     *
     * @param poolSize The pool size
     * @param maxQueueSize The work queue size
     * @param threadNamePrefix The prefix for the thread name
     * @return A daemon thread pool executor with a thread pool of the input size and with a work queue of the input
     * size
     */
    public static ThreadPoolExecutor createDaemonExecutor(
            final int poolSize, final int maxQueueSize, final String threadNamePrefix) {
        final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(maxQueueSize);

        // Create the thread pool executor
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, queue);

        // Set the name of the threads when the thread factory creates them
        final ThreadFactory defaultFactory = executor.getThreadFactory();
        executor.setThreadFactory(
                r -> {
                    final Thread t = defaultFactory.newThread(r);
                    if (threadNamePrefix != null) {
                        t.setName(threadNamePrefix + "-" + t.getId());
                    }
                    t.setDaemon(true);
                    return t;
                }
        );

        // Set the rejection handler
        executor.setRejectedExecutionHandler(
                (r, tpe) -> {
                    try {
                        if (!tpe.isShutdown()) {
                            queue.put(r);
                        }
                    } catch (InterruptedException ex) {
                        throw new RuntimeException("Error placing writer in queue", ex);
                    }
                }
        );

        return executor;
    }

}
