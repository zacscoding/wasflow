package org.wasflow.xtra.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.proxy.IHttpTrace;
import org.wasflow.agent.trace.TraceContext;
import org.wasflow.agent.trace.filter.AntPathFilter;
import org.wasflow.util.CollectionUtil;
import org.wasflow.util.LongKeyGenerator;
import org.wasflow.util.StringUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class HttpTrace implements IHttpTrace {

    private static Method[] LOGIN_INVOKE_METHODS;
    private static Object lock = new Object();

    /**
     * 로그인 관련 Invoke 메소드들의 배열을 초기화하는 메소드
     *
     * @param recursiveInst method.invoke(Object inst)를 최초로 실행 할 인스턴스
     *
     * @throws Exception Method not found등 모든 예외를 전가
     */
    private static void initLoginInvokeMethods(Object recursiveInst) throws Exception {
        Configurer conf = Configurer.getInstance();
        if (conf.has_login_invoker_exception) {
            return;
        }

        synchronized (lock) {
            if (LOGIN_INVOKE_METHODS == null) {
                List<String> invokeMethods = conf.login_invoke_methods;
                if (CollectionUtil.isNotEmpty(invokeMethods)) {
                    LOGIN_INVOKE_METHODS = new Method[invokeMethods.size()];
                    for (int i = 0; i < invokeMethods.size(); i++) {
                        if (recursiveInst == null) {
                            conf.has_login_invoker_exception = true;
                            return;
                        }
                        Method method = recursiveInst.getClass().getMethod(invokeMethods.get(i));
                        LOGIN_INVOKE_METHODS[i] = method;
                        recursiveInst = method.invoke(recursiveInst);
                    }
                } else {
                    LOGIN_INVOKE_METHODS = new Method[] {};
                }
            }
        }
    }


    @Override
    public void start(TraceContext ctx, Object req, Object res) {
        Configurer conf = Configurer.getInstance();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // transaction
        ctx.txId = LongKeyGenerator.next();

        // check uri
        ctx.uri = request.getRequestURI();
        if (!AntPathFilter.getInstance().isTrace(ctx.uri)) {
            ctx.traceUri = false;
            return;
        }

        ctx.agent = conf.agent;
        ctx.sourceIp = conf.source_ip;

        if (ctx.sourceIp == null) {
            ctx.sourceIp = request.getLocalAddr();
            conf.source_ip = ctx.sourceIp;

            LOGGER.info("Configurer`s source ip is null, So set value from request" + ctx.sourceIp);
        }

        int serverPort = request.getServerPort();
        if (serverPort != 0 && conf.server_http_port == 0) {
            conf.server_http_port = serverPort;
        }

        StringBuffer urlBuff = request.getRequestURL();
        if (urlBuff != null) {
            ctx.url = urlBuff.toString();
        }

        ctx.serverHttpPort = serverPort;
        ctx.method = request.getMethod();
        ctx.protocol = request.getProtocol();
        ctx.sessionId = request.getRequestedSessionId();
        ctx.accessTime = System.currentTimeMillis();
        ctx.remoteHost = getRealIp(request);
        ctx.remoteUser = request.getRemoteUser();
        ctx.contextPath = request.getContextPath();

        if (StringUtil.isEmpty(ctx.contextPath)) {
            ServletContext sc = request.getServletContext();
            if (sc != null) {
                ctx.contextPath = sc.getContextPath();
            }
        }

        // Java의 Principal의 권한을 먼저 수집한다(Spring security 등을 이용할 때 추출 함)
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            String loginId = principal.getName();
            if (StringUtil.isNotEmpty(loginId)) {
                LOGGER.info("[Catch login id from Principal] login id : " + loginId);
                ctx.loginId = loginId;
            }
        }

        extractLoginId(ctx, request, response, conf);

        // http request body 수집
        if (conf.trace_http_request_body) {
            extractRequestBody(ctx, request);
        }
    }

    @Override
    public void end(TraceContext ctx, Object req, Object res) {
        Configurer conf = Configurer.getInstance();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // 응답 코드를 flush 한다.
        ctx.flushResponseBody();

        // 로그인 아이디가 존재하지 않으면, 한번더 시도한다.
        extractLoginId(ctx, request, response, conf);
        if (ctx.loginId == null) {
            ctx.loginId = "NULL";
        }
    }

    @Override
    public void addAsyncContextListener(Object asyncContext) {
        // servlet 3.0 이하의 버전에서는 지원X
    }

    /**
     * Config에 저장 된 invoke_methods
     */
    private String extractLoginIdFromMethods(Object recursiveInst, final String defaultValue) {
        try {
            // cache
            if (LOGIN_INVOKE_METHODS == null) {
                initLoginInvokeMethods(recursiveInst);
            }

            for (int i = 0; i < LOGIN_INVOKE_METHODS.length; i++) {
                if (recursiveInst == null) {
                    Configurer.getInstance().has_login_invoker_exception = true;
                    return defaultValue;
                }
                Method method = LOGIN_INVOKE_METHODS[i];
                recursiveInst = method.invoke(recursiveInst);
            }
            return (recursiveInst instanceof String) ? (String) recursiveInst : defaultValue;
        } catch (Throwable t) {
            LOGGER.error("[Failed to extract login id]", t);
            return defaultValue;
        }
    }


    /**
     * Login ID를 얻는 메소드 1) java.security.Principal에 저장하는 경우(시큐리티 등) getName으로 추출 2) Session에 저장하는 경우 => 설정 값에 attributeName, invokeMethods[]를 정의하여 추출 3) Cookie에 저장하는 경우 => 구현 예정
     */
    private void extractLoginId(TraceContext ctx, HttpServletRequest req, HttpServletResponse res, Configurer conf) {
        if (!TraceContext.isTrace(ctx) || (!"NULL".equals(ctx.loginId) && StringUtil.isNotEmpty(ctx.loginId))) {
            return;
        }

        final String defaultValue = "NULL";

        try {
            switch (conf.login_type) {
                // session
                // 세션에 저장 된 경우 method의 invoke 계속 실행 => 마지막이 String 인 경우 로그인 추출 그외 예외 NULL
                case 2:
                    if (conf.has_login_invoker_exception) {
                        LOGGER.error("@@ Trace Login id : conf.has_login_invoker_exception == TRUE, so set default value : " + defaultValue);
                        ctx.loginId = defaultValue;
                    } else {
                        if (res != null && res.isCommitted()) {
                            return;
                        }

                        // 세션 속성 이름의 값을 추출
                        Object obj = req.getSession().getAttribute(conf.login_attribute_name);
                        if (obj == null) {
                            LOGGER.info("[Failed to extract login id] There is no session attribute, where name : " + conf.login_attribute_name);
                            ctx.loginId = defaultValue;
                        } else {
                            Object recursiveInst = obj;
                            // 로그인 아이디 or 커스텀 속성 이름
                            String extractValue = extractLoginIdFromMethods(recursiveInst, defaultValue);
                            String loginId = null;

                            if (conf.exist_custom_login) {
                                ctx.customLoginName = extractValue;
                                loginId = Configurer.getInstance().getCustomLoginId(extractValue);
                            } else {
                                loginId = extractValue;
                            }

                            ctx.loginId = loginId;
                        }
                    }
                    LOGGER.info("[HttpTrace:extractLoginId] extract login id from session, login id : " + ctx.loginId);
                    break;
                // cookie
                case 3:
                    if (conf.login_attribute_name == null) {
                        LOGGER.error("Login type is cookie. But there is no cookie name");
                        ctx.loginId = defaultValue;
                    } else {
                        Cookie[] cookies = req.getCookies();
                        if (cookies != null) {
                            for (Cookie cookie : cookies) {
                                if (cookie.getName().equals(conf.login_attribute_name)) {
                                    // 로그인 아이디 or 커스텀 속성 이름
                                    String extractValue = cookie.getValue();
                                    String loginId = null;
                                    if (conf.exist_custom_login) {
                                        ctx.customLoginName = extractValue;
                                        loginId = Configurer.getInstance().getCustomLoginId(extractValue);
                                    } else {
                                        loginId = extractValue;
                                    }

                                    ctx.loginId = loginId;

                                    if (LOGGER.isTrace()) {
                                        LOGGER.trace("[HttpTrace::extractLoginId login cookie cookie name : " + cookie.getName() + " value : " + cookie.getValue());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    break;
                // db
                case 4:
                    break;
                default:
            }
        } catch (Throwable t) {
            // 오류가 발생하는 경우만 NULL로 넣는다.
            LOGGER.error("[Failed to extract login id]", t);
            if (ctx.loginId == null) {
                ctx.loginId = defaultValue;
            }
        }
    }

    /**
     * 프록시 환경에서 실제 Client 아이피를 가져오는 메소드
     *
     * @param request HttpServletRequest
     */
    private String getRealIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        // 체크 할 헤더 이름
        final String[] headerNames = new String[] {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

        String ip = null;

        for (String headerName : headerNames) {
            ip = request.getHeader(headerName);
            if (isValidIp(ip)) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isInvalidIp(String ip) {
        return (ip == null || ip.length() == 0 || (ip.length() == 7 && "unknown".equalsIgnoreCase(ip)));
    }

    private boolean isValidIp(String ip) {
        return ip != null && ip.length() != 0 && !(ip.length() == 7 && "unknown".equalsIgnoreCase(ip));
    }

    private void extractRequestBody(TraceContext ctx, HttpServletRequest request) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = request.getReader();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            if (sb.length() > 0) {
                ctx.requestBody = sb.toString();
            }
        } catch (IOException e) {
            LOGGER.error("[Failed to extract request body]", e);
            return;
        }
    }
}