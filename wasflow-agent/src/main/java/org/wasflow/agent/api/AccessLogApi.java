package org.wasflow.agent.api;

import java.io.IOException;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.dto.Collector;
import org.wasflow.agent.trace.TraceContext;
import org.wasflow.util.CollectionUtil;
import org.wasflow.util.GsonUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class AccessLogApi implements IAccessLogApi {

    private static IAccessLogApi INSTANCE;
    private static Object lock = new Object();

    public static IAccessLogApi getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new AccessLogApi();
                }
            }
        }

        return INSTANCE;
    }

    private AccessLogApi() {
    }

    @Override
    public void sendAccessLog(TraceContext ctx) {
        LOGGER.info("send access log");
        try {
            sendAccessLogs(GsonUtil.toJsonArray(ctx));
        } catch (Throwable t) {
            LOGGER.error("[sendAccessLog parse exception occur]", t);
        }
    }

    @Override
    public void sendAccessLogs(List<TraceContext> ctxs) {
        LOGGER.info("send access logs size : " + (ctxs == null ? 0 : ctxs.size()));
        try {
            sendAccessLogs(GsonUtil.toJson(ctxs));
        } catch (Throwable t) {
            LOGGER.error("[sendAccessLogs parse exception occur]", t);
        }
    }

    @Override
    public void checkPolicy() {

    }

    /**
     * AccessLog 전송 처리 메소드 => 다중 서버로 전송하는 경우 1개의 실패와 별개로 다른 수집 서버로 보내야 되므로, for문 안에 try-catch 구문이 추가 됨.
     *
     * @param body 단일 or 다중 TraceContext의 JSON string
     *
     * @return 요청 결과 HttpEntity
     */
    private void sendAccessLogs(String body) {
        CloseableHttpClient client = null;
        Configurer conf = Configurer.getInstance();
        List<Collector> collectors = conf.collectors;
        try {
            if (CollectionUtil.isNotEmpty(collectors)) {
                client = HttpClients.createDefault();
                for (Collector collector : collectors) {
                    try {
                        String uri = getRequestUri(collector.getIp(), collector.getPort(), collector.getContext(), ACCESS_LOG_SEND_PATH);
                        HttpPost httpPost = getHttpPostWithJsonHeader(uri, body);
                        CloseableHttpResponse response = client.execute(httpPost);
                        // HttpEntity result = response.getEntity();
                        LOGGER.info("[Request rest client] uri" + uri + ", status : " + response.getStatusLine().getStatusCode());
                    } catch (Throwable t) {
                        LOGGER.error("failed to send access log ip : " + collector.getIp(), t);
                        /*  TEMP CODE :: 예외 처리 해야 됨*/
                        continue;
                    }
                }
                LOGGER.info("[Request rest client] success to send access logs collectors`s size : " + collectors.size());
            } else {
                LOGGER.info("[Request rest client] Collectors is empty");
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 요청 URI를 가져오는 메소드
     */
    private String getRequestUri(String host, int port, String context, String path) {
        return SimpleUriBuilder.buildHttp().host(host).port(port).context(context).path(path).create();
    }

    /**
     * Configurer에 있는 main collector의 URI를 얻는 메소드
     */
    private String getRequestUriFromMain(String path) {
        Configurer conf = Configurer.getInstance();
        return SimpleUriBuilder.buildHttp().host(conf.collector_host).port(conf.collector_port).context(conf.collector_context).path(path).toString();
    }

    /**
     * HttpPost + setJsonHeader 인스턴스를 가져오는 메소드
     * Accept, Content-Type을 application/json으로 담고 body + "UTF-8"을 body에 담는다
     *
     * @param uri  요청 할 URI
     * @param body 몸체에 담을 String body
     *
     * @return HttpPost 인스턴스
     */
    private HttpPost getHttpPostWithJsonHeader(String uri, String body) throws Exception {
        HttpPost post = getHttpPost(uri, body);

        setJsonHeader(post);

        return post;
    }

    /**
     * HttpPost 인스턴스를 가져오는 메소드
     *
     * @param uri  요청 할 URI
     * @param body 몸체에 담을 String body
     *
     * @return HttpPost 인스턴스
     */
    private HttpPost getHttpPost(String uri, String body) throws Exception {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(new StringEntity(body, "UTF-8"));
        return httpPost;
    }

    /**
     * Json Header를 추가하는 메소드 { "Accept" : "application/json" "Conetnt-Type" : "application/json" } 을 담는다
     */
    private void setJsonHeader(AbstractHttpMessage httpMessage) {
        if (httpMessage == null) {
            return;
        }

        httpMessage.setHeader("Accept", "application/json");
        httpMessage.setHeader("Content-type", "application/json;charset=UTF-8");
    }
}