package org.wasflow.xtra.http;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;
import org.wasflow.agent.proxy.IHttpTrace;
import org.wasflow.agent.trace.TraceContext;
import org.wasflow.agent.trace.TraceContextManager;

/**
 * Servlet 3.x 대 이후 Trace
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class HttpTrace3 extends HttpTrace implements IHttpTrace {

    @Override
    public void end(TraceContext ctx, Object req, Object res) {
        super.end(ctx, req, res);
        if (ctx.responseStatus == 0) {
            ctx.responseStatus = ((HttpServletResponse) res).getStatus();
        }
    }

    public void addAsyncContextListener(Object asyncContext) {
        TraceContext ctx = null;
        if (asyncContext == null || !TraceContext.isTrace(ctx = TraceContextManager.getContext()) || !(asyncContext instanceof AsyncContext)) {
            return;
        }

        AsyncContext async = (AsyncContext) asyncContext;
        async.getRequest().setAttribute(REQUEST_ATTR_TRACE_CONTEXT, ctx);

        // System.out.println("@@ addAsyncContextListener is called system hash : " + System.identityHashCode(async));
        async.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                // System.out.println("@@ onComplete is called");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                // System.out.println("@@ onTimeout is called");
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                // System.out.println("@@ onError is called");
                // event.getThrowable().printStackTrace();
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // System.out.println("@@ onStartAsync is called");
            }
        });
    }
}