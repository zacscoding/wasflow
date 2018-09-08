package org.wasflow.agent.trace;

import org.wasflow.agent.trace.TraceContext.Query;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import org.wasflow.agent.LOGGER;
import org.wasflow.util.StringUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TracePreparedStatement {
    private static Field STRING_READ_FIELD;
    private static final Object lock = new Object();

    public static void init() {

    }

    public static void setQuery(String query) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setQuery(String) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null && ctx.collectRepository != null) {
            if (ctx.collectRepository.collectQuery == null) {
                ctx.collectRepository.collectQuery = new TraceContext.Query(query);
            } else {
                ctx.collectRepository.collectQuery.query = query;
            }
            int dynamicQueryCount = StringUtil.countOccurrencesOf(query, '?');
            if (dynamicQueryCount > 0) {
                ctx.collectRepository.collectQuery.prepared = new String[dynamicQueryCount];
            }
        }
    }

    public static void execute(boolean result, PreparedStatement pstmt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::isTrace(boolean, PreparedStatement) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            ctx.collectRepository.collectQuery.result = result;
            // prepared 값 정리
            clearParameters(ctx);
            // update count (select 이외의 쿼리문에서 적용 레코드 수를 추출)
            if (pstmt != null) {
                try {
                    ctx.collectRepository.collectQuery.updated = pstmt.getUpdateCount();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    public static void executeUpdate(int updated) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::executeUpdate() is called] updated : " + updated);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            // prepared 값 정리
            clearParameters(tracedQuery);
            tracedQuery.updated = updated;
            ctx.collectRepository.queries.add(tracedQuery);
            ctx.collectRepository.collectQuery = null;
        }
    }

    public static void executeQuery(ResultSet rs) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::executeQuery(ResultSet) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            clearParameters(ctx);
            TraceResultSet.init(rs);
            TraceResultSet.forceMetaDataCalls(rs);
        }
    }

    public static void executeBatch(int[] result) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::executeBatch(int[]) is called] result : " + Arrays.toString(result));
        }

        TraceContext ctx = TraceContextManager.getContext();
        // prepared 값 정리
        clearParameters(ctx);
        TraceSQL.executeBatch(ctx, result);
    }

    public static void addBatch() {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::addBatch() is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            TraceContext.Query traceQuery = ctx.collectRepository.collectQuery;
            // prepared 값 정리
            clearParameters(traceQuery);
            ctx.collectRepository.collectQuery = new TraceContext.Query(traceQuery.query);

            if (ctx.collectRepository.collectBatchQuery == null) {
                ctx.collectRepository.collectBatchQuery = new LinkedList<Query>();
            }

            ctx.collectRepository.collectBatchQuery.add(traceQuery);
        }
    }

    /**
     * PreparedStatement의 clearParameters()가 호출 되면, 수집한 String[] prepared를 동적 쿼리 수에 맞게 배열을 조절한다.
     */
    public static void clearParameters() throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::clearParameters() is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        clearParameters(ctx);
    }

    /**
     * 동적 쿼리일 경우, prepared 배열 사이즈를 조절한다. (prepared같은 경우 동적 배열로 증가하므로, ?의 개수를 세고, 그 수 만큼 prepared를 조절)
     */
    public static void clearParameters(TraceContext ctx) {
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.prepared != null) {
                int dynamicQueryCount = StringUtil.countOccurrencesOf(tracedQuery.query, '?');
                if (dynamicQueryCount > 0 && tracedQuery.prepared.length > dynamicQueryCount) {
                    String[] newParams = new String[dynamicQueryCount];
                    System.arraycopy(tracedQuery.prepared, 0, newParams, 0, dynamicQueryCount);
                    tracedQuery.prepared = newParams;
                }
            }
        }
    }

    public static void clearParameters(TraceContext.Query tracedQuery) {
        if (tracedQuery == null || tracedQuery.prepared == null) {
            return;
        }
        int dynamicQueryCount = StringUtil.countOccurrencesOf(tracedQuery.query, '?');
        if (dynamicQueryCount > 0 && tracedQuery.prepared.length > dynamicQueryCount) {
            String[] newParams = new String[dynamicQueryCount];
            System.arraycopy(tracedQuery.prepared, 0, newParams, 0, dynamicQueryCount);
            tracedQuery.prepared = newParams;
        }
    }

    public static void setUpdateCount(int updateCount) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setUpdateCount() is called] updateCount : " + updateCount);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            ctx.collectRepository.collectQuery.updated = updateCount;
        }
    }

    public static void closeStmt(Object stmt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::closeStmt(Object) is called]");
        }
    }

    ///////////// Set value
    public static void setNull(int parameterIndex, int sqlType) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setNull] index : " + parameterIndex + ", sqlType : " + sqlType);
        }

        setParamValue(parameterIndex, Integer.toString(sqlType));
    }

    public static void setBoolean(int parameterIndex, boolean x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setBoolean] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Boolean.toString(x));
    }

    public static void setByte(int parameterIndex, byte x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setByte] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Byte.toString(x));
    }

    public static void setShort(int parameterIndex, short x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setShort] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Short.toString(x));
    }

    public static void setInt(int parameterIndex, int x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setInt] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Integer.toString(x));
    }

    public static void setLong(int parameterIndex, long x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setLong] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Long.toString(x));
    }

    public static void setFloat(int parameterIndex, float x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setFloat] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Float.toString(x));
    }

    public static void setDouble(int parameterIndex, double x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setDouble] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, Double.toString(x));
    }

    public static void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setBigDecimal] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, x == null ? null : x.toString());
    }

    public static void setString(int parameterIndex, String x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setString] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, x);
    }

    public static void setString(int parameterIndex, StringReader reader) {
        String readValue = "";
        try {
            if (STRING_READ_FIELD == null) {
                synchronized (lock) {
                    if (STRING_READ_FIELD == null) {
                        STRING_READ_FIELD = StringReader.class.getDeclaredField("str");
                    }
                }
            }
            STRING_READ_FIELD.setAccessible(true);
            readValue = (String) STRING_READ_FIELD.get(reader);
        } catch (Throwable t) {
            // ignore => setValue(1,"");
        }
        setParamValue(parameterIndex, readValue);
    }

    public static void setDate(int parameterIndex, Date x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setDate] index : " + parameterIndex + ", value : " + x);
        }

        setParamValue(parameterIndex, x == null ? "null" : x.toString());
    }

    public static void setTime(int parameterIndex, Time x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setTime] index : " + parameterIndex + ", value : " + x);
        }

        Date date = null;
        if (x != null) {
            date = new Date(x.getTime());
        }
        setDate(parameterIndex, date);
    }

    public static void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setTimestamp] index : " + parameterIndex + ", value : " + x);
        }

        Date date = null;
        if (x != null) {
            date = new Date(x.getTime());
        }
        setDate(parameterIndex, date);
    }

    public static void setObject(int parameterIndex, Object x) throws SQLException {
        if (x instanceof StringReader) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TracePreparedStatement::setObject is called] it is possible to instance of StringReader");
            }
            setString(parameterIndex, (StringReader) x);
        }
        //        else {
        //            setParamValue(parameterIndex, x == null ? "null" : x.toString());
        //        }
    }

    public static void setObjectByCustom(int parameterIndex, Object x) {
        // SqlServer의 경우 StringReader로 String 값을 추출하므로
        if (x instanceof StringReader) {
            setString(parameterIndex, (StringReader) x);
        } else {
            setParamValue(parameterIndex, x == null ? "null" : x.toString());
        }
    }

    public static void bind(int parameterIndex, Object x) {
        setParamValue(parameterIndex, x == null ? "null" : x.toString());
    }

    /*  TEMP CODE :: Blob 후킹 할지 말지 정하고 Blob -> 문자열 변환 하는 거 찾아야 됨 */
    public static void setBlob(int parameterIndex, Blob x) throws SQLException {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TracePreparedStatement::setBlob is called] idx : " + parameterIndex + ", value : " + x);
        }

        return;

        //        if (x != null) {
        //            int blobLength = (int) x.length();
        //            byte[] blobAsBytes = x.getBytes(1, blobLength);
        //            setParamValue(parameterIndex, blobAsBytes);
        //        } else {
        //            setParamValue(parameterIndex, null);
        //        }
    }

    /**
     * PreparedStatement 동적 쿼리 값 추출
     */
    private static void setParamValue(int index, String value) {
        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx)) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TracePreparedStatement::setParamValue] index : " + index + ", value : " + value);
            }

            if (!ctx.existCurrentCollectQuery()) {
                if (LOGGER.isTrace()) {
                    LOGGER.trace("[There is no current collect query]");
                }

                return;
            }

            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.prepared == null) {
                tracedQuery.prepared = new String[1];
            }
            // PreparedStatement는 1부터 시작
            index--;

            if (index < 0) {
                return;
            }

            if (index >= tracedQuery.prepared.length) {
                tracedQuery.prepared = StringUtil.growStringArray(tracedQuery.prepared, index);
            }
            tracedQuery.prepared[index] = value;
        }
    }
}