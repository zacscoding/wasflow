package org.wasflow.agent.trace.filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class AntPathFilter {

    private static AntPathFilter INSTANCE = new AntPathFilter();
    private ShiroAntPathMatcher antPathMatcher = new ShiroAntPathMatcher();
    private Map<String, Boolean> cache = new ConcurrentHashMap<String, Boolean>();
    private boolean traceAll;
    private List<String> ignores;
    private List<String> traces;

    public static AntPathFilter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AntPathFilter();
        }
        return INSTANCE;
    }

    public AntPathFilter() {
        init();
    }

    /**
     * URI 필터링 설정이 값을 갱신하는 메소드
     */
    public static void sync(boolean traceAll, List<String> ignores, List<String> traces) {
        if (INSTANCE != null) {
            INSTANCE.init(traceAll, ignores, traces);
        }
    }

    public boolean isTrace(String uri) {
        if (cache == null) {
            cache = new ConcurrentHashMap<String, Boolean>();
        }

        Boolean isTrace = cache.get(uri);
        if (isTrace != null) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("@@ check uri : " + uri + ", result : " + isTrace + " by using cache");
            }
            return isTrace;
        }

        if (antPathMatcher == null) {
            antPathMatcher = new ShiroAntPathMatcher();
        }

        if (ignores != null && ignores.size() != 0) {
            for (String ignore : ignores) {
                if (antPathMatcher.match(ignore, uri)) {
                    if (LOGGER.isTrace()) {
                        LOGGER.trace("@@ check uri : " + uri + ", result : false");
                    }

                    cache.put(uri, Boolean.FALSE);
                    return false;
                }
            }
        }

        if (traceAll) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("@@ check uri : " + uri + ", result : true not matched ignores & trace all");
            }

            cache.put(uri, Boolean.TRUE);
            return true;
        }

        if (traces != null) {
            for (String trace : traces) {
                if (antPathMatcher.match(trace, uri)) {
                    if (LOGGER.isTrace()) {
                        LOGGER.trace("@@ check uri : " + uri + ", result : true");
                    }

                    cache.put(uri, Boolean.TRUE);
                    return true;
                }
            }
        }

        if (LOGGER.isTrace()) {
            LOGGER.trace("@@ check uri : " + uri + ", result : false not matched any configs");
        }

        cache.put(uri, Boolean.FALSE);
        return false;
    }

    /**
     * 초기 인스턴스 생성 시 Configurer의 값으로 init
     */
    private void init() {
        Configurer conf = Configurer.getInstance();
        this.init(conf.trace_all, conf.ignore_uri, conf.trace_uri);
    }

    private void init(boolean traceAll, List<String> ignores, List<String> traces) {
        if (cache == null) {
            cache = new ConcurrentHashMap<String, Boolean>();
        } else {
            cache.clear();
        }

        this.traceAll = traceAll;
        this.ignores = ignores;
        this.traces = traces;
    }
}