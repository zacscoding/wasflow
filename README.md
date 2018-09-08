# Servlet 기반 웹 서버 + JDBC 접근 정보 추출하는 agent

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

> How to collect  

- thread local의 변수에 정보들을 담음  

e.g)  
