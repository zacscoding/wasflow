package org.wasflow.agent.dto;

import java.util.List;
import java.util.Set;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ReadConfigurer {

    private ReadConfigurer.APP app;
    private ReadConfigurer.JDBC jdbc;
    private List<Collector> collectors;

    public APP getApp() {
        return app;
    }

    public void setApp(APP app) {
        this.app = app;
    }

    public JDBC getJdbc() {
        return jdbc;
    }

    public void setJdbc(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    public List<Collector> getCollectors() {
        return collectors;
    }

    public void setCollectors(List<Collector> collectors) {
        this.collectors = collectors;
    }

    public static class APP {

        // wasflow trace 여부
        private boolean debugApp;
        // 적용 서버 agent 이름
        private String agent;
        // 적용 서버 IP
        private String sourceIp;
        // wasflow log path
        private String logPath;
        // wasflow app log 최대 사이즈
        private int logMaxFileSize;
        // wasflow app log 보관일 수
        private int logMaxHistory;

        // 로그를 기록 할 URL
        private List<String> traceUri;
        // 로그를 제외 할 URL
        private List<String> ignoreUri;
        // 로그를 제외 할 응답 코드
        private Set<Integer> ignoreResponseStatus;
        // 응답 HTML 등 body trace 여부
        private boolean traceHttpResponseBody;
        // 접속 로그를 파일로 쓸지 여부(임시 테스트용으로 사용)
        private boolean writeLogFile;
        // HTTP 프로토콜의 body를 수집할 지 여부
        private boolean traceHttpRequestBody;


        private Login login;

        public boolean isDebugApp() {
            return debugApp;
        }

        public void setDebugApp(boolean debugApp) {
            this.debugApp = debugApp;
        }

        public String getAgent() {
            return agent;
        }

        public void setAgent(String agent) {
            this.agent = agent;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public void setSourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
        }

        public String getLogPath() {
            return logPath;
        }

        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }

        public int getLogMaxFileSize() {
            return logMaxFileSize;
        }

        public void setLogMaxFileSize(int logMaxFileSize) {
            this.logMaxFileSize = logMaxFileSize;
        }

        public int getLogMaxHistory() {
            return logMaxHistory;
        }

        public void setLogMaxHistory(int logMaxHistory) {
            this.logMaxHistory = logMaxHistory;
        }

        public List<String> getTraceUri() {
            return traceUri;
        }

        public void setTraceUri(List<String> traceUri) {
            this.traceUri = traceUri;
        }

        public List<String> getIgnoreUri() {
            return ignoreUri;
        }

        public void setIgnoreUri(List<String> ignoreUri) {
            this.ignoreUri = ignoreUri;
        }

        public Login getLogin() {
            return login;
        }

        public void setLogin(Login login) {
            this.login = login;
        }

        public Set<Integer> getIgnoreResponseStatus() {
            return ignoreResponseStatus;
        }

        public void setIgnoreResponseStatus(Set<Integer> ignoreResponseStatus) {
            this.ignoreResponseStatus = ignoreResponseStatus;
        }

        public boolean isTraceHttpResponseBody() {
            return traceHttpResponseBody;
        }

        public void setTraceHttpResponseBody(boolean traceHttpResponseBody) {
            this.traceHttpResponseBody = traceHttpResponseBody;
        }

        public boolean isWriteLogFile() {
            return writeLogFile;
        }

        public void setWriteLogFile(boolean writeLogFile) {
            this.writeLogFile = writeLogFile;
        }

        public boolean isTraceHttpRequestBody() {
            return traceHttpRequestBody;
        }

        public void setTraceHttpRequestBody(boolean traceHttpRequestBody) {
            this.traceHttpRequestBody = traceHttpRequestBody;
        }

        public static class Login {

            // 로그인 타입 == principal : 1, session : 2, cookie : 3, db : 4
            private int loginType;
            // 세션 or 쿠키 로그인 경우 저장되는 이름
            private String attributeName;
            // 세션 로그인 일 경우, 로그인 아이디 추출을 위한 메소드 이름
            private List<String> loginInvokeMethods;
            // 특정 클래스::메소드에서 로그인 정보를 추출할 수 있으면 사용
            private String loginInvokerClassName;
            private String loginInvokerMethodName;
            private String loginInvokerMethodDesc;

            public int getLoginType() {
                return loginType;
            }

            public void setLoginType(int loginType) {
                this.loginType = loginType;
            }

            public String getAttributeName() {
                return attributeName;
            }

            public void setAttributeName(String attributeName) {
                this.attributeName = attributeName;
            }

            public List<String> getLoginInvokeMethods() {
                return loginInvokeMethods;
            }

            public void setLoginInvokeMethods(List<String> loginInvokeMethods) {
                this.loginInvokeMethods = loginInvokeMethods;
            }

            public String getLoginInvokerClassName() {
                return loginInvokerClassName;
            }

            public void setLoginInvokerClassName(String loginInvokerClassName) {
                this.loginInvokerClassName = loginInvokerClassName;
            }

            public String getLoginInvokerMethodName() {
                return loginInvokerMethodName;
            }

            public void setLoginInvokerMethodName(String loginInvokerMethodName) {
                this.loginInvokerMethodName = loginInvokerMethodName;
            }

            public String getLoginInvokerMethodDesc() {
                return loginInvokerMethodDesc;
            }

            public void setLoginInvokerMethodDesc(String loginInvokerMethodDesc) {
                this.loginInvokerMethodDesc = loginInvokerMethodDesc;
            }
        }

    }

    public static class JDBC {

        private boolean traceWithNoHttpRequest;

        public boolean isTraceWithNoHttpRequest() {
            return traceWithNoHttpRequest;
        }

        public void setTraceWithNoHttpRequest(boolean traceWithNoHttpRequest) {
            this.traceWithNoHttpRequest = traceWithNoHttpRequest;
        }
    }
}