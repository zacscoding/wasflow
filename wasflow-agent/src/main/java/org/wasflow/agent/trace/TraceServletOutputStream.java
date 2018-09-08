package org.wasflow.agent.trace;

import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.util.StringUtil;

/**
 * Response의 HTML 코드를 얻기 위해 사용하는 클래스
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceServletOutputStream {

    public static void print(String x) {
        appendString(x);
    }

    public static void print(boolean x) {
        appendString(Boolean.toString(x));
    }

    public static void print(char x) {
        appendString(Character.toString(x));
    }

    public static void print(int x) {
        appendString(Integer.toString(x));
    }

    public static void print(long x) {
        appendString(Long.toString(x));
    }

    public static void print(float f) {
        appendString(Float.toString(f));
    }

    public static void print(double x) {
        appendString(Double.toString(x));
    }

    // println의 경우 print(), print(\r\n)을 호출하므로 개행만 추가
    public static void println() {
        appendString("\r\n");
    }

    public static void println(String s) {
        appendString("\r\n");
    }

    public static void println(boolean b) {
        appendString("\r\n");
    }

    public static void println(char c) {
        appendString("\r\n");
    }

    public static void println(int i) {
        appendString("\r\n");
    }

    public static void println(long l) {
        appendString("\r\n");
    }

    public static void println(float f) {
        appendString("\r\n");
    }

    public static void println(double d) {
        appendString("\r\n");
    }

    public static void write(byte[] b, int off, int len) {
        if (!isTraceResponseBody()) {
            return;
        }

        System.out.println("TraceServletOutputStream::write(byte,int,int) is called");

        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceServletOutputStream::write(byte[],int,int) is called");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx)) {
            ctx.write(b, off, len);
        }
    }

    public static void write(int x) {
        if (!isTraceResponseBody()) {
            return;
        }

        System.out.println("TraceServletOutputStream::write(int) is called");

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx)) {
            ctx.write(x);
        }
    }

    public static void write(char[] chars, int off, int len) {
        if (!isTraceResponseBody()) {
            return;
        }

        System.out.println("TraceServletOutputStream::write(char[], int, int) is called");

        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceServletOutputStream::write(char[],int,int) is called");
        }

        appendString(new String(chars, off, len));
    }

    /**
     * TraceContext의 StringBuilder에 응답 body or view html을 append 하는 메소드
     *
     * @param str
     */
    private static void appendString(String str) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceServletOutputStream::appendString(String) is called");
        }

        System.out.println("TraceServletOutputStream::appendString(String) is called");

        if (StringUtil.isEmpty(str)) {
            return;
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (isTraceResponseBody(ctx)) {
            ctx.write(str.getBytes());
        }
    }

    /**
     * ResponseBody를 추출할지 여부를 체크하는 메소드
     *
     * TraceContext가 trace이고 설정값의 trace_http_response_body가 true이면 true를 반환한다.
     */
    private static boolean isTraceResponseBody(TraceContext ctx) {
        return TraceContext.isTrace(ctx) && Configurer.getInstance().trace_http_response_body;
    }

    /**
     * ResponseBody를 추출할지 여부를 체크하는 메소드
     * 설정값의 trace_http_response_body 값을 반환
     */
    private static boolean isTraceResponseBody() {
        return Configurer.getInstance().trace_http_response_body;
    }
}
