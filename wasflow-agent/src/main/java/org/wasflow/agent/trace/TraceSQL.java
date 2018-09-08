package org.wasflow.agent.trace;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceContext.Query;

/**
 * SQL 관련 수집 클래스
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceSQL {

    private static final Pattern VENDOR_PATTERN = Pattern.compile("jdbc:([^:]*).*");
    private static Map<String, String> urlCache = new HashMap<String, String>();

    /////////////////////////// DataSource
    public static void rollback() {
        TraceContext ctx = TraceContextManager.getContext();

        if (TraceContext.isTrace(ctx) && ctx.collectRepository != null) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TraceSQL::rollback is called");
            }

            ctx.collectRepository.rollback = true;
        }
    }

    /**
     * Repository 관련 Trace 시작 메소드 getConnection(String url, Properties props...) 가 호출 되면 시작 된다. 즉 하나의 트랜잭션을 Repository 로
     * 본다.
     */
    public static void startSqlTrace(Connection conn, String url, Properties properties) {
        if (isClosedConnection(conn)) {
            return;
        }

        int connHash = getConnectionHash(conn);

        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceSQL::startSqlTrace is called] Conn hash : " + connHash + ", url : " + url + ", properties : " + properties);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            if (Configurer.getInstance().jdbc_trace_with_no_http_request == false) {
                return;
            }
            return;
            // TraceContext 추가 + mock http trace 생성 + Connection 언제 close 되는지 확인??
            // 1) executeXXX -> 1-1)ResultSet 존재X => 수집 종료 // 1-2) ResultSet 존재 => close면 수집 종료 but close 하지 않은 경우?
            // TraceContextManager.startContext();
        }

        if (ctx.collectConnHashCode == connHash) {
            LOGGER.info("[Already started to this conn in TraceSQL::startSqlTrace()");
            return;
        }
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceSQL : start to collect]");
        }

        ctx.clearPrevRepository(true);

        ctx.collectConnHashCode = connHash;

        // 커넥션에 대한 정보가 존재하지 않는 경우, 직접 추출한다.
        if (url == null || properties == null) {
            try {
                DatabaseMetaData meta = conn.getMetaData();
                if (meta != null) {
                    ctx.collectRepository.url = meta.getURL();
                    ctx.collectRepository.user = meta.getUserName();
                }
            } catch (Throwable t) {
                LOGGER.error("failed to get database metadata from connection", t);
            }
        } else {
            ctx.collectRepository.url = url;
            ctx.collectRepository.user = getUserFromProperties(properties);
        }

        ctx.collectRepository.type = TraceSQL.getDatabaseType(ctx.collectRepository.url);
    }

    public static void endSqlTrace(TraceContext ctx) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceSQL::endSqlTrace(TraceContext) is called]");
        }

        if (ctx == null) {
            ctx = TraceContextManager.getContext();
        }

        if (ctx == null) {
            return;
        }

        if (ctx.existCurrentCollectQuery()) {
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.collectColumnValues != null) {
                tracedQuery.columnValues.add(tracedQuery.collectColumnValues);
            }
            ctx.collectRepository.queries.add(tracedQuery);
        }

        if (ctx.existCurrentCollectRepository()) {
            ctx.repositories.add(ctx.collectRepository);
        }
    }

    /**
     * PreparedStatement, Statement executeBatch후 처리
     */
    public static void executeBatch(TraceContext ctx, int[] result) {
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectRepository()) {
            List<Query> batchList = ctx.collectRepository.collectBatchQuery;
            if (result == null && batchList == null) {
                return;
            }

            if (result == null) {
                ctx.collectRepository.collectBatchQuery = null;
                return;
            }

            if (batchList == null) {
                LOGGER.info("[excuted batch but no Query in Repository]");
                return;
            }

            int min = Math.min(result.length, batchList.size());
            for (int i = 0; i < min; i++) {
                batchList.get(i).batchResult = result[i];
            }
            ctx.collectRepository.queries.addAll(batchList);
            ctx.collectRepository.collectBatchQuery = null;
        }
    }

    /////////////////////////// PreparedStatement
    public static void connect(Connection conn, String url, java.util.Properties properties) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceSQL::connect is called] conn hash " + (conn == null ? 0 : System.identityHashCode(conn)) + " url : " + url + ", properties : " + properties);
        }

        startSqlTrace(conn, url, properties);
    }

    /**
     * Connection이 닫혀있는지 여부를 판단하는 메소드
     *
     * @param conn 체크 할 커넥션
     *
     * @return false : conn == null || conn.isClose() , true : 그 외
     */
    private static boolean isClosedConnection(Connection conn) {
        try {
            return (conn == null) || (conn.isClosed());
        } catch (SQLException e) {
            return true;
        }
    }

    /**
     * Connection의 System hash code를 구하는 메소드
     */
    private static int getConnectionHash(Connection conn) {
        return conn == null ? 0 : System.identityHashCode(conn);
    }

    /**
     * Properties로 부터 "user"에 해당하는 값을 구하는 메소드
     */
    private static String getUserFromProperties(Properties properties) {
        String user = null;

        if (properties != null) {
            user = properties.getProperty("user");
        }

        return user == null ? "unknown" : user;
    }

    /**
     * jdbc url로 부터 데이터베이스 타입을 구하는 메소드
     */
    private static String getDatabaseType(String url) {
        if (url == null) {
            return null;
        }

        if (urlCache == null) {
            urlCache = new HashMap<String, String>();
        }

        String type = urlCache.get(url);
        if (type != null) {
            return type;
        }

        Matcher matcher = VENDOR_PATTERN.matcher(url);
        if (matcher.matches()) {
            type = matcher.group(1);
            urlCache.put(url, type);
            return type;
        }

        return "unknown";
    }
}