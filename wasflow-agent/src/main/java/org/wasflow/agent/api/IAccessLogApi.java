package org.wasflow.agent.api;

import java.util.List;
import org.wasflow.agent.trace.TraceContext;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public interface IAccessLogApi {

    String HEADER_ACCESS = "Accept";
    
    /**
     * 로그 전송 path
     */
    String ACCESS_LOG_SEND_PATH = "/logs/was";

    /**
     * 하나의 수집 로그를 전송하는 메소드
     *
     * @param ctx 수집 로그 인스턴스
     */
    void sendAccessLog(TraceContext ctx);

    /**
     * 다중 수집 로그를 전송하는 메소드
     *
     * @param ctxs 수집 로그 리스트
     */
    void sendAccessLogs(List<TraceContext> ctxs);

    /**
     * 정책을 체크하는 메소드
     */
    void checkPolicy();
}