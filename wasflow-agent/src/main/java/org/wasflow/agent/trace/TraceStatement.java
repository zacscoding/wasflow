package org.wasflow.agent.trace;

import java.sql.ResultSet;
import java.util.LinkedList;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceContext.Query;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TraceStatement {
    public static void setQuery(String query) {
        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.collectRepository != null) {
            if (LOGGER.isTrace()) {
                LOGGER.trace("[TraceStatement::setSql() is called]");
            }

            if (ctx.collectRepository.collectQuery == null) {
                ctx.collectRepository.collectQuery = new TraceContext.Query();
                ctx.collectRepository.collectQuery.query = query;
            }
        }
    }

    public static void execute(boolean result, String sql) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceStatement::execute(String,boolean) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectRepository()) {
            TraceContext.Query tracedQuery = new TraceContext.Query();
            tracedQuery.query = sql;
            ctx.collectRepository.queries.add(tracedQuery);
        }
    }

    public static void executeQuery(ResultSet rs, String sql) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceStatement::executeQuery(ResultSet,String) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectRepository()) {
            if (ctx.collectRepository.collectQuery == null) {
                ctx.collectRepository.collectQuery = new TraceContext.Query();
            }
            ctx.collectRepository.collectQuery.query = sql;
            TraceResultSet.init(rs);
            TraceResultSet.forceMetaDataCalls(rs);
        }
    }

    public static void executeBatch(int[] result) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceStatement::executeBatch(int[]) is called]");
        }

        TraceSQL.executeBatch(TraceContextManager.getContext(), result);
    }

    public static void addBatch(String sql) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceStatement::addBatch(String sql) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectRepository()) {
            if (ctx.collectRepository.collectBatchQuery == null) {
                ctx.collectRepository.collectBatchQuery = new LinkedList<Query>();
            }
            ctx.collectRepository.collectBatchQuery.add(new TraceContext.Query(sql));
        }
    }

    public static void executeUpdate(int result, String sql) {
        if (LOGGER.isTrace()) {
            LOGGER.trace("[TraceStatement::executeUpdate(String, boolean) is called]");
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (TraceContext.isTrace(ctx) && ctx.existCurrentCollectRepository()) {
            TraceContext.Query tracedQuery = new TraceContext.Query();
            tracedQuery.query = sql;
            tracedQuery.updated = result;
            ctx.collectRepository.queries.add(tracedQuery);
        }
    }
}