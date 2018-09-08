package org.wasflow.agent.asm;


import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceMain;
import org.wasflow.util.StringUtil;

/**
 * HttpService (Servlet ASM)
 * Servlet의 http 요청과 응답에 대한 정보를 수집하는 클래스
 *
 * - javax/servlet/http/HttpServlet 클래스
 * - weblogic/servlet/jsp/JspBase 클래스
 * - javax/servlet/Filter 구현 클래스
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class HttpServiceASM implements IASM {

    public HashSet<String> servlets = new HashSet<String>();

    public HttpServiceASM() {
        servlets.add("javax/servlet/http/HttpServlet");
        servlets.add("weblogic/servlet/jsp/JspBase");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        // check servlet class
        if (servlets.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new HttpServiceCV(cv, className);
        }

        // check filter interface
        if (classDesc.interfaces != null) {
            for (int i = 0; i < classDesc.interfaces.length; i++) {
                if ("javax/servlet/Filter".equals(classDesc.interfaces[i])) {
                    LOGGER.info("!@ find target class name : " + className + " interface : javax/servlet/Filter");
                    return new HttpServiceCV(cv, className);
                } else if ("javax/servlet/http/HttpServletResponse".equals(classDesc.interfaces[i])) {
                    LOGGER.info("!@ find target class name : " + className + " interface : javax/servlet/http/HttpServletResponse");
                    return new HttpServletResponseCV(cv, className);
                }
            }
        }

        return cv;
    }
}


/* ======================================== ClassVisitor ======================================== */
class HttpServiceCV extends ClassVisitor implements Opcodes {

    private static String TARGET_SERVICE = "service";
    private static String TARGET_DOFILTER = "doFilter";
    private static String TARGET_SIGNATURE = "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;";
    private String className;

    public HttpServiceCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if (desc.startsWith(TARGET_SIGNATURE)) {
            if (TARGET_SERVICE.equals(name)) {
                return new HttpServiceMV(access, desc, mv, true);
            } else if (TARGET_DOFILTER.equals(name)) {
                return new HttpServiceMV(access, desc, mv, false);
            }
        }

        return mv;
    }
}

// setStatus(int) ==> (I)V  setStatus(int,String) ==> (ILjava/lang/String;)V
class HttpServletResponseCV extends ClassVisitor implements Opcodes {

    private String TARGET_SET_STATUS = "setStatus";
    private String TARGET_SEND_ERROR = "sendError";
    private String TARGET_PREFIX_SIGNATURE = "(I";
    private String TARGET_SUFFIX_SIGNATURE = "java/lang/String)V";
    private String className;

    public HttpServletResponseCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if (desc.startsWith(TARGET_PREFIX_SIGNATURE)) {
            boolean setStatus = false;
            if (TARGET_SET_STATUS.equals(name)) {
                setStatus = true;
            } else if (TARGET_SEND_ERROR.equals(name)) {
                // empty
            } else {
                return mv;
            }
            boolean existStringParam = desc.endsWith(TARGET_SUFFIX_SIGNATURE);
            return new HttpServletResponseMV(access, desc, mv, setStatus, existStringParam);
        }

        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class HttpServiceMV extends LocalVariablesSorter implements Opcodes {

    private Label startFinally = new Label();
    private static final String TRACEMAIN = StringUtil.parseClassName(TraceMain.class);
    // start 메소드 이름
    private static final String START_SERVICE = "startHttpTrace";
    private static final String START_FILTER = "startHttpFilterTrace";
    // start 메소드 signature
    private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String END_METHOD = "endTrace";
    private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private boolean isServlet;
    // httpContext 지역변수
    private int httpContextIdx;

    protected HttpServiceMV(int access, String desc, MethodVisitor mv, boolean isServlet) {
        super(ASM5, access, desc, mv);
        this.isServlet = isServlet;
    }


    /**
     * TargetClass.targetMethod(Object arg1, Object arg2) {
     */
    @Override
    public void visitCode() {
        // 1,2번째 매개변수 => TraceMain :: START_SERVICE 실행
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        if (isServlet) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_SERVICE, START_SIGNATURE, false);
        }
        // filter impls
        else {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_FILTER, START_SIGNATURE, false);
        }
        // TraceMain 실행 한 결과(HttpContext 클래스)를 지역변수에 저장
        httpContextIdx = newLocal(Type.getType(Object.class));
        mv.visitVarInsn(Opcodes.ASTORE, httpContextIdx);
        // try 블럭 추가
        mv.visitLabel(startFinally);
        mv.visitCode();
    }

    // 정상 리턴하는 경우, END_METHOD(httpContext, null); 호출
    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitVarInsn(Opcodes.ALOAD, httpContextIdx);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
        }
        mv.visitInsn(opcode);
    }

    /*
    ...
    == 추가 된 부분 ==
    } catch {
     */
    // 예외가 발생하는 경우, END_METHOD(httpContext, Throwable); 호출
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label labelCatch = new Label();
        mv.visitTryCatchBlock(startFinally, labelCatch, labelCatch, null);
        mv.visitLabel(labelCatch);
        // copy top from stack
        mv.visitInsn(DUP);
        // Throwable copy = t;
        int errIdx = newLocal(Type.getType(Throwable.class));
        mv.visitVarInsn(Opcodes.ASTORE, errIdx);
        // invoke END_METHOD
        mv.visitVarInsn(Opcodes.ALOAD, httpContextIdx);
        mv.visitVarInsn(Opcodes.ALOAD, errIdx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
        // throw
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack, maxLocals + 2);
    }
}

/* ======================================== MethodVisitor ======================================== */
class HttpServletResponseMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
    private static final String START_SET_STATUS = "setWebResponseStatus";
    private static final String START_SEND_ERROR = "setWebResponseSendError";
    private static final String INT_SIGNATURE = "(I)V";
    private static final String STRING_SIGNATURE = "(Ljava/lang/String;)V";

    private boolean setStatus;
    private boolean existStringParam;

    protected HttpServletResponseMV(int access, String desc, MethodVisitor mv, boolean setStatus, boolean existStringParam) {
        super(ASM5, access, desc, mv);
        this.setStatus = setStatus;
        this.existStringParam = existStringParam;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ILOAD, 1);
        if (!existStringParam) {
            if (setStatus) {
                mv.visitMethodInsn(INVOKESTATIC, TRACEMAIN, START_SET_STATUS, INT_SIGNATURE, false);
            } else {
                mv.visitMethodInsn(INVOKESTATIC, TRACEMAIN, START_SEND_ERROR, INT_SIGNATURE, false);
            }
        } else {
            mv.visitVarInsn(ALOAD, 2);
            if (setStatus) {
                mv.visitMethodInsn(INVOKESTATIC, TRACEMAIN, START_SET_STATUS, STRING_SIGNATURE, false);
            } else {
                mv.visitMethodInsn(INVOKESTATIC, TRACEMAIN, START_SEND_ERROR, STRING_SIGNATURE, false);
            }
        }
        mv.visitCode();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack, maxLocals + (existStringParam ? 2 : 3));
    }
}