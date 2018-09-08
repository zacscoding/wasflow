package org.wasflow.agent.asm;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceMain;

/**
 * JNDI dataSource 관련 lookup 후킹
 * => JNDI에 대한 이해 후 적용 필요
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class InitialContextASM implements IASM, Opcodes {

    public Set<String> target = new HashSet<String>();

    public InitialContextASM() {
        target.add("javax/naming/InitialContext");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (target.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new InitialContextCV(cv, className);
        }
        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class InitialContextCV extends ClassVisitor implements Opcodes {

    public String className;

    public InitialContextCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }
        if ("lookup".equals(name) && "(Ljava/lang/String;)Ljava/lang/Object;".equals(desc)) {
            return new InitialContextMV(access, desc, mv, className, name, desc);
        }
        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class InitialContextMV extends LocalVariablesSorter implements Opcodes {

    private static final String CLASS = TraceMain.class.getName().replace('.', '/');
    private static final String METHOD = "ctxLookup";
    private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";

    private Type returnType;

    public InitialContextMV(int access, String desc, MethodVisitor mv, String classname, String methodname, String methoddesc) {
        super(ASM5, access, desc, mv);
        this.returnType = Type.getReturnType(desc);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            int i = newLocal(returnType);

            mv.visitVarInsn(Opcodes.ASTORE, i);
            mv.visitVarInsn(Opcodes.ALOAD, i);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, i);

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE, false);
        }
        mv.visitInsn(opcode);
    }
}
