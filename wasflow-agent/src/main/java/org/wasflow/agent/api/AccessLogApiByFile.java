package org.wasflow.agent.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceContext;
import org.wasflow.util.GsonUtil;
import org.wasflow.util.StringUtil;

/**
 * 수집된 로그를 파일로 쓰는 클래스
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class AccessLogApiByFile implements IAccessLogApi {

    private static IAccessLogApi INSTANCE;
    private static Object lock = new Object();

    private PrintWriter pw;

    public static IAccessLogApi getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new AccessLogApiByFile();
                }
            }
        }

        return INSTANCE;
    }

    private AccessLogApiByFile() {
        init();
    }

    @Override
    public void sendAccessLog(TraceContext ctx) {
        writeLogs(GsonUtil.prettyString(ctx));
    }

    @Override
    public void sendAccessLogs(List<TraceContext> ctxs) {
        writeLogs(GsonUtil.prettyString(ctxs));
    }

    @Override
    public void checkPolicy() {
        // empty
    }

    private void writeLogs(String logs) {
        if (pw != null) {
            pw.println(logs);
            pw.println("\n");
            pw.flush();
        }
    }


    private void init() {
        if (pw == null) {
            String logPath = Configurer.getInstance().log_path;
            if (StringUtil.isEmpty(logPath)) {
                LOGGER.error("there is no log path in configurer");
                return;
            }
            try {
                File file = new File(logPath, "wasflow-access-logs.log");
                pw = new PrintWriter(new FileWriter(file, true));
            } catch (IOException e) {
                LOGGER.error("failed to init access-logs file", e);
            }
        }
    }
}