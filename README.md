# Servlet 기반 웹 서버 + JDBC 접근 정보 추출하는 agent

> Getting started  

```
[app@localhost ~]$ git clone https://github.com/zacscoding/wasflow.git
[app@localhost ~]$ cd wasflow/wasflow-agent/

[app@localhost wasflow-agent]$ mvn clean package -Dmaven.test.skip=true
[app@localhost wasflow-agent]$ pwd
/home/app/wasflow/wasflow-agent
[app@localhost wasflow-agent]$ tree -L 2
.
├── README.md
├── pom.xml
├── src
│   └── main
└── target
    ├── antrun
    ├── classes
    ├── generated-sources
    ├── maven-archiver
    ├── maven-status
    ├── original-wasflow-agent.jar
    └── wasflow-agent.jar

8 directories, 4 files

[app@localhost wasflow-agent]$ cd ../testweb/
[app@localhost testweb]$ mvn clean package


[app@localhost testweb]$ vi wasflow-config.json
{
  "app": {
    "agent": "",
    // wasflow app trace 여부
    "debug-app": false,
    // 접속 IP (null일 경우, java 코드로 구함)
    // "source-ip": "192.168.100.152",
    // 로그 저장 경로
    "log-path": "/home/app/wasflow/logs",
    // 로그 최대 용량(MB)
    "log-max-file-size": 1,
    // 로그 최대 보관일 수(일)
    "log-max-history": 1,
    // 로그를 파일로 쓸지 여부
    "write-log-file": true,
    // 수집 할 URI (/context/path..)
    // 전체를 하려면 아무것도 입력X 또는 "/**"
    "collect-uri": [
      "/**"
    ],
    // 제외 할 URI ("/context/path")
    "ignore-uri": [
      "/favicon.ico",
      "/**/resources/**",
      "/**/resource/**",
      "/**/images/**",
      "/**/img/**",
      "/**/js/**",
      "/exclude/**"
    ],
    // 제외 할 응답 코드
    "ignore-response-status": [
      404,
      500
    ],
    // http request body를 수집할 지 여부
    "trace-http-request-body": true,
    // http response body를 수집할 지 여부
    "trace-http-response-body": false,
    // 로그인 관련 설정
    "login": {
      // 로그인 타입 :: principal : 1, session : 2, cookie : 3, db : 4
      "login-type": 1,
      // 세션 로그인을 이용할 경우, 세션 속성 이름
      "attribute-name": "user",
      // 세션 로그인을 이용할 경우, 아이디 값을 추출하기 위한 메소드
      "login-invoke-methods": [
        "getUserDetail",
        "getLoginId"
      ]
      // 특정 클래스의 메소드에서 로그인을 체크할 수 있는 경우 클래스이름, 메소드이름, 메소드 DESC를 적는다.
      // "login-invoker-class-name": "org/test/TestService",
      // "login-invoker-method-name": "getLoginUser",
      // "login-invoker-method-desc": "(I)Ljava/lang/String;"
    }
  },
  "jdbc": {
    // Http 요청이 존재하지 않고 데이터 베이스에 접속 한 경우
    "trace-with-no-http-request": true
  },
  // 수집기에 대한 정보 is-main이 true인 서버로 APP 정책 등을 가져온다.
  "collectors": [
    {
      "ip": "localhost",
      "port": 8080,
      "context": "/",
      "is-main": false
    }
  ]
}

[app@localhost testweb]$ pwd
/home/app/wasflow/testweb
[app@localhost testweb]$ tree -L 1
.
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src
├── target
└── wasflow-config.json


[app@localhost testweb]$ java -javaagent:/home/app/wasflow/wasflow-agent/target/wasflow-agent.jar -Dwasflow.config.path=/home/app/wasflow/testweb/wasflow-config.json -jar target/testweb-0.0.1-SNAPSHOT.jar 
```   

> Access log  

만약 오른쪽 URL을 접속하면, : http://127.0.0.1:8080/sample/index 
, agent에서 수집한 정보를 http://127.0.0.1:8080/exclude/logs/was 로 아래와 같은 데이터를 전송

```
2018-09-10 23:27:10.903  INFO 25689 --- [nio-8080-exec-2] o.wasflow.testweb.web.CollectController  : ## >> Receive access log. 
[{"agent":"","source-ip":"192.168.122.1","server-http-port":8080,"method":"GET","protocol":"HTTP/1.1","remote-host":"192.168.116.1","remote-user":null,"session-id":null,"login-id":"NULL","context-path":"","url":"http://192.168.116.128:8080/sample/index","uri":"/sample/index","response-status":200,"response-message":null,"access-time":1536589629936,"has-exception":false,"request-body":null,"started-async":false,"response-body":null,"repositories":[]}]
```  






---  

> How to work  

- jvm에서 class가 load 되는 시점에 class 변경

e.g) add 메소드 호출 시 파라미터 2개를 추출?

```
// 기존
public class TestClass {
  public int add(int a, int b) {
    return a + b;    
  }
}

// 변경 된 클래스 파일(실제는 바이트 코드!)
public class TestClass {
  public int add(int a, int b) {
    org.wasflow.agent.Collector.INSTACNE.addCalled(a, b);
    return a + b;    
  }
}
```  

---  

> How to collect  

- thread local의 변수에 정보들을 담음  

e.g)  
