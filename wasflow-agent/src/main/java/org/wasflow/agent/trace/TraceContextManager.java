package org.wasflow.agent.trace;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceContextManager {

    // trace context
    private static ThreadLocal<TraceContext> contexts = new ThreadLocal<TraceContext>();

    /**
     * 현재의 Thread에 TraceContext를 반환하는 메소드
     * (존재하지 않으면 생성)
     *
     * @return 현재의 Thread에 저장 된 TraceContext 인스턴스 or 새로 생성 + 저장
     */
    public static TraceContext getOrCreateContext() {
        TraceContext ctx = null;

        if ((ctx = contexts.get()) == null) {
            ctx = new TraceContext();
            contexts.set(ctx);
        }

        return contexts.get();
    }

    /**
     * 직접 TraceContext를 생성한 경우 현재 Thread 지역변수에 ctx를 담는다.
     */
    public static void setTraceContext(TraceContext ctx) {
        contexts.set(ctx);
    }

    /**
     * 현재 Thread에 저장 된 TraceContext 인스턴스를 반환한다.
     */
    public static TraceContext getContext() {
        return contexts.get();
    }

    /**
     * 현재의 Thread의 TraceContext 자원 정리
     *
     * @return 현재의 Thread에 담긴 TraceContext 인스턴스
     */
    public static TraceContext endContext() {
        TraceContext context = contexts.get();

        if (context != null) {
            contexts.set(null);
        }

        return context;
    }
}