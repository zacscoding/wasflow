package org.wasflow.agent.proxy;

import org.wasflow.agent.trace.TraceContext;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public interface IHttpTrace {

    // Trace Context
    String REQUEST_ATTR_TRACE_CONTEXT = "__wasflow__ratc__";
    String REQUEST_ATTR_ASYNC_CONTEXT_HASH = "__wasflow__raach__";

    /**
     * Http 수집 시작 메소드
     * (ctx, req, res의 null 체크는 진행하지 않는다.)
     *
     * @param ctx 현재의 Thread에 저장 된 TraceContext 인스턴스
     * @param req was에 의해 생성 된 HttpServletRequest 인스턴스
     * @param res was에 의해 생성 된 HttpSevletResponse 인스턴스
     */
    public void start(TraceContext ctx, Object req, Object res);

    /**
     * Http 수집 종료 메소드
     * ctx,inst의 null 체크는 진행하지 않는다.
     *
     * @param ctx 현재의 Thread에 저장 된 TraceContext 인스턴스
     * @param req was에 의해 생성 된 HttpServletRequest 인스턴스
     * @param res was에 의해 생성 된 HttpSevletResponse 인스턴스
     */
    public void end(TraceContext ctx, Object req, Object res);

    /**
     * Servlet의 startAsync()가 실행되면, AsyncContext의 리스너를 등록하는 메소드
     */
    public void addAsyncContextListener(Object asyncContext);
}