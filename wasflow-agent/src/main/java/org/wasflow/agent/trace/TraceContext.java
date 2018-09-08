package org.wasflow.agent.trace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceContext {

    // 현재의 Transaction ID 값
    public transient long txId;
    /* ========================================
     * http service
     * ======================================= */

    /*  Server Info */
    public String agent;
    public String sourceIp;
    public int serverHttpPort;

    /*  Http Request */
    public transient boolean traceUri = true;
    public String method;
    public String protocol;
    public String remoteHost;
    public String remoteUser;
    public String sessionId;
    // 커스텀 로그인을 사용하는 경우 쿠키 이름 or 세션 속성 이름을 담는다.
    public transient String customLoginName;
    public String loginId;
    public String contextPath;
    public String url;
    public String uri;
    public int responseStatus;
    public String responseMessage;
    public long accessTime;
    public boolean hasException;
    public String requestBody;
    public boolean startedAsync = false;


    /*  Http Response body */
    public transient String requestEncoding;
    public transient String responseEncoding;
    public String responseBody;
    private transient ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


    /* ========================================
     * jdbc
     * ======================================= */
    public List<Repository> repositories = new LinkedList<Repository>();
    // 현재 수집 중인 Repository
    public transient Repository collectRepository;
    public transient int collectConnHashCode;

    /**
     * trace 여부를 반환
     * TraceMain.start 에서 호출 된다.
     */
    public static boolean isTrace(TraceContext ctx) {
        return (ctx != null) && (ctx.traceUri);
    }

    /**
     * 마지막 로그를 전송할 지 체크
     * TraceMain.end에서 호출된다.
     */
    public static boolean isIgnore(TraceContext ctx) {
        // ctx, traceUri 체크
        if (ctx == null || !ctx.traceUri) {
            return true;
        }

        // 응답 코드 체크
        Configurer conf = Configurer.getInstance();
        if (conf.ignore_response_status != null) {
            boolean isIgnore = conf.ignore_response_status.contains(ctx.responseStatus);
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TraceContext::igIgnore()] check ignore : " + isIgnore + ", status : " + ctx.responseStatus + ", uri : " + ctx.uri);
            }

            return isIgnore;
        }

        return false;
    }

    /**
     * 수집 중인 Repository를 리스트에 담고 null || 새로운 Repository를 생성하여 멤버필드에 담는 메소드
     */
    public void clearPrevRepository(boolean createNewRepository) {
        // check trace repository
        if (existCurrentCollectRepository()) {
            repositories.add(collectRepository);

            // check trace query
            if (existCurrentCollectQuery()) {
                TraceContext.Query tracedQuery = collectRepository.collectQuery;

                if (tracedQuery.collectColumnValues != null) {
                    tracedQuery.columnValues.add(tracedQuery.collectColumnValues);
                }
                collectRepository.queries.add(tracedQuery);
                collectRepository.collectQuery = null;
            }

            collectRepository = null;
        }

        if (createNewRepository) {
            collectRepository = new Repository();
        }
    }

    public void write(int x) {
        if (byteArrayOutputStream == null) {
            byteArrayOutputStream = new ByteArrayOutputStream(1024);
        }

        byteArrayOutputStream.write(x);
    }

    public void write(byte[] bytes, int off, int len) {
        if (byteArrayOutputStream == null) {
            byteArrayOutputStream = new ByteArrayOutputStream(1024);
        }

        byteArrayOutputStream.write(bytes, off, len);
    }

    public void write(byte[] bytes) {
        try {
            if (byteArrayOutputStream == null) {
                byteArrayOutputStream = new ByteArrayOutputStream(1024);
            }

            byteArrayOutputStream.write(bytes);
        } catch (IOException e) {
            //ignore
        }
    }

    /**
     * Response body를 flush하는 메소드
     * 그동안 수집 된 StringBuilder가 존재하면 toString으로 body를 채운다.
     */
    public void flushResponseBody() {
        if (byteArrayOutputStream == null || byteArrayOutputStream.size() == 0) {
            return;
        }

        if (Configurer.getInstance().trace_http_response_body) {
            responseBody = new String(byteArrayOutputStream.toByteArray());
        } else {
            this.byteArrayOutputStream = null;
            this.responseBody = null;
        }
    }

    /**
     * 수집중인 Query의 ColumnLabel 값이 존재하는 지 여부
     *
     * @param idx ResultMetaDataSet의 idx (1부터 시작)
     */
    public boolean existColumnLabel(int idx) {
        idx--;
        if (idx >= 0 && existCurrentCollectQuery() && this.collectRepository.collectQuery.columnLabels != null && idx < this.collectRepository.collectQuery.columnLabels.length) {
            return this.collectRepository.collectQuery.columnLabels[idx] != null;
        }
        return false;
    }

    /**
     * 현재 수집 중인 Repository가 존재하는 지 여부
     */
    public boolean existCurrentCollectRepository() {
        return this.collectRepository != null;
    }

    /**
     * 현재 수집 중인 Query가 존재하는 지 여부
     */
    public boolean existCurrentCollectQuery() {
        return this.collectRepository != null && this.collectRepository.collectQuery != null;
    }

    /**
     * DataBase 트랜잭션에 대한 수집 도메인
     */
    public static class Repository {

        public String type;
        public String url;
        public String user;
        public boolean rollback;
        public long accessTime;
        public List<Query> queries = new LinkedList<Query>();
        // 현재 수집 중인 Query
        public transient Query collectQuery;
        public transient List<Query> collectBatchQuery;

        public Repository() {
            accessTime = System.currentTimeMillis();
        }
    }

    /**
     * 하나의 트랜잭션에서 발생하는 쿼리 도메인
     */
    public static class Query {

        public String query;
        public String[] prepared;
        public String[] columnLabels;
        public List<String[]> columnValues = new LinkedList<String[]>();
        public int updated;
        public boolean result;
        public int batchResult;
        // 현재 수집 중인 ResultSet의 column 수
        public transient int columnCount;
        // 현재 수집 중인 ResultSet
        public transient ResultSet resultSet;
        // 현재 수집 중인 ResultSet의 해시코드 값
        public transient long collectResultSetHash;
        // 현재 수집중인 ResultSet의 값
        public transient String[] collectColumnValues;

        public Query() {
        }

        public Query(String sql) {
            this.query = sql;
        }
    }
}