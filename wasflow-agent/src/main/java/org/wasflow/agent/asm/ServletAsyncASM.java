package org.wasflow.agent.asm;

import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceMain;
import org.wasflow.util.StringUtil;

/**
 * Servlet의 비동기 관련 정보를 수집하기 위한 ASM
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ServletAsyncASM implements IASM, Opcodes {

    public HashSet<String> startTarget = new HashSet<String>();
    public HashSet<String> dispatchTarget = new HashSet<String>();

    public ServletAsyncASM() {
        startTarget.add("javax/servlet/http/HttpServletRequest");
        dispatchTarget.add("javax/servlet/AsyncContext");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        String[] interfaces = classDesc.interfaces;

        if (interfaces != null && interfaces.length > 0) {
            for (String interfaceName : interfaces) {
                if (startTarget.contains(interfaceName)) {
                    LOGGER.info("!@ find target class name : " + className + ", interface name : " + interfaceName);
                    return new StartAsyncCV(cv);
                }

                if (dispatchTarget.contains(interfaceName)) {
                    LOGGER.info("!@ find target class name : " + className + ", interface name : " + interfaceName);
                    return new DispatchAsyncCV(cv);
                }
            }
        }

        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */

/**
 * startAsync()를 위한 ClassVisitor
 */
class StartAsyncCV extends ClassVisitor implements Opcodes {

    public StartAsyncCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("startAsync".equals(name) && "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Ljavax/servlet/AsyncContext;".equals(desc)) {
            return new ServletAsyncStartASM(access, desc, mv);
        }

        return mv;
    }
}

/**
 * dispatch()를 위한 ClassVisitor
 */
class DispatchAsyncCV extends ClassVisitor implements Opcodes {

    public DispatchAsyncCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("dispatch".equals(name) && "(Ljavax/servlet/ServletContext;Ljava/lang/String;)V".equals(desc)) {
            System.out.println("!@ find target method : " + name); /*   TEMP CODE   */
            return new ServletAsyncDispatchMV(access, desc, mv);
        }

        return mv;
    }
}



/* ======================================== MethodVisitor ======================================== */

class ServletAsyncStartASM extends LocalVariablesSorter implements Opcodes {

    private String TRACEMAIN = StringUtil.parseClassName(TraceMain.class);

    protected ServletAsyncStartASM(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "startAsync", "(Ljava/lang/Object;)V", false);
        }
        mv.visitInsn(opcode);
    }
}

class ServletAsyncDispatchMV extends LocalVariablesSorter implements Opcodes {

    private String TRACEMAIN = StringUtil.parseClassName(TraceMain.class);

    protected ServletAsyncDispatchMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "dispatchAsync", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
        }
        mv.visitInsn(opcode);
    }
}