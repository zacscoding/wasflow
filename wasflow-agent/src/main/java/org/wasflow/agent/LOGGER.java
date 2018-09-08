package org.wasflow.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.wasflow.util.FileUtil;
import org.wasflow.util.StringUtil;
import org.wasflow.util.ThreadUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class LOGGER {

    private static final long MEGA_BYTE = 1024L * 1024L;
    // debug-app boolean 값 여부
    private static boolean TRACE = true;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd HH:mm:ss");
    // 일반 로그를 출력 할 pw
    private static PrintWriter pw = null;
    // trace 로그를 출력 할 pw
    private static PrintWriter tracePw = null;
    // 현재 쓰고 있는 로그파일
    private static File curLogFile;
    // 현재 쓰고 있는 trace 로그 파일
    private static File curLogTraceFile;
    private static boolean isOverMaxLogFileSize = false;
    private static boolean isOverMaxTraceFileSize = false;
    // 현재 쓰고 있느 로그 파일의 date(yyMMdd)
    private static String curLogFileDate;

    /**
     * 오늘 날짜 관련된 설정 값들을 초기화
     */
    public static void sync() {
        isOverMaxLogFileSize = false;
        isOverMaxTraceFileSize = false;
    }

    public static boolean isTrace() {
        return TRACE;
    }

    public static void setTrace(boolean trace) {
        TRACE = trace;
    }

    /**
     * Log Message 출력
     */
    public static void info(Object message) {
        println(parse("wasflow : INFO", message));
    }

    public static void info(String id, Object message) {
        println(parse(id, message));
    }

    public static void printStack(String message, int size) {
        println(ThreadUtil.getPrintStack());
    }

    /**
     * Trace 메시지 출력(별도 파일)
     */
    public static void trace(Object message) {
        trace("wasflow : TRACE", message);
    }

    public static void trace(String id, Object message) {
        tracePrintln(parse(id, message));
    }

    /**
     * error 메지지 출력
     */
    public static void error(Object message) {
        error(message, null);
    }

    public static void error(Object message, Throwable t) {
        error("wasflow : ERROR", message, t);
    }

    public static void error(String id, Object message, Throwable t) {
        println(parse(id, message));
        if (t != null) {
            println(getStackTrace(t));
        }
    }

    /**
     * 기본 PrintWriter로 메시지 출력
     */
    private static void println(String message) {
        if (!isOverMaxLogFileSize) {
            println(pw, message);
        }
    }

    /**
     * Trace PrintWriter로 메시지 출력
     */
    private static void tracePrintln(String message) {
        if (!isOverMaxTraceFileSize) {
            println(tracePw, message);
        }
    }

    /**
     * PrintWriter로 메시지 출력 + flush
     */
    private static void println(PrintWriter printWriter, String message) {
        try {
            if (pw != null) {
                printWriter.println(message);
                printWriter.flush();
            }
        } catch (Throwable t) {

        }
    }

    /**
     * Throwable의 stack trace를 구하는 메소드
     */
    private static String getStackTrace(Throwable t) {
        String CRLF = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        sb.append(t.toString() + CRLF);
        StackTraceElement[] se = t.getStackTrace();
        if (se != null) {
            for (int i = 0; i < se.length; i++) {
                if (se[i] != null) {
                    sb.append("\t" + se[i].toString());
                    if (i != se.length - 1) {
                        sb.append(CRLF);
                    }
                }
            }
        }

        return sb.toString();
    }

    private static String parse(String id, Object message) {
        String messageVal = message == null ? "null" : message.toString();
        if (id == null) {
            id = "wasflow";
        }

        return new StringBuilder(20 + id.length() + messageVal.length()).append(sdf.format(new Date())).append(' ').append('[').append(id).append(']').append(' ').append(messageVal).toString();
    }

    /**
     * 로그파일 초기화 Configurer.log_path 디렉터리에 wasflow-yyMMdd.log 파일로 생성 => canWrite()이면 return
     */
    private static synchronized void initLogFile() {
        //        if (pw == null) {
        //            pw = new PrintWriter(System.out);
        //        }
        try {
            Configurer conf = Configurer.getInstance();
            String logPath = conf.log_path;
            if (pw != null) {
                FileUtil.close(pw);
            }

            if (logPath != null) {
                File dir = new File(logPath);
                if (!dir.canWrite()) {
                    dir.mkdirs();
                }
                if (!dir.canWrite()) {
                    return;
                }

                curLogFileDate = getToday();
                File file = new File(logPath, "wasflow-" + curLogFileDate + ".log");
                File traceFile = new File(logPath, "wasflow-trace-" + curLogFileDate + ".log");
                pw = new PrintWriter(new FileWriter(file, true));
                tracePw = new PrintWriter(new FileWriter(traceFile, true));
                curLogFile = file;
                curLogTraceFile = traceFile;
                sync();
            }
        } catch (IOException e) {
            System.out.println("Cant init wasflow log file : " + e.getMessage());
            //e.printStackTrace();
            /*  TEMP CDOE */
            // 예외 처리 결정하기
        }
    }

    private static String getToday() {
        return sdf.format(new Date()).substring(0, 6);
    }

    /**
     * 오래된 로그를 삭제하는 메소드
     */
    private static void clearOldLogs() {
        Configurer conf = Configurer.getInstance();
        int history = conf.log_max_history;
        String logPath = conf.log_path;
        if (history <= 0 || StringUtil.isEmpty(logPath)) {
            return;
        }

        File logDir = new File(logPath);
        File[] files = logDir.listFiles();
        if (files != null && files.length > 0) {
            long today = System.currentTimeMillis();
            SimpleDateFormat logDateFormat = new SimpleDateFormat("yyMMdd");

            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }

                String currentFileName = file.getName();
                int lastDashIdx = 0, lastDotIdx = 0;
                if ((lastDashIdx = currentFileName.lastIndexOf('-')) < 0 || (lastDotIdx = currentFileName.lastIndexOf(".log")) < 0) {
                    continue;
                }

                try {
                    String dateVal = currentFileName.substring(lastDashIdx + 1, lastDotIdx);
                    Date date = logDateFormat.parse(dateVal);
                    long diffInMillis = Math.abs(today - date.getTime());
                    long diffDay = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                    if (diffDay > history) {
                        LOGGER.trace("Clear old log : " + currentFileName);
                        file.delete();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 날짜별로 로그 파일을 갱신하고, 설정 값보다 오래 된 로그를 삭제하는 스레드
     */
    private static Runnable fileManager = new Runnable() {
        private boolean alive = true;

        @Override
        public void run() {
            int prevMaxSize = 0;
            long maxFileSize = 0L;
            while (alive) {
                try {
                    // check log file size
                    int maxSize = Configurer.getInstance().log_max_file_size;
                    // max size가 변경되면, 최대 파일 사이즈를 계산한다.
                    if (prevMaxSize != maxSize) {
                        prevMaxSize = maxSize;
                        maxFileSize = maxSize * MEGA_BYTE;
                    }

                    if (prevMaxSize > 0) {
                        if (curLogFile != null && curLogFile.exists()) {
                            isOverMaxLogFileSize = maxFileSize < curLogFile.length();
                        }
                        if (curLogTraceFile != null && curLogTraceFile.exists()) {
                            isOverMaxTraceFileSize = maxFileSize < curLogTraceFile.length();
                        }
                    }

                    // check today log file
                    if (!getToday().equals(LOGGER.curLogFileDate)) {
                        LOGGER.info("log date changed");
                        initLogFile();
                        // 설정 값의 max history 보다 큰 로그 파일 삭제
                        clearOldLogs();
                    }

                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    alive = false;
                }
            }
        }
    };

    static {
        initLogFile();
        Thread thread = new Thread(fileManager);
        thread.setDaemon(true);
        thread.setName("[WASFLOW-FileManager]");
        thread.start();
    }
}