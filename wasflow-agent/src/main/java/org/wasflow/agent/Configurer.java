package org.wasflow.agent;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.wasflow.agent.dto.Collector;
import org.wasflow.agent.dto.ReadConfigurer;
import org.wasflow.agent.trace.filter.AntPathFilter;
import org.wasflow.util.CollectionUtil;
import org.wasflow.util.StringUtil;
import org.wasflow.util.ThreadUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class Configurer extends Thread {

    // 설정 파일 읽을 GSON
    private static final Gson READ_CONFIG_GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    // 싱글톤
    private static Configurer INSTANCE;
    // Config 파일 관리 데몬
    private static Thread daemon;
    // 커스텀 로그인 정보를 담은 맵
    private Map<String, String> customLoginMap;

    // config 파일
    private File configFile;
    private long last_check;
    private long last_modified;

    // ======================================================
    // APP
    // ======================================================

    // 로그 path
    public String log_path;
    // wasflow app log 최대 사이즈(MB)
    public int log_max_file_size;
    // wasflow app log 보관일 수
    public int log_max_history;

    // 프로그램 running 여부 (에러가 존재하면 running을 하지 않는다)
    private boolean running = true;
    // 후킹 여부
    public boolean trace = true;
    // 적용 에이전트 이름
    public String agent;
    // 서버 정보(was로 부터 추출되는 정보)
    public String server_info;
    // 서버 포트
    public int server_http_port;
    // ip
    public String source_ip;
    // 접속 로그를 파일로 쓸지 여부(임시 테스트용으로 사용)
    public boolean write_log_file;

    // ======================================================
    // HTTP TRACE
    // ======================================================
    public List<String> trace_uri;
    public boolean trace_all;
    public List<String> ignore_uri;
    public Set<Integer> ignore_response_status;
    // 로그인 타입 :: principal : 1, session : 2, cookie : 3, db : 4
    public int login_type;
    // 세션, 쿠키 로그인 사용 시, 세션 속성 or 쿠키 이름
    public String login_attribute_name;
    // 세션 로그인 사용 시, 세션에서 꺼낸 Object의 invoke 메소드 이름들
    public List<String> login_invoke_methods;
    // 특정 클래스:메소드에서 로그인 정보를 추출할 수 있으면 사용
    public boolean exist_custom_login = false;
    public String login_invoker_class_name;
    public String login_invoker_method_name;
    public String login_invoker_method_desc;

    // 세션 로그인 사용 시, 로그인 리플렉션에 문제가 존재하는 지 여부
    public boolean has_login_invoker_exception = false;
    // http 통신 request body trace 여부
    public boolean trace_http_request_body = false;
    // http 통신 response body trace 여부
    public boolean trace_http_response_body = true;
    // JDBC 접근 + HttpRequest가 존재하지 않는 경우 추적할 지 여부
    public boolean jdbc_trace_with_no_http_request = true;


    // ======================================================
    // Collector (로그 수집기)
    // ======================================================
    public List<Collector> collectors;

    // 메인 수집기 Rest api host(ip)
    public String collector_host;
    // 메인 수집기 Rest api port
    public int collector_port;
    // 메인 수집기 Rest api context
    public String collector_context;

    private Configurer() {
        // init config
        boolean result = init(false);
        if (result) {
            // custom 로그인이 존재하는 경우 cache 생성
            if (this.exist_custom_login) {
                if (customLoginMap == null) {
                    customLoginMap = new ConcurrentHashMap<String, String>();
                }
                customLoginMap.clear();
            }
            // init staticsConfig
            initStatics();
        }
    }

    public static Object lock = new Object();

    public static Configurer getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new Configurer();
                    if (daemon == null) {
                        daemon = INSTANCE;
                        daemon.setDaemon(true);
                        daemon.setName("[WASFLOW-ConfigManager]");
                        daemon.start();
                    }
                }
            }
        }

        return INSTANCE;
    }

    public synchronized void changeTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * 커스텀 로그인이 존재하는 경우 map에 담는다
     */
    public void setCustomLoginId(String name, String loginId) {
        if (this.customLoginMap == null) {
            customLoginMap = new ConcurrentHashMap<String, String>();
        }

        customLoginMap.put(name, loginId);
    }

    public String getCustomLoginId(String name) {
        return customLoginMap.get(name);
    }

    private synchronized boolean init(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now < last_check + 3000) {
            return false;
        }
        last_check = now;

        File configFile = getConfigFile();
        if (configFile == null) {
            return false;
        }

        try {
            ReadConfigurer readConfig = READ_CONFIG_GSON.fromJson(new FileReader(configFile), ReadConfigurer.class);
            if (readConfig == null) {
                throw new Exception();
            }
            parseConfigProperties(readConfig);
        } catch (Throwable t) {
            LOGGER.error("Failed to read config file. Please modify config file path : " + configFile.getAbsolutePath(), t);
            changeTrace(false);
            return false;
        }

        clearCaches();
        return true;
    }

    /**
     * 설정값에 관계없이 APP static 관련 초기화 메소드
     */
    private void initStatics() {
        if (StringUtil.isNotEmpty(source_ip)) {
            return;
        }

        try {
            boolean isLoopBack = true;
            Enumeration<NetworkInterface> networkEnum = NetworkInterface.getNetworkInterfaces();
            while (networkEnum.hasMoreElements()) {
                NetworkInterface ni = networkEnum.nextElement();
                if (ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ia = inetAddresses.nextElement();
                    if (ia.getHostAddress() != null && ia.getHostAddress().indexOf(".") != -1) {
                        source_ip = ia.getHostAddress();
                        isLoopBack = false;
                        break;
                    }
                }

                if (!isLoopBack) {
                    break;
                }
            }
        } catch (SocketException e) {
            // 임시 무시 getLocalHost가 언제 예외 발생하는지 아직 모름
            // e.printStackTrace();
            LOGGER.error("failed to extract ip", e);
            if (source_ip == null) {
                source_ip = "unknown";
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            File file = getConfigFile();
            if (file != null && last_modified != file.lastModified()) {
                LOGGER.info("Config file is modified");
                if (init(false)) {
                    last_modified = file.lastModified();
                }
            }
            // ThreadUtil.sleep(60000);
            ThreadUtil.sleep(3000L);
        }
    }

    private File getConfigFile() {
        if (configFile != null) {
            return configFile;
        }

        String path = System.getProperty("wasflow.config.path");
        if (path == null) {
            running = false;
            LOGGER.error("@@ There is no config property in vm args. Please regist config path property like \"-Dwasflow.config.path=/user/local/wasflow/config.json\"");
            changeTrace(false);
            return null;
        }

        File configFile = new File(path);
        if (!configFile.exists()) {
            LOGGER.error("@@ There is no config file {}. Please add config file path : " + path);
            changeTrace(false);
            return null;
        }

        this.configFile = configFile;
        return configFile;
    }

    /**
     * 캐싱을 사용하면, sync를 맞춰주는 메소드
     */
    private void clearCaches() {
        // URI 필터 캐싱 초기화
        AntPathFilter.sync(this.trace_all, this.ignore_uri, this.trace_uri);
        // LOGGER 관련 초기화
        LOGGER.sync();
    }

    private void parseConfigProperties(ReadConfigurer config) {
        ReadConfigurer.APP app = config.getApp();
        ReadConfigurer.JDBC jdbc = config.getJdbc();
        this.collectors = config.getCollectors();

        if (app != null) {
            // trace 여부
            LOGGER.info("change trace " + LOGGER.isTrace());
            LOGGER.setTrace(app.isDebugApp());

            // server agent
            this.agent = app.getAgent();

            // ip
            String configIp = app.getSourceIp();
            if (StringUtil.isNotEmpty(configIp)) {
                this.source_ip = configIp;
            }

            // wasflow logs
            this.log_path = app.getLogPath();
            this.log_max_file_size = app.getLogMaxFileSize();
            this.log_max_history = app.getLogMaxHistory();

            // 수집,제외 URI
            List<String> collect = app.getTraceUri();
            if (CollectionUtil.removeNullValueAndGetExist(collect)) {
                this.trace_uri = collect;
            } else {
                this.trace_uri = Collections.emptyList();
                this.trace_all = true;
            }
            collect = app.getIgnoreUri();
            CollectionUtil.removeNullValue(collect);
            this.ignore_uri = (collect == null || collect.size() == 0) ? Collections.<String>emptyList() : collect;

            // 수집 제외 응답코드
            this.ignore_response_status = app.getIgnoreResponseStatus();
            this.trace_http_response_body = app.isTraceHttpResponseBody();

            // HTTP request body를 수집할 지 여부
            this.trace_http_request_body = app.isTraceHttpRequestBody();

            // 로그를 파일로 쓸지 여부
            this.write_log_file = app.isWriteLogFile();

            // 로그인 정보
            ReadConfigurer.APP.Login login = app.getLogin();
            if (login != null) {
                // 로그인 타입
                this.login_type = login.getLoginType();
                this.login_attribute_name = login.getAttributeName();
                this.login_invoke_methods = login.getLoginInvokeMethods();
                this.login_invoker_class_name = login.getLoginInvokerClassName();
                this.login_invoker_method_name = login.getLoginInvokerMethodName();
                this.login_invoker_method_desc = login.getLoginInvokerMethodDesc();
                this.exist_custom_login =
                    (StringUtil.isNotEmpty(this.login_invoker_class_name)) && (StringUtil.isNotEmpty(this.login_invoker_method_name)) && (StringUtil.isNotEmpty(this.login_invoker_method_desc));
            }
        }

        if (jdbc != null) {
            this.jdbc_trace_with_no_http_request = jdbc.isTraceWithNoHttpRequest();
        }

        if (CollectionUtil.isNotEmpty(collectors)) {
            for (Collector collector : collectors) {
                if (collector.isMain()) {
                    this.collector_host = collector.getIp();
                    this.collector_port = collector.getPort();
                    this.collector_context = collector.getContext();
                    break;
                }
            }
        } else {
            this.collectors = Collections.emptyList();
        }
    }
}