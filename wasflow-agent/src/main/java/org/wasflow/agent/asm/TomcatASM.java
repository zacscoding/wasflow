package org.wasflow.agent.asm;

import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceApp;
import org.wasflow.util.StringUtil;

/**
 * Tomcat을 통해 서버 정보를 추출하는 ASM 코드
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class TomcatASM implements IASM, Opcodes {

    public HashSet<String> targets = new HashSet<String>();

    public TomcatASM() {
        targets.add("org/apache/catalina/core/StandardServer");
        targets.add("org/apache/catalina/connector/Connector");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (targets.contains(className)) {
            LOGGER.info("!@ find target " + className);
            return new TomcatCV(cv, className);
        }

        return cv;
    }
}

class TomcatCV extends ClassVisitor implements Opcodes {

    private String className;

    public TomcatCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if (("startInternal".equals(name) || "start".equals(name)) && "()V".equals(desc)) {
            return new StartInternalMV(access, desc, mv, className, name);
        }

        return mv;
    }
}

class StartInternalMV extends LocalVariablesSorter implements Opcodes {

    private String TRACE = StringUtil.parseClassName(TraceApp.class);

    private String className;
    private String methodName;
    // 1 : StandardServer.startInternal() => server info 추출 ,  2 : Connector.startInternal() => port 추출
    private int action_type;

    protected StartInternalMV(int access, String desc, MethodVisitor mv, String className, String methodName) {
        super(ASM5, access, desc, mv);
        this.className = className;
        this.methodName = methodName;
        String realClassName = className.substring(className.lastIndexOf('/') + 1);
        if (realClassName.equals("StandardServer")) {
            this.action_type = 1;
        } else if (realClassName.equals("Connector")) {
            this.action_type = 2;
        }
    }

    @Override
    public void visitCode() {
        switch (this.action_type) {
            case 1:
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/StandardServer", "getServerInfo", "()Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKESTATIC, TRACE, "setServerInfo", "(Ljava/lang/String;)V", false);
                break;
            case 2:
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/connector/Connector", "getPort", "()I", false);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/connector/Connector", "getProtocol", "()Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKESTATIC, TRACE, "setServerPort", "(ILjava/lang/String;)V", false);
                break;
        }
        mv.visitCode();
    }
}