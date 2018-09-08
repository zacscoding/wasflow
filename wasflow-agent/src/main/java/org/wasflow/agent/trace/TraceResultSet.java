package org.wasflow.agent.trace;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.wasflow.agent.LOGGER;
import org.wasflow.util.StringUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceResultSet {

    /////////////////////////// ResultSetMetaData
    public static void setColumnLabel(String name, int index) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setColumnLabel is called idx : " + index + ", name : " + name);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx)) {
            if (ctx.existCurrentCollectQuery()) {
                TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;

                if (tracedQuery.columnLabels == null) {
                    int defaultSize = tracedQuery.columnCount == 0 ? 1 : tracedQuery.columnCount;
                    tracedQuery.columnLabels = new String[defaultSize];
                }

                index--;
                if (index < 0) {
                    return;
                }

                if (index >= tracedQuery.columnLabels.length) {
                    tracedQuery.columnLabels = StringUtil.growStringArray(tracedQuery.columnLabels, index);
                }

                if (tracedQuery.columnLabels[index] == null) {
                    tracedQuery.columnLabels[index] = name;
                }
            }
        }
    }

    public static void forceMetaDataCalls(ResultSet rs) {
        if (rs == null) {
            return;
        }

        forceMetaDataCalls(TraceContextManager.getContext(), rs);
    }

    public static void forceMetaDataCalls(TraceContext ctx, ResultSet rs) {
        if (ctx != null && rs != null && ctx.existCurrentCollectQuery() && ctx.collectRepository.collectQuery.columnLabels == null) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TraceResultSet::forceMetaDataCalls is called]");
            }
            // mybatis 외에 ResultSet을 직접 사용하는 경우, meta 데이터를 추출하지 않을 수 있음
            // => 미리 setValue 해줌(특정 JDBC 구현체에서는 1.5 미만의 버전이여서, getColumnName을 후킹하지 못할 수 있어서 직접 값을 넣어줌)
            try {
                ResultSetMetaData rsMetaData = rs.getMetaData();
                if (rsMetaData != null) {
                    int colCount = rsMetaData.getColumnCount();
                    for (int i = 1; i <= colCount; i++) {
                        TraceResultSet.setColumnLabel(rsMetaData.getColumnLabel(i), i);
                    }
                }
            } catch (SQLException e) {
            }
        }
    }

    /**
     * ResultSetMeta에서 ResultSet의 컬럼 수 추출 수집중인 Query의 ColumnLabel 배열을 초기화 하는 메소드
     */
    public static void setColumnCount(int count) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setColumnCount(int count) is called] count : " + count);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && count > 0) {
            if (ctx.existCurrentCollectQuery()) {
                TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
                if (tracedQuery.columnCount == 0) {
                    tracedQuery.columnCount = count;
                }
            }
        }
    }

    /////////////////////////// ResultSet

    /**
     * ResultSet 구현 클래스가 생성 될때,
     */
    public static void init(Object rs) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::rsInit(Object rs) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();

        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            int hash = rs == null ? 0 : System.identityHashCode(rs);
            ctx.collectRepository.collectQuery.collectResultSetHash = hash;
            forceMetaDataCalls(ctx, (ResultSet) rs);
        }
    }

    /**
     * ResultSet.next()가 호출되면, 현재 수집 중인 ColumnValues를 TraceContext.Query.columnValues에  담는다.
     *
     * @param b ResultSet의 next() 반환 된 값
     */
    public static void next(boolean b) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::rsNext(boolean) is called] boolean : " + b);
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.collectColumnValues != null) {
                tracedQuery.columnValues.add(tracedQuery.collectColumnValues);
                tracedQuery.collectColumnValues = null;
            }
        }
    }

    /**
     * ResultSet의 구현 인스턴스의 close()가 호출 될 때,
     */
    public static void close(Object rs) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::close(Object rs) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectQuery()) {
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.collectResultSetHash != System.identityHashCode(rs)) {
                LOGGER.trace("[TraceResultSet.close() is called but diff hashcode]");
                return;
            }

            if (tracedQuery.columnLabels == null) {
                forceMetaDataCalls(ctx, (ResultSet) rs);
            }

            if (tracedQuery.collectColumnValues != null) {
                tracedQuery.columnValues.add(tracedQuery.collectColumnValues);
                tracedQuery.collectColumnValues = null;
            }
        }
    }

    // -- ResultSet getXXX()...
    public static void setObject(Object value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setObject(Object value, int paramInt) is called]");
        }

        setResultValue(paramInt, value == null ? "null" : value.toString());
    }

    // getString (I)Ljava/lang/String;
    public static void setString(String value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setString(String value, int paramInt) is called]");
        }

        setResultValue(paramInt, value);
    }

    // getBoolean (I)Z
    public static void setBoolean(boolean value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBoolean(boolean value, int paramInt) is called]");
        }

        setResultValue(paramInt, Boolean.toString(value));
    }

    // getByte (I)B
    public static void setByte(byte value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setByte(byte value, int paramInt) is called]");
        }

        setResultValue(paramInt, Byte.toString(value));
    }

    // getShort (I)S
    public static void setShort(short value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setShort(short value, int paramInt) is called]");
        }

        setResultValue(paramInt, Short.toString(value));
    }

    // getInt (I)I
    public static void setInt(int value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setInt(int value, int paramInt) is called]");
        }

        setResultValue(paramInt, Integer.toString(value));
    }

    // getLong (I)J
    public static void setLong(long value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setLong(int value, int paramInt) is called]");
        }

        setResultValue(paramInt, Long.toString(value));
    }

    // getFloat (I)F
    public static void setFloat(float value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setFloat(float value, int paramInt) is called]");
        }

        setResultValue(paramInt, Float.toString(value));
    }

    // getDouble (I)D
    public static void setDouble(double value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setDouble(double value, int paramInt) is called]");
        }

        setResultValue(paramInt, Double.toString(value));
    }

    // getBigDecimal (II)Ljava/math/BigDecimal;
    public static void setBigDecimal(BigDecimal value, int paramInt, int scare) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBigDecimal(BigDecimal value, int paramInt, int scare) is called]");
        }

        setResultValue(paramInt, value == null ? "null" : value.toString());
    }

    // getBigDecimal (II)Ljava/math/BigDecimal;
    public static void setBigDecimal(BigDecimal value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBigDecimal(BigDecimal value, int paramInt) is called]");
        }

        setResultValue(paramInt, value == null ? "null" : value.toString());
    }

    // getDate (I)Ljava/util/Date;
    public static void setDate(Date value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setDate(Date value, int paramInt) is called]");
        }

        setResultValue(paramInt, value == null ? null : value.toString());
    }

    // getTimestamp (I)Ljava/sql/Timestamp;
    public static void setTimestamp(Timestamp value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setTimestamp(Timestamp value, int paramInt) is called]");
        }

        Date date = null;
        if (value != null) {
            date = new Date(value.getTime());
        }

        setDate(date, paramInt);
    }

    // getBlob (I)Ljava/sql/Blob;
    public static void setBlob(Blob value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBlob(Timestamp value, int paramInt) is called]");
        }
    }

    // setClob (I)Ljava/sql/Clob;
    public static void setClob(Clob value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBlob(Clob value, int paramInt) is called]");
        }
    }

    // setBytes (I)[B
    public static void setBytes(byte[] value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setBytes(byte[] value, int paramInt) is called]");
        }
    }

    // set
    public static void setNString(String value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setNString(String value, int paramInt) is called]");
        }
    }

    // setValue (ILcom/microsoft/sqlserver/jdbc/JDBCType;Lcom/microsoft/sqlserver/jdbc/InputStreamGetterArgs;Ljava/util/Calendar;)Ljava/lang/Object;
    public static void setValue(Object value, int paramInt) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceResultSet::setValue(Object value, int paramInt) is called]");
        }

        setObject(value, paramInt);
    }

    private static void setResultValue(int index, String value) {
        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx)) {
            if (!ctx.existCurrentCollectQuery()) {
                LOGGER.info("[There is no current collect query]");
                return;
            }
            TraceContext.Query tracedQuery = ctx.collectRepository.collectQuery;
            if (tracedQuery.collectColumnValues == null) {
                int defaultSize = tracedQuery.columnCount == 0 ? 1 : tracedQuery.columnCount;
                tracedQuery.collectColumnValues = new String[defaultSize];
            }

            index--;
            if (index < 0) {
                return;
            }

            if (index >= tracedQuery.collectColumnValues.length) {
                tracedQuery.collectColumnValues = StringUtil.growStringArray(tracedQuery.collectColumnValues, index);
            }
            tracedQuery.collectColumnValues[index] = value;
        }
    }
}