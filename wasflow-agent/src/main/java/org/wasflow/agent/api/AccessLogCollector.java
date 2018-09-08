package org.wasflow.agent.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.trace.TraceContext;

/**
 * TraceContext 수집 클래스
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class AccessLogCollector {

    // 싱글톤을 위한 인스턴스 변수
    private static AccessLogCollector INSTANCE;
    // thread safe를 위한 lock 인스턴스
    private static Object lock = new Object();

    // 로그를 쌓을 큐
    protected BlockingQueue<TraceContext> ctxBlockingQueue;
    private ConcurrentLinkedQueue<TraceContext> ctxConcurrentQueue;
    // while문 반복 period (confi로 설정해야 됨)
    private long period = 500L;
    // 최대 전송 로그 수
    private final int maxSendLogSize = 1000;
    // rest api 인터페이스
    private IAccessLogApi accessLogApi;
    private IAccessLogApi accessLogByFile;
    // 접속 로그 전송 스레드
    private Thread sendTask;

    public static AccessLogCollector getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new AccessLogCollector();
                }
            }
        }

        return INSTANCE;
    }

    private AccessLogCollector() {
        init();
    }

    /**
     * 로그 큐에 쌓는 메소드
     */
    public void setTraceContext(TraceContext ctx) {
        if (ctx != null) {
            if (ctxConcurrentQueue == null) {
                ctxConcurrentQueue = new ConcurrentLinkedQueue<TraceContext>();
            }

            ctxConcurrentQueue.offer(ctx);
        }
    }

    /**
     * 초기화 메소드
     */
    private void init() {
        accessLogApi = AccessLogApi.getInstance();
        accessLogByFile = AccessLogApiByFile.getInstance();

        if (sendTask == null || !sendTask.isAlive()) {
            sendTask = new SendMultipleLogTask();
            sendTask.setDaemon(true);
            sendTask.setName("WASFLOW-SendLogger");
            sendTask.start();
        }
    }

    /**
     * 로그 전송 스레드
     */
    private class SendMultipleLogTask extends Thread {

        public SendMultipleLogTask() {
            if (ctxConcurrentQueue == null) {
                ctxConcurrentQueue = new ConcurrentLinkedQueue<TraceContext>();
            }
        }

        @Override
        public void run() {

            while (true) {
                int size = ctxConcurrentQueue.size();
                if (size != 0) {
                    size = Math.min(ctxConcurrentQueue.size(), maxSendLogSize);
                    List<TraceContext> ctxs = new ArrayList<TraceContext>(size);

                    for (int i = 0; i < size; i++) {
                        ctxs.add(ctxConcurrentQueue.poll());
                    }

                    accessLogApi.sendAccessLogs(ctxs);

                    // write logs to file
                    if (Configurer.getInstance().write_log_file) {
                        if (accessLogByFile == null) {
                            accessLogByFile = AccessLogApiByFile.getInstance();
                        }
                        accessLogByFile.sendAccessLogs(ctxs);
                    }
                } else {
                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }
    }
}