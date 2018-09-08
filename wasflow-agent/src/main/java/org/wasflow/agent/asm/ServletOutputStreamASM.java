package org.wasflow.agent.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceServletOutputStream;
import org.wasflow.util.StringUtil;

/**
 * ServletOutputStream 관련 클래스 ASM (HTML 코드를 추출하기 위한 BCI)
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ServletOutputStreamASM implements IASM, Opcodes {

    public HashSet<String> target = new HashSet<String>();

    public ServletOutputStreamASM() {
        target.add("javax/servlet/ServletOutputStream");
        target.add("org/apache/catalina/connector/CoyoteWriter");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (target.contains(className)) {
            LOGGER.info("!@ find target class name " + className);
            return new ServletOutputStreamCV(cv);
        } else if (target.contains(classDesc.superName)) {
            LOGGER.info("!@ find target class name " + className + " super : " + classDesc.superName);
            return new ServletOutputStreamCV(cv);
        }

        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class ServletOutputStreamCV extends ClassVisitor implements Opcodes {

    public ServletOutputStreamCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (mv == null) {
            return mv;
        }

        if (ServletOutputStreamMV.isTargetMethod(name, desc)) {
            return new ServletOutputStreamMV(access, desc, mv, name);
        }

        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class ServletOutputStreamMV extends LocalVariablesSorter implements Opcodes {

    private static Map<String, String[]> targets = new HashMap<String, String[]>();
    private String TRACE = StringUtil.parseClassName(TraceServletOutputStream.class);
    private String methodName;
    private String desc;
    private Type[] params;

    static {
        String[] printDescs = new String[] {"(Ljava/lang/String;)V", "(Z)V", "(I)V", "(J)V", "(F)V", "(D)V", "()V"};
        String[] writeDesc = new String[] {"([BII)V", "(I)V", "([CII)V"};
        targets.put("print", printDescs);
        targets.put("println", printDescs);
        targets.put("write", writeDesc);
    }

    public static boolean isTargetMethod(String name, String desc) {
        String[] targetMethods = targets.get(name);
        if (targetMethods == null) {
            return false;
        }

        for (String targetMethod : targetMethods) {
            if (targetMethod.equals(desc)) {
                return true;
            }
        }

        return false;
    }

    protected ServletOutputStreamMV(int access, String desc, MethodVisitor mv, String methodName) {
        super(ASM5, access, desc, mv);

        Type[] params = Type.getArgumentTypes(desc);
        this.methodName = methodName;
        this.desc = desc;
        this.params = params;
    }

    @Override
    public void visitCode() {
        for (int i = 0; i < params.length; i++) {
            int idx = i + 1;
            switch (params[i].getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    mv.visitVarInsn(Opcodes.ILOAD, idx);
                    break;
                case Type.LONG:
                    mv.visitVarInsn(Opcodes.LLOAD, idx);
                    break;
                case Type.FLOAT:
                    mv.visitVarInsn(Opcodes.FLOAD, idx);
                    break;
                case Type.DOUBLE:
                    mv.visitVarInsn(Opcodes.DLOAD, idx);
                    break;
                case Type.OBJECT:
                case Type.ARRAY:
                    mv.visitVarInsn(Opcodes.ALOAD, idx);
                    break;
                default:
                    LOGGER.info("@ServletOutputStreamASM.visitCode : default is called getDescriptor : " + params[i].getDescriptor());
                    mv.visitVarInsn(Opcodes.ALOAD, idx);
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, methodName, desc, false);
        mv.visitCode();
    }
}