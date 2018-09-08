```aidl
-javaagent:/usr/local/wasflow/wasflow-agent.jar -Dwasflow.config.path=/usr/local/wasflow/wasflow-config.json
```

1. VM 옵션에 Java agent 옵션을 추가

```aidl
-javaagent:path/wasflow-agent.jar
e.g : -javaagent:C:\Users\go-in\git\wasflow\target\wasflow-agent.jar
```

2. VM argument에 wasflow 설정 파일 추가
```aidl
-Dwasflow.config.path=???
``` 

3. wasflow 동작여부 테스트 
```
-Dwasflow.trace=true
```

4. Tomcat
__
1)bin/catalina.sh  

```aidl
export JAVA_OPTS="$JAVA_OPTS -javaagent:/usr/local/wasflow/wasflow-agent.jar -Dwasflow.config.path=/usr/local/wasflow/wasflow-config.json"
```

2)setenv.sh  

```aidl
JAVA_OPTS=".. -javaagent:/usr/local/wasflow/wasflow-agent.jar -Dwasflow.config.path=/usr/local/wasflow/wasflow-config.json"
```

5. config  

```$xslt
{
  "app": {
    "agent": "",
    // wasflow app trace 여부
    "debug-app": true,
    // 접속 IP (null일 경우, java 코드로 구함)
    "source-ip": "192.168.100.152",
    // 로그 저장 경로
    "log-path": "D:\\wasflow\\logs\\",
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
      "/**/js/**"
    ],
    // 제외 할 응답 코드
    "ignore-response-status": [
      404,
      500
    ],
    // http request body를 수집할 지 여부
    "trace-http-request-body": true,
    // http response body를 수집할 지 여부
    "trace-http-response-body": true,
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
      ],
      // 특정 클래스의 메소드에서 로그인을 체크할 수 있는 경우 클래스이름, 메소드이름, 메소드 DESC를 적는다.
      "login-invoker-class-name": "org/test/TestService",
      "login-invoker-method-name": "getLoginUser",
      "login-invoker-method-desc": "(I)Ljava/lang/String;"
    }
  },
  "jdbc": {
    // Http 요청이 존재하지 않고 데이터 베이스에 접속 한 경우
    "trace-with-no-http-request": true
  },
  // 수집기에 대한 정보 is-main이 true인 서버로 APP 정책 등을 가져온다.
  "collectors": [
    {
      "ip": "192.168.100.114",
      "port": 7005,
      "context": "",
      "is-main": true
    },
    {
      "ip": "192.168.100.152",
      "port": 8989,
      "context": "/collector",
      "is-main": false
    }
  ]
}
```
