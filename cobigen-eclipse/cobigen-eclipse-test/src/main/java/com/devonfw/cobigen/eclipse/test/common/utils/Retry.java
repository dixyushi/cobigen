package com.devonfw.cobigen.eclipse.test.common.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Class implementing a way of retrying callables
 */
public class Retry {

    /**
     * Run a given callable retrying on a given exception up to the given numbers of retries
     * @param <R>
     *            the result type
     * @param <E>
     *            the exception type to retry for
     * @param callable
     *            the function to execute
     * @param retryException
     *            the class object for the exception to be retried
     * @param retries
     *            the number of max retries
     * @return the result of the callable
     * @throws Exception
     *             in case another exception occurred different to the retryException or even if after the
     *             given number of retries no result could be returned without exception.
     */
    public static <R, E extends Exception> R runWithRetry(Callable<R> callable, Class<E> retryException, int retries)
        throws Exception {
        for (int i = 0; i < retries; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                if (retryException.isAssignableFrom(e.getClass())) {
                    continue;
                }
                throw e;
            }
        }
        throw new TimeoutException("Unable to get a result after " + retries + " retries");
    }

    /**
     * Run a given runnable retrying on a given exception up to the given numbers of retries
     * @param <E>
     *            the exception type to retry for
     * @param bot
     *            SWTBot
     * @param runnable
     *            the function to execute
     * @param retryException
     *            the class object for the exception to be retried
     * @param retries
     *            the number of max retries
     * @throws Exception
     *             in case another exception occurred different to the retryException or even if after the
     *             given number of retries no result could be returned without exception.
     */
    public static <E extends Exception> void runWithRetry(SWTWorkbenchBot bot, ExceptionRunnable runnable,
        Class<E> retryException, int retries) throws Exception {
        for (int i = 0; i < retries; i++) {
            try {
                runnable.run();
            } catch (Exception e) {
                if (retryException.isAssignableFrom(e.getClass())) {
                    Thread.sleep(1000);
                    continue;
                }
                throw e;
            }
        }
        EclipseUtils.openErrorsTreeInProblemsView(bot);
        throw new TimeoutException("Unable to get a result after " + retries + " retries");
    }
}
