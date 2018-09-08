package org.wasflow.agent.trace;

import javax.sql.DataSource;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.api.AccessLogCollector;
import org.wasflow.agent.proxy.HttpTraceFactory;
import org.wasflow.agent.proxy.IHttpTrace;
import org.wasflow.util.StringUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceMain {

    public static class HttpContext {

        public TraceContext ctx;
        public Object req;
        public Object res;

        public HttpContext(TraceContext ctx) {
            this.ctx = ctx;
        }

        public HttpContext(TraceContext ctx, Object req, Object res) {
            this.ctx = ctx;
            this.req = req;
            this.res = res;
        }
    }

    private static Object lock = new Object();
    private static IHttpTrace httpTrace = null;

    public static Object startHttpTrace(Object req, Object res) {
        return startTrace(req, res);
    }

    public static Object startHttpFilterTrace(Object req, Object res) {
        return startTrace(req, res);
    }

    /**
     * Http 통신 시작 메소드
     *
     * @param req HttpServletRequest 구현 객체
     * @param res HttpServletResponse 구현 객체
     *
     * @return TraceMain.httpContext 인스턴스 or null
     */
    private static Object startTrace(Object req, Object res) {
        try {
            if (TraceContextManager.getContext() != null) {
                return null;
            }

            Configurer conf = Configurer.getInstance();

            if (!conf.trace) {
                return null;
            }

            if (httpTrace == null) {
                initHttp(req);
            }

            TraceContext ctx = TraceContextManager.getOrCreateContext();
            httpTrace.start(ctx, req, res);
            HttpContext httpContext = new HttpContext(ctx, req, res);

            return httpContext;
        } catch (Throwable t) {
            LOGGER.error("@@ [failed to startTrace]", t);
        }

        return null;
    }

    /**
     * Http request 종료 메소드
     */
    public static void endTrace(Object httpContextObj, Throwable thr) {
        try {
            HttpContext httpContext = (HttpContext) httpContextObj;
            if (httpContext == null) {
                return;
            }

            if (httpContext.ctx == null) {
                return;
            }

            if (httpContext.ctx.startedAsync) {
                // 구현해야 됨
            } else {
                endTraceFinal(httpContext, thr);
            }
        } catch (Throwable t) {
            LOGGER.error("@@ [error in TraceMain::endTrace()]", t);
        }
    }

    public static void endTraceFinal(HttpContext httpContext, Throwable thr) {
        Configurer conf = Configurer.getInstance();
        if (!conf.trace) {
            return;
        }

        if (TraceContextManager.endContext() == null) {
            return;
        }

        if (TraceContext.isIgnore(httpContext.ctx)) {
            return;
        }

        // 예외 발생 여부 체크
        if (thr != null && httpContext.ctx != null) {
            httpContext.ctx.hasException = true;
        }

        // Http Trace 정리
        httpTrace.end(httpContext.ctx, httpContext.req, httpContext.res);
        // Repository Trace 정리
        httpContext.ctx.clearPrevRepository(false);

        LOGGER.info("Complete collect http trace httpContext : " + httpContext + ", thr : " + thr);
        // 수집 로그 que에 쌓기
        AccessLogCollector.getInstance().setTraceContext(httpContext.ctx);
    }

    /**
     * HttpServletResponse setWebResponseStatus 호출 시 실행
     */
    public static void setWebResponseStatus(String ms) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::setWebResponseStatus(String) is called] message : " + ms);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            ctx.responseMessage = ms;
        }
    }

    public static void setWebResponseStatus(int status) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::setWebResponseStatus(int status) is called] status : " + status);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            ctx.responseStatus = status;
        }
    }

    /**
     * HttpServletResponse setWebResponseSendError 호출 시 실행
     */
    public static void setWebResponseSendError(int status) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::setWebResponseSendError(int status) is called] status : " + status);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            ctx.responseStatus = status;
        }
    }

    public static void setWebResponseSendError(String ms) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::setWebResponseSendError(String) is called] message : " + ms);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            ctx.responseMessage = ms;
        }
    }

    public static void setLoginId(Object inst) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::setLogin(Object) is called]");
        }

        if (inst == null) {
            return;
        }

        TraceContext ctx = null;
        if (inst == null || (ctx = TraceContextManager.getContext()) == null || httpTrace == null) {
            return;
        }
        if (StringUtil.isNotEmpty(ctx.customLoginName)) {
            String extractLoginId = String.valueOf(inst);
            Configurer.getInstance().setCustomLoginId(ctx.customLoginName, extractLoginId);
            if ((ctx.loginId == null || "NULL".equals(ctx.loginId))) {
                ctx.loginId = extractLoginId;
            }
        }
    }

    /**
     * HttpServletRequest의 startAsync()가 호출 시 실행
     *
     * @param asyncContext : startAsync()의 반환 인스턴스
     */
    public static void startAsync(Object asyncContext) {
        // 개발해야 됨
        boolean temp = true;
        if (temp) {
            return;
        }

        TraceContext ctx = null;

        if (httpTrace == null || !TraceContext.isTrace(ctx = TraceContextManager.getContext())) {
            return;
        }

        httpTrace.addAsyncContextListener(asyncContext);
    }

    /**
     * AsyncContext의 dispatch 메소드 호출 시 수집
     */
    public static void dispatchAsync(Object contextObj, String path) {
        // System.out.println("[@@ TraceMain::dispatchAsync is called] contextObj : " + contextObj + ", path : " + path + ", " + ThreadUtil.getThreadInform());
    }


    /**
     * CtxLookup check 해야됨
     * (JNDI에 대한 정보를 수집할 수 있다)
     */
    public static void ctxLookup(Object this1, Object ctx) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceMain::ctxLookup(Object,Object) is called] this1 : " + this1 + ", ctx : " + ctx);
        }

        if (ctx instanceof DataSource) {
        }
    }


    /**
     * HttpTrace 인스턴스를 초기화 하는 메소드
     */
    private static void initHttp(Object req) {
        synchronized (lock) {
            if (httpTrace == null) {
                httpTrace = HttpTraceFactory.create(req.getClass().getClassLoader(), req);
            }
        }
    }
}