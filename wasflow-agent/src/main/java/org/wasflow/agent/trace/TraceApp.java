package org.wasflow.agent.trace;

import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceApp {

    /**
     * 서버 정보 수집
     * - org/apache/catalina/core/StandardServer :: startInternal() 호출 시 서버 정보 추출
     */
    public static void setServerInfo(String serverInfo) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceApp::setServerInfo is called] server info : " + serverInfo);
        }

        Configurer.getInstance().server_info = serverInfo;
    }

    /**
     * 서버 포트 정보 수집
     * - org/apache/catalina/connector/Connector :: startInternal() 호출 시 포트를 추출
     */
    public static void setServerPort(int port, String protocol) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceApp::setServerPort is called] port : " + port + ", protocol : " + protocol);
        }

        if ("org.apache.coyote.http11.Http11NioProtocol".equalsIgnoreCase(protocol)) {
            Configurer conf = Configurer.getInstance();
            if (conf.server_http_port == 0) {
                conf.server_http_port = port;
            }
        }
    }
}