package org.wasflow.util;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ThreadUtil {

    private static String NEW_LINE;

    static {
        NEW_LINE = System.getProperty("line.separator");
        if (NEW_LINE == null) {
            NEW_LINE = "\n";
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public static void printStackTrace() {
        System.out.println(getPrintStack(0));
    }

    public static String getPrintStack() {
        return getPrintStack(0);
    }

    public static String getPrintStack(int skip) {
        return getPrintStack(Thread.currentThread().getStackTrace(), skip);
    }

    public static String getPrintStack(StackTraceElement[] elts, int skip) {
        if (elts == null || elts.length == 0 || elts.length <= skip) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = skip; i < elts.length; i++) {
            sb.append(elts[i].toString());
            if (i != elts.length - 1) {
                sb.append(NEW_LINE);
            }
        }

        return sb.toString();
    }

    public static String getThreadInform() {
        Thread currentThread = Thread.currentThread();
        if (currentThread == null) {
            return "Thread : null";
        }

        return "Thread id : " + currentThread.getId() + ", name : " + currentThread.getName();
    }
}