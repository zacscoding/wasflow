# DEV!!

- <a href="#asm"> ASM </a>  


---  


## ASM  

> 참고해야 할 사이트

- asm 사이트 : https://asm.ow2.io/  
- pinpoint : https://github.com/naver/pinpoint  
(https://github.com/naver/pinpoint/tree/master/plugins)
- scouter : https://github.com/scouter-project/scouter  
(https://github.com/scouter-project/scouter/tree/master/scouter.agent.java)  

### ASM 개발(intellij asm plugin)    

- 기존 코드  

```
public class ASMGuide {

    public int add(int a, int b) {
        return a + b;
    }    
}
```  

- 변경하려는 코드  

```
public int addModify(int a, int b) {
  guide.CollectGuide.addCalled(a, b);
  return a + b;
}
```

- 우측버튼 > ASM Bytecode Viewer의 ASMified 탭*  

```
...

        {
            mv = cw.visitMethod(ACC_PUBLIC, "add", "(II)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(11, l0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "Lguide/ASMGuide;", null, l0, l1, 0);
            mv.visitLocalVariable("a", "I", null, l0, l1, 1);
            mv.visitLocalVariable("b", "I", null, l0, l1, 2);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "addModify", "(II)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(15, l0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "guide/CollectGuide", "addCalled", "(II)V", false);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(16, l1);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Lguide/ASMGuide;", null, l0, l2, 0);
            mv.visitLocalVariable("a", "I", null, l0, l2, 1);
            mv.visitLocalVariable("b", "I", null, l0, l2, 2);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }

...
```  

위의 차이를 보면 아래가 추가 된 것 확인

```
mv.visitVarInsn(ILOAD, 1);
mv.visitVarInsn(ILOAD, 2);
mv.visitMethodInsn(INVOKESTATIC, "guide/CollectGuide", "addCalled", "(II)V", false);
```  
