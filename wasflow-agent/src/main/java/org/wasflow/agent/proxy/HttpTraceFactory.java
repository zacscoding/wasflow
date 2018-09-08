package org.wasflow.agent.proxy;

import org.wasflow.agent.LOGGER;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class HttpTraceFactory {

    // servlet 3.0 이하 버전
    private static final String HTTP_TRACE = "org.wasflow.xtra.http.HttpTrace";
    // servlet 3.0 이상 버전
    private static final String HTTP_TRACE3 = "org.wasflow.xtra.http.HttpTrace3";

    /**
     * httpTrace 인스턴스를 생성하는 메소드
     * was의 경우 별도의 servlet jar를 이용하므로, HttpServletRequest 인스턴스의 클래스 로더를 부모 로더로하고
     * wasflow.http.jar로부터 HttpTrace 인스턴스를 생성한다.
     */
    public static IHttpTrace create(ClassLoader parent, Object requestObj) {
        try {
            // 생성 된 HttpServletRequest 인스턴스를 부모 로더로하고, wasflow.http.jar를 로더로 가져옴
            ClassLoader loader = LoaderManager.getHttpLoader(parent);
            if (loader == null) {
                LOGGER.info("@@ Http ClassLoader is null..");
            }

            // check servlet 3.x ~
            boolean isServlet3 = true;
            try {
                requestObj.getClass().getMethod("getParts");
            } catch (Exception e) {
                isServlet3 = false;
            }

            Class c = null;
            if (isServlet3) {
                // c = loader.loadClass(HTTP_TRACE3, true, loader);
                c = Class.forName(HTTP_TRACE3, true, loader);
            } else {
                c = Class.forName(HTTP_TRACE, true, loader);
            }

            if (LOGGER.isTrace()) {
                LOGGER.trace(isServlet3 ? "@@ created HTTP_TRACE3" : "@@ created HTTP_TRACE");
            }

            return (IHttpTrace) c.newInstance();
        } catch (Throwable e) {
            LOGGER.error("@@ [error] HttpTraceFactory :: create()", e);
            return null;
        }
    }
}